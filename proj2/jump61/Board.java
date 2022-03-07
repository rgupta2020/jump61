
package jump61;

import java.util.ArrayDeque;
import java.util.Formatter;

import java.util.function.Consumer;

import static jump61.Side.*;

/** Represents the state of a Jump61 game.  Squares are indexed either by
 *  row and column (between 1 and size()), or by square number, numbering
 *  squares by rows, with squares in row 1 numbered from 0 to size()-1, in
 *  row 2 numbered from size() to 2*size() - 1, etc. (i.e., row-major order).
 *
 *  A Board may be given a notifier---a Consumer<Board> whose
 *  .accept method is called whenever the Board's contents are changed.
 *
 *  @author Rishit Gupta
 */
class Board {

    /** An uninitialized Board.  Only for use by subtypes. */
    protected Board() {
        _notifier = NOP;
    }

    /** An N x N board in initial configuration. */
    Board(int N) {
        this();
        _boardGrid = new Square[N * N];
        for (int i = 0; i < N * N; i++) {
            _boardGrid[i] = Square.square(WHITE, 1);
        }
        _size = N;
        _history = new ArrayDeque<>();
    }

    /** A board whose initial contents are copied from BOARD0, but whose
     *  undo history is clear, and whose notifier does nothing. */
    Board(Board board0) {
        this(board0.size());
        for (int i = 0; i < board0.size() * board0.size(); i++) {
            _boardGrid[i] = board0.get(i);
        }
        _history = new ArrayDeque<>();
        _size = board0.size();
        _readonlyBoard = new ConstantBoard(this);
    }

    /** Returns a readonly version of this board. */
    Board readonlyBoard() {
        return _readonlyBoard;
    }

    /** (Re)initialize me to a cleared board with N squares on a side. Clears
     *  the undo history and sets the number of moves to 0. */
    void clear(int N) {
        _boardGrid = new Square[N * N];
        for (int i = 0; i < N * N; i++) {
            _boardGrid[i] = Square.square(WHITE, 1);
        }
        _size = (int) Math.sqrt(_boardGrid.length);
        _history.clear();
        announce();
    }

    /** Copy the contents of BOARD into me. */
    void copy(Board board) {
        internalCopy(board);
    }

    /** Copy the contents of BOARD into me, without modifying my undo
     *  history. Assumes BOARD and I have the same size. */
    private void internalCopy(Board board) {
        assert size() == board.size();
        for (int i = 0; i < board.size() * board.size(); i++) {
            _boardGrid[i] = board.get(i);
        }
        _size = (int) Math.sqrt(_boardGrid.length);
    }

    /** Return the number of rows and of columns of THIS. */
    int size() {
        return (int) Math.sqrt(_boardGrid.length);
    }

    /** Returns the contents of the square at row R, column C
     *  1 <= R, C <= size (). */
    Square get(int r, int c) {
        return get(sqNum(r, c));
    }

    /** Returns the contents of square #N, numbering squares by rows, with
     *  squares in row 1 number 0 - size()-1, in row 2 numbered
     *  size() - 2*size() - 1, etc. */

    Square get(int n) {
        assert exists(n);
        return _boardGrid[n];
    }

    /** Returns the total number of spots on the board. */
    int numPieces() {
        int count = 0;
        for (int i = 0; i < size() * size(); i++) {
            count += _boardGrid[i].getSpots();
        }
        return count;
    }

    /** Returns the total number of blue spots on the board. */
    int bluePieces() {
        int count = 0;
        for (int i = 0; i < size() * size(); i++) {
            if (get(i).getSide() == BLUE) {
                count += get(i).getSpots();
            }
        }
        return count;
    }

    /** Returns the total number of red spots on the board. */
    int redPieces() {
        int count = 0;
        for (int i = 0; i < size() * size(); i++) {
            if (get(i).getSide() == RED) {
                count += get(i).getSpots();
            }
        }
        return count;
    }

    /** Returns the Side of the player who would be next to move.  If the
     *  game is won, this will return the loser (assuming legal position). */
    Side whoseMove() {
        return ((numPieces() + size()) & 1) == 0 ? RED : BLUE;
    }

