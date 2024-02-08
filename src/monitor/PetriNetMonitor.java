package monitor;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import monitor.policies.BalancedPolicy;
import monitor.policies.PriorityPolicy;
import monitor.policies.RandomPolicy;
import monitor.policies.TransitionExecutor;
import monitor.policies.TransitionPolicy;
import petrinet.PetriNet;
import petrinet.Transition;

/**
 * This class represents a Concurrency Monitor for a Petri Net. It provides methods to check if
 * transitions are enabled and to execute transitions.
 *
 * @author Loretta
 * @since 2023-06-28
 */
@SuppressWarnings("unused")
public class PetriNetMonitor implements Runnable {

    /**
     * The Petri Net associated with this Concurrency Monitor.
     */
    private PetriNet petriNet;
    private TransitionPolicy policy;

    /**
     * Constructs a new Concurrency Monitor for the given Petri Net.
     *
     * @param petriNet The Petri Net to be monitored.
     */
    public PetriNetMonitor(PetriNet petriNet, TransitionPolicy policy) {
        this.petriNet = petriNet;
        this.policy = policy;
    }

    /**
     * Returns a list of all transitions that are currently enabled in the Petri Net. A transition
     * is considered enabled if all input places have enough tokens.
     *
     * @return A list of all enabled transitions.
     */
    public List<Transition> getEnabledTransitions() {
        return petriNet.getTransitions().stream()
                .filter(transition -> transition.getInputArcs().stream()
                        .allMatch(arc -> arc.getPlace().getTokens() >= arc.getWeight()))
                .collect(Collectors.toList());
    }

    @Override
    public void run() {
        List<Transition> transitions = getEnabledTransitions();
        TransitionExecutor transitionExecutor = new TransitionExecutor(transitions);
        policy.accept(transitionExecutor);
    }

}
