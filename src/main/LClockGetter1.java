import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.MalformedURLException;
import java.net.URL;


// aggregation server lamport clock getter class
public class LClockGetter1 {
    private static int AggStamp;

    public int getStamp() {
        return AggStamp;
    }

    // constructor
    public LClockGetter1(String sURL) {
        LClockGetter1.AggStamp = 0;
        LClockGetter1.start(sURL);
    }

    // start method for lamport clock getter
    private static void start(String serverUrl) {
        try {

            // connecting to socket 
            URL url = new URL(serverUrl);
            int serverPort = url.getPort();
            Socket clientSocket = new Socket(url.getHost(), serverPort);
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

            // creating a GET request
            String getRequest = ("GET: Lamport Clock" + "\n");
            writer.println(getRequest);
            writer.flush();

            // handling server response
            StringBuilder responseData = new StringBuilder();

            String line;

            // reading response to extract timestamp
            while ((line = reader.readLine()) != null) {
                responseData.append(line).append("\n"); 
                if (line.startsWith("Timestamp:")) {
                    try {
                        // split line by : 
                        String[] parts = line.split(":");
                        if (parts.length >= 2) {
                            AggStamp = Integer.parseInt(parts[1].trim());
                        }
                    } catch (NumberFormatException error) {
                        System.out.println("Error parsing Timestamp value.");
                    }
                }
            }

            // close the socket, reader and writer
            clientSocket.close();
            reader.close();
            writer.close();

        } catch (MalformedURLException error) {
            System.out.println("Invalid URL -> " + serverUrl);
        } catch (IOException error) {
            System.err.println("Error communicating with the server, retrying in 5 seconds...");
            try {
                Thread.sleep(5000); // Sleep for 5 seconds before retrying
            } catch (InterruptedException error1) {
                System.err.println("Thread interrupted: " + error1.getMessage());
            }
        }
    }
}