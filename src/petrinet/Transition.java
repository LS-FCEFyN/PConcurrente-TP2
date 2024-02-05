package petrinet;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

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
    private Map<Arc, Boolean> arcs;

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

    /**
     * Sets the firing rate of the Transition.
     *
     * @param firingRate The firing rate of the Transition.
     */
    public void setFiringRate(int firingRate) {
        this.firingRate = firingRate;
    }

    /**
     * Retrieves the firing rate of the Transition.
     *
     * @return int The firing rate of the Transition.
     */
    public int getFiringRate() {
        return firingRate;
    }

    /**
     * Sets whether the Transition is timed.
     *
     * @param isTimed Whether the Transition is timed.
     */
    public void setIsTimed(boolean isTimed) {
        this.isTimed = isTimed;
    }

    /**
     * Checks if the Transition is timed.
     *
     * @return boolean Whether the Transition is timed.
     */
    public boolean isTimed() {
        return isTimed;
    }

    /**
     * Sets the arcs of the Transition.
     *
     * @param transitionArcs The arcs of the Transition.
     */
    public void setArcs(Map<Arc, Boolean> transitionArcs) {
        this.arcs = transitionArcs;
    }

    /**
     * Retrieves the input arcs of the Transition.
     *
     * @return Collection<Arc> The input arcs of the Transition.
     */
    public Collection<Arc> getInputArcs() {
        return arcs.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves the output arcs of the Transition.
     *
     * @return Collection<Arc> The output arcs of the Transition.
     */
    public Collection<Arc> getOutputArcs() {
        return arcs.entrySet().stream().filter(entry -> !entry.getValue()).map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "ID: " + getId() + ", Is Timed: " + isTimed() + ", Firing Rate: " + getFiringRate() + "\n";
    }
}
