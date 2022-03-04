import java.io.File;
import java.io.FileNotFoundException;
import java.io.StreamCorruptedException;
import java.net.HttpURLConnection;
import java.util.*;

/**
 * A class to represent a single configuration in the Nurikabe puzzle.
 *
 * @author RIT CS
 * @author Giovanni Coppola
 */
public class NurikabeConfig implements Configuration {

    private String filename;
    private int rowDimension;
    private int colDimension;
    private int[][] grid;
    private int rowCursor;
    private int colCursor;
    private int numberedCells = 0;
    private int numberOfNumberedCells = 0;
    private int numberOfIslands = 0;
    private int numberOfSeaCells = 0;
    private int totalNumberOfCells = 0;
    private record Coordinates(int row, int column) {}

    /**
     * Construct the initial configuration from an input file whose contents
     * are, for example:<br>
     * <tt><br>
     * 3 3          # rows columns<br>
     * 1 . #        # row 1, .=empty, 1-9=numbered island, #=island, &#64;=sea<br>
     * &#64; . 3    # row 2<br>
     * 1 . .        # row 3<br>
     * </tt><br>
     *
     * @param filename the name of the file to read from
     * @throws FileNotFoundException if the file is not found
     */
    public NurikabeConfig(String filename) throws FileNotFoundException {
        try (Scanner in = new Scanner(new File(filename))) {
            // Set both the row dimension and column dimension
            rowDimension = in.nextInt();
            colDimension = in.nextInt();

            // Scan in the next line which will represent the first row of the grid
            String line = in.next();

            // Create a new grid with the proper row and column dimensions
            this.grid = new int[rowDimension][colDimension];

            // Nested for loops to set the values in the grid
            for (int row = 0; row < rowDimension; ++row) {
                for (int col = 0; col < colDimension; ++col) {
                    if (line.contains(".")) {
                        this.grid[row][col] = 0;
                    } else {
                        this.grid[row][col] = Integer.parseInt(line);
                        this.numberedCells += this.grid[row][col] - 1;
                        this.numberOfNumberedCells++;
                        this.totalNumberOfCells++;
                    }
                    line = in.next();
                }
            }

            // Set the row and column cursor to the proper areas
            this.rowCursor = 0;
            this.colCursor = -1;
        } // try-with-resources, the file is closed automatically
    }

    /**
     * The copy constructor takes a config, other, and makes a full "deep" copy
     * of its instance data.
     *
     * @param other - the config to copy
     * @param hashtag - the boolean value to determine if the value of the grid will be an island or sea cell
     */
    protected NurikabeConfig(NurikabeConfig other, boolean hashtag) {
        // Copy the values of the configuration copied in
        this.rowCursor = other.rowCursor;
        this.colCursor = other.colCursor;
        this.numberedCells = other.numberedCells;
        this.numberOfNumberedCells = other.numberOfNumberedCells;
        this.rowDimension = other.rowDimension;
        this.colDimension = other.colDimension;
        this.numberOfIslands = other.numberOfIslands;
        this.numberOfSeaCells = other.numberOfSeaCells;
        this.totalNumberOfCells = other.totalNumberOfCells;

        // Find the next column dimension
        this.colCursor += 1;
        if (this.colCursor == colDimension) {
            this.rowCursor += 1;
            this.colCursor = 0;
        }

        // Create a new grid and make a new copy of the grid from the config passed in
        this.grid = new int[rowDimension][];
        for (int row = 0; row < rowDimension; ++row) {
            this.grid[row] = Arrays.copyOf(other.grid[row], rowDimension);
        }

        //Set the value of the grid based on if it was a hashtag or not
        if (this.grid[rowCursor][colCursor] == 0) {
            if (hashtag) {
                this.grid[rowCursor][colCursor] = 10;
                numberOfIslands++;
                totalNumberOfCells++;
            } else {
                this.grid[rowCursor][colCursor] = 11;
                numberOfSeaCells++;
                totalNumberOfCells++;
            }
        }
    }

    /**
     * Method to find all possible successors of the current grid, but also checking for pruning
     *
     * @return - the collection of grids
     */
    @Override
    public Collection<Configuration> getSuccessors() {
        List<Configuration> successors = new LinkedList<>();

        if (this.rowCursor < rowDimension && this.colCursor < colDimension) {
            successors.add(new NurikabeConfig(this, true));
            successors.add(new NurikabeConfig(this, false));
        }

        return successors;
    }

