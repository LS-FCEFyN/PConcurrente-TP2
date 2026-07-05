package policies;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import petri.ConcurrentMatrixPetriNet;

/**
 * Resolves the net's structural conflicts when firing transitions,
 * according to a configurable {@link Strategy}.
 *
 * <p>The net modeled here has a single structural conflict: the choice of
 * which processing-complexity chain (simple, medium, or high) consumes the
 * shared resources represented by places P3 (data waiting) and P6
 * (processing unit). This choice is made among transitions T2 (start of
 * medium), T5 (start of simple), and T7 (start of high) - see
 * {@link #COMPLEXITY_CHOICE}. Every other transition in the net is
 * uncontested and always allowed to fire once enabled.
 */
public class Policy {

	/** The available conflict-resolution strategies for the T2/T5/T7 choice. */
	public enum Strategy {
		/** Prioritizes the simple-complexity chain (T5) whenever it is enabled. */
		SIMPLE_PRIORITY,
		/** Picks uniformly at random among whichever of T2/T5/T7 are enabled. */
		RANDOM_COMPLEXITY
	}

	// The net's only structural conflict: choosing which processing-complexity chain
	// consumes the shared resources P3 (data waiting) + P6 (processing unit).
	// T2 = start of medium, T5 = start of simple, T7 = start of high.
	private static final Set<Integer> COMPLEXITY_CHOICE = Set.of(2, 5, 7);

	private final Strategy strategy;
	private final SimpleProcessingComplexityInterface simpleStrategy;
	private final RandomProcessingComplexityInterface randomStrategy;

	/** Creates a policy using {@link Strategy#SIMPLE_PRIORITY}. */
	public Policy() {
		this(Strategy.SIMPLE_PRIORITY);
	}

	/**
	 * Creates a policy using the given conflict-resolution strategy.
	 *
	 * @param strategy the strategy to use for the T2/T5/T7 conflict
	 */
	public Policy(Strategy strategy) {
		this.strategy = strategy;
		this.simpleStrategy = new SimpleProcessingComplexity();
		this.randomStrategy = new RandomProcessingComplexity();
	}

	/**
	 * @return the strategy this policy was configured with
	 */
	public Strategy getStrategy() {
		return strategy;
	}

	/**
	 * Checks whether the given transition is allowed to fire right now,
	 * according to this policy's strategy.
	 *
	 * @param transitionIndex    the transition being considered for firing
	 * @param net                the Petri net the transition belongs to
	 * @param enabledTransitions the transitions currently enabled in the net
	 * @return {@code true} if {@code transitionIndex} is enabled and the
	 *         configured strategy allows it to fire
	 */
	public boolean canFire(int transitionIndex, ConcurrentMatrixPetriNet net, List<Integer> enabledTransitions) {
		if (!enabledTransitions.contains(transitionIndex)) {
			return false;
		}

		return switch (strategy) {
			case SIMPLE_PRIORITY -> simpleStrategy.isAllowed(transitionIndex, enabledTransitions, net.getTimedTransitions());
			case RANDOM_COMPLEXITY -> randomStrategy.isAllowed(transitionIndex, enabledTransitions, net.getMarkingSnapshot(),
					net.getTimedTransitions());
		};
	}

	/**
	 * Selects which enabled transition to fire next, according to this
	 * policy's strategy.
	 *
	 * @param net the Petri net to select a transition from
	 * @return the chosen transition's index, or empty if no transition is
	 *         currently enabled
	 */
	public OptionalInt selectTransition(ConcurrentMatrixPetriNet net) {
		List<Integer> enabledTransitions = net.getEnabledTransitions();
		if (enabledTransitions.isEmpty()) {
			return OptionalInt.empty();
		}

		return switch (strategy) {
			case SIMPLE_PRIORITY -> simpleStrategy.select(enabledTransitions, net.getTimedTransitions());
			case RANDOM_COMPLEXITY -> randomStrategy.select(enabledTransitions, net.getMarkingSnapshot(),
					net.getTimedTransitions());
		};
	}

	/**
	 * Priority policy: whenever the simple-mode start (T5) is enabled, it wins the
	 * three-way conflict against T2 (medium) and T7 (high). Everything else in the
	 * net is uncontested and must not be blocked by this rule.
	 */
	private static final class SimpleProcessingComplexity implements SimpleProcessingComplexityInterface {
		private static final int SIMPLE_START = 5; // T5: entry to the simple-complexity chain

		/**
		 * Selects T5 (simple-complexity chain) whenever it is enabled;
		 * otherwise selects the first enabled transition.
		 *
		 * @param enabledTransitions the transitions currently enabled
		 * @param timedTransitions   per-transition timed flags (unused by
		 *                           this strategy)
		 * @return the selected transition's index, or empty if none are
		 *         enabled
		 */
		@Override
		public OptionalInt select(List<Integer> enabledTransitions, boolean[] timedTransitions) {
			if (enabledTransitions.isEmpty()) {
				return OptionalInt.empty();
			}
			if (enabledTransitions.contains(SIMPLE_START)) {
				return OptionalInt.of(SIMPLE_START);
			}
			return OptionalInt.of(enabledTransitions.get(0));
		}

