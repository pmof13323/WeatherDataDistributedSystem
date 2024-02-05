package tests;

// lamport clock unit tester class
public class lamportClockUnitTester {
    public static void main(String[] args) {
        LamportClock clock = new LamportClock();

        // testing initial stamp
        assert clock.getTimestamp() == 0;

        // testing setter and getter
        clock.setTimestamp(5);
        assert clock.getTimestamp() == 5;

        // testing tick
        clock.tick();
        assert clock.getTimestamp() == 6;

        // testing receive action
        clock.receiveAction(8);
        assert clock.getTimestamp() == 9;

        clock.receiveAction(7);
        assert clock.getTimestamp() == 10;

        clock.receiveAction(10);
        assert clock.getTimestamp() == 11;

        // testing compare
        assert clock.compare(12) == -1;

        assert clock.compare(8) == 1;

        assert clock.compare(11) == 0;

        System.out.println("all tests passed");
    }
}