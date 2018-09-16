import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The only 'smart' object in this system. In-charge of accepting 
 * and processing all incoming requests (in a real life scenario, button 
 * presses), fulfilling them as efficiently as possible and updating 
 * object states as required. Lift and Request objects neither know 
 * anything about each another nor do they call any methods apart from 
 * their own - they are controlled by the Dispatcher alone.
 *
 * @author	Tan Ting Yu
 * @version	1.0 (15 September 2018)
 */
public class Dispatcher extends Thread {

    public int numFloors;
    public int numLifts;
    public AtomicBoolean isDispatching;

    Map < Integer, Map < Direction, Request >> floors;
    List < Lift > lifts;

    public Dispatcher(int numFloors, int numLifts) {
        this.floors = new ConcurrentHashMap < > ();
        this.lifts = new LinkedList < > ();
        this.isDispatching = new AtomicBoolean();

        this.numLifts = numLifts;
        this.numFloors = numFloors;

        this.buildFloorsMap();
        this.buildLiftsList();
    }

    /**
     * Continually checks the floors map for pending requests in both 
     * the up and down directions, dispatching lifts accordingly.
     */
    @Override public void run() {
        for (Lift lift: this.lifts) {
            lift.start();
        }

        while (true) {
            List < Request > up = this.getPendingRequests(Direction.UP);
            List < Request > down = this.getPendingRequests(Direction.DOWN);

            if (!up.isEmpty()) {
                for (Request request: up) {
                    this.dispatchLift(request);
                }
            }

            if (!down.isEmpty()) {
                for (Request request: down) {
                    this.dispatchLift(request);
                }
            }
        }
    }

    /**
     * Button presses (up/down to call a lift, destination floor) should 
     * trigger this method. Adds the new destinations to the corresponding 
     * request object in the map.
     *
     * @param	floor - request's floor of origin
     * @param	direction - direction of request, either Direction.UP or 
     * 			Direction.Down
     * @param	destinations - a list of floors to stop at (if going up: 
     * 			values must be greater than the floor of origin, but not 
     * 			exceeding the topmost floor; if going down: values must 
     * 			be smaller than the floor of origin, and cannot be less 
     * 			than 0)
     */
    public void handleRequest(int floor, Direction direction,
                              List < Integer > destinations) {
        Request request = this.floors.get(floor).get(direction);

        for (int destination: destinations) {
            //	for simulation purposes, in reality this check is not 
            //	required - you can't press an already-selected button
            if (!request.destinations.contains(destination))
                request.destinations.add(destination);
        }

        request.setStatus(Status.AWAITING);
        request.print();
    }

    public void print() {
        System.out.println(
            "\nDispatcher -" +
            "\n\tNo. of Floors : " + this.numFloors +
            "\n\tNo. of Lifts : " + this.numLifts
        );
    }

    /**
     * Called on initialization, populating the floors map with requests
     * according to the number of floors. The outer key (Integer) is 
     * the floor number (starts from zero), and its value is a map, 
     * which has two keys, Direction.UP and Direction.DOWN. It stores a 
     * Request object as its value, that is persistent for as long as 
     * the program is running.
     */
    private void buildFloorsMap() {
        for (int i = 0; i < this.numFloors; i++) {
            Map < Direction, Request > floor = new ConcurrentHashMap < > ();
            Request up = new Request(i, Direction.UP);
            Request down = new Request(i, Direction.DOWN);

            // is bottom floor, can't go down
            if (i == 0) {
                floor.put(Direction.UP, up);
            // is top floor, can't go up
            } else if (i == numFloors - 1) {
                floor.put(Direction.DOWN, down);
            // both up and down
            } else {
                floor.put(Direction.UP, up);
                floor.put(Direction.DOWN, down);
            }

            this.floors.put(i, floor);
        }
    }

    /**
     * Called on initialization, populating the lifts list with lifts
     * according to the number of lifts. Each Lift object is persistent
     * for as long as the program is running.
     */
    private void buildLiftsList() {
        for (int i = 0; i < this.numLifts; i++) {
            int start =  i * (this.numFloors / this.numLifts);
            Lift lift = new Lift(i, start, this);
            this.lifts.add(lift);
        }
    }

