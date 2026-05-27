/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.game;

/**
 *
 * @author lenovo
 */
import java.util.ArrayList;
import java.util.List;


//This class saves a copy of the entire game state at a specific moment. It's used for Undo/Redo functionality.
public class BoardSnapshot {
    private final Point p1Pos;
    private final Point p2Pos;
    private final int p1Walls;  // Player 1's remaining walls
    private final int p2Walls;   // Player 2's remaining walls
    private final List<Walls> walls; // Player 2's remaining walls
    private final int currentPlayerIndex; //Whose turn

    public BoardSnapshot(Board board, int currentPlayerIndex) {
        this.p1Pos = new Point(board.getPlayers().get(0).getPosition().row, board.getPlayers().get(0).getPosition().col);
        this.p2Pos = new Point(board.getPlayers().get(1).getPosition().row, board.getPlayers().get(1).getPosition().col);
        this.p1Walls = board.getPlayers().get(0).getWallsRemaining();
        this.p2Walls = board.getPlayers().get(1).getWallsRemaining();
        // Deep copy - create new Walls objects
       // this.walls = new ArrayList<>(board.getPlacedWalls());
       this.walls = new ArrayList<>();
        for (Walls w : board.getPlacedWalls()) {
              this.walls.add(new Walls( w.getPosition1().row,   w.getPosition1().col,   w.getOrientation()   ));
          }
        this.currentPlayerIndex = currentPlayerIndex;
    }
    
     // Getters
    public Point getP1Pos() { return p1Pos; }
    public Point getP2Pos() { return p2Pos; }
    public int getP1Walls() { return p1Walls; }
    public int getP2Walls() { return p2Walls; }
    public List<Walls> getWalls() { return walls; }
    public int getCurrentPlayerIndex() { return currentPlayerIndex; }
    
}