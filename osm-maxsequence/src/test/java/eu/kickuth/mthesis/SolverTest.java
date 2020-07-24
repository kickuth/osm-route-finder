package eu.kickuth.mthesis;

import crosby.binary.osmosis.OsmosisReader;
import eu.kickuth.mthesis.graph.Graph;
import eu.kickuth.mthesis.graph.Node;
import eu.kickuth.mthesis.solvers.GASolver;
import eu.kickuth.mthesis.solvers.SPESolver;
import eu.kickuth.mthesis.solvers.SmartSPESolver;
import eu.kickuth.mthesis.solvers.Solver;
import eu.kickuth.mthesis.utils.OSMReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Random;

class SolverTest {


    private static final Logger logger = LogManager.getLogger(SolverTest.class);


    private static final File logFolder = new File("out/logs/");

    @BeforeAll
    static void init() {
        logger.info("Initializing solver tests");
        // ensure log folder exists
        if (!logFolder.exists()){
            File logPath = new File(logFolder.getParent());
            if (!logPath.mkdirs()) {
                logger.fatal("Failed to create log directory!");
            }
        }
    }


    @Test
    void augmentedData() {
        try (var ignored = pipeConsoleToFile("augmented.txt")) {
            test(loadGraph("src/main/resources/osm_data/de_proc_real.osm.pbf"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    void gaussianData() {
        String logFile = "gaussian.txt";
        try (var ignored = pipeConsoleToFile(logFile)) {
            test(loadGraph("src/main/resources/osm_data/250 - 100.000/de_proc_gaussian.osm.pbf"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    void expData() {
        String logFile = "exp.txt";
        try (var ignored = pipeConsoleToFile(logFile)) {
            test(loadGraph("src/main/resources/osm_data/250 - 100.000/de_proc_exp-high.osm.pbf"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    void linearData() {
        String logFile = "linear.txt";
        try (var ignored = pipeConsoleToFile(logFile)) {
            Graph g = loadGraph("src/main/resources/osm_data/250 - 100.000/de_proc_linear.osm.pbf");
            test(g);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private Graph loadGraph(String importFile) {
        logger.info("Loading graph from file: {}", importFile);
        OSMReader graphReader = new OSMReader();
        try {
            OsmosisReader reader = new OsmosisReader(new FileInputStream(importFile));
            reader.setSink(graphReader);
            reader.run();
            return graphReader.getOsmGraph();
        } catch (FileNotFoundException e) {
            logger.error("Graph file not found: {}", importFile);
        }
        return new Graph(new double[4]);
    }


    private void test(Graph g) throws IOException {

        // ==================== SETTINGS ====================
        // ==================================================
        int testCount = 1000;
        double maxDistFactor = 1.25;

        Solver[] solvers = {
                new SPESolver(g),
                new GASolver(g),
                new SmartSPESolver(g)
        };
        // ==================================================
        // ==================================================

        int nodeCount = g.nodes.size();


        double[] totalDurations = new double[solvers.length];
        int totalUB = 0;
        int[] totalLBs = new int[solvers.length];

        int successfullTests = 0;
        Random r = new Random();
        while (successfullTests < testCount) {
            Node source = g.getNode(r.nextInt(nodeCount));
            Node target;
            do {  // select a target that is not the source
                target = g.getNode(r.nextInt(nodeCount));
            } while (source.equals(target));

            double[] durations = new double[solvers.length];
            int[] LBs = new int[solvers.length];

            try {
                // solve for all solvers
                for (int i = 0; i < solvers.length; i++) {
                    solvers[i].update(source, target, maxDistFactor);
                    double maxDist = solvers[i].getMaxDistance();
                    long startTime = System.currentTimeMillis();
                    Graph.Path solution = solvers[i].solve();
                    double endTime = (System.currentTimeMillis() - startTime) / 1000.0;
                    if (solution.isEmpty()) {
                        System.out.println("no s-t-path exists!");
                        System.err.println("no s-t-path exists!");
                        throw new IllegalArgumentException();
                    }
                    durations[i] = endTime;
                    LBs[i] = solvers[i].uniqueClassScore(solution);
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Solver bug. Trying new setting.");
                System.err.println("Solver bug. Trying new setting.");
                continue;
            }
            successfullTests++;

            double maxDistance = solvers[0].getMaxDistance();
            double shortestPathLength = maxDistance / maxDistFactor;
            int UB = solvers[0].getUpperBound();

            totalUB += UB;

            String current = String.format("run %d: s: %s; t: %s; min st: %.3f; UB: %d", successfullTests, source, target, shortestPathLength, UB);
            System.out.println(current);
            logger.info(current);

            for (int i = 0; i < solvers.length; i++) {
                System.out.println(String.format("%s: Score: %d; duration: %.2f", solvers[i].toString(), LBs[i], durations[i]));
                totalDurations[i] += durations[i];
                totalLBs[i] += LBs[i];
            }
        }

        String overall = String.format("Finished %d runs. (Avg UB: %.3f):", testCount, totalUB/(double)testCount);
        logger.info(overall);
        System.out.println(overall);
        for (int i = 0; i < solvers.length; i++) {
            overall = String.format("%s: Avg runtime: %.3f; avg LB: %.3f", solvers[i].toString(), totalDurations[i]/testCount, totalLBs[i]/(double)testCount);
            logger.info(overall);
            System.out.println(overall);
        }
    }


    private AutoCloseable pipeConsoleToFile(String fileName) throws FileNotFoundException {
        PrintStream originalOut = System.out;
        PrintStream fileOut = new PrintStream(logFolder.getPath() + "/" + fileName);
        System.setOut(fileOut);

        return () -> {
            fileOut.close();
            System.setOut(originalOut);
        };
    }
}
