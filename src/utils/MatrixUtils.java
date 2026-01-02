package utils;

import java.util.*;
import java.util.stream.*;

public class MatrixUtils {
    private static final double EPS = 1e-10;

    public static List<double[]> getNaturalBasis(double[][] matrix, boolean forPlaces) {
        double[][] rref = rref(forPlaces ? transpose(matrix) : matrix);
        List<Integer> pivots = IntStream.range(0, rref.length).map(i -> pivot(rref[i])).filter(p -> p >= 0).boxed()
                .collect(Collectors.toList());
        List<double[]> basis = IntStream.range(0, rref[0].length).filter(c -> !pivots.contains(c)).mapToObj(free -> {
            double[] v = new double[rref[0].length];
            v[free] = 1;
            IntStream.range(0, rref.length).forEach(r -> {
                int p = pivot(rref[r]);
                if (p >= 0)
                    v[p] = -IntStream.range(p + 1, v.length).mapToDouble(c -> rref[r][c] * v[c]).sum();
            });
            return v;
        }).collect(Collectors.toList());
        List<double[]> natBasis = new ArrayList<>();
        IntStream.range(1, basis.size() * 4).boxed()
                .flatMap(sum -> combinations(basis.size(), sum, new int[basis.size()], 0).stream())
                .map(coeffs -> IntStream.range(0, basis.get(0).length)
                        .mapToDouble(i -> IntStream.range(0, coeffs.length)
                                .mapToDouble(j -> coeffs[j] * basis.get(j)[i]).sum())
                        .toArray())
                .filter(v -> Arrays.stream(v).allMatch(x -> x > -EPS && Math.abs(Math.round(x) - x) < EPS)
                        && natBasis.stream()
                                .noneMatch(e -> IntStream.range(0, v.length).allMatch(i -> e[i] <= v[i] + EPS)
                                        && !Arrays.equals(e, v)))
                .forEach(v -> {
                    if (isIndependent(natBasis, v))
                        natBasis.add(v);
                });
        return natBasis;
    }

    private static double[][] rref(double[][] m) {
        double[][] r = Arrays.stream(m).map(double[]::clone).toArray(double[][]::new);
        int rows = r.length, cols = r[0].length;
        for (int i = 0, lead = 0; i < rows && lead < cols;) {
            int p = i, l = lead; // p is effectively final and safe for lambdas
            int pivotRow = IntStream.range(i, rows).filter(row -> Math.abs(r[row][l]) > EPS).findFirst().orElse(-1);
            if (pivotRow < 0) {
                lead++;
                continue;
            }
            double[] t = r[i];
            r[i] = r[pivotRow];
            r[pivotRow] = t;
            double div = r[i][lead];
            Arrays.setAll(r[p], k -> r[p][k] / div); // Fixed: Use p instead of i
            IntStream.range(0, rows).filter(row -> row != p).forEach(row -> {
                double f = r[row][l];
                Arrays.setAll(r[row], k -> r[row][k] - f * r[p][k]);
            });
            i++;
            lead++;
        }
        return r;
    }

    private static int pivot(double[] row) {
        return IntStream.range(0, row.length).filter(i -> Math.abs(row[i]) > EPS).findFirst().orElse(-1);
    }

    private static double[][] transpose(double[][] m) {
        return IntStream.range(0, m[0].length).mapToObj(j -> Arrays.stream(m).mapToDouble(r -> r[j]).toArray())
                .toArray(double[][]::new);
    }

    private static boolean isIndependent(List<double[]> set, double[] vec) {
        double[][] m = Stream.concat(set.stream(), Stream.of(vec)).toArray(double[][]::new);
        return Arrays.stream(rref(m)).filter(r -> pivot(r) >= 0).count() == m.length;
    }

    private static List<int[]> combinations(int len, int target, int[] cur, int idx) {
        if (idx == len)
            return target == 0 ? Collections.singletonList(cur.clone()) : Collections.emptyList();
        List<int[]> res = new ArrayList<>();
        IntStream.rangeClosed(0, Math.min(3, target)).forEach(i -> {
            cur[idx] = i;
            res.addAll(combinations(len, target - i, cur, idx + 1));
        });
        return res;
    }
}