package eu.kickuth.mthesis;

import eu.kickuth.mthesis.graph.Graph;
import eu.kickuth.mthesis.graph.Node;
import eu.kickuth.mthesis.solvers.GreedySolver;
import eu.kickuth.mthesis.solvers.NaiveSolver;
import eu.kickuth.mthesis.solvers.SPSolver;
import eu.kickuth.mthesis.solvers.Solver;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

class IntegrationTest {


    private static final Logger logger = LogManager.getLogger(IntegrationTest.class);

//    @BeforeAll
//    static void start() throws InterruptedException, IOException {
//        logger.info("Running integration test.");
//        Main.main();
//        Thread.sleep(5_000);  // Give the Webserver time to start
//    }
//
//    @Test
//    void pingSite() throws IOException {
//        Socket socket = new Socket();
//        socket.connect(new InetSocketAddress("127.0.0.1", 4567), 10_000);
//    }

    @Test
    void randomQueries() throws IOException {
        int testCount = 100;
        BufferedWriter writer = new BufferedWriter(
                new FileWriter("out/logs/sameRandomQueries.txt", true)
        );

        Main.preprocess();
        Graph g = Main.loadGraph();
        Solver s1 = new NaiveSolver(g);
        Solver s2 = new GreedySolver(g);
        //Solver s3 = new SPSolver(g);
        int nodeCount = g.nodes.size();
        double totalDuration1 = 0;
        double totalDuration2 = 0;
        //double totalDuration3 = 0;
        int totalUB = 0;
        int totalLB1 = 0;
        int totalLB2 = 0;
        //int totalLB3 = 0;
        try {
            for (int i = 1; i <= testCount; i++) {
                Random r = new Random();
                Node source = g.getNode(r.nextInt(nodeCount));
                Node target = g.getNode(r.nextInt(nodeCount));
                double maxDistFactor = 1 + r.nextDouble() / 2;

                Graph.Path solverPath1;
                Graph.Path solverPath2;
                //Graph.Path solverPath3;
                float duration1 = 0;
                float duration2 = 0;
                float duration3 = 0;
                try {

                    s1.update(source, target, maxDistFactor);
                    long startTime = System.currentTimeMillis();
                    solverPath1 = s1.solve();
                    duration1 = (System.currentTimeMillis() - startTime) / 1000.0f;

                    s2.update(source, target, maxDistFactor);
                    startTime = System.currentTimeMillis();
                    solverPath2 = s2.solve();
                    duration2 = (System.currentTimeMillis() - startTime) / 1000.0f;

                    //s3.update(source, target, maxDistFactor);
                    //startTime = System.currentTimeMillis();
                    //solverPath3 = s3.solve();
                    //duration3 = (System.currentTimeMillis() - startTime) / 1000.0f;

                } catch (IllegalArgumentException e) {
                    writer.append("solver bug. trying new setting.");
                    i--;
                    continue;
                }

                double maxDistance = s1.getMaxDistance();
                double shortestPathLength = maxDistance / maxDistFactor;
                int lowerBound1 = s1.uniqueClassScore(solverPath1);
                int lowerBound2 = s2.uniqueClassScore(solverPath2);
                //int lowerBound3 = s3.uniqueClassScore(solverPath3);
                int lowerBound3 = 0;
                int upperBound = s1.getUpperBound();

                String current = String.format("run %d: s: %s; t: %s; st factor: %.3f duration1: %.2f; duration2: %.2f; duration3: %.2f; min st: %.3f; ub: %d; lb1: %d; lb2: %d; lb3: %d\n",
                        i, source, target, maxDistFactor, duration1, duration2, duration3, shortestPathLength, upperBound, lowerBound1, lowerBound2, lowerBound3);
                logger.info(current);
                writer.append(current);
                totalDuration1 += duration1;
                totalDuration2 += duration2;
                //totalDuration3 += duration3;
                totalUB += upperBound;
                totalLB1 += lowerBound1;
                totalLB2 += lowerBound2;
                //totalLB3 += lowerBound3;
            }
            String total1 = String.format("total runs: %d: avg duration: %.3f; avg ub: %.3f; avg lb: %.3f\n\n\n",
                    testCount, totalDuration1/testCount, totalUB/(double)testCount, totalLB1/(double)testCount);
            String total2 = String.format("total runs: %d: avg duration: %.3f; avg ub: %.3f; avg lb: %.3f\n\n\n",
                    testCount, totalDuration2/testCount, totalUB/(double)testCount, totalLB2/(double)testCount);
            //String total3 = String.format("total runs: %d: avg duration: %.3f; avg ub: %.3f; avg lb: %.3f\n\n\n",
            //        testCount, totalDuration3/testCount, totalUB/(double)testCount, totalLB3/(double)testCount);
            logger.info(total1);
            writer.append(total1);
            logger.info(total2);
            writer.append(total2);
            //logger.info(total3);
            //writer.append(total3);
        } finally {
            writer.close();
        }
    }

    /**
     * Context to temporary set log level
     * @param level temporary log level
     * @return AutoClosable object, that resets log level to previous state on close
     */
    private AutoCloseable withLogLevel(Level level) {
        Level previousLogLevel = logger.getLevel();
        LoggerContext logCtx = (LoggerContext) LogManager.getContext(false);
        Configuration config = logCtx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        loggerConfig.setLevel(level);
        logCtx.updateLoggers();  // This causes all Loggers to refetch information from their LoggerConfig
        return () -> loggerConfig.setLevel(previousLogLevel);  // method run when closed (e.g. after try block)
    }
}
