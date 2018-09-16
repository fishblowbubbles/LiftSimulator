import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class Lift extends Thread {

    public final int id;
    public final int start;

    Status status;
    int floor;
    Direction direction;
    // use thread-safe CopyOnWriteArrayList, so that synchronized
    // get/set will not be required
    public List < Integer > destinations;
    Dispatcher dispatcher;

    public Lift(int id, int start, Dispatcher dispatcher) {
        this.id = id;
        this.start = start;

        this.floor = start;
        this.direction = Direction.NONE;
        this.destinations = new CopyOnWriteArrayList < > ();

        this.status = Status.AVAILABLE;
        this.dispatcher = dispatcher;
    }

    @Override public void run() {
        while (true) {
            if (Thread.interrupted()) {
                this.startService();
                this.print();
            }
        }
    }

    public void print() {
        System.out.println(
            "\nLift " + this.id + " -" +
            "\n\tStatus : " + this.status +
            "\n\tFloor : " + this.floor +
            "\n\tDirection : " + this.direction +
            "\n\tDestinations : " + this.destinations
        );
    }

    public synchronized Status getStatus() {
        return this.status;
    }

    public synchronized int getFloor() {
        return this.floor;
    }

    public synchronized Direction getDirection() {
        return this.direction;
    }

    private void startService() {
        // wait for dispatcher to finish assigning new requests
        while (this.dispatcher.isDispatching.get());

        while (this.destinations.size() != 0) {
            int destination = this.destinations.get(0);
            this.setDirectionTowards(destination);
            this.travelTo(destination);
        }

        this.direction = Direction.NONE;
    }

    private void setDirectionTowards(int floor) {
        if (this.floor < floor) {
            direction = Direction.UP;
        } else if (this.floor > floor) {
            direction = Direction.DOWN;
        } else {
            direction = Direction.NONE;
        }
    }

    private void travelTo(int floor) {
        this.setStatus(Status.BUSY);
        this.print();

        while (this.floor != floor) {
            this.floor += direction.getValue();
            // simulate traveling time
            delay(1000);
        }

        this.destinations.remove(0);
        this.setStatus(Status.AVAILABLE);

        // window for dispatcher to add new destinations that are in the
        // same direction (or perhaps for the alighting of passengers?)
        delay(500);
    }

    private synchronized void setStatus(Status status) {
        this.status = status;
    }

    private static void delay(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            // e.printStackTrace();
        }
    }
}