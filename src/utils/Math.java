package utils;

import java.util.Arrays;

public class Math {
    public static double[][] rref(double[][] m) {
        int lead = 0;
        int rowCount = m.length;
        int colCount = m[0].length;
        int i;
        boolean quit = false;
        for (int row = 0; row < rowCount && !quit; row++) {
            if (colCount <= lead) {
                quit = true;
                break;
            }
            i = row;

            while (!quit && m[i][lead] == 0) {
                i++;
                if (rowCount == i) {
                    i = row;
                    lead++;
                    if (colCount == lead) {
                        quit = true;
                        break;
                    }
                }
            }
            if (!quit) {
                swapRows(m, i, row);
                if (m[row][lead] != 0)
                    multiplyRow(m, row, 1.0f / m[row][lead]);
                for (i = 0; i < rowCount; i++) {
                    if (i != row)
                        subtractRows(m, m[i][lead], row, i);
                }
            }
        }

        return Arrays.stream(m)
                          .map(innerArray -> Arrays.stream(innerArray)
                              .map(num -> num == -0.0 ? 0.0 : num)
                              .toArray())
                          .toArray(double[][]::new);

    }

    public static double[][] transpose(double[][] m) {
        double[][] transposed = new double[m[0].length][m.length];
        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < m[0].length; j++) {
                transposed[j][i] = m[i][j];
            }
        }
        return transposed;
    }

    private static void swapRows(double[][] m, int row1, int row2) {
        double[] swap = new double[m[0].length];

        for (int c1 = 0; c1 < m[0].length; c1++)
            swap[c1] = m[row1][c1];

        for (int c1 = 0; c1 < m[0].length; c1++) {
            m[row1][c1] = m[row2][c1];
            m[row2][c1] = swap[c1];
        }
    }

    private static void multiplyRow(double[][] m, int row, double scalar) {
        for (int c1 = 0; c1 < m[0].length; c1++) {
            m[row][c1] *= scalar;
        }
    }

    private static void subtractRows(double[][] m, double scalar, int subtract_scalar_times_this_row, int from_this_row) {
        for (int c1 = 0; c1 < m[0].length; c1++) {
            m[from_this_row][c1] -= scalar * m[subtract_scalar_times_this_row][c1];
        }
    }
}