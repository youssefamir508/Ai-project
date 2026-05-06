/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Admin
 */
public class Walls {
    private final Point position1; 
    private final Point position2;
    private final Orientation orientation;

   
    public Walls(int row, int col, Orientation orientation) {
       
        this.position1 = new Point(row, col);
        this.orientation = orientation;

       
        if (orientation == Orientation.HORIZONTAL) {
   
            this.position2 = new Point(row, col + 1);
        } else {
           
            this.position2 = new Point(row + 1, col);
        }
    }

    
    public Point getPosition1() { return position1; }
    public Point getPosition2() { return position2; }
    public Orientation getOrientation() { return orientation; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Walls otherWall = (Walls) o;
        
        // A wall is equal to another if BOTH positions and orientation match
        return this.position1.equals(otherWall.position1) && 
               this.position2.equals(otherWall.position2) && 
               this.orientation == otherWall.orientation;
    }
}