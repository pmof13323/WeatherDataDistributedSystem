import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import org.json.JSONObject;


// request handler class, responsible for get and put operations across the entire program
public class RequestHandler implements Runnable {

    private final Object fileWriteLock = new Object();
    private final Socket clientSocket1;
    private final BufferedReader reader1;
    private final int AggServlClock;
    
    // request handler constructor  
    public RequestHandler(Socket clientSocket, BufferedReader reader, int lClockVal) {
        this.clientSocket1 = clientSocket;
        this.reader1 = reader;
        this.AggServlClock = lClockVal;
    }

    // handle request method, takes in a socket and reader, responsible for discerning between a put and get request
    private void handleRequest(Socket clientSocket, BufferedReader reader) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {
    
            // reading request header
            String requestLine = reader.readLine();
            
            if (requestLine != null) {
                if (requestLine.startsWith("GET: Lamport Clock")) { // if get lamport clock request is made
                    handleGetRequest(clientSocket, reader, writer, null, 0, true);
                } else if (requestLine.startsWith("GET")) { // if get information request is made
                    
                    // extracting id and reading timestamp line
                    String idRequested = requestLine.substring(8).trim(); 
                    String timestampLine = reader.readLine();
                    
                    // extracting timestamp and running get
                    if (timestampLine.startsWith("Timestamp: ") && idRequested.startsWith("IDS")) {
                        int timestamp = Integer.parseInt(timestampLine.substring(11).trim());
                        handleGetRequest(clientSocket, reader, writer, idRequested, timestamp, false);
                    } else {
                        handleGetRequest(clientSocket, reader, writer, "IDS60901", 0, false); // default ID & timestamp if not specificed in GET request
                    }

                } else if (requestLine.startsWith("PUT")) { // if put request is made
                    // running put
                    handlePutRequest(clientSocket, reader, writer);

                } else {
                    sendResponse(writer, 400, "Bad Request", "No data provided");
                }
            }

        } catch (IOException error) {
            error.printStackTrace();
        }
    }
    
    // get request handler, takes an id, socket, reader, writer, timestamp and a boolean variable to discern between a get information or get timestamp request
    private void handleGetRequest(Socket clientSocket, BufferedReader reader, BufferedWriter writer, String idRequested, int timestamp, boolean clockRequested) throws IOException { // public for unit testing
        
        if (!clockRequested) {
            System.out.println("handling get request");        
        }

        try {

            // if clock request send clock response
            if (clockRequested == true) {
                sendResponse(writer, 200, "OK", "Timestamp: " + Integer.toString(AggServlClock));
                return;
            }

            synchronized (fileWriteLock) {

                System.out.println("id requested was " + idRequested + "\n");

                // reading data from the dataEntry.txt file based on id
                File dataFile = new File("entryData.txt");

                // if the file does not exist send appropriate response
                if (!dataFile.exists()) {
                    sendResponse(writer, 404, "Not Found", "data file not found");
                    return;
                }

                BufferedReader fileReader = new BufferedReader(new FileReader(dataFile));
                String line;
                StringBuilder resBuild = new StringBuilder();
                boolean found = false;

                // look for id
                while ((line = fileReader.readLine()) != null) {
                    if (line.startsWith("id: " + idRequested)) {
                        found = true;
                    } else if (found && !line.isEmpty() && line.startsWith("{")) {
                        resBuild.append(line).append("\n");
                    }
                }

                fileReader.close();
    
                // building response 
                String responseData = formatData(resBuild.toString());
                
                // sending responses
                if (!responseData.isEmpty()) {
                    sendResponse(writer, 200, "OK", responseData);
                } else {
                    sendResponse(writer, 204, "No Content", "");
                }
            }

        } catch (IOException | NumberFormatException error) {
            error.printStackTrace();
            sendResponse(writer, 500, "Internal Server Error", "");
        }
    }
    

    // put request handler, takes in socket, reader and writer, and returns a boolean value indiciting whether it is the first put made or not
    private void handlePutRequest(Socket clientSocket, BufferedReader reader, BufferedWriter writer) throws IOException { 
        
        System.out.println("handling put request");
        
        try {
            // getting information out of the socket and assigning it to variables
            String[] req = thingGetter(clientSocket, reader);
            String id = req[0];
            int timestamp = Integer.parseInt(req[1]); 
            String jsonDataString = req[2]; 
            int contentLength = Integer.parseInt(req[3]);      
            
            synchronized (fileWriteLock) {

                // locating data file
                File dataFile = new File("entryData.txt");

                // if file does not exist, create it
                if (!dataFile.exists()) {
                    dataFile.createNewFile();
                }

                // if data for the ID already exists, remove it
                boolean isFirstTime = isNewData(id);
                if (!isFirstTime) {
                    removeDataForId(id);
                }

                String contentServerAddress = clientSocket.getRemoteSocketAddress().toString();
                
                // writing the new data to the intermediate storage
                FileWriter fileWriter = new FileWriter(dataFile, true);
                fileWriter.write("id: " + id + "\n");
                fileWriter.write("Content Server Address: " + contentServerAddress + "\n");
                fileWriter.write("Timestamp: " + timestamp + "\n");
                fileWriter.write("Content length: " + contentLength + "\n");
                fileWriter.write(jsonDataString + "\n");
                fileWriter.close();
                
                boolean empty = false;

                if (jsonDataString.equals("{}")) {
                    empty = true;
                }

                // update the hashmap in the aggregation server responsible for cleanup order
                AggregationServer.updateMap(contentServerAddress); 

                // send appropriate response
                if (isFirstTime && !empty) {
                    sendResponse(writer, 201, "HTTP_CREATED", "First-time data received");
                } else if (!isFirstTime && !empty) {
                    sendResponse(writer, 200, "OK", "Data updated successfully");
                } else {
                    sendResponse(writer, 204, "No Content", "No Content");
                }

            }

        } catch (IOException | NumberFormatException error) {
            error.printStackTrace();
            sendResponse(writer, 500, "Internal Server Error", "");
        }
    }

    // helper method to remove data from input id inside textfile to be replaced
    private void removeDataForId(String id) throws IOException {
        String path = "entryData.txt";
        BufferedReader fileReader = new BufferedReader(new FileReader(path));
        StringBuilder newData = new StringBuilder();
        String line;

        while ((line = fileReader.readLine()) != null) {
            if (line.startsWith("id: " + id)) {
                // skip necessary lines
                while ((line = fileReader.readLine()) != null && !line.isEmpty()) {

                }
            } else {
                newData.append(line).append("\n");
            }
        }

        fileReader.close();

        // write the new data wirthout old id's data back to the file
        FileWriter fileWriter = new FileWriter(path);
        fileWriter.write(newData.toString());
        fileWriter.close();
    }

    // thing getter method that gets reader and socket, extracts necessary information out of socket and returns to other components of the requestHandler class
    private String[] thingGetter(Socket clientSocket, BufferedReader reader) {
        String[] info = new String[4];
        String line;
        int contentLength = 0;
        StringBuilder req = new StringBuilder();
        
        try { 

            // reading request headers to get timestamp and content length
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Timestamp:")) {
                    info[1] = line.substring(10).trim();
                } else if (line.startsWith("Content-Length:")) {
                    contentLength = Integer.parseInt(line.substring(15).trim());
                    info[3] = String.valueOf(contentLength);
                }
            }

            // reading main body
            if (contentLength > 0) {
                char[] body = new char[contentLength];
                reader.read(body);
                req.append(body);
                info[2] = req.toString(); 
            }

            // getting id out of json data
            if (req.length() > 0) {
                String jsonBody = req.toString();
                JSONObject jsonObject = new JSONObject(jsonBody);
                if (jsonObject.has("id")) {
                    info[0] = jsonObject.getString("id");
                }
            }
            return info;

        } catch (IOException error) {
            error.printStackTrace();
        }

        return info;
    }

    // response writer once operations are complete
    private void sendResponse(BufferedWriter writer, int statusCode, String statusMessage, String responseData) throws IOException {
        writer.write("HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n"); 
        writer.write("Content-Type: application/json\r\n");
        writer.write("Content-Length: " + responseData.length() + "\r\n");
        writer.write("Timestamp: " + AggServlClock + "\r\n");
        writer.write("\r\n");
        writer.write(responseData);
        writer.close();
        clientSocket1.close();
    }

    // method to format jason data, takes string in json format and returns a correctly formatted string
    public static String formatData(String data) {
        // splitting into lines
        String[] lines = data.split("\\n");
        StringBuilder FormattedData = new StringBuilder();

        JSONObject jsonObject = new JSONObject();

        for (String line : lines) {
            // checking if JSON data has started and adding data to string
            if (line.trim().startsWith("{")) {
                jsonObject = new JSONObject(line);
                FormattedData.append("id:" + jsonObject.getString("id")).append("\n");
                FormattedData.append("name:" + jsonObject.getString("name")).append("\n");
                FormattedData.append("state:" + jsonObject.getString("state")).append("\n");
                FormattedData.append("time_zone:" + jsonObject.getString("time_zone")).append("\n");
                FormattedData.append("lat:" + jsonObject.getString("lat")).append("\n");
                FormattedData.append("lon:" + jsonObject.getString("lon")).append("\n");
                FormattedData.append("local_date_time:" + jsonObject.getString("local_date_time_full").substring(6, 8) + "/" + jsonObject.getString("local_date_time_full").substring(8, 10) + ":" + jsonObject.getString("local_date_time_full").substring(10, 12) + "pm").append("\n");
                FormattedData.append("local_date_time_full:" + jsonObject.getString("local_date_time_full")).append("\n");
                FormattedData.append("air_temp:" + jsonObject.getString("air_temp")).append("\n");
                FormattedData.append("apparent_t:" + jsonObject.getString("apparent_t")).append("\n");
                FormattedData.append("cloud:" + jsonObject.getString("cloud")).append("\n");
                FormattedData.append("dewpt:" + jsonObject.getString("dewpt")).append("\n");
                FormattedData.append("press:" + jsonObject.getString("press")).append("\n");
                FormattedData.append("rel_hum:" + jsonObject.getString("rel_hum")).append("\n");
                FormattedData.append("wind_dir:" + jsonObject.getString("wind_dir")).append("\n");
                FormattedData.append("wind_spd_kmh:" + jsonObject.getString("wind_spd_kmh")).append("\n");
                FormattedData.append("wind_spd_kt:" + jsonObject.getString("wind_spd_kt")).append("\n");
                }
            }

        // converting to string and returning
        return FormattedData.toString();
    }

    // helper method to check if data for the given ID already exists
    private boolean isNewData(String id) throws IOException {
        String path = "entryData.txt";
        BufferedReader fileRead = new BufferedReader(new FileReader(path));
        String line;
        while ((line = fileRead.readLine()) != null) {
            if (line.startsWith("id: " + id)) {
                fileRead.close();
                return false;
            }
        }
        fileRead.close();
        return true; 
    }
    

    // runner from agg server
    @Override
    public void run() {
        handleRequest(clientSocket1, reader1);
    }

}