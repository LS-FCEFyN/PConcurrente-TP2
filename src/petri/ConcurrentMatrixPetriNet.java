package petri;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A matrix-based representation of a Petri net whose current marking is
 * stored in a thread-safe manner, suitable for concurrent simulation.
 *
 * <p>The net's structure is captured by the pre-condition matrix
 * ({@code preMatrix[place][transition]}: tokens consumed from a place when
 * a transition fires) and the post-condition matrix
 * ({@code postMatrix[place][transition]}: tokens produced into a place when
 * a transition fires). The incidence matrix ({@code post - pre}) is derived
 * once at construction time. The current marking is held in an
 * {@link AtomicIntegerArray} so that concurrent transition firings can
 * safely update token counts.
 */
public class ConcurrentMatrixPetriNet {
    private final int[][] preMatrix;
    private final int[][] postMatrix;
    private final int[][] incidenceMatrix;
    private final boolean[] isTimedTransition;
    private final int[] firingRates;
    
    // Concurrent state
    private final AtomicIntegerArray marking; // Current tokens in each place

    private final int[] M0;

    /**
     * Creates a new concurrent Petri net.
     *
     * @param preMatrix  the pre-condition matrix, {@code preMatrix[place][transition]}
     *                   giving the tokens consumed from {@code place} when
     *                   {@code transition} fires
     * @param postMatrix the post-condition matrix, {@code postMatrix[place][transition]}
     *                   giving the tokens produced into {@code place} when
     *                   {@code transition} fires
     * @param isTimed    whether each transition (by index) is timed
     * @param firingRates the firing rate of each transition (by index)
     * @param marking    the initial marking (tokens per place); also
     *                   captured as the net's initial marking {@code M0}
     */
    public ConcurrentMatrixPetriNet(int[][] preMatrix, int[][] postMatrix,
            boolean[] isTimed, int[] firingRates, AtomicIntegerArray marking) {
        this.preMatrix = preMatrix;
        this.postMatrix = postMatrix;
        this.incidenceMatrix = IntStream.range(0, postMatrix.length)
                .mapToObj(i -> IntStream.range(0, postMatrix[i].length)
                        .map(j -> postMatrix[i][j] - preMatrix[i][j])
                        .toArray())
                .toArray(int[][]::new);
        this.isTimedTransition = isTimed;
        this.firingRates = firingRates;
        this.marking = marking;
        M0 = IntStream.range(0, marking.length()).map(marking::get).toArray();
    }

    /** Prints the incidence matrix ({@code post - pre}) to standard output, one row per line. */
    public void printIncidenceMatrix() {
        Arrays.stream(this.incidenceMatrix).map(row -> Arrays.toString(row)).forEach(System.out::println);
    }

    /** Prints the initial marking {@code M0} to standard output as a space-separated line. */
    public void printInitialState() {
        System.out.println(
                Arrays.stream(M0)
                        .mapToObj(String::valueOf)
                        .collect(Collectors.joining(" ")));
    }

    /** Prints the current marking to standard output as a space-separated line. */
    public void printCurrentState() {
        System.out.println(
                IntStream.range(0, marking.length())
                        .mapToObj(i -> String.valueOf(marking.get(i)))
                        .collect(Collectors.joining(" ")));
    }

    /** Prints, for each transition in order, whether it is timed, as a space-separated line. */
    public void printTimedTransitions() {
        System.out.println(
                IntStream.range(0, isTimedTransition.length)
                        .mapToObj(i -> String.valueOf(isTimedTransition[i]))
                        .collect(Collectors.joining(" ")));
    }

    /** Prints each transition's firing rate to standard output as a space-separated line. */
    public void printFiringRates() {
        System.out.println(
                Arrays.stream(firingRates)
                        .mapToObj(String::valueOf)
                        .collect(Collectors.joining(" ")));
    }

    /**
     * Computes the transitions that are currently enabled given the net's
     * present marking.
     *
     * @return the indices of every enabled transition, in ascending order
     */
    public List<Integer> getEnabledTransitions() {
        return IntStream.range(0, isTimedTransition.length)
                .filter(this::isTransitionEnabled)
                .boxed()
                .collect(Collectors.toList());
    }

    /**
     * @return a defensive copy of the per-transition timed flags
     */
    public boolean[] getTimedTransitions() {
        return Arrays.copyOf(isTimedTransition, isTimedTransition.length);
    }

    /**
     * Takes a consistent, point-in-time snapshot of the current marking.
     *
     * @return an array with the current token count of every place, indexed
     *         by place id
     */
    public int[] getMarkingSnapshot() {
        return IntStream.range(0, marking.length()).map(marking::get).toArray();
    }

    /**
     * @return the number of places in the net
     */
    public int getPlaceCount() {
        return preMatrix.length;
    }

    /**
     * @return the number of transitions in the net
     */
    public int getTransitionCount() {
        return isTimedTransition.length;
    }

    /**
     * @param placeId      the place index
     * @param transitionId the transition index
     * @return the number of tokens consumed from {@code placeId} when
     *         {@code transitionId} fires
     */
    public int getPreWeight(int placeId, int transitionId) {
        return preMatrix[placeId][transitionId];
    }

    /**
     * @param placeId      the place index
     * @param transitionId the transition index
     * @return the number of tokens produced into {@code placeId} when
     *         {@code transitionId} fires
     */
    public int getPostWeight(int placeId, int transitionId) {
        return postMatrix[placeId][transitionId];
    }

    /**
     * Atomically adds {@code delta} tokens to a place's current marking.
     *
     * @param placeId the place to modify
     * @param delta   the number of tokens to add (may be negative to remove
     *                tokens)
     */
    public void addTokens(int placeId, int delta) {
        marking.addAndGet(placeId, delta);
    }

    /**
     * @return a defensive copy of the net's initial marking {@code M0}
     */
    public int[] getInitialMarking() {
        return Arrays.copyOf(M0, M0.length);
    }

    /**
     * @return the pre-condition matrix, {@code preMatrix[place][transition]}
     */
    public int[][] getPreMatrix() {
        return preMatrix;
    }

    /**
     * @return the post-condition matrix, {@code postMatrix[place][transition]}
     */
    public int[][] getPostMatrix() {
        return postMatrix;
    }

    /**
     * @return the incidence matrix, {@code postMatrix - preMatrix}
     */
    public int[][] getIncidenceMatrix() {
        return incidenceMatrix;
    }

    /**
     * Checks whether a transition is enabled in the current marking, i.e.
     * every place feeding it holds at least as many tokens as its
     * pre-condition weight requires.
     *
     * @param transitionId the transition to check
     * @return {@code true} if the transition is currently enabled
     */
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