    /** Return true iff row R and column C denotes a valid square. */
    final boolean exists(int r, int c) {
        return 1 <= r && r <= size() && 1 <= c && c <= size();
    }

    /** Return true iff S is a valid square number. */
    final boolean exists(int s) {
        int N = size();
        return 0 <= s && s < N * N;
    }

    /** Return the row number for square #N. */
    final int row(int n) {
        return n / size() + 1;
    }

    /** Return the column number for square #N. */
    final int col(int n) {
        return n % size() + 1;
    }

    /** Return the square number of row R, column C. */
    final int sqNum(int r, int c) {
        return (c - 1) + (r - 1) * size();
    }

    /** Return a string denoting move (ROW, COL)N. */
    String moveString(int row, int col) {
        return String.format("%d %d", row, col);
    }

    /** Return a string denoting move N. */
    String moveString(int n) {
        return String.format("%d %d", row(n), col(n));
    }

    /** Returns true iff it would currently be legal for PLAYER to add a spot
     to square at row R, column C. */
    boolean isLegal(Side player, int r, int c) {
        return isLegal(player, sqNum(r, c));
    }

    /** Returns true iff it would currently be legal for PLAYER to add a spot
     *  to square #N. */
    boolean isLegal(Side player, int n) {
        if (get(n).getSide().equals(player) || get(n).getSide().equals(WHITE)) {
            return true;
        }
        return false;
    }

    /** Returns true iff PLAYER is allowed to move at this point. */
    boolean isLegal(Side player) {
        if (player == whoseMove()) {
            return true;
        }
        return false;
    }

    /** Returns the winner of the current position, if the game is over,
     *  and otherwise null. */
    final Side getWinner() {
        Side player = get(0).getSide();

        for (int i = 1; i < size() * size(); i++) {
            if (!get(i).getSide().equals(player) || player == WHITE) {
                return null;
            }
        }
        return player;
    }

    /** Return the number of squares of given SIDE. */
    int numOfSide(Side side) {
        int count = 0;
        for (int i = 0; i < size() * size(); i++) {
            if (get(i).getSide().equals(side)) {
                count = count + 1;
            }
        }
        return count;
    }

    /** Add a spot from PLAYER at row R, column C.  Assumes
     *  isLegal(PLAYER, R, C). */
    void addSpot(Side player, int r, int c) {
        addSpot(player, sqNum(r, c));
    }

    /** Add a spot from PLAYER at square #N.  Assumes isLegal(PLAYER, N). */
    void addSpot(Side player, int n) {
        markUndo();

        int num = get(n).getSpots() + 1;
        internalSet(n, num, player);

        if (get(n).getSpots() > neighbors(n)) {
            jump(n);
        }

        int jumps = 1;
        while (getWinner() == null && jumps < size() * size()) {
            for (int i = 0; i < size() * size(); i++) {
                if (get(i).getSpots() > neighbors(i)) {
                    jump(i);
                }
            }
            jumps += 1;
        }
        announce();
    }

    /** Set the square at row R, column C to NUM spots (0 <= NUM), and give
     *  it color PLAYER if NUM > 0 (otherwise, white). */
    void set(int r, int c, int num, Side player) {
        internalSet(r, c, num, player);
        announce();
    }

    /** Set the square at row R, column C to NUM spots (0 <= NUM), and give
     *  it color PLAYER if NUM > 0 (otherwise, white).  Does not announce
     *  changes. */
    private void internalSet(int r, int c, int num, Side player) {
        internalSet(sqNum(r, c), num, player);
    }

    /** Set the square #N to NUM spots (0 <= NUM), and give it color PLAYER
     *  if NUM > 0 (otherwise, white). Does not announce changes. */
    private void internalSet(int n, int num, Side player) {

        if (num > 0) {
            _boardGrid[n] = Square.square(player, num);
        } else if (num == 0) {
            _boardGrid[n] = Square.square(WHITE, 1);
        } else {
            throw new GameException("Invalid num spots");
        }

    }

    /** Undo the effects of one move (that is, one addSpot command).  One
     *  can only undo back to the last point at which the undo history
     *  was cleared, or the construction of this Board. */
    void undo() {
        internalCopy(_history.pop());
    }

    /** Record the beginning of a move in the undo history. */
    private void markUndo() {
        Board C = new Board(this);
        _history.push(C);
    }

