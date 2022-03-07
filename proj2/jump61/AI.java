
package jump61;

import java.util.ArrayList;
import java.util.Random;

import static jump61.Side.*;

/** An automated Player.
 *  @author P. N. Hilfinger
 */
class AI extends Player {

    /** Depth of heuristic. **/
    public static final int TEST_DEPTH = 4;
    /** Minimum heuristic value. **/
    public static final int MIN = Integer.MIN_VALUE;
    /** Maximum heuristic value. **/
    public static final int MAX = Integer.MAX_VALUE;

    /** A new player of GAME initially COLOR that chooses moves automatically.
     *  SEED provides a random-number seed used for choosing moves.
     */
    AI(Game game, Side color, long seed) {
        super(game, color);
        _random = new Random(seed);
    }

    @Override
    String getMove() {
        Board board = getGame().getBoard();

        assert getSide() == board.whoseMove();
        int choice = searchForMove();
        getGame().reportMove(board.row(choice), board.col(choice));
        return String.format("%d %d", board.row(choice), board.col(choice));
    }

    /** Return a move after searching the game tree to DEPTH>0 moves
     *  from the current position. Assumes the game is not over. */
    private int searchForMove() {
        Board work = new Board(getBoard());
        int value = 0;
        assert getSide() == work.whoseMove();
        _foundMove = -1;
        if (getSide() == RED) {
            value = 0;
            minMax(work, TEST_DEPTH, true, 1, MIN, MAX);
        } else {
            value = 0;
            minMax(work, TEST_DEPTH, true, -1, MIN, MAX);
        }

        return _foundMove;
    }


    /** Find a move from position BOARD and return its value, recording
     *  the move found in _foundMove iff SAVEMOVE. The move
     *  should have maximal value or have value > BETA if SENSE==1,
     *  and minimal value or value < ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels.  Searching at level 0 simply returns a static estimate
     *  of the board value and does not set _foundMove. If the game is over
     *  on BOARD, does not set _foundMove. */
    private int minMax(Board board, int depth, boolean saveMove,
                       int sense, int alpha, int beta) {
        if (depth == 0) {
            return staticEval(board, MAX);
        } else {
            ArrayList<Integer> moves = new ArrayList<>();
            for (int i = 0; i < board.size(); i++) {
                for (int j = 0; j < board.size(); j++) {
                    if (board.isLegal(getSide(), i + 1, j + 1)
                            && board.isLegal(getSide())) {
                        moves.add(board.sqNum(i + 1, j + 1));
                    }
                }
            }
            int aScore = alpha;
            int bScore = beta;
            for (int i = 0; i < moves.size(); i++) {
                if (sense == 1) {
                    Board temp = new Board(board);
                    temp.addSpot(getSide(), moves.get(i));
                    int best = minMax(board, depth - 1,
                            false, -1, aScore, bScore);
                    if (best > aScore) {
                        aScore = best;
                        if (saveMove) {
                            _foundMove = moves.get(i);
                            board.addSpot(getSide(), moves.get(i));
                        }
                    }
                } else {
                    Board temp2 = new Board(board);
                    temp2.addSpot(getSide(), moves.get(i));
                    int best = minMax(board, depth - 1,
                            false, 1, aScore, bScore);
                    if (best < bScore) {
                        bScore = best;
                        if (saveMove) {
                            _foundMove = moves.get(i);
                            board.addSpot(getSide(), moves.get(i));
                        }
                    }
                }
            }
        }
        return staticEval(board, MAX);
    }

    /** Return a heuristic estimate of the value of board position B.
     *  Use WINNINGVALUE to indicate a win for Red and -WINNINGVALUE to
     *  indicate a win for Blue. */
    private int staticEval(Board b, int winningValue) {
        int value = b.bluePieces() + b.redPieces();
        if (winningValue > value) {
            return value;
        }
        return winningValue;
    }

    /** A random-number generator used for move selection. */
    private Random _random;

    /** Used to convey moves discovered by minMax. */
    private int _foundMove;
}
