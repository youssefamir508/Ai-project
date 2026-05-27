 /*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.game;

/**
 *
 * @author lenovo
 */
import java.util.*;

//Breadth-First Search (BFS)  //"Can the player reach their goal row without being blocked by walls or the other player?"
//readth-First Search = Explore ALL places at the same distance BEFORE going further.



public class Pathfinder {
    
      MovementValidator validator = new MovementValidator();
    public boolean hasPath(Player player, Board board) { //if true path exists player can win, if false no path
        Queue<Point> queue = new LinkedList<>();   ////nodes i need to check
        Set<Point> visited = new HashSet<>();  //nodes i already visited

        Point start = player.getPosition();
        queue.add(start);
        visited.add(start);

        // Determine goal row
    
       int goalRow = player.getTargetRow();
               
        while (!queue.isEmpty()) {
            Point current = queue.poll();

            // Reached goal?
            if (current.row == goalRow) return true;

            // Check all neighbors
            for (Point neighbor : getValidNeighbors(current, board)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return false;
    }

//From a given square p, find all squares you are allowed to move to
    private List<Point> getValidNeighbors(Point p, Board board) { 
        List<Point> neighbors = new ArrayList<>();
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};  //up down right left  no diagonal

        for (int[] d : dirs) {
            int newRow = p.row + d[0];
            int newCol = p.col + d[1];
            
            // Bounds check - MUST be 0-8
            if (newRow < 0 || newRow >= 9 || newCol < 0 || newCol >= 9) {
                continue;
            }
            
            Point neighbor = new Point(newRow, newCol);
            
            if (validator.validateMove(p, neighbor, board)) {
                neighbors.add(neighbor);
            }
        }
        return neighbors;
    }
    
    
    
}
