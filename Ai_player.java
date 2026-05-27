/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.game;

/**
 * AI player using Minimax with Alpha-Beta pruning.
 *
 * Medium difficulty: search depth = 3
 *   - Considers all pawn moves
 *   - Considers a filtered set of wall placements (those that meaningfully
 *     lengthen the opponent's path) to keep search tractable.
 *
 * Usage from your game loop:
 *
 *   Ai_player ai = new Ai_player(1);          // AI controls player index 1
 *   AiMove best  = ai.getBestMove(engine);
 *   engine.playTurn(best.row, best.col, best.isWall, best.orientation);
 */
//use heuristicprovider class
// Using Minimax with limited depth:
//The difficulty is controlled by the depth parameter in Minimax:
 
 
 
import java.util.*;
 
 public class Ai_player {
     // ------------------------------------------------------------------ //
    //  Configuration
    // ------------------------------------------------------------------ //
 
    /** 0-based index of the AI-controlled player (0 or 1). */
    private final int aiIndex;
 
    /**
     * Search depth by difficulty:
     *   Easy   (1) → depth 1  (looks 1 move ahead, often random-ish)
     *   Medium (2) → depth 3  (balanced, default)
     *   Hard   (3) → depth 5  (strong, slow on complex positions)
     */
    private final int searchDepth;
 
    /** Maximum wall candidates to consider per node (performance guard). */
    private static final int MAX_WALL_CANDIDATES = 6;
 
    // ------------------------------------------------------------------ //
    //  Helpers
    // ------------------------------------------------------------------ //
 
    private final HeuristicProvider heuristic = new HeuristicProvider();
    private final MovementValidator validator  = new MovementValidator();
    private final Pathfinder         pathfinder = new Pathfinder();
 
    // ------------------------------------------------------------------ //
    //  Constructor
    // ------------------------------------------------------------------ //
 
    /** Create AI for player at aiIndex (0-based). difficulty: 1=Easy, 2=Medium, 3=Hard */
    public Ai_player(int aiIndex, int difficulty) {
        this.aiIndex = aiIndex;
        switch (difficulty) {
            case 1:  this.searchDepth = 1; break;
            case 3:  this.searchDepth = 4; break;
            default: this.searchDepth = 3; break;
        }
    }
 
    /** Default constructor – Medium difficulty */
    public Ai_player(int aiIndex) {
        this(aiIndex, 2);
    }
 
    // ------------------------------------------------------------------ //
    //  Public API
    // ------------------------------------------------------------------ //
 
    /**
     * Returns the best AiMove for the AI player given the current engine state.
     * Call this when it is the AI's turn.
     */
    public AiMove getBestMove(GameEngine engine) {
 
        List<AiMove> moves = generateMoves(engine, aiIndex,searchDepth);
 
        AiMove bestMove = null;
        int bestScore   = Integer.MIN_VALUE;
 
        for (AiMove move : moves) {
 
            // Try the move
            boolean ok = applyMove(engine, move);
            if (!ok) continue;
 
            // Recurse: now it's the opponent's turn (minimising)
            int score = minimax(engine, searchDepth - 1, Integer.MIN_VALUE,
                                Integer.MAX_VALUE, false);
 
            // Undo
            engine.undo();
 
            if (score > bestScore) {
                bestScore = score;
                bestMove  = move;
            }
        }
 
        // Fallback: just move forward if nothing was found
        if (bestMove == null && !moves.isEmpty()) {
            bestMove = moves.get(0);
        }
 
        return bestMove;
    }
 
    // ------------------------------------------------------------------ //
    //  Minimax with Alpha-Beta
    // ------------------------------------------------------------------ //
 
    /**
     * @param engine     live engine (mutated and restored during search)
     * @param depth      remaining search depth
     * @param alpha      best score the maximiser is assured of so far
     * @param beta       best score the minimiser is assured of so far
     * @param maximising true  → AI's turn (maximise score)
     *                   false → opponent's turn (minimise score)
     */
    private int minimax(GameEngine engine, int depth,
                        int alpha, int beta, boolean maximising) {
 
        // --- Terminal conditions ---
        if (engine.checkForWin()) {
            // Whoever just moved won; the score is from aiIndex's perspective.
            // If it's now the maximiser's turn the minimiser just won → bad for AI.
            int prevIdx = 1 - (engine.getCurrentPlayer().getId() - 1);
            return prevIdx == aiIndex ? 10_000 + depth : -10_000 - depth;
            //return maximising ? -10_000 - depth : 10_000 + depth;
        }
        if (depth == 0) {
            return heuristic.evaluate(engine.getBoard(), aiIndex);
        }
 
        int currentIdx = engine.getCurrentPlayer().getId() - 1; // convert to 0-based
        List<AiMove> moves = generateMoves(engine, currentIdx,depth);
 
        if (moves.isEmpty()) {
            return heuristic.evaluate(engine.getBoard(), aiIndex);
        }
 
        if (maximising) {
            int maxEval = Integer.MIN_VALUE;
 
            for (AiMove move : moves) {
                boolean ok = applyMove(engine, move);
                if (!ok) continue;
 
                int eval = minimax(engine, depth - 1, alpha, beta, false); ///////////
                engine.undo();
 
                maxEval = Math.max(maxEval, eval);
                alpha   = Math.max(alpha, eval);
                if (beta <= alpha) break; // β-cutoff
            }
            return maxEval == Integer.MIN_VALUE
                   ? heuristic.evaluate(engine.getBoard(), aiIndex)
                   : maxEval;
 
        } else {
            int minEval = Integer.MAX_VALUE;
 
            for (AiMove move : moves) {
                boolean ok = applyMove(engine, move);
                if (!ok) continue;
 
                int eval = minimax(engine, depth - 1, alpha, beta, true);
                engine.undo();
 
                minEval = Math.min(minEval, eval);
                beta    = Math.min(beta, eval);
                if (beta <= alpha) break; // α-cutoff
            }
            return minEval == Integer.MAX_VALUE
                   ? heuristic.evaluate(engine.getBoard(), aiIndex)
                   : minEval;
        }
    }
 
    // ------------------------------------------------------------------ //
    //  Move generation
    // ------------------------------------------------------------------ //
 
    /**
     * Generates candidate moves for the player at playerIdx.
     * Order: pawn moves first (faster to evaluate), then wall placements.
     */
   /* private List<AiMove> generateMoves(GameEngine engine, int playerIdx) {
        List<AiMove> moves = new ArrayList<>();
        moves.addAll(generatePawnMoves(engine, playerIdx));
        moves.addAll(generateWallMoves(engine, playerIdx));
        return moves;
    }*/
    
    
private List<AiMove> generateMoves(GameEngine engine, int playerIdx, int depth) {
    List<AiMove> moves = new ArrayList<>();
    moves.addAll(generatePawnMoves(engine, playerIdx));
    if (depth >= searchDepth - 2)
        moves.addAll(generateWallMoves(engine, playerIdx));

    // Cheap ordering: pawn moves toward goal first, then walls
    // No engine calls — just use position heuristic
    int goalRow = engine.getBoard().getPlayers().get(playerIdx).getTargetRow();
    moves.sort((m1, m2) -> {
        if (m1.isWall != m2.isWall) return m1.isWall ? 1 : -1; // pawns first
        if (!m1.isWall) {
            // closer to goal = better
            int d1 = Math.abs(m1.row - goalRow);
            int d2 = Math.abs(m2.row - goalRow);
            return Integer.compare(d1, d2);
        }
        return 0;
    });

    return moves;
} 
    /** All legal pawn moves (including jumps and diagonal bypasses). */
    private List<AiMove> generatePawnMoves(GameEngine engine, int playerIdx) {
        List<AiMove> moves = new ArrayList<>();
        Board board  = engine.getBoard();
        Player me    = board.getPlayers().get(playerIdx);
        Point  pos   = me.getPosition();
 
        // Normal + jump distances: check up to 2 steps in each orthogonal direction
        // plus diagonals (for bypass).
        int[][] offsets = {
            {1,0},{-1,0},{0,1},{0,-1},   // normal
            {2,0},{-2,0},{0,2},{0,-2},   // straight jumps
            {1,1},{1,-1},{-1,1},{-1,-1}  // diagonal bypass
        };
 
        for (int[] off : offsets) {
            int nr = pos.row + off[0];
            int nc = pos.col + off[1];
            if (nr < 0 || nr >= 9 || nc < 0 || nc >= 9) continue;
            Point to = new Point(nr, nc);
            if (validator.validateMove(pos, to, board, me)) {
                moves.add(new AiMove(nr, nc));
            }
        }
        return moves;
    }
 
    /**
     * Filtered wall placements.
     *
     * Strategy (medium):
     *   Pick walls that increase the opponent's BFS distance by at least 1.
     *   Limit to MAX_WALL_CANDIDATES (sorted by how much they slow the opponent).
     */
    private List<AiMove> generateWallMoves(GameEngine engine, int playerIdx) {
        Board  board = engine.getBoard();
        Player me    = board.getPlayers().get(playerIdx);
 
        if (me.getWallsRemaining() == 0) return Collections.emptyList();
 
        Player opp   = board.getPlayers().get(1 - playerIdx);
        int baseOppDist = heuristic.bfsDistance(opp, board);
 
        List<int[]> candidates = new ArrayList<>(); // {row, col, orientation, gain}
 
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                for (Orientation ori : Orientation.values()) {
 
                    Walls w = new Walls(r, c, ori);
 
                    // Quick duplicate / crossing check before touching engine
                    if (isDuplicateOrCrossing(w, board)) continue;
 
                    // Temporarily add the wall to measure effect
                    board.addWall(w);
 
                    boolean p1Path = pathfinder.hasPath(board.getPlayers().get(0), board);
                    boolean p2Path = pathfinder.hasPath(board.getPlayers().get(1), board);
 
                    if (p1Path && p2Path) {
                        int newOppDist = heuristic.bfsDistance(opp, board);
                        int gain = newOppDist - baseOppDist;
                        if (gain > 0) {
                            candidates.add(new int[]{r, c,
                                ori == Orientation.HORIZONTAL ? 0 : 1, gain});
                        }
                    }
 
                    board.removeWall(w);
                }
            }
        }
 
        // Sort by gain descending, take top N
        candidates.sort((a, b) -> b[3] - a[3]);
 
        List<AiMove> wallMoves = new ArrayList<>();
        int limit = Math.min(candidates.size(), MAX_WALL_CANDIDATES);
        for (int i = 0; i < limit; i++) {
            int[] c = candidates.get(i);
            Orientation ori = c[2] == 0 ? Orientation.HORIZONTAL : Orientation.VERTICAL;
            wallMoves.add(new AiMove(c[0], c[1], ori));
        }
        return wallMoves;
    }
 
    /** Returns true if the wall is a duplicate or crosses an existing wall. */
    private boolean isDuplicateOrCrossing(Walls w, Board board) {
        for (Walls existing : board.getPlacedWalls()) {
            if (existing.equals(w)) return true;
            if (existing.getOrientation() != w.getOrientation()) {
                if (existing.getPosition1().equals(w.getPosition1())) return true;
            }
        }
        return false;
    }
 
    // ------------------------------------------------------------------ //
    //  Apply / undo helpers
    // ------------------------------------------------------------------ //
 
    /**
     * Applies a move through the engine (which also saves an undo snapshot).
     * Returns false if the engine rejected the move.
     */
    private boolean applyMove(GameEngine engine, AiMove move) {
        return engine.playTurn(move.row, move.col, move.isWall, move.orientation);
    }
}