package monitor.policies;

import petrinet.Transition;

public class PriorityPolicy extends TransitionPolicy {

    public PriorityPolicy() {}

    @Override
    @SuppressWarnings("unchecked")
    public Transition which(Object enabled) {
        throw new UnsupportedOperationException("Unimplemented method 'which'");
    }

    @Override
    public void accept(PolicyVisitor visitor) {
        visitor.visit(this);
    }
}
