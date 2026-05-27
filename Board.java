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

    
public class Board {
    public static final int SIZE = 9;
    private final List<Player> players;
    private final List<Walls> placedWalls;

    public Board() {
        this.players = new ArrayList<>();
        this.placedWalls = new ArrayList<>();
        initializeGame();
    }

    private void initializeGame() {
        
        players.add(new Player(1, new Point(8, 4), 0, "BLUE"));

        players.add(new Player(2, new Point(0, 4), 8, "RED"));
    }

    public void addWall(Walls wall) {
        placedWalls.add(wall);
    }
    
    public void removeWall(Walls wall) {
        placedWalls.remove(wall);
    }
    
    public List<Walls> getPlacedWalls() {
        return placedWalls;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Player getPlayer(int id) {
        return players.get(id - 1);
    }

    
    //This method prevents collision - two players cannot stand on the same square.


  public boolean isSquareOccupied(Point pos) {
    
    for (Player p : this.players) {
        
      
        if (p.getPosition().equals(pos)) {
            return true; 
        }
    }
    return false; 
}
  

  
}
