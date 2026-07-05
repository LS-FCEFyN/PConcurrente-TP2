package algebra;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Computes the natural (minimal-support) basis of P-invariants and T-invariants
 * of a Petri net incidence matrix, using the Farkas / Martinez-Silva algorithm.
 *
 * <p>The algorithm eliminates the equations of {@code M x = 0} one at a time,
 * Fourier-Motzkin style. It maintains a "tableau" of rows, each pairing a
 * candidate combination of variables (the invariant-in-progress) with the
 * value that combination currently produces against the equations not yet
 * eliminated. At every step, rows of opposite sign in the next equation's
 * column are combined - in minimal positive integer proportions - into new
 * rows that are zero there, discarding anything whose support isn't minimal.
 * What survives once every equation has been eliminated is exactly the
 * natural (minimal-support) basis of invariants: complete and exact, with no
 * bound on coefficient size and no combinatorial search over guesses.
 */
public class Matrix {

    private static final double EPS = 1e-9;

    /**
     * @param matrix    the Petri net incidence matrix
     * @param forPlaces {@code true} to compute P-invariants (uses the transpose),
     *                  {@code false} to compute T-invariants
     * @return the natural basis of invariants: the minimal-support, non-negative,
     *         integer solutions of {@code matrix * x = 0} (or its transpose)
     */
    public static List<double[]> getNaturalBasis(double[][] matrix, boolean forPlaces) {
        return farkas(forPlaces ? transpose(matrix) : matrix);
    }

    // ---------------------------------------------------------------------
    // Farkas / Martinez-Silva elimination
    // ---------------------------------------------------------------------

    /**
     * A tableau row: {@code support} is the combination of variables built so far
     * (the invariant-in-progress), {@code value} is what that combination currently
     * produces against each equation of {@code M} - zero in every equation already
     * eliminated, and something not-yet-necessarily-zero in the rest.
     */
    private record Row(double[] support, double[] value) {}

    /**
     * The natural basis of {@code M x = 0}: eliminate each equation (row of {@code M})
     * in turn, until every equation has been zeroed out across the whole tableau.
     */
    private static List<double[]> farkas(double[][] m) {
        int variableCount = m[0].length;
        List<Row> tableau = initialTableau(m, variableCount);
        for (int equation = 0; equation < m.length; equation++) {
            tableau = eliminate(tableau, equation);
        }
        return tableau.stream().map(Row::support).collect(Collectors.toList());
    }

    /** One row per variable: its unit vector, paired with its column of {@code m}. */
    private static List<Row> initialTableau(double[][] m, int variableCount) {
        return IntStream.range(0, variableCount)
                .mapToObj(j -> new Row(unit(variableCount, j), column(m, j)))
                .collect(Collectors.toList());
    }

    /**
     * Zeroes out {@code equation} across the tableau: rows already zero there pass
     * through unchanged, and every positive/negative pair in that column is combined
     * into a new zero row. The result keeps only support-minimal rows - a combined
     * row is dropped if its support properly contains some other surviving row's.
     */
    private static List<Row> eliminate(List<Row> tableau, int equation) {
        List<Row> zero = tableau.stream().filter(r -> isZero(r.value()[equation])).toList();
        List<Row> positive = tableau.stream().filter(r -> r.value()[equation] > EPS).toList();
        List<Row> negative = tableau.stream().filter(r -> r.value()[equation] < -EPS).toList();

        List<Row> candidates = new ArrayList<>(zero);
        for (Row p : positive) {
            for (Row n : negative) {
                candidates.add(combine(p, n, equation));
            }
        }
        return minimalSupportRows(candidates);
    }

    /**
     * The smallest positive integer combination {@code a*p + b*n} that zeroes
     * {@code equation} (where {@code p}'s entry there is positive and {@code n}'s
     * is negative), reduced by the gcd of its entries.
     */
    private static Row combine(Row p, Row n, int equation) {
        double a = -n.value()[equation];
        double b = p.value()[equation];
        double[] support = addScaled(p.support(), a, n.support(), b);
        double[] value = addScaled(p.value(), a, n.value(), b);
        long g = gcd(support, value);
        return new Row(divide(support, g), divide(value, g));
    }

    /** {@code a*x + b*y}, component-wise. */
    private static double[] addScaled(double[] x, double a, double[] y, double b) {
        return IntStream.range(0, x.length).mapToDouble(i -> a * x[i] + b * y[i]).toArray();
    }

    /**
     * Keeps only the support-minimal, distinct rows of {@code rows}: duplicates are
     * collapsed, and any row whose support properly contains another row's support
     * is dropped (a simpler invariant covering part of it already exists).
     */
    private static List<Row> minimalSupportRows(List<Row> rows) {
        List<Row> distinct = dedupe(rows);
        return distinct.stream()
                .filter(r -> distinct.stream().noneMatch(other -> other != r
                        && isSubsetSupport(other.support(), r.support())
                        && !sameSupport(other.support(), r.support())))
                .collect(Collectors.toList());
    }

    /** Removes rows with a support vector already seen (same non-zero pattern and values). */
    private static List<Row> dedupe(List<Row> rows) {
        Map<String, Row> seen = new LinkedHashMap<>();
        rows.forEach(r -> seen.putIfAbsent(key(r.support()), r));
        return new ArrayList<>(seen.values());
    }

    /** Whether every non-zero index of {@code a} is also non-zero in {@code b}. */
    private static boolean isSubsetSupport(double[] a, double[] b) {
        return IntStream.range(0, a.length).allMatch(i -> isZero(a[i]) || !isZero(b[i]));
    }

    private static boolean sameSupport(double[] a, double[] b) {
        return IntStream.range(0, a.length).allMatch(i -> isZero(a[i]) == isZero(b[i]));
    }

    private static boolean isZero(double x) {
        return Math.abs(x) < EPS;
    }

    // ---------------------------------------------------------------------
    // Small vector / number helpers
    // ---------------------------------------------------------------------

    private static double[] unit(int length, int index) {
        double[] v = new double[length];
        v[index] = 1;
        return v;
    }

    private static double[] column(double[][] m, int j) {
        return Arrays.stream(m).mapToDouble(row -> row[j]).toArray();
    }

    private static double[] divide(double[] v, long divisor) {
        return divisor <= 1 ? v : Arrays.stream(v).map(x -> x / divisor).toArray();
    }

    /** The gcd of the rounded absolute values of every non-zero entry across both vectors. */
    private static long gcd(double[] a, double[] b) {
        return Stream.concat(Arrays.stream(a).boxed(), Arrays.stream(b).boxed())
                .mapToLong(x -> Math.round(Math.abs(x)))
                .filter(x -> x != 0)
                .reduce(Matrix::gcd)
                .orElse(1);
    }

    private static long gcd(long a, long b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    /** A string key identifying a support vector's rounded values, for dedup lookups. */
    private static String key(double[] v) {
        return Arrays.stream(v).mapToLong(Math::round).mapToObj(String::valueOf).collect(Collectors.joining(","));
    }

    /** The transpose of {@code m}. */
    private static double[][] transpose(double[][] m) {
        return IntStream.range(0, m[0].length)
                .mapToObj(j -> Arrays.stream(m).mapToDouble(row -> row[j]).toArray())
                .toArray(double[][]::new);
    }
}