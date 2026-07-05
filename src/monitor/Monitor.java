package monitor;

import java.util.List;

import petri.ConcurrentMatrixPetriNet;
import policies.Policy;

/**
 * A monitor (in the concurrent-programming sense) that guards a
 * {@link ConcurrentMatrixPetriNet} so that transitions are fired under
 * mutual exclusion, according to a configurable {@link Policy}.
 *
 * <p>All access to the underlying net's marking during a firing attempt is
 * synchronized on the net instance itself, ensuring that concurrent callers
 * cannot interleave the enabled-check, policy check, and token
 * consumption/production steps of a single transition firing.
 */
public class Monitor implements MonitorInterface {
    private final ConcurrentMatrixPetriNet petriNet;
    private final Policy policy;

    /**
     * Creates a monitor for {@code petriNet} using the default {@link Policy}.
     *
     * @param petriNet the Petri net to guard
     */
    public Monitor(ConcurrentMatrixPetriNet petriNet) {
        this(petriNet, new Policy());
    }

    /**
     * Creates a monitor for {@code petriNet} using the given firing policy.
     *
     * @param petriNet the Petri net to guard
     * @param policy   the firing policy to apply; if {@code null}, the
     *                 default {@link Policy} is used instead
     */
    public Monitor(ConcurrentMatrixPetriNet petriNet, Policy policy) {
        this.petriNet = petriNet;
        this.policy = policy == null ? new Policy() : policy;
    }

    /**
     * Attempts to fire a transition on the guarded Petri net under mutual
     * exclusion.
     *
     * <p>If {@code transitionIndex} is negative, the configured
     * {@link Policy} selects which enabled transition to fire. Otherwise,
     * the requested transition is fired only if it is currently enabled and
     * the policy allows it. On success, tokens are atomically consumed from
     * each input place and produced into each output place according to the
     * net's pre/post weight matrices.
     *
     * @param transitionIndex the index of the transition to fire, or a
     *                        negative value to let the policy choose
     * @return {@code true} if a transition was fired, {@code false} if no
     *         transition was enabled, the requested transition was not
     *         enabled, or the policy disallowed it
     */
    @Override
    public boolean fireTransition(int transitionIndex) {
        synchronized (petriNet) {
            List<Integer> enabled = petriNet.getEnabledTransitions();
            if (enabled.isEmpty()) {
                return false;
            }

            int chosen = transitionIndex;
            if (chosen < 0) {
                chosen = policy.selectTransition(petriNet).orElse(enabled.get(0));
            }
            if (!enabled.contains(chosen)) {
                return false;
            }
            if (!policy.canFire(chosen, petriNet, enabled)) {
                return false;
            }

            int places = petriNet.getPlaceCount();
            for (int placeId = 0; placeId < places; placeId++) {
                int consume = petriNet.getPreWeight(placeId, chosen);
                if (consume > 0 && petriNet.getMarkingSnapshot()[placeId] < consume) {
                    return false;
                }
            }
            for (int placeId = 0; placeId < places; placeId++) {
                int consume = petriNet.getPreWeight(placeId, chosen);
                int produce = petriNet.getPostWeight(placeId, chosen);
                if (consume > 0) {
                    petriNet.addTokens(placeId, -consume);
                }
                if (produce > 0) {
                    petriNet.addTokens(placeId, produce);
                }
            }
            return true;
        }
    }
}