package petrinet;

/**
 * Represents a Transition in a Petri Net.
 *
 * <p>
 * A Transition in a Petri Net is an event that occurs, moving tokens from input
 * Places to output Places.
 * Each Transition has an ID and may have a delay.
 * </p>
 *
 * @author Loretta
 * @since 2023-06-28
 */
public class Transition {
    private String id;

    /**
     * Constructs a Transition with the specified ID and no delay.
     *
     * @param id The ID of the Transition.
     */
    public Transition(String id) {
        this.id = id;
    }

    /**
     * Returns the ID of the Transition.
     *
     * @return String The ID of the Transition.
     */
    public String getId() {
        return id;
    }
}