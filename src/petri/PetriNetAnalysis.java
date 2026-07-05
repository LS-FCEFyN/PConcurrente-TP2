package petri;


import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import algebra.Matrix;

/**
 * Structural analysis utilities for a {@link ConcurrentMatrixPetriNet}:
 * estimating the number of worker threads a simulation needs (both a global
 * bound via T-invariants and a segmented, per-structural-chain bound), and
 * checking classical Petri net properties (boundedness, safety, deadlocks,
 * and liveness) by exhaustively exploring the net's reachability graph.
 *
 * <p>Several methods build the full reachability graph via breadth-first
 * search; for unbounded nets this is capped at {@code MAX_STATES} states to
 * avoid unbounded memory growth (see {@link #generateReachabilityGraphWithEdges}).
 */
public class PetriNetAnalysis {

    /**
     * Estimates the maximum number of concurrently active "action" threads
     * the net can require, using T-invariants.
     *
     * <p>Computes the natural basis of T-invariants from the incidence
     * matrix, takes the union of the action places (places not in
     * {@code nonActionPlaces}) associated with every invariant, and returns
     * the maximum total token count across those places over every
     * reachable marking.
     *
     * @param petriNet        the Petri net to analyze
     * @param nonActionPlaces places to exclude from the thread-count
     *                        computation (e.g. control/synchronization
     *                        places that don't represent an active worker)
     * @return the maximum number of active threads observed across all
     *         reachable markings, or {@code 0} if no T-invariants exist or
     *         no action places remain after exclusion
     */
    public int calculateMaxActiveThreads(ConcurrentMatrixPetriNet petriNet, Set<Integer> nonActionPlaces) {
        double[][] incidenceMatrix = getIncidenceMatrixDouble(petriNet);
        List<int[]> tInvariants = Matrix.getNaturalBasis(incidenceMatrix, false).stream()
                .map(this::toIntArray).collect(Collectors.toList());

        if (tInvariants.isEmpty()) {
            System.out.println("No T-Invariants found. Cannot determine cyclic thread behavior.");
            return 0;
        }

        // Union of every invariant's action places, per eq. (4)/(5) - MA is built over
        // this union, not per-invariant.
        Set<Integer> unionPA = new HashSet<>();
        for (int[] inv : tInvariants) {
            Set<Integer> pa = getAssociatedPlaces(petriNet, inv);
            pa.removeAll(nonActionPlaces);
            unionPA.addAll(pa);
        }
        if (unionPA.isEmpty())
            return 0;

        Set<List<Integer>> reachabilityGraph = generateReachabilityGraph(petriNet);
        int max = 0;
        for (List<Integer> marking : reachabilityGraph) {
            int sum = 0;
            for (Integer placeId : unionPA)
                sum += marking.get(placeId);
            max = Math.max(max, sum);
        }
        return max;
    }

    /**
     * Converts a net's integer incidence matrix to a {@code double[][]},
     * as required by {@link Matrix#getNaturalBasis}.
     *
     * @param petriNet the net whose incidence matrix to convert
     * @return the incidence matrix, with entries widened to {@code double}
     */
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
     *
     * @param pn        the Petri net supplying the pre/post matrices
     * @param invariant a T-invariant firing-count vector, indexed by
     *                  transition
     * @return the set of places that are an input or output of any
     *         transition participating in {@code invariant}
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
     *
     * @param pn the Petri net to explore
     * @return the set of every reachable marking, each represented as a
     *         list of per-place token counts
     */
    private Set<List<Integer>> generateReachabilityGraph(ConcurrentMatrixPetriNet pn) {
        return generateReachabilityGraphWithEdges(pn).keySet();
    }

    // ---------------------------------------------------------------------
    // Structural properties: boundedness, safety, deadlock, liveness
    // ---------------------------------------------------------------------

    /** One transition of the reachability graph: firing {@code transition} from a marking leads to {@code target}. */
    private record Edge(int transition, List<Integer> target) {}

