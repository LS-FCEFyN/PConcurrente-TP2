package monitor.policies;

import java.util.Arrays;
import java.util.List;
import petrinet.Transition;

public class TransitionExecutor implements PolicyVisitor {
    private final List<Transition> transitions;

    public TransitionExecutor(List<Transition> transitions) {
        this.transitions = transitions;
    }

    /**
     * Executes a given transition in the Petri Net.
     *
     * @param transition The transition to execute.
     * @throws InterruptedException
     */
    public boolean executeTransition(Transition transition) {
        try {
            synchronized (transition) {

                transition.getInputArcs()
                        .forEach(arc -> arc.getPlace().removeTokens(arc.getWeight()));

                System.out.println(Thread.currentThread().getName() + " is firing transition: "
                        + transition.getId());

                if (transition.isTimed()) {
                    Thread.sleep(transition.getFiringRate());
                }

                transition.getOutputArcs()
                        .forEach(arc -> arc.getPlace().addTokens(arc.getWeight()));
            }
        } catch (IllegalArgumentException | InterruptedException e) {
            System.err.println("Error executing transition: " + e.getMessage());
        }

        return true;
    }

    @Override
    public void visit(BalancedPolicy balancedPolicy) {
        executeTransition(balancedPolicy.which(transitions));
    }

    @Override
    public void visit(PriorityPolicy priorityPolicy) {
        // Logic for executing transitions with GreedyPolicy
    }

    @Override
    public void visit(RandomPolicy randomPolicy) {
        Boolean[] enabled = new Boolean[transitions.size()];
        Arrays.setAll(enabled, index -> true);
        executeTransition(transitions.get(randomPolicy.which(enabled)));
    }
}
