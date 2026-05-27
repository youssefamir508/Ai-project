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
public class Point {
    
    public final int row;
    public final int col;

    public Point(int row, int col) {
        this.row = row;
        this.col = col;
    }

   
    @Override
    //to compare coordinates based on values (row and col),
    public boolean equals(Object obj) {
        if (obj instanceof Point) {
            Point other = (Point) obj;
            return this.row == other.row && this.col == other.col;
        }
        return false;
    }
    
    ////for hashset in pathfinder
     @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }
    
      //sceanios :Is pawn at (4,4)?
           //Can I move to (0,0)? or occupied
    //Does this wall exist?
}