    /**
     * Method to find if the total sum of the islands is exceeded
     *
     * @return - the comparison of the number of islands to the total that should be in the grid
     */
    private boolean ifIslandSumCorrect() {
        // Return if the total number of island is less than or equal to the count
        return this.numberOfIslands <= this.numberedCells;
    }

    /**
     * Method to find if the total number of seas is exceeded
     *
     * @return - the comparison of the total seas to the total amount of filled in tiles - the total amount of non-sea tiles
     */
    private boolean ifSeaSumCorrect() {
        // Return if the total sea tiles is less than or equal to the total non-empty tiles - the total non-empty and non-sea tile tiles
        return this.numberOfSeaCells <= (this.totalNumberOfCells - (this.numberOfNumberedCells + this.numberOfIslands));
    }

    /*private boolean ifFilled() {
        if (this.rowCursor+1 < rowDimension && this.grid[rowCursor+1][colCursor] == 0) {
            return false;
        }
        if (this.colCursor+1 < colDimension && this.grid[rowCursor][colCursor+1] == 0) {
            return false;
        }
        return true;
    }
    private boolean isCurrentSeaTileConnected() {
        int count = 0;
        if (this.rowCursor-1 >= 0 && this.grid[rowCursor-1][colCursor] == 11) {
            count++;
        }
        if (this.rowCursor+1 < rowDimension && this.grid[rowCursor+1][colCursor] == 11) {
            count++;
        }
        if (this.colCursor-1 >= 0 && this.grid[rowCursor][colCursor-1] == 11) {
            count++;
        }
        if (this.colCursor+1 < colDimension && this.grid[rowCursor][colCursor+1] == 11) {
            count++;
        }
        if (count > 0) {
            return true;
        }
        return false;
    }*/

    /**
     * Find the coordinates for the first numbered cell
     *
     * @return - the record of coordinates
     */
    private Coordinates findFirstSeaCell() {
        int row = 0;
        int col = 0;

        while (true) {
            if (this.grid[row][col] == 11) {
                break;
            }
            col += 1;
            if (col == colDimension) {
                row += 1;
                col = 0;
            }
        }

        return new Coordinates(row, col);
    }

    /**
     * Find the amount of island cells in the current grid
     *
     * @param cell - the coordinated of the cell passed in
     * @param visited - the list of visited cells
     * @return - the total number of island cells
     */
    private int countSeaCellsDFS(Coordinates cell, Set<Coordinates> visited) {
        int count = 0;
        int row = cell.row();
        int col = cell.column();

        // Check if the north cell has been visited and is a sea cell
        Coordinates north = new Coordinates(row-1, col);
        if (row-1 >= 0 && this.grid[row-1][col] == 11 && !visited.contains(north)) {
            visited.add(north);
            count += 1 + countSeaCellsDFS(north, visited);
        }

        // Check if the south cell has been visited and is a sea cell
        Coordinates south = new Coordinates(row+1, col);
        if (row+1 < rowDimension && this.grid[row+1][col] == 11 && !visited.contains(south)) {
            visited.add(south);
            count += 1 + countSeaCellsDFS(south, visited);
        }

        // Check if the east cell has been visited and is a sea cell
        Coordinates east = new Coordinates(row, col+1);
        if (col+1 < colDimension && this.grid[row][col+1] == 11 && !visited.contains(east)) {
            visited.add(east);
            count += 1 + countSeaCellsDFS(east, visited);
        }

        // Check if the west cell has been visited and is a sea cell
        Coordinates west = new Coordinates(row, col-1);
        if (col-1 >= 0 && this.grid[row][col-1] == 11 && !visited.contains(west)) {
            visited.add(west);
            count += 1 + countSeaCellsDFS(west, visited);
        }

        // Return the total count of sea cells found in the DFS algorithm
        return count;
    }

    /**
     * Method to count the number of island cells and make sure they are connected the proper island tiles, but
     *  not other islands
     *
     * @param cell - the coordinates of the current cell
     * @param visited - the set of visited coordinates
     * @return - the total number of island cells attached to a numbered cell
     */
    private int countIslandCells(Coordinates cell, Set<Coordinates> visited) {
        int count = 0;
        int row = cell.row();
        int col = cell.column();

        // Check if the top cell is an island and if it has been visited
        Coordinates top = new Coordinates(row-1, col);
        if (row-1 >= 0 && this.grid[row-1][col] == 10 && !visited.contains(top)) {
            visited.add(top);
            count += 1 + countIslandCells(top, visited);
        }
        // Check if the bottom cell is an island and has been visited
        Coordinates bottom = new Coordinates(row+1, col);
        if (row+1 < this.rowDimension && this.grid[row+1][col] == 10 && !visited.contains(bottom)) {
            visited.add(bottom);
            count += 1 + countIslandCells(bottom, visited);
        }
        // Check if the bottom cell is an island and has been visited
        Coordinates left = new Coordinates(row, col-1);
        if (col-1 >= 0 && this.grid[row][col-1] == 10 && !visited.contains(left)) {
            visited.add(left);
            count += 1 + countIslandCells(left, visited);
        }
        // Check if the bottom cell is an island and has been visited
        Coordinates right = new Coordinates(row, col+1);
        if (col+1 < this.colDimension && this.grid[row][col+1] == 10 && !visited.contains(right)) {
            visited.add(right);
            count += 1 + countIslandCells(right, visited);
        }

        return count;
    }

