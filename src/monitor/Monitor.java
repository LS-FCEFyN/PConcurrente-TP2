package monitor;

import java.util.List;

import petri.ConcurrentMatrixPetriNet;
import policies.Policy;

public class Monitor implements MonitorInterface {
    private final ConcurrentMatrixPetriNet petriNet;
    private final Policy policy;

    public Monitor(ConcurrentMatrixPetriNet petriNet) {
        this(petriNet, new Policy());
    }

    public Monitor(ConcurrentMatrixPetriNet petriNet, Policy policy) {
        this.petriNet = petriNet;
        this.policy = policy == null ? new Policy() : policy;
    }

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
