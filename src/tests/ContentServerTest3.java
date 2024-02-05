package tests;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import org.json.JSONException;
import org.json.JSONObject;


// content server test class
public class ContentServerTest3 {
    private String servAdr;
    private int servPort;
    private LamportClock lclock;

    public ContentServerTest3(String serverAddress, int serverPort) {
        this.servAdr = serverAddress;
        this.servPort = serverPort;
        this.lclock = new LamportClock();
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            return;
        }
        String serverUrl = args[0];
        String fileName = args[1];
        try {
            URL url = new URL(serverUrl);
            int serverPort = url.getPort();
            ContentServerTest3 contentServer4 = new ContentServerTest3(url.getHost(), serverPort);
            contentServer4.getLClockOfAS(serverUrl);
            contentServer4.start(fileName);
        } catch (MalformedURLException error) {
            System.err.println("Invalid server address: " + error.getMessage());
        }
    }

    private void getLClockOfAS(String serverUrl) {
        LClockGetter1 lclockGET = new LClockGetter1(serverUrl);
        int ASTStamp = lclockGET.getStamp();
        lclock.setTimestamp(ASTStamp + 1);
    }

    public void start(String fileName) {
        lclock.tick();
        try {
            BufferedReader fileReader = new BufferedReader(new FileReader(fileName));

            while (true) { 
                try {
                    Socket socket = new Socket(servAdr, servPort);

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
                    String putRequest = constructPutRequest(jsonStr);
                    String response = sendPutRequest(putRequest, socket);
                    int statusCode = getStatusCode(response);
                    String statusMessage = "";
                    if (statusCode == 201) {
                        statusMessage = "HTTP_CREATED - First-time data received";
                    } else if (statusCode == 200) {
                        statusMessage = "OK - Data updated successfully";
                    }
                    System.out.println("Received Status Code: " + statusCode);
                    System.out.println(statusMessage);
                    lclock.tick();
                    socket.close();
                    fileReader.close();
                    break; 
                } catch (ConnectException error) {

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
        }
    }

    private String constructPutRequest(String jsonData) {
        return "PUT /weather.json HTTP/1.1\r\n" +
               "User-Agent: ContentServer/1.0\r\n" +
               "Timestamp: " + lclock.getTimestamp() + "\r\n" +
               "Content-Type: application/json\r\n" +
               "Content-Length: " + jsonData.length() + "\r\n\r\n" + jsonData;
    }

    private String sendPutRequest(String putRequest, Socket socket) throws IOException {
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(putRequest);
        out.flush();
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        StringBuilder responseBuilder = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            responseBuilder.append(line).append("\n");
        }
        in.close();
        return responseBuilder.toString();
    }

    private static int getStatusCode(String response) {
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
        return -1; 
    }
}