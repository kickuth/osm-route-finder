package eu.kickuth.mthesis.utils;

public final class Settings {

    public static final String OSM_DUMP = "src/main/resources/osm_data/bw.osm.pbf";  // OSM data file
    public static final int  PORT = 4567;  // Webserver port

    public static final double POI_GRID_FIDELITY = 0.1;  // size (lat/lon) of a cell in POI grid


    // don't allow instantiation
    private Settings() {
        throw new UnsupportedOperationException();
    }
}
