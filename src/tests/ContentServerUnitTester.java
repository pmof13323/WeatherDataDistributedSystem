package tests;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.*;

// content server unit tester class
public class ContentServerUnitTester {
    private static final Logger logger = Logger.getLogger(ContentServerUnitTester.class.getName());

    public static void main(String[] args) throws MalformedURLException, IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        System.setOut(printStream);

        if (testValidInput()) {
            logger.info("content server startup successful");
        } else {
            logger.warning("startup failed");
        }

        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));

        String terminalOutput = outputStream.toString();
        if (terminalOutput.contains("HTTP_CREATED - First-time data received") || terminalOutput.contains("OK - Data updated successfully")) {
            logger.info("test success");
        } else {
            logger.warning("test fail");
        }
    }

    public static boolean testValidInput() throws MalformedURLException, IOException {
        String serverAddress = "http://localhost:5678";
        String fileName = "ConServData.txt";

        try {
            URL url = new URL(serverAddress);
            int serverPort = url.getPort();

            ContentServer contentServer = new ContentServer(url.getHost(), serverPort);
            contentServer.start(fileName);

            return true; 
        } catch (Exception error) {
            logger.log(Level.SEVERE, "startup failed ->  " + error.getMessage(), error);
            return false;
        }
    }
}