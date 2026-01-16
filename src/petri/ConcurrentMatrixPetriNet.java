package petri;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ConcurrentMatrixPetriNet {
    private final int[][] preMatrix;
    private final int[][] postMatrix;
    private final int[][] incidenceMatrix;
    private final boolean[] isTimedTransition;
    private final int[] firingRates;
    
    // Concurrent state
    private final AtomicIntegerArray marking; // Current tokens in each place

    private final int[] M0;

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

    public void printIncidenceMatrix() {
        Arrays.stream(this.incidenceMatrix).map(row -> Arrays.toString(row)).forEach(System.out::println);
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

    public int[][] getIncidenceMatrix() {
        return incidenceMatrix;
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
