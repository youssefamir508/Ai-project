/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Admin
 */

    
  public class Player {
      
    private final int id;
    private Point position;
    private int wallsRemaining;
    private final int targetRow; 
    private final String color;

    public Player(int id, Point startPos, int targetRow, String color) {
        this.id = id;
        this.position = startPos;
        this.targetRow = targetRow;
        this.color = color;
        this.wallsRemaining = 10; 
    }

    public void moveTo(Point newPos) {
        this.position = newPos;
    }

    public boolean useWall() {
        if (wallsRemaining > 0) {
            wallsRemaining--;
            return true;
        }
        return false;
    }

    public boolean hasWon() {
        return this.position.row == targetRow;
    }

    // Getters
    public Point getPosition() { return position; }
    public int getWallsRemaining() { return wallsRemaining; }
    public int getId() { return id; }
}