    /** Add DELTASPOTS spots of side PLAYER to row R, column C,
     *  updating counts of numbers of squares of each color. */
    private void simpleAdd(Side player, int r, int c, int deltaSpots) {
        internalSet(r, c, deltaSpots + get(r, c).getSpots(), player);
    }

    /** Add DELTASPOTS spots of color PLAYER to square #N,
     *  updating counts of numbers of squares of each color. */
    private void simpleAdd(Side player, int n, int deltaSpots) {
        internalSet(n, deltaSpots + get(n).getSpots(), player);
    }

    /** Used in jump to keep track of squares needing processing.  Allocated
     *  here to cut down on allocations. */
    private final ArrayDeque<Integer> _workQueue = new ArrayDeque<>();

    /** Do all jumping on this board, assuming that initially, S is the only
     *  square that might be over-full. */
    public void jump(int S) {
        internalSet(S, 1, get(S).getSide());

        if (col(S) + 1 <= size()) {
            _workQueue.add(S + 1);
        }
        if (col(S) - 1 > 0) {
            _workQueue.add(S - 1);
        }
        if (row(S) + 1 <= size()) {
            _workQueue.add(S + size());
        }
        if (row(S) - 1 > 0) {
            _workQueue.add(S - size());
        }
        while (getWinner() == null && !_workQueue.isEmpty()) {
            int first = _workQueue.removeFirst();
            simpleAdd(get(S).getSide(), first, 1);
        }
    }

    /** Returns my dumped representation. */
    @Override
    public String toString() {
        String dump = "===" + "\n";
        for (int i = 0; i < size(); i++) {
            dump += "    ";
            for (int j = 0; j < size(); j++) {
                Square curr = get(sqNum(i + 1, j + 1));
                dump += curr.getSpots();
                if (curr.getSide().equals(WHITE)) {
                    dump += "- ";
                } else if (curr.getSide().equals(RED)) {
                    dump += "r ";
                } else {
                    dump += "b ";
                }
            }
            dump += "\n";
        }
        if (getWinner() != null) {
            dump += "===";
            dump +=  "* " + getWinner().toCapitalizedString() + " wins";
            return dump;
        }
        dump += "===";
        return dump;
    }

    /** Returns an external rendition of me, suitable for human-readable
     *  textual display, with row and column numbers.  This is distinct
     *  from the dumped representation (returned by toString). */
    public String toDisplayString() {
        String[] lines = toString().trim().split("\\R");
        Formatter out = new Formatter();
        for (int i = 1; i + 1 < lines.length; i += 1) {
            out.format("%2d %s%n", i, lines[i].trim());
        }
        out.format("  ");
        for (int i = 1; i <= size(); i += 1) {
            out.format("%3d", i);
        }
        return out.toString();
    }

    /** Returns the number of neighbors of the square at row R, column C. */
    int neighbors(int r, int c) {
        int size = size();
        int n;
        n = 0;
        if (r > 1) {
            n += 1;
        }
        if (c > 1) {
            n += 1;
        }
        if (r < size) {
            n += 1;
        }
        if (c < size) {
            n += 1;
        }
        return n;
    }

    /** Returns the number of neighbors of square #N. */
    int neighbors(int n) {
        return neighbors(row(n), col(n));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Board)) {
            return false;
        } else {
            Board B = (Board) obj;
            return this == obj;
        }
    }

    @Override
    public int hashCode() {
        return numPieces();
    }

    /** Set my notifier to NOTIFY. */
    public void setNotifier(Consumer<Board> notify) {
        _notifier = notify;
        announce();
    }

    /** Take any action that has been set for a change in my state. */
    private void announce() {
        _notifier.accept(this);
    }

    /** A notifier that does nothing. */
    private static final Consumer<Board> NOP = (s) -> { };

    /** A read-only version of this Board. */
    private ConstantBoard _readonlyBoard;

    /** Use _notifier.accept(B) to announce changes to this board. */
    private Consumer<Board> _notifier;

    /** Holds board values. **/
    private Square[] _boardGrid;

    /** Holds size of board. **/
    private int _size;

    /** Holds history of moves. **/
    private ArrayDeque<Board> _history;

}
