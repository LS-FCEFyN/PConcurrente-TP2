package petrinet;

/**
 * Represents an Arc in a Petri Net.
 *
 * <p>
 * An Arc in a Petri Net is a directed edge that connects a Place to a
 * Transition or vice versa. Each Arc has an ID, an associated Place, an
 * associated Transition, an input status, and a weight.
 * </p>
 *
 * @author Loretta
 * @since 2023-06-28
 */

public class Arc {
    private String id;
    private Place place;
    private Transition transition;
    private boolean isInput;
    private int weight;

    /**
     * Constructs an Arc with the specified ID, Place, Transition, input status, and weight.
     *
     * @param id The ID of the Arc.
     * @param place The Place associated with the Arc.
     * @param transition The Transition associated with the Arc.
     * @param isOutput Whether the Arc is an output Arc or not. i.e. is this arc flowing from a
     *        place to a transition (it's an output arc so it's value is true) or the arc is flowing
     *        from a transition to a place
     * @param weight The weight of the Arc.
     */
    public Arc(String id, Place place, Transition transition, boolean isInput, int weight) {
        this.id = id;
        this.place = place;
        this.transition = transition;
        this.isInput = isInput;
        this.weight = weight;
    }

    /**
     * Returns the ID of the Arc.
     *
     * @return String The ID of the Arc.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the Place associated with the Arc.
     *
     * @return Place The Place associated with the Arc.
     */
    public Place getPlace() {
        return place;
    }

    /**
     * Returns the Transition associated with the Arc.
     *
     * @return Transition The Transition associated with the Arc.
     */
    public Transition getTransition() {
        return transition;
    }

    /**
     * Returns whether the Arc is an input Arc to a transition (i.e. an input arc) or an input arc
     * to a place (i.e. an output arc).
     *
     * @return boolean True if the Arc is an input Arc to a transition, false otherwise.
     */
    public boolean isInput() {
        return isInput;
    }

    /**
     * Returns the weight of the Arc.
     *
     * @return int The weight of the Arc.
     */
    public int getWeight() {
        return weight;
    }

    @Override
    public String toString() {
        return "ID: " + getId() + ", Place: " + getPlace().getId() + ", Transition: "
                + this.getTransition().getId() + ", Is Input: " + isInput() + ", Weight: "
                + getWeight() + "\n";
    }
}
