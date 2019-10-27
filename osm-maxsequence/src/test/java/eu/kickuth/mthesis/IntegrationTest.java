package eu.kickuth.mthesis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

class IntegrationTest {


    private static final Logger logger = LogManager.getLogger(IntegrationTest.class);

//    @BeforeAll
//    static void start() throws InterruptedException {
//        logger.trace("Running integration test.");
//        Main.main(new String[]{""});
//
//        Thread.sleep(5_000);  // Give the Webserver time to start
//    }
//
//    @Test
//    void pingSite() throws IOException {
//        Socket socket = new Socket();
//        socket.connect(new InetSocketAddress("127.0.0.1", 4567), 10_000);
//    }

    // TODO access /path to check path computation
}
