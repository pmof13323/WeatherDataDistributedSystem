// lamport clock class
public class LamportClock {
    private int timestamp;

    // constructor
    public LamportClock() {
        this.timestamp = 0;
    }

    // getter for timestamp
    public synchronized int getTimestamp() {
        return timestamp;
    }

    // setter for timestamp 
    public synchronized void setTimestamp(int newStamp) {
        if (newStamp > timestamp) {
            timestamp = newStamp;
        }
    }

    // ticker for timestamp
    public synchronized void tick() {
        timestamp++;
    }

    // action receiver, takes in a timestamp, makes comparison and adds one, otherwise ticks timestamp
    public synchronized void receiveAction(int stamp) {
        if (stamp > timestamp) {
            timestamp = stamp + 1;
        } else {
            tick();
        }
    }

    // compares two timestamps if input is larger returns -1 if input is lesser returns 1 otherwise returns 0
    public synchronized int compare(int stamp) {
        if (timestamp < stamp) {
            return -1;
        } else if (timestamp > stamp) {
            return 1;
        } else {
            return 0;
        }
    }
}