    /**
     * Extracts all pending requests in the specified direction.
     *
     * @param   direction - to search, either Direction.UP or
     * 			Direction.DOWN
     * @return  a list of pending requests, empty if there are none
     */
    private List < Request > getPendingRequests(Direction direction) {
        List < Request > requests = new LinkedList < > ();
        int start = 0;
        int end = this.floors.size() - 1;

        if (direction == Direction.DOWN) {
            start = 1;
            end = this.floors.size();
        }

        for (int i = start; i < end; i++) {
            Map < Direction, Request > floor = this.floors.get(i);
            Request request = floor.get(direction);

            if (request.getStatus().equals(Status.AWAITING))
                requests.add(request);
        }

        return requests;
    }

    /**
     * Dispatches the nearest available lift to the given request. The 
     * request is reset once the lift has obtained all the necessary 
     * information from it (i.e. destinations and direction).
     *
     * @param	request - a pending request to be fulfilled
     */
    private void dispatchLift(Request request) {
        // informs lifts that it is assigning a new request, so that
        // they will not proceed until the process is complete,
        // preventing possible race conditions
        this.isDispatching.set(true);

        Lift lift = getNearestAvailableLift(request.floor,
                request.direction);
        List < Integer > destinations = lift.destinations;

        for (int destination : request.destinations) {
            if (!destinations.contains(destination))
                destinations.add(destination);
        }

        Collections.sort(destinations);
        if (request.direction == Direction.DOWN)
            Collections.reverse(destinations);
        request.reset();

        lift.destinations = destinations;
        lift.interrupt();

        this.isDispatching.set(false);
    }

    /**
     * From a list of available lifts, select the nearest lift. If there 
     * are several, one is picked randomly, and if there are none, try
     * again.
     *
     * @param	floor - request's floor of origin
     * @param	direction - request's direction, either Direction.UP or
     * 			Direction.DOWN
     * @return	a nearest available lift to the given floor in the given
     * 			direction
     */
    private Lift getNearestAvailableLift(int floor, Direction direction) {
        while (true) {
            List < Lift > available =
                    this.getAvailableLifts(floor, direction);
            int closestDistance =
                    this.getClosestDistance(available,floor);

            List < Lift > nearestAvailable = new LinkedList < > ();
            for (Lift lift: available) {
                int distance = Math.abs(lift.getFloor() - floor);
                if (distance == closestDistance)
                    nearestAvailable.add(lift);
            }

            if (!nearestAvailable.isEmpty())
                return nearestAvailable.get(new Random()
                        .nextInt(nearestAvailable.size()));
        }
    }

    /**
     * Finds available lifts. A lift that has stopped at a floor to let 
     * off its passengers is considered available even though it still 
     * has floors to visit - it can be assigned the request if the new 
     * destinations are along the way.
     *
     * @param 	floor - request's floor of origin
     * @param 	direction - request's direction, either Direction.UP or
     * 			Direction.DOWN
     * @return	a list of available lifts
     */
    private List < Lift > getAvailableLifts(int floor, Direction direction) {
        List < Lift > availableLifts = new LinkedList < > ();
        for (Lift lift: this.lifts) {
            if (lift.getStatus().equals(Status.AVAILABLE)) {
                // lift is not in use
                if (lift.getDirection() == Direction.NONE)
                    availableLifts.add(lift);

                // lift still has floors to visit
                if (lift.getDirection() == direction) {
                    if (direction == Direction.UP) {
                        // on the way up
                        if (floor >= lift.getFloor())
                            availableLifts.add(lift);
                    } else {
                        // on the way down
                        if (floor <= lift.getFloor())
                            availableLifts.add(lift);
                    }
                }
            }
        }

        return availableLifts;
    }

    /**
     * From the a list of available lifts, finds the distance between the
     * closest lift and the given lift.
     *
     * @param	lifts - a list of available lifts
     * @param	floor - floor of reference
     * @return	distance between the closest available lift and the given
     * 			floor
     */
    private int getClosestDistance(List < Lift > lifts, int floor) {
        int closestDistance = numFloors;
        for (Lift lift: lifts) {
            int distance = Math.abs(lift.getFloor() - floor);
            if (distance < closestDistance)
                closestDistance = distance;
        }

        return closestDistance;
    }
}