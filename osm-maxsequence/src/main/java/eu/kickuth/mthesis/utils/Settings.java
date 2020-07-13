package eu.kickuth.mthesis.utils;

import java.io.File;

public final class Settings {

    public static final int  PORT = 4567;  // Webserver port

    public static final double POI_GRID_FIDELITY = 0.1;  // size (lat/lon) of a cell in POI grid



    // FILE NAMES
    //    public static final File OSM_DUMP = new File("src/main/resources/osm_data/tue.osm.pbf");  // TUE OSM data file
//    public static final File OSM_DUMP = new File("src/main/resources/osm_data/bw.osm.pbf");  // BW OSM data file
//    public static final File OSM_DUMP = new File("src/main/resources/osm_data/de.osm.pbf");  // DE data file
    public static final File OSM_DUMP = new File("src/main/resources/osm_data/de_proc.osm.pbf");  // DE (preprocessed) data file

    public static final File OSM_DUMP_PROCESSED = new File(OSM_DUMP.getPath().replace(".osm.pbf", "_processed.osm.pbf"));  // preprocessed OSM data file


    // PREPROCESSING
    public static final boolean FORCE_PREPROCESS = true;  // preprocess the .pbf file, even if preprocessed version exists?

    // FAKE CLASSES
    // use (augmented) traffic signs from file, or generate completely fake ones?
    static final boolean GENERATE_FAKE_SIGNS = true;
    // do nodes in dense regions get higher chances for POIs, or does each node have the same chance?
    static final boolean CLUSTERED_DISTRIBUTION = true;
    // from how many classes do we sample?
    static final int POSSIBLE_DISTINCT_CLASSES = 200;
    // expected value for the total number of POIs
    static final int EXPECTED_TOTAL_POIS = 30_000;
    /*
     size (lat/lon) of a cell for node distribution.
     Given Bound(top=55.14777, bottom=47.26543, left=5.864417, right=15.050780000000001) [Germany]:
     --> (7.882335 / fidelity) * (9.186363 / fidelity) = num cells
     0.0269091 results in about 100_000 cells.
     */
    static final double NODE_DISTRIBUTION_GRID_FIDELITY = 0.0850941;



    // don't allow instantiation
    private Settings() {
        throw new UnsupportedOperationException();
    }
}
