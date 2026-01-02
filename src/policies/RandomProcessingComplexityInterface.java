package policies;

import java.util.List;
import java.util.OptionalInt;

/*
 * Random processing complexity strategy generates a random firing vector.
 * Simple:   T5, T6 driven by P7
 * Medium:   T2, T3, T4 driven by P4, P5
 * High:     T7, T8, T9, T10 driven by P8, P9, P10
 */
public interface RandomProcessingComplexityInterface {
	OptionalInt select(List<Integer> enabledTransitions, int[] markingSnapshot, boolean[] timedTransitions);

	boolean isAllowed(int transitionIndex, List<Integer> enabledTransitions, int[] markingSnapshot,
			boolean[] timedTransitions);
}
