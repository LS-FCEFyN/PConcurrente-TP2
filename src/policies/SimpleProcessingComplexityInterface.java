package policies;

import java.util.List;
import java.util.OptionalInt;

// Simple processing complexity strategy prioritizes transition 5 (timed) and 6 (immediate)
public interface SimpleProcessingComplexityInterface {
	OptionalInt select(List<Integer> enabledTransitions, boolean[] timedTransitions);

	boolean isAllowed(int transitionIndex, List<Integer> enabledTransitions, boolean[] timedTransitions);
}
