/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.game;

import java.util.*;

/**
 * Evaluates a board position from the perspective of a given player.
 *
 * Score = (opponent's BFS distance to goal) - (my BFS distance to goal)
 *         + wall advantage bonus
 *
 * A higher score means the position is better for playerIdx.
 */
//HeuristicProvider = function that gives a score to a board so AI knows which move is better
// use it in ai_player

public class HeuristicProvider {
    
   /**
     * @param board      current board state
     * @param playerIdx  0-based index of the player we are evaluating FOR
     * @return           integer score — higher is better for playerIdx
     */
            MovementValidator validator = new MovementValidator();

    public int evaluate(Board board, int playerIdx) {
        Player me  = board.getPlayers().get(playerIdx);
        Player opp = board.getPlayers().get(1 - playerIdx);

        // --- Win / loss detection ---
        if (me.hasWon())  return  10_000;
        if (opp.hasWon()) return -10_000;

        // --- BFS shortest-path distances ---
        int myDist  = bfsDistance(me,  board);
        int oppDist = bfsDistance(opp, board);

        // Unreachable (should not happen after wall validation, but guard anyway)
        if (myDist  == Integer.MAX_VALUE) return -10_000;
        if (oppDist == Integer.MAX_VALUE) return  10_000;

        // Core score: I want my distance small and opponent's large
        int score = (oppDist - myDist) * 10;

        // --- Wall count bonus: having walls in reserve is an asset ---
        int wallBonus = (me.getWallsRemaining() - opp.getWallsRemaining()) * 2;
        score += wallBonus;

        return score;
    }

    /**
     * BFS from the player's current position to any cell in their target row.
     * Returns Integer.MAX_VALUE if no path exists.
     */
    public int bfsDistance(Player player, Board board) {
        Queue<Point> queue = new LinkedList<>();
        Map<Point, Integer> dist = new HashMap<>();

        Point start = player.getPosition();
        queue.add(start);
        dist.put(start, 0);

        int goalRow = player.getTargetRow();

        while (!queue.isEmpty()) {
            Point cur = queue.poll();
            int d = dist.get(cur);

            if (cur.row == goalRow) return d;

            int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
            for (int[] dir : dirs) {
                int nr = cur.row + dir[0];
                int nc = cur.col + dir[1];
                if (nr < 0 || nr >= 9 || nc < 0 || nc >= 9) continue;
                Point next = new Point(nr, nc);
                if (dist.containsKey(next)) continue;
                if (validator.validateMove(cur, next, board)) {
                    dist.put(next, d + 1);
                    queue.add(next);
                }
            }
        }
        return Integer.MAX_VALUE;
    }
}