    /** Summary of the structural analysis of a net's reachability graph. */
    public record PropertiesReport(
            int[] maxTokensPerPlace,
            int bound,
            boolean safe,
            List<List<Integer>> deadlocks,
            Map<Integer, Boolean> liveTransitions,
            boolean live) {

        /** Prints a human-readable summary of this report to standard output. */
        public void print() {
            System.out.println("Max tokens per place: " + Arrays.toString(maxTokensPerPlace));
            System.out.println(bound + "-bounded" + (safe ? " (safe)" : " (not safe: some place exceeds 1 token)"));
            if (deadlocks.isEmpty()) {
                System.out.println("No deadlocks found.");
            } else {
                System.out.println(deadlocks.size() + " deadlock marking(s) found, e.g. " + deadlocks.get(0));
            }
            liveTransitions.forEach((t, isLive) ->
                    System.out.println("T" + t + ": " + (isLive ? "live (L4)" : "NOT live - can get stuck unable to fire again")));
            System.out.println("Net is live: " + live);
        }
    }

    /**
     * Builds the reachability graph and analyzes it for boundedness, safety,
     * deadlocks, and liveness (L4: from every reachable marking, every
     * transition can eventually be re-enabled).
     *
     * @param pn the Petri net to analyze
     * @return a {@link PropertiesReport} summarizing the structural
     *         properties found
     */
    public PropertiesReport analyzeProperties(ConcurrentMatrixPetriNet pn) {
        Map<List<Integer>, List<Edge>> graph = generateReachabilityGraphWithEdges(pn);
        int numPlaces = pn.getPlaceCount();
        int numTrans = pn.getTransitionCount();

        int[] maxPerPlace = new int[numPlaces];
        for (List<Integer> marking : graph.keySet()) {
            for (int p = 0; p < numPlaces; p++) {
                maxPerPlace[p] = Math.max(maxPerPlace[p], marking.get(p));
            }
        }
        int bound = Arrays.stream(maxPerPlace).max().orElse(0);
        boolean safe = bound <= 1;

        List<List<Integer>> deadlocks = graph.entrySet().stream()
                .filter(e -> e.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        Map<List<Integer>, List<List<Integer>>> reverse = reverseGraph(graph);
        Map<Integer, Boolean> liveTransitions = new LinkedHashMap<>();
        for (int t = 0; t < numTrans; t++) {
            liveTransitions.put(t, isTransitionLive(pn, graph, reverse, t));
        }
        boolean live = liveTransitions.values().stream().allMatch(Boolean::booleanValue);

        return new PropertiesReport(maxPerPlace, bound, safe, deadlocks, liveTransitions, live);
    }

    /**
     * Whether every reachable marking can reach some marking that enables
     * {@code t} (L4 liveness of {@code t}).
     *
     * @param pn      the Petri net supplying the pre-matrix
     * @param graph   the full reachability graph (marking to outgoing edges)
     * @param reverse the reachability graph with edges reversed
     * @param t       the transition to check for liveness
     * @return {@code true} if {@code t} is live, {@code false} if it is
     *         dead or can become permanently disabled from some reachable
     *         marking
     */
    private boolean isTransitionLive(ConcurrentMatrixPetriNet pn, Map<List<Integer>, List<Edge>> graph,
            Map<List<Integer>, List<List<Integer>>> reverse, int t) {
        int[][] pre = pn.getPreMatrix();
        Set<List<Integer>> enabling = graph.keySet().stream()
                .filter(m -> isEnabled(toIntArray(m), pre, t))
                .collect(Collectors.toSet());
        if (enabling.isEmpty()) {
            return false; // dead transition (L0): never enabled in any reachable marking
        }
        Set<List<Integer>> canReachEnabling = backwardReachable(reverse, enabling);
        return canReachEnabling.size() == graph.size();
    }

    /**
     * Every marking from which some marking in {@code targets} can be
     * reached, found via reverse BFS.
     *
     * @param reverse the reachability graph with edges reversed
     * @param targets the set of markings to search backward from
     * @return every marking that can reach a marking in {@code targets}
     *         (including the targets themselves)
     */
    private Set<List<Integer>> backwardReachable(Map<List<Integer>, List<List<Integer>>> reverse, Set<List<Integer>> targets) {
        Set<List<Integer>> visited = new HashSet<>(targets);
        Deque<List<Integer>> queue = new ArrayDeque<>(targets);
        while (!queue.isEmpty()) {
            List<Integer> current = queue.poll();
            for (List<Integer> predecessor : reverse.get(current)) {
                if (visited.add(predecessor)) {
                    queue.add(predecessor);
                }
            }
        }
        return visited;
    }

    /**
     * The reachability graph with every predecessor edge reversed, for
     * backward traversal.
     *
     * @param graph the forward reachability graph (marking to outgoing edges)
     * @return a map from each marking to the list of markings that have a
     *         direct edge leading into it
     */
    private Map<List<Integer>, List<List<Integer>>> reverseGraph(Map<List<Integer>, List<Edge>> graph) {
        Map<List<Integer>, List<List<Integer>>> reverse = new HashMap<>();
        graph.keySet().forEach(m -> reverse.put(m, new ArrayList<>()));
        graph.forEach((from, edges) -> edges.forEach(edge -> reverse.get(edge.target()).add(from)));
        return reverse;
    }

    /**
     * Converts a marking represented as a {@link List} of {@link Integer}
     * (as used for reachability-graph keys) into a primitive {@code int[]}.
     *
     * @param marking the marking to convert
     * @return the marking as a primitive int array, in the same order
     */
    private int[] toIntArray(List<Integer> marking) {
        return marking.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Builds the reachability graph via BFS, keeping both the visited markings
     * and the transition-labeled edges between them.
     * WARNING: For unbounded nets, this will run indefinitely.
     * Added a safety cap for states to prevent memory overflow.
     *
     * @param pn the Petri net to explore, starting from its initial marking
     * @return a map from each reachable marking to the list of edges
     *         (transition fired, resulting marking) leading out of it; a
     *         marking with an empty edge list is a deadlock
     */
    private Map<List<Integer>, List<Edge>> generateReachabilityGraphWithEdges(ConcurrentMatrixPetriNet pn) {
        Map<List<Integer>, List<Edge>> graph = new LinkedHashMap<>();
        Queue<int[]> queue = new LinkedList<>();

        int[] m0 = pn.getInitialMarking();
        queue.add(m0);
        graph.put(IntStream.of(m0).boxed().collect(Collectors.toList()), new ArrayList<>());

        int[][] pre = pn.getPreMatrix();
        int[][] post = pn.getPostMatrix();
        int numTrans = pn.getTransitionCount();
        int numPlaces = pn.getPlaceCount();

        // Safety cap for large/unbounded nets
        final int MAX_STATES = 50000;

        while (!queue.isEmpty()) {
            int[] currentM = queue.poll();
            List<Integer> currentKey = IntStream.of(currentM).boxed().collect(Collectors.toList());

            for (int t = 0; t < numTrans; t++) {
                if (isEnabled(currentM, pre, t)) {
                    int[] nextM = new int[numPlaces];
                    for (int p = 0; p < numPlaces; p++) {
                        nextM[p] = currentM[p] - pre[p][t] + post[p][t];
                    }
                    List<Integer> nextKey = IntStream.of(nextM).boxed().collect(Collectors.toList());

                    if (!graph.containsKey(nextKey)) {
                        graph.put(nextKey, new ArrayList<>());
                        queue.add(nextM);

                        if (graph.size() >= MAX_STATES) {
                            System.err.println(
                                    "Warning: Reachability graph generation truncated at " + MAX_STATES + " states.");
                            graph.get(currentKey).add(new Edge(t, nextKey));
                            return graph;
                        }
                    }
                    graph.get(currentKey).add(new Edge(t, nextKey));
                }
            }
        }
        return graph;
    }

    /**
     * Checks whether transition {@code t} is enabled in {@code marking},
     * i.e. every place feeding it holds at least as many tokens as its
     * pre-condition weight requires.
     *
     * @param marking the marking to check against
     * @param pre     the net's pre-condition matrix
     * @param t       the transition to check
     * @return {@code true} if {@code t} is enabled in {@code marking}
     */
    private boolean isEnabled(int[] marking, int[][] pre, int t) {
        for (int p = 0; p < pre.length; p++) {
            if (pre[p][t] > 0 && marking[p] < pre[p][t]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Estimates the total number of worker threads needed by partitioning
     * the net's action places into structural segments (maximal linear
     * chains of places not broken by a fork/join), and summing each
     * segment's own maximum concurrent token count over the reachability
     * graph.
     *
     * <p>This is generally a tighter (or equal) bound than
     * {@link #calculateMaxActiveThreads}, since threads in structurally
     * independent segments cannot all peak simultaneously if the segments
     * are mutually exclusive, but are still counted per-segment here as an
     * upper bound per structural chain.
     *
     * @param petriNet        the Petri net to analyze
     * @param nonActionPlaces places to exclude from segmentation and the
     *                        thread-count computation
     * @return the sum of each segment's maximum concurrent token count
     *         across all reachable markings
     */
    public int calculateSegmentedMaxThreads(ConcurrentMatrixPetriNet petriNet, Set<Integer> nonActionPlaces) {
        // 1. Get Segments (Algorithm 4.2)
        // PS_i in the text
        List<Set<Integer>> segments = getSegments(petriNet, nonActionPlaces);

        System.out.println("Identified " + segments.size() + " structural segments.");

        // 2. Generate Reachability Graph (Global Reachability Set M)
        // Note: The text refers to MS_i as markings of the segment, which is derived
        // from
        // projecting the global reachability graph onto the segment's places.
        Set<List<Integer>> reachabilityGraph = generateReachabilityGraph(petriNet);

        int totalSystemThreads = 0;

        // 3. Process each segment
        for (int i = 0; i < segments.size(); i++) {
            Set<Integer> segmentPlaces = segments.get(i);
            int maxSegmentThreads = 0;

            // Iterate through all reachable markings (MS_i logic)
            for (List<Integer> marking : reachabilityGraph) {
                int currentMarkingSum = 0;

                // Sum tokens only for places in this segment
                for (Integer placeId : segmentPlaces) {
                    currentMarkingSum += marking.get(placeId);
                }

                // Keep the maximum found for this segment
                if (currentMarkingSum > maxSegmentThreads) {
                    maxSegmentThreads = currentMarkingSum;
                }
            }

            System.out.println("Segment " + i + " Max Threads: " + maxSegmentThreads);

            // 4. Accumulate total threads
            totalSystemThreads += maxSegmentThreads;
        }

        return totalSystemThreads;
    }

    /**
     * Partitions the net's action places into structural segments
     * (Algorithm 4.2): places are merged into the same segment when they
     * are connected by a strictly linear (non-fork, non-join, 1-in-1-out)
     * transition, using a disjoint-set (union-find) structure to group
     * transitively-connected places.
     *
     * @param pn              the Petri net to segment
     * @param nonActionPlaces places excluded from segmentation entirely
     * @return the resulting segments, each a set of place indices
     */
    private List<Set<Integer>> getSegments(ConcurrentMatrixPetriNet pn, Set<Integer> nonActionPlaces) {
        int numPlaces = pn.getPlaceCount();
        int numTrans = pn.getTransitionCount();
        int[][] pre = pn.getPreMatrix();
        int[][] post = pn.getPostMatrix();

        // Identify Action Places (Total Places - NonActionPlaces)
        Set<Integer> actionPlaces = IntStream.range(0, numPlaces)
                .boxed()
                .filter(p -> !nonActionPlaces.contains(p))
                .collect(Collectors.toSet());

        // We will use a Disjoint Set (Union-Find) approach to group linear places
        // together.
        // Initially, every action place is its own segment.
        DisjointSet dsu = new DisjointSet(numPlaces);

        // We iterate over Transitions to find connections between Action Places.
        // Connection: Place A -> Transition T -> Place B
        for (int t = 0; t < numTrans; t++) {

            // Identify Input Places (Action places only) for this transition
            List<Integer> inputPlaces = new ArrayList<>();
            for (int p = 0; p < numPlaces; p++) {
                if (pre[p][t] > 0 && actionPlaces.contains(p))
                    inputPlaces.add(p);
            }

            // Identify Output Places (Action places only) for this transition
            List<Integer> outputPlaces = new ArrayList<>();
            for (int p = 0; p < numPlaces; p++) {
                if (post[p][t] > 0 && actionPlaces.contains(p))
                    outputPlaces.add(p);
            }

            // Segmentation Rules based on Alg 4.2:

            // We only merge Place A and Place B into the same segment if the flow is
            // strictly linear.
            // Flow is linear if:
            // 1. Place A is NOT a Fork (it doesn't go to other transitions).
            // 2. Place B is NOT a Join (it doesn't come from other transitions).
            // 3. The transition T itself is 1-in-1-out (not a sync or spread transition
            // relative to action places).

            if (inputPlaces.size() == 1 && outputPlaces.size() == 1) {
                int u = inputPlaces.get(0);
                int v = outputPlaces.get(0);

                boolean uIsFork = isPlaceFork(u, pre, numTrans);
                boolean vIsJoin = isPlaceJoin(v, post, numTrans);

                // If strictly linear (No structural conflict at U, No Join at V), merge them.
                if (!uIsFork && !vIsJoin) {
                    dsu.union(u, v);
                }
                // If U is a Fork, the segment ends at U. V starts a new segment. (No merge)
                // If V is a Join, the segment ends before V. V starts a new segment. (No merge)
            }

            // If transition has >1 inputs (Join Transition) or >1 outputs (Fork
            // Transition),
            // it naturally acts as a boundary, so we do not merge inputs with outputs.
        }

        // Collect results from DSU
        Map<Integer, Set<Integer>> groups = new HashMap<>();
        for (Integer p : actionPlaces) {
            int root = dsu.find(p);
            groups.computeIfAbsent(root, k -> new HashSet<>()).add(p);
        }

        return new ArrayList<>(groups.values());
    }

    /**
     * A place is a "fork" if it feeds more than one transition, i.e. firing
     * it represents a structural choice/split rather than a linear chain
     * step.
     *
     * @param placeId   the place to check
     * @param preMatrix the net's pre-condition matrix
     * @param numTrans  the number of transitions in the net
     * @return {@code true} if the place has more than one outgoing
     *         transition
     */
    private boolean isPlaceFork(int placeId, int[][] preMatrix, int numTrans) {
        int outputCount = 0;
        for (int t = 0; t < numTrans; t++) {
            if (preMatrix[placeId][t] > 0)
                outputCount++;
        }
        return outputCount > 1;
    }

    /**
     * A place is a "join" if it is fed by more than one transition, i.e. it
     * represents a structural merge point rather than a linear chain step.
     *
     * @param placeId    the place to check
     * @param postMatrix the net's post-condition matrix
     * @param numTrans   the number of transitions in the net
     * @return {@code true} if the place has more than one incoming
     *         transition
     */
    private boolean isPlaceJoin(int placeId, int[][] postMatrix, int numTrans) {
        int inputCount = 0;
        for (int t = 0; t < numTrans; t++) {
            if (postMatrix[placeId][t] > 0)
                inputCount++;
        }
        return inputCount > 1;
    }

    /**
     * A simple disjoint-set (union-find) structure over integer elements
     * {@code 0..size-1}, used to group places into structural segments.
     * Uses path compression on {@link #find} but no union-by-rank.
     */
    private static class DisjointSet {
        private int[] parent;

        /**
         * Creates a disjoint-set structure with {@code size} singleton sets.
         *
         * @param size the number of elements, indexed {@code 0..size-1}
         */
        public DisjointSet(int size) {
            parent = new int[size];
            for (int i = 0; i < size; i++)
                parent[i] = i;
        }

        /**
         * Finds the representative (root) of the set containing {@code i},
         * compressing the path along the way.
         *
         * @param i the element to look up
         * @return the root of {@code i}'s set
         */
        public int find(int i) {
            if (parent[i] != i) {
                parent[i] = find(parent[i]);
            }
            return parent[i];
        }

        /**
         * Merges the sets containing {@code i} and {@code j}.
         *
         * @param i an element of the first set
         * @param j an element of the second set
         */
        public void union(int i, int j) {
            int rootI = find(i);
            int rootJ = find(j);
            if (rootI != rootJ) {
                parent[rootJ] = rootI;
            }
        }
    }
}