    /**
     * Method to find if all the seas are connected or not
     *
     * @return - if the sea is fully connected or not
     */
    private boolean allSeasConnected() {
        // Create an empty visited set
        Set<Coordinates> visited = new HashSet<>();

        //Find the first numbered cell and visit it
        Coordinates firstSeaCell = findFirstSeaCell();
        visited.add(firstSeaCell);

        // Find if the total number of numbered cells + 1 is equal to the total number of cells
        return 1 + countSeaCellsDFS(firstSeaCell, visited) == this.numberOfSeaCells;
    }

    /**
     * Method to determine if the islands have the correct sums attached
     *
     * @return - if the islands are correct
     */
    private boolean islandsAreCorrect() {
        Set<Coordinates> visited = new HashSet<>();

        for (int row = 0; row < rowDimension; row++) {
            for (int col = 0; col < colDimension; col++) {
                // If the current cell is a numbered cell
                if (this.grid[row][col] >= 1 && this.grid[row][col] <= 9) {
                    visited.add(new Coordinates(row, col));
                    // If the count found through the DFS + 1 is not equal to the magnitude of the grid, return false
                    if (1 + countIslandCells(new Coordinates(row, col), visited) != this.grid[row][col]) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Method to find if there are no 2x2 pools of sea cells
     *
     * @return - if there was or wasn't a 2x2 pool
     */
    private boolean noPools() {
        // Check the cells on the left, top, and left top diagonal are sea cells with the current sea cell
        if (this.grid[rowCursor][colCursor] == 11 && rowCursor-1 >= 0 && this.grid[rowCursor-1][colCursor] == 11 &&
                colCursor-1 >= 0 && this.grid[rowCursor][colCursor-1] == 11 && this.grid[rowCursor-1][colCursor-1] == 11) {
            return false;
        }

        return true;
    }

    /**
     * Method to determine if the grid is valid or not
     *
     * @return - boolean for if the grid is valid or not
     */
    @Override
    public boolean isValid() {

        // Check if the island counts were exceeded
        if (!ifIslandSumCorrect()) {
            return false;
        }

        // Check if the sea counts were exceeded
        if (!ifSeaSumCorrect()) {
            return false;
        }

        // Check if there are 2x2 pools
        if (!noPools()) {
            return false;
        }

        if (this.rowCursor == rowDimension-1 && this.colCursor == colDimension-1) {
            if (this.numberOfIslands == this.numberedCells) {
                // Check if all the islands are correct on the whole grid
                if (!islandsAreCorrect()) {
                    return false;
                }
            } else {
                return false;
            }

            // Check if the sea is fully connected on the whole grid
            if (!allSeasConnected()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Method to determine if the current grid is the solution or not
     *
     * @return - if the grid is the solution
     */
    @Override
    public boolean isGoal() {
        // Return true if the grid is at the end of the list
        if (this.rowCursor == rowDimension-1 && this.colCursor == colDimension-1) {
            return true;
        }
        return false;
    }

    /**
     * Returns the string representation of the puzzle, e.g.: <br>
     * <tt><br>
     * 1 . #<br>
     * &#64; . 3<br>
     * 1 . .<br>
     * </tt><br>
     */
    @Override
    public String toString() {
        // Build the string output of the grid by assigning the proper characters to the values of the grid
        StringBuilder result = new StringBuilder();
        for (int row = 0; row < rowDimension; ++row) {
            result.append("\n");
            for (int col = 0; col < colDimension; ++col) {
                if (this.grid[row][col] >= 1 && this.grid[row][col] <= 9) {
                    result.append(String.valueOf(this.grid[row][col]));
                } else if (this.grid[row][col] == 10) {
                    result.append("#");
                } else if (this.grid[row][col] == 11) {
                    result.append("@");
                } else {
                    result.append(".");
                }
                result.append(" ");
            }
        }
        return result.toString();
    }
}