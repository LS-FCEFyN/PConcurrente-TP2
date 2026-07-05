package policies;

import java.util.List;
import java.util.OptionalInt;

/**
 * Simple processing complexity strategy prioritizes transition 5 (timed)
 * and 6 (immediate).
 */
public interface SimpleProcessingComplexityInterface {
	/**
	 * Selects the next transition to fire, prioritizing T5 (simple
	 * complexity chain) whenever it is enabled.
	 *
	 * @param enabledTransitions the transitions currently enabled
	 * @param timedTransitions   per-transition timed flags
	 * @return the selected transition's index, or empty if none are enabled
	 */
	OptionalInt select(List<Integer> enabledTransitions, boolean[] timedTransitions);

	/**
	 * Checks whether the given transition is allowed to fire under this
	 * strategy.
	 *
	 * @param transitionIndex    the transition being considered for firing
	 * @param enabledTransitions the transitions currently enabled
	 * @param timedTransitions   per-transition timed flags
	 * @return {@code true} if {@code transitionIndex} is allowed to fire
	 */
	boolean isAllowed(int transitionIndex, List<Integer> enabledTransitions, boolean[] timedTransitions);
}