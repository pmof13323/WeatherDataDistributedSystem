import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONException;
import org.json.JSONObject;


// content server class
public class ContentServer {
    private String servAdr;
    private int servPort;
    private static String serverUrl;
    private LamportClock lclock;

    // constructor that sets server address and serverport
    public ContentServer(String serverAddress, int serverPort) {
        this.servAdr = serverAddress;
        this.servPort = serverPort;
        this.lclock = new LamportClock();
    }

    // main method for content server
    public static void main(String[] args) throws InterruptedException {
        if (args.length != 2) {
            System.out.println("please input in the form -> java ContentServer <server_address> <file_name>");
            return;
        }

        serverUrl = args[0];
        String fileName = args[1];

        try {

            // connect socket
            URL url = new URL(serverUrl);
            int serverPort = url.getPort();
            ContentServer contentServer = new ContentServer(url.getHost(), serverPort);
            
            // get lamport clock value of aggregation server
            contentServer.getLClockOfAS(serverUrl);

            // sleeper for content server that updates weather data every 15 seconds
            while (true) {
                contentServer.start(fileName);
                System.out.println("Sleeping for 15 seconds...");
                Thread.sleep(15000); 
            }

        } catch (MalformedURLException error) {
            System.err.println("Invalid server address: " + error.getMessage());
        }
    }

    // helper method to get lamport clock value of aggregation server
    private void getLClockOfAS(String serverUrl) {
        LClockGetter1 lclockGET = new LClockGetter1(serverUrl);
        int ASTStamp = lclockGET.getStamp();
        lclock.setTimestamp(ASTStamp + 1);
    }

    // content server start method
    public void start(String fileName) {
        try {

            // locating data file
            File dataFile = new File(fileName);

            // if file does not exist, create it
            if (!dataFile.exists()) {
                dataFile.createNewFile();
            }
            
            BufferedReader fileReader = new BufferedReader(new FileReader(fileName));

            while (true) { 
                try {
                    Socket socket = new Socket(servAdr, servPort);

                    // reading from data source and parsing into JSON
                    String line;
                    JSONObject jsonData = new JSONObject();

                    while ((line = fileReader.readLine()) != null) {
                        String[] parts = line.split(":");
                        if (parts.length == 2) {
                            String key = parts[0].trim();
                            String value = parts[1].trim();
                            jsonData.put(key, value);
                        }
                    }

                    String jsonStr = jsonData.toString();

                    // constructing and sending put request, incrementing timestamp, getting response
                    String putRequest = constructPutRequest(jsonStr);
                    lclock.tick();
                    String response = sendPutRequest(putRequest, socket);

                    // parsing status response & returns timestamp
                    int statusCode = getStatusCode(response);
                    int newStamp = extractTimestamp(response);
                    String statusMessage = "";

                    // confirming status message
                    if (statusCode == 201) {
                        statusMessage = "HTTP_CREATED - First-time data received";
                    } else if (statusCode == 200) {
                        statusMessage = "OK - Data updated successfully";
                    }

                    // printing out status;
                    System.out.println("Received Status Code: " + statusCode);
                    System.out.println(statusMessage);

                    lclock.receiveAction(newStamp);

                    socket.close();
                    fileReader.close();
                    break; 

                } catch (ConnectException error) { // if there is no aggregation server running, retry connecting after 5 seconds
                    System.err.println("Failed to connect to AG server. Retrying in 5 seconds...");
                    Thread.sleep(5000); 
                } catch (IOException | JSONException error) {
                    error.printStackTrace();
                    break;
                }
            }
        } catch (FileNotFoundException error) {
            System.err.println("File not found: " + error.getMessage());
        } catch (InterruptedException error) {
            System.err.println("Thread interrupted: " + error.getMessage());
        } catch (IOException error) {
            error.printStackTrace();
        }
    }

    // put request maker that takes in JSON data and returns the request string
    public String constructPutRequest(String jsonData) {
        return "PUT /weather.json HTTP/1.1\r\n" +
               "User-Agent: ATOMClient/1/0\r\n" +
               "Timestamp: " + lclock.getTimestamp() + "\r\n" +
               "Content-Type: application/json\r\n" +
               "Content-Length: " + jsonData.length() + "\r\n\r\n" + jsonData;
    }

    // put request send method that takes socket and request and writes into the socket, returns response
    public String sendPutRequest(String putRequest, Socket socket) throws IOException {
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(putRequest);
        out.flush();

        // read response from the server
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        StringBuilder responseBuilder = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            responseBuilder.append(line).append("\n");
        }

        in.close();

        return responseBuilder.toString();
    }

    // helper method to parse the status code from response
    public int getStatusCode(String response) {
        String[] lines = response.split("\r\n");
        if (lines.length > 0) {
            String[] statusLine = lines[0].split(" ");
            if (statusLine.length >= 2) {
                try {
                    return Integer.parseInt(statusLine[1]);
                } catch (NumberFormatException error) {
                    error.printStackTrace();
                }
            }
        }
        return -1; // return -1 if the status code not gotten
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
