package org.odk.collect.android.map;

public class DummyTileSource implements TileHttpServer.TileSource {
    public byte[] getTile(int zoom, int x, int y) {
        return "Hello!\n".getBytes();
    }
}
