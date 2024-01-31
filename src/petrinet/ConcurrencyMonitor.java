package petrinet;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a Concurrency Monitor for a Petri Net.
 * It provides methods to check if transitions are enabled and to execute
 * transitions.
 * 
 * @author Loretta
 * @since 2023-06-28
 */

public class ConcurrencyMonitor {

    /**
     * The Petri Net associated with this Concurrency Monitor.
     */
    private PetriNet petriNet;

    /**
     * Constructs a new Concurrency Monitor for the given Petri Net.
     *
     * @param petriNet The Petri Net to associate with this Concurrency Monitor.
     */
    public ConcurrencyMonitor(PetriNet petriNet) {
        this.petriNet = petriNet;
    }

    /**
     * Returns a list of all transitions that are currently enabled in the Petri
     * Net.
     * A transition is considered enabled if all input places have enough tokens.
     *
     * @return A list of all enabled transitions.
     */
    public List<Transition> getEnabledTransitions() {
        List<Transition> enabledTransitions = new ArrayList<>();
        for (Transition transition : petriNet.getTransitions()) {
            if (areInputPlacesReady(transition)) {
                enabledTransitions.add(transition);
            }
        }
        return enabledTransitions;
    }

    /**
     * Checks if all input places for a given transition are ready.
     * A place is considered ready if it has enough tokens.
     *
     * @param transition The transition to check.
     * @return True if all input places are ready, false otherwise.
     */
    private boolean areInputPlacesReady(Transition transition) {
        List<Arc> arcs = petriNet.getAllInputArcs();
        for (Arc arc : arcs) {
            if (arc.getTransition().equals(transition)) {
                Place place = arc.getPlace();
                if (place.getTokens() >= arc.getWeight()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Executes a given transition in the Petri Net.
     * This involves removing tokens from input places and adding tokens to output
     * places.
     *
     * @param transition The transition to execute.
     */
    public void executeTransition(Transition transition) {
        List<Arc> arcs = petriNet.getAllInputArcs();

        for (Arc arc : arcs) {
            if (arc.getTransition().equals(transition)) {
                Place place = arc.getPlace();
                place.removeTokens(arc.getWeight());
            }
        }

        arcs = petriNet.getAllOutputArcs();

        for (Arc arc : arcs) {
            if (arc.getTransition().equals(transition)) {
                Place place = arc.getPlace();
                place.addTokens(arc.getWeight());
            }
        }
    }

}