		/**
		 * Allows any uncontested transition to fire freely; among the
		 * contested T2/T5/T7 choice, allows only T5 when T5 is enabled, and
		 * allows both T2 and T7 otherwise.
		 *
		 * @param transitionIndex    the transition being considered for firing
		 * @param enabledTransitions the transitions currently enabled
		 * @param timedTransitions   per-transition timed flags (unused by
		 *                           this strategy)
		 * @return {@code true} if {@code transitionIndex} is allowed to fire
		 */
		@Override
		public boolean isAllowed(int transitionIndex, List<Integer> enabledTransitions, boolean[] timedTransitions) {
			if (!enabledTransitions.contains(transitionIndex)) {
				return false;
			}
			// Only the T2/T5/T7 choice is contested; every other transition fires freely,
			// regardless of whether T5 happens to be enabled at the same time.
			if (!COMPLEXITY_CHOICE.contains(transitionIndex)) {
				return true;
			}
			// Simple mode wins the conflict whenever it's available.
			if (enabledTransitions.contains(SIMPLE_START)) {
				return transitionIndex == SIMPLE_START;
			}
			// Simple isn't available right now: medium/high are free, no ordering required between them.
			return true;
		}
	}

	/**
	 * Random policy: the three-way conflict (T2/T5/T7) is resolved with a uniform
	 * random pick among whichever of them are currently enabled. Every other
	 * transition is uncontested and must not be biased by this rule -- in
	 * particular, continuation transitions (T3,T4,T6,T8,T9,T10) are each the only
	 * possible next step once a chain has started (the P6 mutex guarantees at most
	 * one processing chain is ever in flight), so there is nothing to "detect" or
	 * randomize about them.
	 */
	private static final class RandomProcessingComplexity implements RandomProcessingComplexityInterface {

		/**
		 * Selects uniformly at random among the contested T2/T5/T7
		 * transitions if two or more of them are enabled; otherwise selects
		 * uniformly at random among all enabled transitions.
		 *
		 * @param enabledTransitions the transitions currently enabled
		 * @param markingSnapshot    the net's current marking (unused by
		 *                           this strategy)
		 * @param timedTransitions   per-transition timed flags (unused by
		 *                           this strategy)
		 * @return the selected transition's index, or empty if none are
		 *         enabled
		 */
		@Override
		public OptionalInt select(List<Integer> enabledTransitions, int[] markingSnapshot, boolean[] timedTransitions) {
			if (enabledTransitions.isEmpty()) {
				return OptionalInt.empty();
			}
			List<Integer> pool = conflictPool(enabledTransitions).orElse(enabledTransitions);
			return OptionalInt.of(pool.get(ThreadLocalRandom.current().nextInt(pool.size())));
		}

		/**
		 * Allows any uncontested transition to fire freely; among the
		 * contested T2/T5/T7 choice, allows only the transition that was
		 * randomly chosen from the currently-competing subset.
		 *
		 * @param transitionIndex    the transition being considered for firing
		 * @param enabledTransitions the transitions currently enabled
		 * @param markingSnapshot    the net's current marking (unused by
		 *                           this strategy)
		 * @param timedTransitions   per-transition timed flags (unused by
		 *                           this strategy)
		 * @return {@code true} if {@code transitionIndex} is allowed to fire
		 */
		@Override
		public boolean isAllowed(int transitionIndex, List<Integer> enabledTransitions, int[] markingSnapshot,
				boolean[] timedTransitions) {
			if (!enabledTransitions.contains(transitionIndex)) {
				return false;
			}
			if (!COMPLEXITY_CHOICE.contains(transitionIndex)) {
				return true; // uncontested transition, not part of the 3-way conflict
			}
			Optional<List<Integer>> competitors = conflictPool(enabledTransitions);
			if (competitors.isEmpty()) {
				return true; // nothing contested right now
			}
			List<Integer> pool = competitors.get();
			int pick = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
			return pick == transitionIndex;
		}

		/**
		 * Returns the enabled subset of {T2,T5,T7} only when there is an actual
		 * conflict to arbitrate (2 or 3 of them enabled at once). When at most one
		 * is enabled there is nothing to randomize.
		 *
		 * @param enabledTransitions the transitions currently enabled
		 * @return the competing subset of {@link #COMPLEXITY_CHOICE} if two
		 *         or more are enabled, otherwise empty
		 */
		private Optional<List<Integer>> conflictPool(List<Integer> enabledTransitions) {
			List<Integer> competitors = enabledTransitions.stream()
					.filter(COMPLEXITY_CHOICE::contains)
					.collect(Collectors.toList());
			return competitors.size() >= 2 ? Optional.of(competitors) : Optional.empty();
		}
	}
}