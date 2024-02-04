package petrinet;

import java.util.List;

/**
 * This class represents a Concurrency Monitor for a Petri Net. It provides
 * methods to check if transitions are enabled and to execute transitions.
 *
 * @author Loretta
 * @since 2023-06-28
 */
 @SuppressWarnings("unused")
public class ConcurrencyMonitor implements Runnable {

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
     * This method is deprecated due to the introduction of atomicity in checking for a transition's
     * enabledness and firing it in the {@link #executeTransition(Transition)} method.
     * 
     * As per the principles of Petri nets, these two operations should be performed together
     * without the possibility of another thread or process intervening in between.
     * 
     * Previously, this method returned a list of all transitions that are currently enabled in the
     * Petri Net. A transition is considered enabled if all input places have enough tokens.
     * 
     * @deprecated since 2024-02-04
     * @see ConcurrencyMonitor#executeTransition(Transition)
     */
    @Deprecated
    private List<Transition> getEnabledTransitions() {
        throw new UnsupportedOperationException("This method is deprecated");
    }

    /**
     * This method is deprecated due to the introduction of atomicity in checking for a transition's
     * enabledness and firing it in the {@link #executeTransition(Transition)} method.
     * 
     * Therefore, the logic from this method has been moved to the
     * {@link #executeTransition(Transition)} method to ensure atomicity.
     * 
     * This method was used internally by the {@link #getEnabledTransitions()} method
     * 
     * @deprecated since 2024-02-04
     * @see ConcurrencyMonitor#executeTransition(Transition)
     */
    @Deprecated
    private boolean areInputPlacesReady(Transition transition) {
        throw new UnsupportedOperationException("This method is deprecated");
    }

    /**
     * Executes a given transition in the Petri Net. This involves removing tokens from input places
     * and adding tokens to output places.
     *
     * @param transition The transition to execute.
     * @throws InterruptedException
     */
    public void executeTransition(Transition transition) {
        try {
            synchronized (transition) {

                boolean areInputPlacesReady = transition.getInputArcs().stream()
                .allMatch(arc -> arc.getPlace().getTokens() >= arc.getWeight());

                if(!areInputPlacesReady) {
                    return;
                }

                transition.getInputArcs()
                .forEach(arc -> arc.getPlace().removeTokens(arc.getWeight()));

                if (transition.isTimed()) {
                    Thread.sleep(transition.getFiringRate());
                }

                transition.getOutputArcs()
                .forEach(arc -> arc.getPlace().addTokens(arc.getWeight()));
            }
        } catch (IllegalArgumentException | InterruptedException e) {
            System.err.println("Error executing transition: " + e.getMessage());
        }
    }

    @Override
    public void run() {
    }

}