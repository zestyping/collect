package org.odk.collect.android.map;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.mapbox.mapboxsdk.style.sources.RasterSource;
import com.mapbox.mapboxsdk.style.sources.Source;
import com.mapbox.mapboxsdk.style.sources.TileSet;
import com.mapbox.mapboxsdk.style.sources.VectorSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

public class MbtilesFile implements Closeable, TileHttpServer.TileSource {
    public enum Type { RASTER, VECTOR }

    final SQLiteDatabase db;
    final String format;
    final Type type;
    String contentType = "application/octet-stream";
    String contentEncoding = "identity";

    public MbtilesFile(File file) throws SQLiteException, UnsupportedFormatException {
        db = SQLiteDatabase.openOrCreateDatabase(file, null);
        format = getMetadata("format").toLowerCase();
        if (format.equals("pbf") || format.equals("mvt")) {
            contentType = "application/protobuf";
            contentEncoding = "gzip";
            type = Type.VECTOR;
        } else if (format.equals("jpg") || format.equals("jpeg")) {
            contentType = "image/jpeg";
            type = Type.RASTER;
        } else if (format.equals("png")) {
            contentType = "image/png";
            type = Type.RASTER;
        } else {
            throw new UnsupportedFormatException(format);
        }
    }

    public Type getType() {
        return type;
    }

    public void close() {
        db.close();
    }

    public Source getSource(String name, String urlTemplate) {
        TileSet tileSet = createTileSet(this, urlTemplate);
        if (type == Type.VECTOR) {
            return new VectorSource(name, tileSet);
        } else if (type == Type.RASTER) {
            return new RasterSource(name, tileSet);
        }
        return null;
    }

    public @NonNull String getMetadata(String key) {
        try (Cursor results = db.query("metadata", new String[] {"value"},
            "name = ?", new String[] {key}, null, null, null, null)) {
            return results.moveToFirst() ? results.getString(0) : "";
        }
    }

    public TileHttpServer.Response getTile(int zoom, int x, int y) {
        // TMS coordinates are used in .mbtiles files, so Y needs to be flipped.
        byte[] data = getTileBlob(zoom, x, (1 << zoom) - 1 - y);
        return data == null ? null :
            new TileHttpServer.Response(data, contentType, contentEncoding);
    }

    public byte[] getTileBlob(int zoom, int column, int row) {
        String selection = String.format(
            "zoom_level = %d and tile_column = %d and tile_row = %d",
            zoom, column, row
        );
        try (Cursor results = db.query("tiles", new String[] {"tile_data"},
            selection, null, null, null, null)) {
            return results.moveToFirst() ? results.getBlob(0) : null;
        }
    }

    protected static TileSet createTileSet(MbtilesFile mbtiles, String urlTemplate) {
        TileSet tileSet = new TileSet("2.2.0", urlTemplate);

        // Configure the TileSet using the metadata in the .mbtiles file.
        tileSet.setName(mbtiles.getMetadata("name"));
        try {
            tileSet.setMinZoom(Integer.parseInt(mbtiles.getMetadata("minzoom")));
            tileSet.setMaxZoom(Integer.parseInt(mbtiles.getMetadata("maxzoom")));
        } catch (NumberFormatException e) { /* ignore */ }

        String[] parts = mbtiles.getMetadata("center").split(",");
        if (parts.length == 3) {  // latitude, longitude, zoom
            try {
                tileSet.setCenter(
                    Float.parseFloat(parts[0]), Float.parseFloat(parts[1]),
                    (float) Integer.parseInt(parts[2])
                );
            } catch (NumberFormatException e) { /* ignore */ }
        }

        parts = mbtiles.getMetadata("bounds").split(",");
        if (parts.length == 4) {  // left, bottom, right, top
            try {
                tileSet.setBounds(
                    Float.parseFloat(parts[0]), Float.parseFloat(parts[1]),
                    Float.parseFloat(parts[2]), Float.parseFloat(parts[3])
                );
            } catch (NumberFormatException e) { /* ignore */ }
        }

        return tileSet;
    }

    /** Returns information about the vector layers available in the tiles. */
    public List<VectorLayer> getVectorLayers() {
        List<VectorLayer> layers = new ArrayList<>();
        JSONArray jsonLayers;
        try {
            JSONObject json = new JSONObject(getMetadata("json"));
            jsonLayers = json.getJSONArray("vector_layers");
            for (int i = 0; i < jsonLayers.length(); i++) {
                layers.add(new VectorLayer(jsonLayers.getJSONObject(i)));
            }
        } catch (JSONException e) { /* ignore */ }
        return layers;
    }

    public static class VectorLayer {
        public final String name;
        public final String description;

        public VectorLayer(JSONObject json) {
            name = json.optString("id", "");
            description = json.optString("description", "");
        }
    }

    public class UnsupportedFormatException extends IOException {
        public UnsupportedFormatException(String format) {
            super(String.format("Unrecognized .mbtiles format \"%s\"", format));
        }
    }
}
