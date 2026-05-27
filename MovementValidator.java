  /*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.game;

/**
 *
 * @author lenovo
 */
import java.util.List;

public class MovementValidator {
     //called by game engine
     public boolean validateMove(Point from, Point to, Board board, Player movingPlayer) {
        return isValidMove(from, to, board, movingPlayer);
    }
     //called by pathfinder
      public boolean validateMove(Point from, Point to, Board board) {
        return isValidMove(from, to, board, null);
    }

    public boolean isValidMove(Point from, Point to, Board board,Player movingPlayer) {
      // 1. Make sure the new square is within the boundaries (0-8)
        if (to.row < 0 || to.row >= 9 || to.col < 0 || to.col >= 9) return false;

       // 2. Make sure the square is not occupied by a player
        if (movingPlayer != null && board.isSquareOccupied(to)) return false;
        
     

        //These calculate the distance between where you are (from) and where you want to go (to).
        int rowDiff = Math.abs(to.row - from.row);
        int colDiff = Math.abs(to.col - from.col);

       // 3. Normal movement (one step vertically or horizontally no diagonal if diagonal diff would be 0 or 2) 
        if (rowDiff + colDiff == 1) {
            return !isWallBlocking(from, to, board.getPlacedWalls());
        }

       // 4. Handle Jumping (moving 2 squares if opponent is in the middle)
          if ((rowDiff == 2 && colDiff == 0) || (rowDiff == 0 && colDiff == 2)) {
              
            return handleJumping(from, to, board, movingPlayer);
        }
        
        // 5. Handle Diagonal Bypass (moving around opponent)
        if (rowDiff == 1 && colDiff == 1) {
            return handleDiagonalBypass(from, to, board,movingPlayer);
        }

        return false; // Invalid move (too far or invalid direction)
    }

    
    //This method answers the question: "Is there a wall between Point p1 and Point p2?" so i can move to next 

 public boolean isWallBlocking(Point p1, Point p2, List<Walls> walls) {

    for (Walls wall : walls) {
        
        if (p1.row == p2.row) {
            // HORIZONTAL movement (same row, different col)
            // Blocked by a VERTICAL wall between the two columns
            if (wall.getOrientation() == Orientation.VERTICAL) {
                int wallCol = Math.min(p1.col, p2.col); // edge between the two cells
                int wallRow = p1.row;
                
                // A vertical wall at (r, c) covers rows r and r+1
                // It blocks if position1 is at (wallRow, wallCol) OR (wallRow-1, wallCol)
                
                // Boundary check: wallRow-1 only valid if wallRow > 0
                if (wallRow > 0) {
                    Point checkPos1 = new Point(wallRow, wallCol);
                    Point checkPos2 = new Point(wallRow - 1, wallCol);
                    
                    if (wall.getPosition1().equals(checkPos1) || 
                        wall.getPosition1().equals(checkPos2)) {
                        return true;
                    }
                } else {
                    // wallRow is 0, only check current row
                    Point checkPos1 = new Point(wallRow, wallCol);
                    if (wall.getPosition1().equals(checkPos1)) {
                        return true;
                    }
                }
            }
            
        } else {
            // VERTICAL movement (same col, different row)
            // Blocked by a HORIZONTAL wall between the two rows
            if (wall.getOrientation() == Orientation.HORIZONTAL) {
                int wallRow = Math.min(p1.row, p2.row); // edge between the two cells
                int wallCol = p1.col;
                
                // A horizontal wall at (r, c) covers cols c and c+1
                // It blocks if position1 is at (wallRow, wallCol) OR (wallRow, wallCol-1)
                
                // Boundary check: wallCol-1 only valid if wallCol > 0
                if (wallCol > 0) {
                    Point checkPos1 = new Point(wallRow, wallCol);
                    Point checkPos2 = new Point(wallRow, wallCol - 1);
                    
                    if (wall.getPosition1().equals(checkPos1) || 
                        wall.getPosition1().equals(checkPos2)) {
                        return true;
                    }
                } else {
                    // wallCol is 0, only check current column
                    Point checkPos1 = new Point(wallRow, wallCol);
                    if (wall.getPosition1().equals(checkPos1)) {
                        return true;
                    }
                }
            }
        }
    }
    
    return false;
}
      
    
    

