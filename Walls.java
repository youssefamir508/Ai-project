/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.game;
import java.util.Objects;

/**
 *
 * @author lenovo
 */
public class Walls {
    private final Point position1; 
    private final Point position2;
    private final Orientation orientation;

   
   public Walls(int row, int col, Orientation orientation) {
       
        this.position1 = new Point(row, col);
        this.orientation = orientation;

       
        if (orientation == Orientation.HORIZONTAL) {
           // horizontal wall at (4,4) sits: between row 4 and row 5 and blocks
           // (4,4) ↕ (5,4)   ❌             (4,4)      (4,5)
            // (4,5) ↕ (5,5)   ❌  POINT             ================
                  //                        (5,4)      (5,5)
            this.position2 = new Point(row, col + 1);
            
        } 
          else  {
           // vertical wall at (4,4) sits: between column 4 and column 5 and blocks   (4,4)     ||     (4,5)
           //(4,4) ↔ (4,5)   ❌                                                                 ||
           //(5,4) ↔ (5,5)   ❌                                                       (5,4)     ||     (5,5)
            this.position2 = new Point(row + 1, col);                                
        }
    }

    
    public Point getPosition1() { return position1; }
    public Point getPosition2() { return position2; }
    public Orientation getOrientation() { return orientation; }

    @Override
    //overrridng equal parameter must be object
    public boolean equals(Object o) {
        if (this == o) return true;  //If both objects point to the same memory location, they are definitely equal
        if (o == null || getClass() != o.getClass()) return false;  //false if object is null and checks if the two objects are the diff type returnf.
        Walls otherWall = (Walls) o;
        
        // A wall is equal to another if BOTH positions and orientation match
        return this.position1.equals(otherWall.position1) && 
               this.position2.equals(otherWall.position2) && 
               this.orientation == otherWall.orientation;
    }
    
      @Override
public int hashCode() {
    return Objects.hash(position1, position2, orientation);
}
    
   @Override
public String toString() {
    return orientation + " wall @ (" + position1.row + "," + position1.col + ")";
}
  
}