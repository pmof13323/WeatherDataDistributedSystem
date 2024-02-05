import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Comparator; 
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// aggregation server class 
public class AggregationServer {
    private static final int defaultPort = 4567;
    private int port;
    private LamportClock lClock; 
    public static ConcurrentHashMap<String, Long> cServData; // public for testing
    private volatile boolean stopRequested = false;
    private ServerSocket serverSocket;
    private PriorityQueue<RequestWithTimestamp> orderQueue;
    private int lastTimestamp = -1;

    // main method for aggregation server initialises and starts a new aggregation server
    public static void main(String[] args) {
        // setting port, if not specified in argument use default port
        int port = (args.length > 0) ? Integer.parseInt(args[0]) : defaultPort;
        AggregationServer server = new AggregationServer(port);
        server.start();
    }

    // aggregation server constructor
    public AggregationServer(int port) {
        this.port = port;
        this.lClock = new LamportClock();
        AggregationServer.cServData = new ConcurrentHashMap<>();
        serverSocket = null;
        orderQueue = new PriorityQueue<>(Comparator.comparingInt(RequestWithTimestamp::getTimestamp));
    }

    // start method to start aggregation server and run requests
    public void start() {

        // creating executor service with sufficient number of available threads
        ExecutorService executorService = Executors.newFixedThreadPool(12);

        try {
            // connect socket
            serverSocket = new ServerSocket(port);
            System.out.println("Aggregation server has started on port " + port);

            // startup cleanup method in new thread
            Thread cleanupThread = new Thread(this::cleanup);
            cleanupThread.start();

            // create a new thread to process requests
            Thread requestProcessorThread = new Thread(() -> processRequests(executorService));
            requestProcessorThread.start();
            
            // while stop has not been requested, accept connections, read requests
            while (!stopRequested) {

                Socket clientSocket = serverSocket.accept();
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                reader.mark(4096);
                StringBuilder reqBuilder = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    reqBuilder.append(line).append("\n");
                }
                
                String requestLine = reqBuilder.toString();
                reader.reset();

                // extracting and updating timestamps if necessary
                if (requestLine != null) {
                    int timestamp = extractTimestamp(requestLine);

                    // is received stamp does not match order, update it
                    if (lClock.compare(timestamp) == 1) {
                        timestamp = lClock.getTimestamp() + 1;
                    }

                    // receiving/ticking lamport clock
                    lClock.receiveAction(timestamp);

                    // add request and timestamp to priority queue to be run
                    orderQueue.add(new RequestWithTimestamp(requestLine, reader, timestamp, clientSocket));
                }
    
            }

        } catch (IOException error) {
            error.printStackTrace();
        }
    }

    // request processor method, manages priority queue and runs requests in correct order
    private void processRequests(ExecutorService executorService) {

        while (!stopRequested) {

            // get next request to be run
            RequestWithTimestamp nextRequest = orderQueue.poll();
    
            if (nextRequest == null) {
                
                try {
                    // sleep briefly to allow requests to come in prior to running poll again
                    Thread.sleep(100);
                } catch (InterruptedException error) {
                    error.printStackTrace(); 
                }

                continue; // Skip processing if the queue is empty
            }

            // error catching mechanism to ensure that the next request being processed is in correct timestamp order 
            if (nextRequest.getTimestamp() <= lastTimestamp) {
                System.err.println("Out-of-order processing at timestamp: " + nextRequest.getTimestamp());
            }

            lastTimestamp = nextRequest.getTimestamp();
    
            //System.out.println("next run is "+ nextRequest.getTimestamp());
            
            // process the request next in line
            Socket requestSocket = nextRequest.getSocket();
            BufferedReader requestReader = nextRequest.getReader();
            Runnable handler = new RequestHandler(requestSocket, requestReader, lClock.getTimestamp());
            
            // run request
            executorService.execute(handler);
            lClock.tick();
            
        }
    }
    
    

    // helper method to extract the timestamp from request
    private int extractTimestamp(String requestString) {
        Pattern pattern = Pattern.compile("Timestamp: (\\d+)");
        Matcher matcher = pattern.matcher(requestString);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException error) {
                error.printStackTrace();
            }
        }
        return -1; 
    }


    // method to stop aggregation server used during unit testing
    public void stop() {

        stopRequested = true;

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException error) {
                error.printStackTrace();
            }
        }
    }

    // cleanup method that is responsible for expunging expired data -> public for testing
    public void cleanup() {

        try {
            Thread.sleep(30000);
        } catch (InterruptedException error) {
            error.printStackTrace();
        }

        while (!stopRequested) {
            long timeNow = System.currentTimeMillis();

            // Iterate through entries in hashmap
            for (Map.Entry<String, Long> entry : cServData.entrySet()) {
                String contentServerAddress = entry.getKey();
                Long lastCommsTime = entry.getValue();

                // If last communication time is over 30 seconds, remove data for the expired content server if it is still in dataEntry
                if (timeNow - lastCommsTime >= 30000) {
                    removeDataForExpiredServer(contentServerAddress);
                    cServData.remove(contentServerAddress);
                }
            }
        }
    }

    // helper method to remove all data for an expired content server from entryData.txt
    private void removeDataForExpiredServer(String contentServerAddress) {
        String fileName = "entryData.txt";

        try {
            // read the content of entryData.txt
            BufferedReader fileRead = new BufferedReader(new FileReader(fileName));
            String line;

            while ((line = fileRead.readLine()) != null) {
                if (line.startsWith("Content Server Address: ")) {
                    String currentAddress = line.substring(24).trim();

                    // if there is data in the file from the expired server, then wipe the file
                    if (currentAddress.equals(contentServerAddress)) {
                        FileWriter fileWrite = new FileWriter(fileName);
                        fileWrite.close();
                        System.out.println("Expunged data from: " + currentAddress + "\n");
                    }
                } 
            }

            fileRead.close();

        } catch (IOException error) {
            error.printStackTrace();
        }
    }

    // method to update hashmap storing content server puts
    public static void updateMap(String contentServerAddress) {
        cServData.put(contentServerAddress, System.currentTimeMillis());
    }
}