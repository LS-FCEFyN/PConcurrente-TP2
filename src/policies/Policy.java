package policies;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import petri.ConcurrentMatrixPetriNet;

public class Policy {

	public enum Strategy {
		SIMPLE_PRIORITY,
		RANDOM_COMPLEXITY
	}

	private enum Complexity {
		SIMPLE,
		MEDIUM,
		HIGH
	}

	private final Strategy strategy;
	private final SimpleProcessingComplexityInterface simpleStrategy;
	private final RandomProcessingComplexityInterface randomStrategy;

	public Policy() {
		this(Strategy.SIMPLE_PRIORITY);
	}

	public Policy(Strategy strategy) {
		this.strategy = strategy;
		this.simpleStrategy = new SimpleProcessingComplexity();
		this.randomStrategy = new RandomProcessingComplexity();
	}

	public Strategy getStrategy() {
		return strategy;
	}

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

	private static final class SimpleProcessingComplexity implements SimpleProcessingComplexityInterface {
		@Override
		public OptionalInt select(List<Integer> enabledTransitions, boolean[] timedTransitions) {
			if (enabledTransitions.contains(5)) {
				return OptionalInt.of(5);
			}
			if (enabledTransitions.contains(6)) {
				return OptionalInt.of(6);
			}
			return enabledTransitions.isEmpty() ? OptionalInt.empty() : OptionalInt.of(enabledTransitions.get(0));
		}

		@Override
		public boolean isAllowed(int transitionIndex, List<Integer> enabledTransitions, boolean[] timedTransitions) {
			if (!enabledTransitions.contains(transitionIndex)) {
				return false;
			}
			if (enabledTransitions.contains(5) && transitionIndex != 5) {
				return false;
			}
			if (enabledTransitions.contains(6) && transitionIndex != 6 && !enabledTransitions.contains(5)) {
				return false;
			}
			return true;
		}
	}

	private static final class RandomProcessingComplexity implements RandomProcessingComplexityInterface {
		private static final int[] SIMPLE_TRANSITIONS = { 5, 6 };
		private static final int[] MEDIUM_TRANSITIONS = { 2, 3, 4 };
		private static final int[] HIGH_TRANSITIONS = { 7, 8, 9, 10 };

		@Override
		public OptionalInt select(List<Integer> enabledTransitions, int[] markingSnapshot, boolean[] timedTransitions) {
			List<Integer> candidates = candidatesForComplexity(enabledTransitions, detectComplexity(markingSnapshot));
			if (candidates.isEmpty()) {
				return OptionalInt.empty();
			}
			int choice = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
			return OptionalInt.of(choice);
		}

		@Override
		public boolean isAllowed(int transitionIndex, List<Integer> enabledTransitions, int[] markingSnapshot,
				boolean[] timedTransitions) {
			if (!enabledTransitions.contains(transitionIndex)) {
				return false;
			}
			return candidatesForComplexity(enabledTransitions, detectComplexity(markingSnapshot)).contains(transitionIndex);
		}

		private List<Integer> candidatesForComplexity(List<Integer> enabledTransitions, Complexity complexity) {
			Set<Integer> target = switch (complexity) {
				case SIMPLE -> toSet(SIMPLE_TRANSITIONS);
				case MEDIUM -> toSet(MEDIUM_TRANSITIONS);
				case HIGH -> toSet(HIGH_TRANSITIONS);
			};

			List<Integer> filtered = enabledTransitions.stream()
					.filter(target::contains)
					.collect(Collectors.toList());

			return filtered.isEmpty() ? enabledTransitions : filtered;
		}

		private Complexity detectComplexity(int[] markingSnapshot) {
			if (hasTokens(markingSnapshot, 7)) {
				return Complexity.SIMPLE;
			}
			if (hasTokens(markingSnapshot, 4) || hasTokens(markingSnapshot, 5)) {
				return Complexity.MEDIUM;
			}
			if (hasTokens(markingSnapshot, 8) || hasTokens(markingSnapshot, 9) || hasTokens(markingSnapshot, 10)) {
				return Complexity.HIGH;
			}
			return Complexity.SIMPLE;
		}

		private boolean hasTokens(int[] markingSnapshot, int placeIndex) {
			return placeIndex < markingSnapshot.length && markingSnapshot[placeIndex] > 0;
		}

		private Set<Integer> toSet(int[] values) {
			return Arrays.stream(values)
					.boxed()
					.collect(Collectors.toCollection(LinkedHashSet::new));
		}
	}
}
