package petri;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ConcurrentMatrixPetriNet {
    // Immutable matrix structures
    private final int[][] preMatrix; // Place -> Transition
    private final int[][] postMatrix; // Transition -> Place
    private final boolean[] isTimedTransition;
    private final int[] firingRates; // 0 means that the transition will fire as soon as it's enabled : units are in
                                     // [ms]

    // Concurrent state
    private final AtomicIntegerArray marking; // Current tokens in each place

    // Initial state
    private final int[] M0;

    // State equation
    /*
     * Where:
     * M = New marking vector
     * M0 = Initial marking vector
     * C = Incidence matrix (C = Post - Pre)
     * X = Firing count vector
     */

    /*
     * For timed transitions the state equation is extended to include timing
     * constraints,
     * but this is not represented directly in the incidence matrix.
     * Where the timing constraints ensure that transitions fire according to their
     * specified rates.
     * We can then define a firing vector X(t) where each element X_i(t) represents
     * the number of times
     * a transition i has to fire at time t. This can be constructed in a manner
     * similar to the untimed case,
     * but with the addition of a shifted dirac delta function to account for the
     * timing of each transition.
     * Thus, the state equation for timed transitions can be expressed as:
     * M(t) = M0 + C * X(t)
     * Where X(t) incorporates the timing constraints for each transition.
     * E.G., for a transition with a firing rate of r_i, the corresponding
     * element in X(t) would be influenced by a term like δ(t - k*r_i) for k = 0, 1,
     * 2, ...
     * This ensures that the transition fires at intervals defined by its firing
     * rate.
     * If we wish to assign one thread to each timed transition we can exploit the
     * fact that the firing vector
     * is linearly independent, meaning that the firing of one transition does not
     * directly affect the firing
     * of another transition in terms of the state equation. Each timed transition
     * can be managed by its own
     * thread, which independently tracks its firing schedule based on its firing
     * rate.
     * So the state equation could be decomposed into multiple independent
     * equations, one with all immediate
     * transitions and one for each timed transition.
     * Each thread would then update the marking vector M(t) based on its own firing
     * schedule,
     * without needing to coordinate with other threads for timing.
     * This approach leverages the linear independence of the firing vector to
     * simplify the management
     * of timed transitions in a concurrent environment.
     * So we would effectively have:
     * M(t) = M0 + C_immediate * X_immediate(t) + Σ (C_timed_i * X_timed_i(t))
     * Where:
     * C_immediate is the incidence matrix for immediate transitions,
     * X_immediate(t) is the firing count vector for immediate transitions,
     * C_timed_i is the incidence matrix for the i-th timed transition,
     * X_timed_i(t) is the firing count vector for the i-th timed transition.
     * It is worth noting that while this decomposition simplifies the management of
     * timed transitions,
     * it does not change the fundamental nature of the state equation, which
     * remains a linear combination
     * of the incidence matrix and the firing count vectors. And also that M0 is
     * only valid at t = 0.
     * After a transition fires, the new marking becomes the initial marking for
     * subsequent firings.
     */

    /*
     * Onto the policy management:
     * policies are nothing more than constraints on the firing of transitions.
     * Thus policies affect only the firing vector X(t) and not the incidence matrix
     * C or the initial marking
     * / current marking.
     * Meaning that a policy can be implemented as a set of rules that govern how
     * the firing vector X(t)
     * is constructed and updated over time.
     * Onto the Concurrency Monitor:
     * Similarly the concurrency monitor, is nothing more than some "Matrix A" that
     * constrains the firing vector X(t),
     * and is actually in charge of firing the transitions when allowed by the
     * policy.
     */


    public ConcurrentMatrixPetriNet(int[][] preMatrix, int[][] postMatrix,
            boolean[] isTimed, int[] firingRates, AtomicIntegerArray marking) {
        this.preMatrix = preMatrix;
        this.postMatrix = postMatrix;
        this.isTimedTransition = isTimed;
        this.firingRates = firingRates;
        this.marking = marking;
        M0 = IntStream.range(0, marking.length()).map(marking::get).toArray();
    }

    public void printIncidenceMatrix() {
        Arrays.stream(generateIncidenceMatrix()).map(row -> Arrays.toString(row)).forEach(System.out::println);
    }

    public void printInitialState() {
        System.out.println(
                Arrays.stream(M0)
                        .mapToObj(String::valueOf)
                        .collect(Collectors.joining(" ")));
    }

    public void printCurrentState() {
        System.out.println(
                IntStream.range(0, marking.length())
                        .mapToObj(i -> String.valueOf(marking.get(i)))
                        .collect(Collectors.joining(" ")));
    }

    public void printPlaceInvariants() {
    }

    public void printTransitionInvariants() {
    }

    public void printTimedTransitions() {
        System.out.println(
                IntStream.range(0, isTimedTransition.length)
                        .mapToObj(i -> String.valueOf(isTimedTransition[i]))
                        .collect(Collectors.joining(" ")));
    }

    public void printFiringRates() {
        System.out.println(
                Arrays.stream(firingRates)
                        .mapToObj(String::valueOf)
                        .collect(Collectors.joining(" ")));
    }

    public List<Integer> getEnabledTransitions() {
        return IntStream.range(0, isTimedTransition.length)
                .filter(this::isTransitionEnabled)
                .boxed()
                .collect(Collectors.toList());
    }

    public boolean[] getTimedTransitions() {
        return Arrays.copyOf(isTimedTransition, isTimedTransition.length);
    }

    public int[] getMarkingSnapshot() {
        return IntStream.range(0, marking.length()).map(marking::get).toArray();
    }

    public int getPlaceCount() {
        return preMatrix.length;
    }

    public int getTransitionCount() {
        return isTimedTransition.length;
    }

    public int getPreWeight(int placeId, int transitionId) {
        return preMatrix[placeId][transitionId];
    }

    public int getPostWeight(int placeId, int transitionId) {
        return postMatrix[placeId][transitionId];
    }

    public void addTokens(int placeId, int delta) {
        marking.addAndGet(placeId, delta);
    }

    public int[] getInitialMarking() {
        return Arrays.copyOf(M0, M0.length);
    }

    public int[][] getPreMatrix() {
        return preMatrix;
    }

    public int[][] getPostMatrix() {
        return postMatrix;
    }

    public int[][] generateIncidenceMatrix() {
        return IntStream.range(0, postMatrix.length)
                .mapToObj(i -> IntStream.range(0, postMatrix[i].length)
                        .map(j -> postMatrix[i][j] - preMatrix[i][j])
                        .toArray())
                .toArray(int[][]::new);
    }

    private boolean isTransitionEnabled(int transitionId) {
        for (int placeId = 0; placeId < preMatrix.length; placeId++) {
            int required = preMatrix[placeId][transitionId];
            if (required > 0 && marking.get(placeId) < required) {
                return false;
            }
        }
        return true;
    }
}
