import java.io.BufferedReader;
import java.net.Socket;

// adt of request including timestamp to be used by aggregation server for request ordering
class RequestWithTimestamp {
    private final String request;
    private final int timestamp;
    private final Socket socket;
    private final BufferedReader reader;

    // constructor
    public RequestWithTimestamp(String request, BufferedReader reader, int timestamp, Socket socket) {
        this.request = request;
        this.timestamp = timestamp;
        this.socket = socket;
        this.reader = reader;
    }

    // necessary getters
    public String getRequest() {
        return request;
    }

    public BufferedReader getReader() {
        return reader;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public Socket getSocket() {
        return socket;
    }
}