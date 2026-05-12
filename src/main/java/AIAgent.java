import java.util.*;

public class AIAgent {
  Random rand;

  public AIAgent() {
    rand = new Random();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // EXISTING AGENTS (kept for backward compatibility)
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Random agent: selects a random valid move
   */
  public Move randomMove(Stack possibilities) {
    if (possibilities.isEmpty()) return null;
    int moveID = rand.nextInt(possibilities.size());
    System.out.println("AI agent selected RandomMove: " + moveID);
    for (int i = 1; i < (possibilities.size() - moveID); i++) {
      possibilities.pop();
    }
    Move selectedMove = (Move) possibilities.pop();
    return selectedMove;
  }

  /**
   * Next Best Move agent: evaluates immediate captures and center control
   */
  public Move nextBestMove(Stack whitePossibleMoves, Stack blackPossibleMoves) {
    Stack whitePieces = (Stack) whitePossibleMoves.clone();
    Stack black = (Stack) blackPossibleMoves.clone();
    Move whiteMove, attackingMove, regularMove;
    int Points = 0;
    int chosenPiece = 0;
    Square blackPosition;
    attackingMove = null;

    while (!whitePossibleMoves.empty()) {
      whiteMove = (Move) whitePossibleMoves.pop();
      regularMove = whiteMove;

      // Assign points for center control
      if ((regularMove.getStart().getYC() < regularMove.getLanding().getYC())
              && (regularMove.getLanding().getXC() == 3 || regularMove.getLanding().getXC() == 4)
              && (regularMove.getLanding().getYC() == 3 || regularMove.getLanding().getYC() == 4)) {
        Points = 10;
        if (Points > chosenPiece) {
          chosenPiece = Points;
          attackingMove = regularMove;
        }
      }

      // Check for captures
      while (!black.isEmpty()) {
        Points = 0;
        blackPosition = (Square) black.pop();
        if ((regularMove.getLanding().getXC() == blackPosition.getXC())
                && (regularMove.getLanding().getYC() == blackPosition.getYC())) {
          Points = getPieceValue(blackPosition.getName());
        }
        if (Points > chosenPiece) {
          chosenPiece = Points;
          attackingMove = regularMove;
        }
      }
      black = (Stack) blackPossibleMoves.clone();
    }

    if (chosenPiece > 0) {
      System.out.println("AI agent selected NextBestMove: " + chosenPiece);
      return attackingMove;
    }

    return randomMove(whitePieces);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // MINIMAX WITH ALPHA-BETA PRUNING (NEW IMPLEMENTATION)
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Minimax agent with alpha-beta pruning.
   * Looks ahead multiple moves to find the best move.
   *
   * @param whiteMoves All possible white moves from current position
   * @param blackMoves All possible black moves from current position
   * @param depth Search depth (2-4 recommended, higher = stronger but slower)
   * @param boardState Current 8x8 board state (piece names or "empty")
   * @return Best move for White according to minimax evaluation
   */
  public Move minimaxMove(Stack whiteMoves, Stack blackMoves, int depth, String[][] boardState) {
    if (whiteMoves.isEmpty()) return randomMove(whiteMoves);

    long startTime = System.currentTimeMillis();
    System.out.println("AI: Starting minimax search at depth " + depth);

    Move bestMove = null;
    int bestScore = Integer.MIN_VALUE;
    int alpha = Integer.MIN_VALUE;
    int beta = Integer.MAX_VALUE;

    Stack movesToEvaluate = (Stack) whiteMoves.clone();

    while (!movesToEvaluate.isEmpty()) {
      Move move = (Move) movesToEvaluate.pop();

      // Simulate this move
      String[][] newBoard = simulateMove(boardState, move);

      // Evaluate this position (White just moved, so Black's turn = minimizing)
      int score = minimax(newBoard, depth - 1, alpha, beta, false);

      if (score > bestScore) {
        bestScore = score;
        bestMove = move;
      }

      alpha = Math.max(alpha, score);
    }

    long elapsed = System.currentTimeMillis() - startTime;
    System.out.println("AI: Minimax selected move with score " + bestScore + " (took " + elapsed + "ms)");

    return bestMove != null ? bestMove : randomMove(whiteMoves);
  }

  /**
   * Recursive minimax with alpha-beta pruning.
   *
   * @param board Current board state
   * @param depth Remaining search depth
   * @param alpha Best score for maximizer (White)
   * @param beta Best score for minimizer (Black)
   * @param isMaximizing True if White's turn (maximizing), false if Black's turn (minimizing)
   * @return Evaluated score for this position
   */
  private int minimax(String[][] board, int depth, int alpha, int beta, boolean isMaximizing) {
    // Base case: reached max depth or game over
    if (depth == 0 || isGameOver(board)) {
      return evaluateBoard(board);
    }

    if (isMaximizing) {
      // White's turn - maximize score
      int maxScore = Integer.MIN_VALUE;
      List<Move> possibleMoves = generateMoves(board, true); // true = White

      for (Move move : possibleMoves) {
        String[][] newBoard = simulateMove(board, move);
        int score = minimax(newBoard, depth - 1, alpha, beta, false);
        maxScore = Math.max(maxScore, score);
        alpha = Math.max(alpha, score);

        // Alpha-beta pruning
        if (beta <= alpha) {
          break; // Beta cutoff
        }
      }
      return maxScore;

    } else {
      // Black's turn - minimize score
      int minScore = Integer.MAX_VALUE;
      List<Move> possibleMoves = generateMoves(board, false); // false = Black

      for (Move move : possibleMoves) {
        String[][] newBoard = simulateMove(board, move);
        int score = minimax(newBoard, depth - 1, alpha, beta, true);
        minScore = Math.min(minScore, score);
        beta = Math.min(beta, score);

        // Alpha-beta pruning
        if (beta <= alpha) {
          break; // Alpha cutoff
        }
      }
      return minScore;
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // BOARD EVALUATION AND UTILITIES
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Evaluates the board position.
   * Positive score = good for White, negative = good for Black.
   */
  private int evaluateBoard(String[][] board) {
    int score = 0;

    // Piece-square tables for positional bonuses
    int[][] pawnTable = {
            {  0,  0,  0,  0,  0,  0,  0,  0},
            { 50, 50, 50, 50, 50, 50, 50, 50},
            { 10, 10, 20, 30, 30, 20, 10, 10},
            {  5,  5, 10, 25, 25, 10,  5,  5},
            {  0,  0,  0, 20, 20,  0,  0,  0},
            {  5, -5,-10,  0,  0,-10, -5,  5},
            {  5, 10, 10,-20,-20, 10, 10,  5},
            {  0,  0,  0,  0,  0,  0,  0,  0}
    };

    int[][] knightTable = {
            {-50,-40,-30,-30,-30,-30,-40,-50},
            {-40,-20,  0,  0,  0,  0,-20,-40},
            {-30,  0, 10, 15, 15, 10,  0,-30},
            {-30,  5, 15, 20, 20, 15,  5,-30},
            {-30,  0, 15, 20, 20, 15,  0,-30},
            {-30,  5, 10, 15, 15, 10,  5,-30},
            {-40,-20,  0,  5,  5,  0,-20,-40},
            {-50,-40,-30,-30,-30,-30,-40,-50}
    };

    for (int y = 0; y < 8; y++) {
      for (int x = 0; x < 8; x++) {
        String piece = board[y][x];
        if (piece == null || piece.equals("empty")) continue;

        int pieceValue = getPieceValue(piece);
        int positionalBonus = 0;

        // Add positional bonuses
        if (piece.equals("WhitePawn")) {
          positionalBonus = pawnTable[y][x];
        } else if (piece.equals("BlackPawn")) {
          positionalBonus = -pawnTable[7 - y][x]; // Mirror for Black
        } else if (piece.equals("WhiteKnight")) {
          positionalBonus = knightTable[y][x];
        } else if (piece.equals("BlackKnight")) {
          positionalBonus = -knightTable[7 - y][x];
        }

        // Add to score (positive for White, negative for Black)
        if (piece.contains("White")) {
          score += pieceValue + positionalBonus;
        } else {
          score -= pieceValue + positionalBonus;
        }
      }
    }

    return score;
  }

  /**
   * Returns material value of a piece.
   */
  private int getPieceValue(String pieceName) {
    if (pieceName == null || pieceName.equals("empty")) return 0;

    if (pieceName.contains("Pawn")) return 100;
    if (pieceName.contains("Knight")) return 320;
    if (pieceName.contains("Bishop")) return 330;
    if (pieceName.contains("Rook")) return 500;
    if (pieceName.contains("Queen")) return 900;
    if (pieceName.contains("King")) return 20000;
    return 0;
  }

  /**
   * Checks if the game is over (King captured).
   */
  private boolean isGameOver(String[][] board) {
    boolean whiteKing = false;
    boolean blackKing = false;

    for (int y = 0; y < 8; y++) {
      for (int x = 0; x < 8; x++) {
        String piece = board[y][x];
        if (piece == null) continue;
        if (piece.equals("WhiteKing")) whiteKing = true;
        if (piece.equals("BlackKing")) blackKing = true;
      }
    }

    return !whiteKing || !blackKing;
  }

  /**
   * Simulates a move on the board and returns the new board state.
   */
  private String[][] simulateMove(String[][] board, Move move) {
    String[][] newBoard = new String[8][8];

    // Copy board
    for (int y = 0; y < 8; y++) {
      for (int x = 0; x < 8; x++) {
        newBoard[y][x] = board[y][x];
      }
    }

    // Apply move
    int startX = move.getStart().getXC();
    int startY = move.getStart().getYC();
    int endX = move.getLanding().getXC();
    int endY = move.getLanding().getYC();

    newBoard[endY][endX] = newBoard[startY][startX];
    newBoard[startY][startX] = "empty";

    return newBoard;
  }

  /**
   * Generates all legal moves for a given side.
   * Note: This is a simplified version - in production you'd want full move validation.
   */
  private List<Move> generateMoves(String[][] board, boolean isWhite) {
    List<Move> moves = new ArrayList<>();
    String color = isWhite ? "White" : "Black";

    // Find all pieces of the given color
    for (int y = 0; y < 8; y++) {
      for (int x = 0; x < 8; x++) {
        String piece = board[y][x];
        if (piece == null || !piece.contains(color)) continue;

        // Generate moves for this piece (simplified - just checks adjacent/L-shape/diagonals)
        if (piece.contains("Pawn")) {
          addPawnMoves(moves, board, x, y, isWhite);
        } else if (piece.contains("Knight")) {
          addKnightMoves(moves, board, x, y, isWhite);
        } else if (piece.contains("King")) {
          addKingMoves(moves, board, x, y, isWhite);
        } else if (piece.contains("Rook")) {
          addStraightMoves(moves, board, x, y, isWhite);
        } else if (piece.contains("Bishop")) {
          addDiagonalMoves(moves, board, x, y, isWhite);
        } else if (piece.contains("Queen")) {
          addStraightMoves(moves, board, x, y, isWhite);
          addDiagonalMoves(moves, board, x, y, isWhite);
        }
      }
    }

    return moves;
  }

  private void addPawnMoves(List<Move> moves, String[][] board, int x, int y, boolean isWhite) {
    int direction = isWhite ? 1 : -1; // White moves down (+Y), Black moves up (-Y)

    // Forward 1
    int ny = y + direction;
    if (ny >= 0 && ny < 8 && (board[ny][x] == null || board[ny][x].equals("empty"))) {
      moves.add(new Move(new Square(x, y, board[y][x]), new Square(x, ny)));
    }

    // Forward 2 from start
    int startRow = isWhite ? 1 : 6;
    if (y == startRow) {
      int ny2 = y + 2 * direction;
      if (board[ny][x].equals("empty") && board[ny2][x].equals("empty")) {
        moves.add(new Move(new Square(x, y, board[y][x]), new Square(x, ny2)));
      }
    }

    // Captures
    for (int dx : new int[]{-1, 1}) {
      int nx = x + dx;
      if (nx >= 0 && nx < 8 && ny >= 0 && ny < 8) {
        String target = board[ny][nx];
        if (target != null && !target.equals("empty") &&
                target.contains(isWhite ? "Black" : "White")) {
          moves.add(new Move(new Square(x, y, board[y][x]), new Square(nx, ny)));
        }
      }
    }
  }

  private void addKnightMoves(List<Move> moves, String[][] board, int x, int y, boolean isWhite) {
    int[][] offsets = {{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}};
    for (int[] offset : offsets) {
      int nx = x + offset[0];
      int ny = y + offset[1];
      if (nx >= 0 && nx < 8 && ny >= 0 && ny < 8) {
        String target = board[ny][nx];
        if (target == null || target.equals("empty") ||
                target.contains(isWhite ? "Black" : "White")) {
          moves.add(new Move(new Square(x, y, board[y][x]), new Square(nx, ny)));
        }
      }
    }
  }

  private void addKingMoves(List<Move> moves, String[][] board, int x, int y, boolean isWhite) {
    int[][] offsets = {{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}};
    for (int[] offset : offsets) {
      int nx = x + offset[0];
      int ny = y + offset[1];
      if (nx >= 0 && nx < 8 && ny >= 0 && ny < 8) {
        String target = board[ny][nx];
        if (target == null || target.equals("empty") ||
                target.contains(isWhite ? "Black" : "White")) {
          moves.add(new Move(new Square(x, y, board[y][x]), new Square(nx, ny)));
        }
      }
    }
  }

  private void addStraightMoves(List<Move> moves, String[][] board, int x, int y, boolean isWhite) {
    int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
    for (int[] dir : dirs) {
      for (int step = 1; step < 8; step++) {
        int nx = x + dir[0] * step;
        int ny = y + dir[1] * step;
        if (nx < 0 || nx >= 8 || ny < 0 || ny >= 8) break;

        String target = board[ny][nx];
        if (target == null || target.equals("empty")) {
          moves.add(new Move(new Square(x, y, board[y][x]), new Square(nx, ny)));
        } else {
          if (target.contains(isWhite ? "Black" : "White")) {
            moves.add(new Move(new Square(x, y, board[y][x]), new Square(nx, ny)));
          }
          break;
        }
      }
    }
  }

  private void addDiagonalMoves(List<Move> moves, String[][] board, int x, int y, boolean isWhite) {
    int[][] dirs = {{1,1},{1,-1},{-1,1},{-1,-1}};
    for (int[] dir : dirs) {
      for (int step = 1; step < 8; step++) {
        int nx = x + dir[0] * step;
        int ny = y + dir[1] * step;
        if (nx < 0 || nx >= 8 || ny < 0 || ny >= 8) break;

        String target = board[ny][nx];
        if (target == null || target.equals("empty")) {
          moves.add(new Move(new Square(x, y, board[y][x]), new Square(nx, ny)));
        } else {
          if (target.contains(isWhite ? "Black" : "White")) {
            moves.add(new Move(new Square(x, y, board[y][x]), new Square(nx, ny)));
          }
          break;
        }
      }
    }
  }

  /**
   * Two levels deep agent (kept for compatibility).
   * Now uses minimax with depth=2.
   */
  public Move twoLevelsDeep(Stack whitePossibleMoves, Stack blackPossibleMoves, int depth) {
    // For now, fall back to nextBestMove since we need board state for true minimax
    // To use real minimax, ChessProject needs to pass board state
    return nextBestMove(whitePossibleMoves, blackPossibleMoves);
  }
}