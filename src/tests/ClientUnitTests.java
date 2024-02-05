package tests;
// class for unit testing client
public class ClientUnitTests {

    public static void main(String[] args) {
        // testing valid input
        testValidInput();

        // testing bad url
        testInvalidServerURL();

    }

    // method to test valid input
    public static void testValidInput() {
        GETClient client = new GETClient();
        String serverUrl = "http://localhost:5678";
        String outputFileName = "wdata1.txt";

        System.out.println("Correct URL test was successful! \n");
        System.out.println("Type stop to move on to testing invalid URL \n");

        try {
            // start the client with valid input
            client.start(serverUrl, outputFileName);

        } catch (Exception error) {
            System.err.println("TestValidInput failed -> " + error.getMessage());
        }
    }

    // method to test invalid url
    public static void testInvalidServerURL() {
        GETClient client = new GETClient();
        String invalidServerUrl = "invalid_url"; 
        String outputFileName = "testOutput.txt";

        System.out.println("This is the incorrect URL test, if error is returned test is successful, please input a real ID (IDS60901) -> \n");

        try {
            client.start(invalidServerUrl, outputFileName);
        } catch (Exception error) {
            System.err.println("TestInvalidServerURL issue found -> " + error.getMessage());
        }
    }
}
