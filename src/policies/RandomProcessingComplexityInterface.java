package policies;

import java.util.List;
import java.util.OptionalInt;

/**
 * Random processing complexity strategy generates a random firing vector.
 * Simple:   T5, T6 driven by P7
 * Medium:   T2, T3, T4 driven by P4, P5
 * High:     T7, T8, T9, T10 driven by P8, P9, P10
 */
public interface RandomProcessingComplexityInterface {
	/**
	 * Selects the next transition to fire, resolving the T2/T5/T7 conflict
	 * (if applicable) with a uniform random pick.
	 *
	 * @param enabledTransitions the transitions currently enabled
	 * @param markingSnapshot    the net's current marking
	 * @param timedTransitions   per-transition timed flags
	 * @return the selected transition's index, or empty if none are enabled
	 */
	OptionalInt select(List<Integer> enabledTransitions, int[] markingSnapshot, boolean[] timedTransitions);

	/**
	 * Checks whether the given transition is allowed to fire under this
	 * strategy.
	 *
	 * @param transitionIndex    the transition being considered for firing
	 * @param enabledTransitions the transitions currently enabled
	 * @param markingSnapshot    the net's current marking
	 * @param timedTransitions   per-transition timed flags
	 * @return {@code true} if {@code transitionIndex} is allowed to fire
	 */
	boolean isAllowed(int transitionIndex, List<Integer> enabledTransitions, int[] markingSnapshot,
			boolean[] timedTransitions);
}