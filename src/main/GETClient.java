import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


// get client class
public class GETClient {
    private LamportClock lclock;
    private BufferedReader userInputReader;


    // client constructor
    public GETClient() {
        this.lclock = new LamportClock();
        this.userInputReader = new BufferedReader(new InputStreamReader(System.in));
    }

    // main method for the client
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Please input in the form -> java GETClient <server_url>");
            return;
        }

        String serverUrl = args[0];
        String outputFileName = "wdata1.txt";

        // create client instance
        GETClient client = new GETClient(); 

        // get lamport clock of aggregation server
        client.getLClockOfAS(serverUrl);
        client.start(serverUrl, outputFileName); 
    }

    // helper method to get aggregaiton server lamport clock value
    private void getLClockOfAS(String serverUrl) {
        LClockGetter1 lclockGET = new LClockGetter1(serverUrl);
        int ASTStamp = lclockGET.getStamp();
        lclock.setTimestamp(ASTStamp + 1);
    }

    // start method for the client
    public void start(String serverUrl, String outputFileName) {
        try {
        
            while (true) {

                // taking in station id, or stopping if requested
                System.out.print("Enter a station ID (or 'stop' to exit): ");
                String stationID = userInputReader.readLine();
                if (stationID.equals("stop")) {
                    break;
                }

                while ((stationID.length() != 8) || !stationID.startsWith("IDS")) {
                    System.out.println("Invalid ID, should be in the Form of IDS*****! Try again: ");
                    stationID = userInputReader.readLine();
                    if (stationID.equals("stop")) {
                        break;
                    }
                }

                if (stationID.equals("stop")) {
                    break;
                }

                try {

                    // connecting socket
                    URL url = new URL(serverUrl);
                    int serverPort = url.getPort();
                    Socket clientSocket = new Socket(url.getHost(), serverPort);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

                    // constructing get request and writing into socket
                    String getRequest = ("GET id: " + stationID + "\n" + "Timestamp: " + lclock.getTimestamp() + "\n");
                    writer.println(getRequest);
                    lclock.tick();

                    // receiving response
                    StringBuilder responseData = new StringBuilder();
                    String line1;
                    while ((line1 = reader.readLine()) != null) {
                        responseData.append(line1).append("\n");
                    }

                    // retrieve timestamp
                    int newStamp = extractTimestamp(responseData.toString());
                    lclock.receiveAction(newStamp);
                    //System.out.println("client getting dis ->"+newStamp);
                    
                    // locating data file
                    File dataFile = new File(outputFileName);

                    // if file does not exist, create it
                    if (!dataFile.exists()) {
                        dataFile.createNewFile();
                    }

                    // writing data into text file
                    try (PrintWriter fileWriter = new PrintWriter(new FileWriter(outputFileName))) {
                        fileWriter.print(responseData.toString());
                    }

                    // printing data in terminal
                    System.out.println(responseData.toString());
                    System.out.println("Information saved to " + outputFileName);
                    
                    clientSocket.close();

                } catch (ConnectException error) { // if there is no aggregation server available retry in 5 seconds
                    System.err.println("Failed to connect to AG server. Retrying in 5 seconds...");
                    Thread.sleep(5000); 
                }
            }
            System.out.println("Client stopped.");
        } catch (MalformedURLException error) {
            System.out.println("Invalid URL -> " + serverUrl);
        } catch (IOException error) {
            error.printStackTrace();
        } catch (InterruptedException error) {
            System.err.println("Thread interrupted: " + error.getMessage());
        }
    }
    
    // helper method to extract the timestamp from response
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
}