package petrinet;

/**
 * Represents a Transition in a Petri Net.
 *
 * <p>
 * A Transition in a Petri Net is an event that occurs, moving tokens from input
 * Places to output Places. Each Transition has an ID and may have a delay.
 * </p>
 *
 * @author Loretta
 * @since 2023-06-28
 */
public class Transition {
    private String id;
    private int firingRate;
    private boolean isTimed;

    /**
     * Constructs a Transition with the specified ID and no delay.
     *
     * @param id The ID of the Transition.
     */
    public Transition(String id) {
        this.id = id;
        this.firingRate = 0;
        this.isTimed = false;
    }

    /**
     * Returns the ID of the Transition.
     *
     * @return String The ID of the Transition.
     */
    public String getId() {
        return id;
    }

    public void setFiringRate(int firingRate) {
        this.firingRate = firingRate;
    }

    public int getFiringRate() {
        return firingRate;
    }

    public void setIsTimed(boolean isTimed) {
        this.isTimed = isTimed;
    }

    public boolean isTimed() {
        return isTimed;
    }
}
