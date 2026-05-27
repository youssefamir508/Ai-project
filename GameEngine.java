package com.mycompany.game;

import java.util.Stack;

public class GameEngine {

    private Board board;
    private MovementValidator validator;
    private Pathfinder pathfinder;
    private int currentPlayerIndex;

    private Stack<BoardSnapshot> undoStack = new Stack<>();
    private Stack<BoardSnapshot> redoStack = new Stack<>();

    public GameEngine() {
        this.board = new Board();
        this.validator = new MovementValidator();
        this.pathfinder = new Pathfinder();
        this.currentPlayerIndex = 0;
    }

    public boolean playTurn(int targetRow, int targetCol,
                            boolean isWall, Orientation orientation) {

        Player current = board.getPlayers().get(currentPlayerIndex);

        // save BEFORE move
        saveToUndo();

        if (isWall) {
            return handleWall(targetRow, targetCol, orientation, current);
        } else {
            return handleMove(targetRow, targetCol, current);
        }
    }

    // ---------------- PAWN MOVE ----------------

    private boolean handleMove(int row, int col, Player current) {

        Point to = new Point(row, col);

        // FIX: you MUST pass currentPlayer (not opponent)
        boolean ok = validator.validateMove(
                current.getPosition(),
                to,
                board,
                current
        );

        if (!ok) {
            undoStack.pop();
            return false;
        }

        current.moveTo(to); //function in player changes its position
        switchTurn(); //next player's turn
        redoStack.clear();
        return true;
    }

    // ---------------- WALL ----------------

    private boolean handleWall(int row, int col,
                              Orientation orientation, Player current) {

   
        Walls w = new Walls(row, col, orientation);
        
  
   // duplicate or crossing check
for (Walls existing : board.getPlacedWalls()) {

    // exact duplicate
    if (existing.equals(w)) {
        undoStack.pop();
        return false;
    }

    // crossing
    if (existing.getOrientation() == w.getOrientation()) {
    // Same orientation → overlap check
    if (existing.getOrientation() == Orientation.HORIZONTAL) {
        int er = existing.getPosition1().row;
        int ec = existing.getPosition1().col;
        int nr = w.getPosition1().row;
        int nc = w.getPosition1().col;
        if (er == nr && Math.abs(ec - nc) == 1) {
            undoStack.pop();
            return false;
        }
    } else {
        int er = existing.getPosition1().row;
        int ec = existing.getPosition1().col;
        int nr = w.getPosition1().row;
        int nc = w.getPosition1().col;
        if (ec == nc && Math.abs(er - nr) == 1) {
            undoStack.pop();
            return false;
        }
    }
} else {
    // Different orientation → crossing check
    if (existing.getPosition1().equals(w.getPosition1())) {
        undoStack.pop();
        return false;
        }
    }
}
        
        
        
        
    
    // player has walls left?
    if (!current.useWall()) {
        undoStack.pop();
        return false;
    }
        board.addWall(w);   //add wall
        //rule You are NOT allowed to block all paths.
        boolean p1 = pathfinder.hasPath(board.getPlayers().get(0), board);
        boolean p2 = pathfinder.hasPath(board.getPlayers().get(1), board);

        if (p1 && p2) {
            switchTurn();
            redoStack.clear();
            return true;
        }
        
        // rollback if itll block path
       board.removeWall(w);
       current.restoreWall();
        undoStack.pop();
        return false;
    }

    // ---------------- UNDO / REDO ----------------

    public void undo() {
        if (!undoStack.isEmpty()) {
            redoStack.push(takeSnapshot());
            applySnapshot(undoStack.pop());
           
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            undoStack.push(takeSnapshot());
            applySnapshot(redoStack.pop());
        }
    }

    // ---------------- SNAPSHOTS ----------------

    private void saveToUndo() {  //save before any step
        undoStack.push(takeSnapshot());
    }

    private BoardSnapshot takeSnapshot() { //return a boardsnapshot taken
        return new BoardSnapshot(board, currentPlayerIndex);
    }

    private void applySnapshot(BoardSnapshot s) {  //restore previous stuff

        // restore turn
        currentPlayerIndex = s.getCurrentPlayerIndex();

        // restore players
        board.getPlayers().get(0).moveTo(s.getP1Pos());
        board.getPlayers().get(1).moveTo(s.getP2Pos());

        board.getPlayers().get(0).setWallsRemaining(s.getP1Walls());
        board.getPlayers().get(1).setWallsRemaining(s.getP2Walls());

        // restore walls
        board.getPlacedWalls().clear();
        for (Walls w : s.getWalls()) {
            board.addWall(new Walls(
                    w.getPosition1().row,
                    w.getPosition1().col,
                    w.getOrientation()
            ));
        }
    }
    public boolean checkForWin() {
    for (Player p : board.getPlayers()) {
        if (p.hasWon()) return true;
    }
    return false;
}
    // ---------------- UTILS ----------------

    private void switchTurn() {
        currentPlayerIndex = 1 - currentPlayerIndex;
    }

    public Board getBoard() {
        return board;
    }

    public Player getCurrentPlayer() {
        return board.getPlayers().get(currentPlayerIndex);
    }
}



