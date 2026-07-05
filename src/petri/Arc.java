package petri;

/**
 * An immutable representation of an arc connecting a {@link Place} and a
 * {@link Transition} in a Petri net, as parsed from the net's XML
 * definition.
 *
 * <p>An arc is directional: an input arc runs from a place to a transition
 * (consuming tokens when the transition fires), while an output arc runs
 * from a transition to a place (producing tokens when the transition
 * fires).
 */
public class Arc {
    private final String id;
    private final Place place;
    private final Transition transition;
    private final boolean isInput;
    private final int weight;

    /**
     * Creates a new arc.
     *
     * @param id         the arc's XML identifier
     * @param place      the place this arc connects to
     * @param transition the transition this arc connects to
     * @param isInput    {@code true} if this is an input arc (place to
     *                   transition), {@code false} if it is an output arc
     *                   (transition to place)
     * @param weight     the number of tokens consumed/produced when the
     *                   connected transition fires
     */
    public Arc(String id, Place place, Transition transition, boolean isInput, int weight) {
        this.id = id;
        this.place = place;
        this.transition = transition;
        this.isInput = isInput;
        this.weight = weight;
    }

    /**
     * @return the arc's XML identifier
     */
    public String getId() {
        return id;
    }

    /**
     * @return the place this arc connects to
     */
    public Place getPlace() {
        return place;
    }

    /**
     * @return the transition this arc connects to
     */
    public Transition getTransition() {
        return transition;
    }

    /**
     * @return {@code true} if this is an input arc (place to transition),
     *         {@code false} if it is an output arc (transition to place)
     */
    public boolean isInput() {
        return isInput;
    }

    /**
     * @return the number of tokens consumed/produced when the connected
     *         transition fires
     */
    public int getWeight() {
        return weight;
    }
}