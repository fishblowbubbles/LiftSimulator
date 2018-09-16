import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class Request {

    public final int floor;
    public final Direction direction;

    Status status;
    // use thread-safe CopyOnWriteArrayList, so that synchronized
    // get/set will not be required
    public List < Integer > destinations;

    public Request(int floor, Direction direction) {
        this.floor = floor;
        this.direction = direction;
        this.reset();
    }

    public void print() {
        System.out.println(
            "\nRequest -" +
            "\n\tStatus : " + this.status +
            "\n\tFloor : " + this.floor +
            "\n\tDirection : " + this.direction +
            "\n\tDestinations : " + this.destinations
        );
    }

    public synchronized void reset() {
        this.destinations = new CopyOnWriteArrayList < > ();
        // first destination must always be the floor of origin
        this.destinations.add(this.floor);
        this.status = Status.NONE;
    }

    public synchronized Status getStatus() {
        return this.status;
    }

    public synchronized void setStatus(Status status) {
        this.status = status;
    }
}