    private boolean handleJumping(Point from, Point to, Board board,Player movingPlayer) {
        // Calculate the middle point (where opponent would be)
        int midRow = (from.row + to.row) / 2;
        int midCol = (from.col + to.col) / 2;
        Point midPoint = new Point(midRow, midCol);
        
        // Check if opponent is in the middle
        Player opponent = getOpponentInMiddle( midPoint, board,movingPlayer);
        if (opponent == null) {
            return false; // No opponent to jump over
        }
        
        // Check if wall blocks the landing
        if (isWallBlocking(from, midPoint, board.getPlacedWalls())) {
            return false; // Cannot jump over wall
        }
        
        if (isWallBlocking(midPoint, to, board.getPlacedWalls())) {
            return false; // Cannot jump over wall
        }
        return true; // Valid jump!
    }    
         private Player getOpponentInMiddle(Point pos, Board board, Player movingPlayer) {
        for (Player p : board.getPlayers()) {
            // Don't count the moving player as an obstacle at their own square
            if (movingPlayer != null && p == movingPlayer) continue;
            if (p.getPosition().equals(pos)) return p;
        }
        return null;
    }
         
         private boolean handleDiagonalBypass(Point from, Point to, Board board, Player movingPlayer) {
    if (movingPlayer == null) return false;

    int dr = to.row - from.row;
    int dc = to.col - from.col;

    if (Math.abs(dr) != 1 || Math.abs(dc) != 1) return false;

    Player opponent = getAdjacentOpponent(from, board, movingPlayer);
    if (opponent == null) return false;

    Point opp = opponent.getPosition();

    // MUST be adjacent orthogonally (not diagonal relationship)
    boolean validAdjacency =
            (Math.abs(opp.row - from.row) + Math.abs(opp.col - from.col) == 1);

    if (!validAdjacency) return false;

    // straight jump direction depends on relative position
    int jumpRow = opp.row + (opp.row - from.row);
    int jumpCol = opp.col + (opp.col - from.col);

    boolean jumpInside =
            jumpRow >= 0 && jumpRow < 9 &&
            jumpCol >= 0 && jumpCol < 9;

    boolean straightBlocked =
            !jumpInside ||
            isWallBlocking(from, opp, board.getPlacedWalls()) ||
            (jumpInside && isWallBlocking(opp, new Point(jumpRow, jumpCol), board.getPlacedWalls()));

    if (!straightBlocked) return false;

    // allow ONLY side escape squares
    boolean validDiagonal =
            (to.row == opp.row && Math.abs(to.col - opp.col) == 1) ||
            (to.col == opp.col && Math.abs(to.row - opp.row) == 1);
    
    

    if (!validDiagonal) return false;

    if (isWallBlocking(opp, to, board.getPlacedWalls())) return false;

    return true;
}
        
