package eu.kickuth.mthesis.utils;

import java.io.File;

public final class Settings {

//    public static final String OSM_DUMP = "src/main/resources/osm_data/tue.osm.pbf";  // TUE OSM data file
    public static final String OSM_DUMP = "src/main/resources/osm_data/bw.osm.pbf";  // BW OSM data file

    public static final File OSM_DUMP_PROCESSED = new File(OSM_DUMP + "_processed.osm.pbf");  // preprocessed OSM data file
    public static final boolean FORCE_PREPROCESS = false;  // preprocess the .pbf file, even if preprocessed version exists?
    public static final int  PORT = 4567;  // Webserver port

    public static final double POI_GRID_FIDELITY = 0.1;  // size (lat/lon) of a cell in POI grid


    // don't allow instantiation
    private Settings() {
        throw new UnsupportedOperationException();
    }
}
