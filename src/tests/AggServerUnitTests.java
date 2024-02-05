package tests;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Logger;

// aggregation server unit tester class
public class AggServerUnitTests { 
    
    // main method for aggregation server unit tester
    public static void main(String[] args) {

        // reading info
        try (BufferedReader reader = new BufferedReader(new FileReader("AggServerTests.txt"))) {
            String[] ports = reader.readLine().split(" ");
            String[] contentServerAddresses = new String[3];
            
            for (int i = 0; i < 3; i++) {
                contentServerAddresses[i] = reader.readLine();
            }

            // testing map update
            AggregationServer server1 = new AggregationServer(8888);
            for (String contentServerAddress : contentServerAddresses) {
                testUpdateMap(server1, contentServerAddress);
            }
            stopAggregationServer(server1);

            // testing server cleanup
            AggregationServer server2 = new AggregationServer(3333);
            testCleanup(server2);
            stopAggregationServer(server2);

            // run tests for each server port and content server address
            for (String portStr : ports) {
                int port = Integer.parseInt(portStr);
                AggregationServer server = new AggregationServer(port);

                // testing server startup
                testStart(server, port);

                // stopping aggregations servers
                stopAggregationServer(server);
            }

            System.out.println("unit tests complete");

        } catch (IOException error) {
            error.printStackTrace();
        }
    }

    // method that tests aggregation server startup
    public static void testStart(AggregationServer server, int port) {
        // using logger for output
        Logger logger = Logger.getLogger("AggServerUnitTests");
        PrintStream ogOut = System.out;
        ByteArrayOutputStream cOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(cOut));

        // start in separate thread
        Thread serverThread = new Thread(() -> {
            server.start();
        });
        serverThread.start();

        // stop server
        stopAggregationServer(server);

        // restore original output stream
        System.setOut(ogOut);

        // get output
        String capturedOutput = cOut.toString();

        // confirm response
        if (capturedOutput.contains("Aggregation server has started on port " + port)) {
            logger.info("Start test passed for port " + port);
        } else {
            logger.warning("Start test failed for port " + port);
        }
    }

    // method to test map update
    public static void testUpdateMap(AggregationServer server, String contentServerAddress) {

        AggregationServer.updateMap(contentServerAddress);

        // confirm response
        if (AggregationServer.cServData.containsKey(contentServerAddress)) {
            System.out.println("Update map passed for content server " + contentServerAddress);
        } else {
            System.out.println("Update map failed for content server " + contentServerAddress);
        }
    }

    // method to test cleanup
    public static void testCleanup(AggregationServer server) {

        // starting cleanup method
        Thread cleanupThread = new Thread(() -> server.cleanup());
        cleanupThread.start();

        // dummy data
        AggregationServer.updateMap("cServ1");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException error) {
            error.printStackTrace();
        }
        AggregationServer.updateMap("cServ2");
        
        try {
            Thread.sleep(30000);
        } catch (InterruptedException error) {
            error.printStackTrace();
        }
        AggregationServer.updateMap("cServ3");
        
        try {
            Thread.sleep(10000);
        } catch (InterruptedException error) {
            error.printStackTrace();
        }
        
        // check if data for expired content servers removed or not
        if (!AggregationServer.cServData.containsKey("cServ1") &&
            !AggregationServer.cServData.containsKey("cServ2") &&
            AggregationServer.cServData.containsKey("cServ3")) {
            System.out.println("Cleanup test passed");
        } else {
            System.out.println("Cleanup test failed");
        }
    }

    // method to test stop
    public static void stopAggregationServer(AggregationServer server) {
        server.stop();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException error) {
            error.printStackTrace();
        }
    }
}