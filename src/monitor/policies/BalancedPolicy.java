package monitor.policies;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import petrinet.Transition;

public class BalancedPolicy extends TransitionPolicy {

    private final Map<Transition, Integer> transitionCounts = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public Transition which(Object enabledTransitions) {
        Transition selectedTransition = null;
        int minCount = Integer.MAX_VALUE;

        for (Transition transition : (List<Transition>) enabledTransitions) {
            int count = transitionCounts.getOrDefault(transition, 0);

            if (count < minCount) {
                minCount = count;
                selectedTransition = transition;
            }
        }

        if (selectedTransition != null) {
            transitionCounts.put(selectedTransition, minCount + 1);
        }
        return selectedTransition;
    }


    @Override
    public void accept(PolicyVisitor visitor) {
        visitor.visit(this);
    }
}
