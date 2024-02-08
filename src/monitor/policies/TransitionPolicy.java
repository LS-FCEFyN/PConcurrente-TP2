package monitor.policies;

public abstract class TransitionPolicy {

    public TransitionPolicy() {}

    public abstract void accept(PolicyVisitor visitor);

    /**
     * Given an array of booleans, or a list of transitions, specifying the
     * transition ready to be fired, the policy decides which should be fired
     *
     * @param enabled an array of boolean containing true if the matching
     *        transition is ready to be fired
     * @return the transition to fire, either a transition object or index, or
     * -1 if none is enabled
     */
    public abstract <T> T which(Object enabled);

}