      /* claude   private boolean handleDiagonalBypass(Point from, Point to, Board board, Player movingPlayer) {
    if (movingPlayer == null) return false;

    int dr = to.row - from.row; // signed, not abs — direction matters
    int dc = to.col - from.col;

    if (Math.abs(dr) != 1 || Math.abs(dc) != 1) return false;

    // The opponent must be specifically in EITHER:
    // - same row direction as the diagonal (opponent is ahead in row, we go sideways)
    // - same col direction as the diagonal (opponent is beside us, we go forward/back)

    // Case 1: opponent is directly ahead (row direction), wall blocks forward jump
    // e.g. from=(4,4), opp=(3,4), to=(3,3) or (3,5)
    Point oppAhead = new Point(from.row + dr, from.col);

    // Case 2: opponent is directly to the side (col direction), wall blocks side jump
    // e.g. from=(4,4), opp=(4,5), to=(3,5) or (5,5)
    Point oppSide = new Point(from.row, from.col + dc);

    boolean caseAhead = isOpponent(oppAhead, board, movingPlayer);
    boolean caseSide  = isOpponent(oppSide,  board, movingPlayer);

    // at least one of these must be true
    if (!caseAhead && !caseSide) return false;

    if (caseAhead) {
        // opponent is ahead in row direction
        // straight jump would land at (from.row + 2*dr, from.col)
        int jumpRow = from.row + 2 * dr;
        int jumpCol = from.col;

        boolean jumpBlockedByWall = (jumpRow < 0 || jumpRow >= 9)
                || isWallBlocking(oppAhead, new Point(jumpRow, jumpCol), board.getPlacedWalls());

        if (!jumpBlockedByWall) return false; // straight jump is available, must use it

        // to must be (oppAhead.row, oppAhead.col +/- 1) i.e. lateral from opponent
        if (to.row != oppAhead.row) return false;
        if (Math.abs(to.col - oppAhead.col) != 1) return false;

        // check no wall between opponent and diagonal destination
        if (isWallBlocking(oppAhead, to, board.getPlacedWalls())) return false;

        return true;
    }

    if (caseSide) {
        // opponent is beside us in col direction
        // straight jump would land at (from.row, from.col + 2*dc)
        int jumpRow = from.row;
        int jumpCol = from.col + 2 * dc;

        boolean jumpBlockedByWall = (jumpCol < 0 || jumpCol >= 9)
                || isWallBlocking(oppSide, new Point(jumpRow, jumpCol), board.getPlacedWalls());

        if (!jumpBlockedByWall) return false; // straight jump is available

        // to must be (oppSide.row +/- 1, oppSide.col) i.e. forward/back from opponent
        if (to.col != oppSide.col) return false;
        if (Math.abs(to.row - oppSide.row) != 1) return false;

        // check no wall between opponent and diagonal destination
        if (isWallBlocking(oppSide, to, board.getPlacedWalls())) return false;

        return true;
    }

    return false;
}     
// small helper to avoid repeating the loop
private boolean isOpponent(Point pos, Board board, Player movingPlayer) {
    for (Player p : board.getPlayers()) {
        if (p == movingPlayer) continue;
        if (p.getPosition().equals(pos)) return true;
    }
    return false;
} */





 /*private boolean handleDiagonalBypass(Point from, Point to, Board board,Player movingPlayer) {
     if (movingPlayer == null) return false;

    // step 1: find adjacent opponent
    Player opponent = getAdjacentOpponent(from, board, movingPlayer);
    if (opponent == null) return false;
    Point oppPos = opponent.getPosition();

    // must be diagonal move
  int dr = Math.abs(to.row - from.row);
    int dc = Math.abs(to.col - from.col);
    
      // must be diagonal move
    if (dr != 1 || dc != 1) return false;
    
 // opponent must be adjacent orthogonally
    boolean adjacent =
            (Math.abs(oppPos.row - from.row) + Math.abs(oppPos.col - from.col) == 1);

    if (!adjacent) return false;

     // straight jump landing
    //int jumpRow = oppPos.row + (oppPos.row - from.row);
    //int jumpCol = oppPos.col + (oppPos.col - from.col);
    int oppDr = oppPos.row - from.row;
int oppDc = oppPos.col - from.col;
    int jumpRow = from.row + 2 * oppDr;  // 2 squares PAST
     int jumpCol = from.col + 2 * oppDc;
    
   // assume jump blocked until proven otherwise
boolean jumpBlocked = true;

// if jump square exists inside board
if (jumpRow >= 0 && jumpRow < 9 &&
    jumpCol >= 0 && jumpCol < 9) {

    Point straightJump = new Point(jumpRow, jumpCol);

    // jump is blocked only if wall exists
    jumpBlocked = isWallBlocking(
            oppPos,
            straightJump,
            board.getPlacedWalls()
    );
}

// if jump is not blocked and square exists,
// player should jump straight instead of diagonal
if (!jumpBlocked) {
    return false;
}

 return true;
 } */
 
  
    private Player getAdjacentOpponent(Point playerPos, Board board, Player currentPlayer) { 
    for (Player opp : board.getPlayers()) {
        if (currentPlayer != null && opp == currentPlayer) continue;
        int rowDiff = Math.abs(playerPos.row - opp.getPosition().row);
        int colDiff = Math.abs(playerPos.col - opp.getPosition().col);
        if (rowDiff + colDiff == 1) {
            return opp;
        }
    }
    return null;
}
} 