package tests;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;

// get client test class
public class GetClientTest3 {
    private LamportClock lclock;

    public GetClientTest3() {
        this.lclock = new LamportClock();
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            return;
        }
        String serverUrl = args[0];
        String outputFileName = "wdata4.txt";
        GetClientTest3 client = new GetClientTest3(); 
        client.getLClockOfAS(serverUrl);
        client.start(serverUrl, outputFileName);
    }

    private void getLClockOfAS(String serverUrl) {
        LClockGetter1 lclockGET = new LClockGetter1(serverUrl);
        int ASTStamp = lclockGET.getStamp();
        lclock.setTimestamp(ASTStamp + 1);
    }


    public void start(String serverUrl, String outputFileName) {
        try {
            String stationID = "IDS60901";
            URL url = new URL(serverUrl);
            int serverPort = url.getPort();
            Socket clientSocket = new Socket(url.getHost(), serverPort);
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
            String getRequest = ("GET id: " + stationID + "\n" + "Timestamp: " + lclock.getTimestamp() + "\n");
            writer.println(getRequest);
            lclock.tick();
            StringBuilder responseData = new StringBuilder();
            String line1;
            while ((line1 = reader.readLine()) != null) {
                responseData.append(line1).append("\n");
            }
            lclock.tick();
            try (PrintWriter fileWriter = new PrintWriter(new FileWriter(outputFileName))) {
                fileWriter.print(responseData.toString());
            }
            String data = responseData.toString();
            System.out.println(data);
            System.out.println("Data received and saved to " + outputFileName);
            clientSocket.close();
        } catch (MalformedURLException error) {
            System.out.println("Invalid URL -> " + serverUrl);
        } catch (IOException error) {
            error.printStackTrace();
        }
    }
}