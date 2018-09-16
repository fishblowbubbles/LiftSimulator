import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Simulator extends Thread {

    Dispatcher dispatcher;

    public Simulator(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override public void run() {
        while (true) {
            this.createRequest();
            delay(5000);
        }
    }

    private void createRequest() {
        int floor = new Random().nextInt(dispatcher.numFloors);
        List < Integer > destinations;
        Direction direction;

        if (floor == 0) {
            direction = Direction.UP;
        } else if (floor == dispatcher.numFloors - 1) {
            direction = Direction.DOWN;
        } else {
            direction = randomDirection();
        }

        if (direction == Direction.UP) {
            destinations = randomDestinations(floor, dispatcher.numFloors);
        } else {
            destinations = randomDestinations(0, floor);
        }

        dispatcher.handleRequest(floor, direction, destinations);
    }

    private List < Integer > randomDestinations(int start, int end) {
        List < Integer > destinations = new LinkedList < > ();
        int range = end - start;
        int numDestinations = new Random().nextInt(range);

        for (int i = 0; i < numDestinations; i++) {
            int destination = new Random().nextInt(range) + start;
            if (!destinations.contains(destination))
                destinations.add(destination);
        }

        return destinations;
    }

    private Direction randomDirection() {
        int random = new Random().nextInt(2);
        if (random == 0) {
            return Direction.UP;
        } else {
            return Direction.DOWN;
        }
    }

    private static void delay(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            // e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Dispatcher dispatcher = new Dispatcher(10, 3);
        Simulator simulator = new Simulator(dispatcher);

        dispatcher.start();
        simulator.start();
    }
}