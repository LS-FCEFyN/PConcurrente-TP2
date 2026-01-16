package petri;

import utils.MatrixUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PetriNetAnalysis {

    public int calculateMaxActiveThreads(ConcurrentMatrixPetriNet petriNet, Set<Integer> nonActionPlaces) {
        // 1. Obtain Transition Invariants (T-Invariants)
        // We utilize the existing MatrixUtils. The incidence matrix C needs to be
        // converted to double[][].
        double[][] incidenceMatrix = getIncidenceMatrixDouble(petriNet);

        // false indicates we want the basis for the columns (Transitions), solving C*x
        // = 0
        List<double[]> tInvariantsDouble = MatrixUtils.getNaturalBasis(incidenceMatrix, false);

        // Convert doubles to integers (firing counts)
        List<int[]> tInvariants = tInvariantsDouble.stream()
                .map(this::toIntArray)
                .collect(Collectors.toList());

        if (tInvariants.isEmpty()) {
            System.out.println("No T-Invariants found. Cannot determine cyclic thread behavior.");
            return 0;
        }

        int globalMaxThreads = 0;

        // Iterate through each T-Invariant (Step 1)
        for (int[] inv : tInvariants) {
            // 2. Get the set of places associated with the current IT (Eq 4)
            Set<Integer> pi = getAssociatedPlaces(petriNet, inv);

            // 3. Determine "Action Places" (PA_i) by removing non-action places (Eq 5)
            Set<Integer> pa = new HashSet<>(pi);
            pa.removeAll(nonActionPlaces); // PA_i = PI_i - {Restrictions U Resources U Idle}

            if (pa.isEmpty()) {
                continue;
            }

            // 4. Obtain MA (Set of all reachable markings) from the Reachability Tree
            // Note: We generate the reachability graph starting from M0.
            Set<List<Integer>> reachabilityGraph = generateReachabilityGraph(petriNet);

            // 5. For each marking, sum the tokens in the PA subset and find the max
            int currentInvMax = 0;
            for (List<Integer> marking : reachabilityGraph) {
                int sum = 0;
                for (Integer placeId : pa) {
                    sum += marking.get(placeId);
                }
                if (sum > currentInvMax) {
                    currentInvMax = sum;
                }
            }

            if (currentInvMax > globalMaxThreads) {
                globalMaxThreads = currentInvMax;
            }
        }

        return globalMaxThreads;
    }

    private double[][] getIncidenceMatrixDouble(ConcurrentMatrixPetriNet petriNet) {
        return Arrays.stream(petriNet.getIncidenceMatrix()).map(
                row -> Arrays.stream(row).asDoubleStream().toArray()).toArray(double[][]::new);
    }

    /**
     * Converts a double array to an int array (rounding is used to handle floating
     * point precision).
     */
    private int[] toIntArray(double[] arr) {
        return Arrays.stream(arr).mapToInt(d -> (int) Math.round(d)).toArray();
    }

    /**
     * Implements Eq (4): PI_i = Union(Pre(t)) U Union(Post(t)) for all t in
     * Invariant.
     */
    private Set<Integer> getAssociatedPlaces(ConcurrentMatrixPetriNet pn, int[] invariant) {
        Set<Integer> places = new HashSet<>();
        int[][] pre = pn.getPreMatrix();
        int[][] post = pn.getPostMatrix();

        for (int t = 0; t < invariant.length; t++) {
            // If the transition is part of the invariant (firing count > 0)
            if (invariant[t] > 0) {
                // Add input places
                for (int p = 0; p < pre.length; p++) {
                    if (pre[p][t] > 0)
                        places.add(p);
                }
                // Add output places
                for (int p = 0; p < post.length; p++) {
                    if (post[p][t] > 0)
                        places.add(p);
                }
            }
        }
        return places;
    }

    /**
     * Generates the Reachability Set (MA) using BFS.
     * WARNING: For unbounded nets, this will run indefinitely.
     * Added a safety cap for states to prevent memory overflow.
     */
    private Set<List<Integer>> generateReachabilityGraph(ConcurrentMatrixPetriNet pn) {
        Set<List<Integer>> visited = new HashSet<>();
        Queue<int[]> queue = new LinkedList<>();

        int[] m0 = pn.getInitialMarking();
        queue.add(m0);

        // Store as List for easier Set uniqueness checks
        visited.add(IntStream.of(m0).boxed().collect(Collectors.toList()));

        int[][] pre = pn.getPreMatrix();
        int[][] post = pn.getPostMatrix();
        int numTrans = pn.getTransitionCount();
        int numPlaces = pn.getPlaceCount();

        // Safety cap for large/unbounded nets
        final int MAX_STATES = 50000;

        while (!queue.isEmpty()) {
            int[] currentM = queue.poll();

            // Try firing every transition
            for (int t = 0; t < numTrans; t++) {
                if (isEnabled(currentM, pre, t)) {
                    // Create new marking M' = M - Pre + Post
                    int[] nextM = new int[numPlaces];
                    for (int p = 0; p < numPlaces; p++) {
                        nextM[p] = currentM[p] - pre[p][t] + post[p][t];
                    }

                    List<Integer> nextMList = IntStream.of(nextM).boxed().collect(Collectors.toList());
                    if (!visited.contains(nextMList)) {
                        visited.add(nextMList);
                        queue.add(nextM);

                        if (visited.size() >= MAX_STATES) {
                            System.err.println(
                                    "Warning: Reachability graph generation truncated at " + MAX_STATES + " states.");
                            return visited;
                        }
                    }
                }
            }
        }
        return visited;
    }

    private boolean isEnabled(int[] marking, int[][] pre, int t) {
        for (int p = 0; p < pre.length; p++) {
            if (pre[p][t] > 0 && marking[p] < pre[p][t]) {
                return false;
            }
        }
        return true;
    }
}