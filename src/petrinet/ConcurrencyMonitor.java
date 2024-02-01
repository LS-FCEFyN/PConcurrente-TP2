package petrinet;

import java.util.List;
import java.util.stream.Collectors;

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
        return petriNet.getTransitions().stream()
        .filter(this::areInputPlacesReady)
        .collect(Collectors.toList());
    }

    /**
     * Checks if all input places for a given transition are ready.
     * A place is considered ready if it has enough tokens.
     *
     * @param transition The transition to check.
     * @return True if all input places are ready, false otherwise.
     */
    private boolean areInputPlacesReady(Transition transition) {
        return petriNet.getAllInputArcs().stream()
        .anyMatch(arc -> arc.getTransition()
        .equals(transition) && arc.getPlace().getTokens() >= arc.getWeight());
    }

    /**
     * Executes a given transition in the Petri Net.
     * This involves removing tokens from input places and adding tokens to output
     * places.
     *
     * @param transition The transition to execute.
     * @throws InterruptedException 
     */
    public synchronized void executeTransition(Transition transition) 
    throws IllegalArgumentException, InterruptedException {
        synchronized (transition) {
            List<Arc> arcs = petriNet.getAllInputArcs();

            arcs.stream().filter(arc -> arc.getTransition().equals(transition))
            .forEach(arc -> arc.getPlace().removeTokens(arc.getWeight()));
        

        if (transition.isTimed()) {
            Thread.sleep(transition.getFiringRate());
        }

        arcs = petriNet.getAllOutputArcs();

        arcs.stream().filter(arc -> arc.getTransition().equals(transition))
        .forEach(arc -> arc.getPlace().addTokens(arc.getWeight()));
    }
}

}