package org.odk.collect.android.map;

import org.odk.collect.android.R;
import org.odk.collect.android.map.WmsBaseLayerType.WmsOption;
import org.odk.collect.android.preferences.GeneralKeys;
import org.odk.collect.android.spatial.TileSourceFactory;

/** A static class that defines the set of available base layer types. */
public class BaseLayerTypeRegistry {
    private BaseLayerTypeRegistry() { }  // prevent instantiation of this utility class

    private static BaseLayerType[] BASE_LAYER_TYPES = initBaseLayerTypes();

    /** This array determines the order in which they appear in the menu. */
    private static BaseLayerType[] initBaseLayerTypes() {
        TileSourceFactory factory = new TileSourceFactory();
        return new BaseLayerType[] {
            new GoogleBaseLayerType(),
            new MapboxBaseLayerType(),
            new WmsBaseLayerType("osm", R.string.base_layer_type_osm,
                TileSourceFactory.MAPNIK
            ),
            new WmsBaseLayerType("usgs", R.string.base_layer_type_usgs,
                GeneralKeys.KEY_USGS_MAP_STYLE, R.string.usgs_map_style,
                new WmsOption(R.string.usgs_map_style_topo, "topo", factory.getUSGSTopo()),
                new WmsOption(R.string.usgs_map_style_hybrid, "hybrid", factory.getUsgsSat()),
                new WmsOption(R.string.usgs_map_style_imagery, "imagery", factory.getUsgsImg())
            ),
            new WmsBaseLayerType("stamen", R.string.base_layer_type_stamen,
                factory.getStamenTerrain()
            ),
            new WmsBaseLayerType("carto", R.string.base_layer_type_usgs,
                GeneralKeys.KEY_CARTO_MAP_STYLE, R.string.carto_map_style,
                new WmsOption(R.string.carto_map_style_positron, "positron", factory.getCartoDbPositron()),
                new WmsOption(R.string.carto_map_style_dark_matter, "dark_matter", factory.getCartoDbDarkMatter())
            )
        };
    }

    /**
     * Gets the base layer type with the given ID.  If no match for the ID is
     * found, this defaults to returning the first item defined in the array.
     */
    public static BaseLayerType get(String bltId) {
        for (BaseLayerType blt : BASE_LAYER_TYPES) {
            if (blt.getId().equals(bltId)) {
                return blt;
            }
        }
        return BASE_LAYER_TYPES[0];
    }

    public static String[] getIds() {
        String[] bltIds = new String[BASE_LAYER_TYPES.length];
        for (int i = 0; i < bltIds.length; i++) {
            bltIds[i] = BASE_LAYER_TYPES[i].getId();
        }
        return bltIds;
    }

    public static int[] getNameResourceIds() {
        int[] ids = new int[BASE_LAYER_TYPES.length];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = BASE_LAYER_TYPES[i].getNameResourceId();
        }
        return ids;
    }
}
