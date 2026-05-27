package com.mycompany.game;

/**
 * Represents a single candidate move for the AI.
 * Either a pawn move (isWall=false) or a wall placement (isWall=true).
 */
public class AiMove {
    public final int row;
    public final int col;
    public final boolean isWall;
    public final Orientation orientation; // null for pawn moves

    /** Pawn move constructor */
    public AiMove(int row, int col) {
        this.row = row;
        this.col = col;
        this.isWall = false;
        this.orientation = null;
    }

    /** Wall placement constructor */
    public AiMove(int row, int col, Orientation orientation) {
        this.row = row;
        this.col = col;
        this.isWall = true;
        this.orientation = orientation;
    }

    @Override
    public String toString() {
        if (isWall) return "Wall(" + row + "," + col + "," + orientation + ")";
        return "Move(" + row + "," + col + ")";
    }
}
