package game;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class GameLogic {

    public static List<Point> getNeighbors(int row, int col, int size, BoardShape shape) {
        switch (shape) {
            case HEXAGONAL:
                return getNeighborsHex(row, col, size);
            case SQUARE:
            default:
                return getNeighborsSquare(row, col, size);
        }
    }

    private static List<Point> getNeighborsSquare(int row, int col, int size) {
        List<Point> neighbors = new ArrayList<>();
        for (int r = row - 1; r <= row + 1; r++) {
            for (int c = col - 1; c <= col + 1; c++) {
                if (r == row && c == col)
                    continue;
                if (r >= 0 && r < size && c >= 0 && c < size) {
                    neighbors.add(new Point(r, c));
                }
            }
        }
        return neighbors;
    }

    private static List<Point> getNeighborsHex(int row, int col, int size) {
        // Convert offset to axial, get neighbors, convert back
        // even-q vertical layout: odd columns are shifted down
        int q = col;
        int r = row - (col >> 1); // col/2 using integer division

        // 6 axial directions for flat-top hexes
        int[][] axialDirs = { { 1, 0 }, { 1, -1 }, { 0, -1 }, { -1, 0 }, { -1, 1 }, { 0, 1 } };

        List<Point> neighbors = new ArrayList<>();
        for (int[] dir : axialDirs) {
            int nq = q + dir[0];
            int nr = r + dir[1];
            // Convert axial back to offset (even-q)
            int nrow = nr + (nq >> 1);
            int ncol = nq;
            if (nrow >= 0 && nrow < size && ncol >= 0 && ncol < size) {
                neighbors.add(new Point(nrow, ncol));
            }
        }
        return neighbors;
    }

    public static int countMines(int row, int col, int size, BoardShape shape, boolean[][] mines) {
        int count = 0;
        for (Point p : getNeighbors(row, col, size, shape)) {
            if (mines[p.x][p.y]) {
                count++;
            }
        }
        return count;
    }
}
