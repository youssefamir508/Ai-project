package com.mycompany.game;

import java.util.Scanner;

public class Ai {

    static GameEngine engine = new GameEngine();
    static Scanner scanner = new Scanner(System.in);
//https://www.youtube.com/watch?v=6ISruhN0Hc0&t=256s 3:15 rules if u want to try out in console 
    public static void main(String[] args) {

        System.out.println("==================================");
        System.out.println("      QUORIDOR TEST RUNNER        ");
        System.out.println("==================================");

        while (true) {

            if (engine.checkForWin()) {
                printWinner();
                System.out.println("\nType 'reset' or 'exit'");
                String cmd = scanner.nextLine().trim().toLowerCase();

                if (cmd.equals("reset")) {
                    engine = new GameEngine();
                    continue;
                } else {
                    break;
                }
            }

            printBoard();
            printTurn();

            System.out.println("\nCommands:");
            System.out.println("move r c");
            System.out.println("wall r c H/V");
            System.out.println("undo");
            System.out.println("redo");
            System.out.println("exit");
            System.out.print("> ");

            String input = scanner.nextLine().trim();
            handleInput(input);
        }

        System.out.println("Game closed.");
    }

    // ---------------- INPUT ----------------

    private static void handleInput(String input) {
        String[] p = input.split(" ");

        if (p.length == 0) return;

        switch (p[0].toLowerCase()) {

            case "move":
                if (p.length != 3) return;

                int r = Integer.parseInt(p[1]);
                int c = Integer.parseInt(p[2]);

                boolean ok = engine.playTurn(r, c, false, null);
                System.out.println(ok ? "Move done" : "Invalid move");
                break;

            case "wall":
                if (p.length != 4) return;

                int wr = Integer.parseInt(p[1]);
                int wc = Integer.parseInt(p[2]);

                Orientation o = p[3].equalsIgnoreCase("H")
                        ? Orientation.HORIZONTAL
                        : Orientation.VERTICAL;

                boolean ok2 = engine.playTurn(wr, wc, true, o);
                System.out.println(ok2 ? "Wall placed" : "Invalid wall");
                break;

            case "undo":
                engine.undo();
                System.out.println("Undo done");
                break;

            case "redo":
                engine.redo();
                System.out.println("Redo done");
                break;

            case "exit":
                System.exit(0);
                break;
        }
    }

    // ---------------- BOARD DISPLAY ----------------

    private static void printBoard() {

        Board b = engine.getBoard();
        Player p1 = b.getPlayers().get(0);
        Player p2 = b.getPlayers().get(1);

        System.out.println("\n     0    1    2    3    4    5    6    7    8");

        for (int i = 0; i < 9; i++) {

            System.out.print(i + "  ");

            // ---------- ROW CELLS ----------
            for (int j = 0; j < 9; j++) {

                Point current = new Point(i, j);

                if (current.equals(p1.getPosition())) {
                    System.out.print(" B  ");
                } else if (current.equals(p2.getPosition())) {
                    System.out.print(" R  ");
                } else {
                    System.out.print(" .   ");
                }

                // vertical wall separator
                if (j < 8) {
                    boolean blocked = new MovementValidator()
                            .isWallBlocking(current,
                                    new Point(i, j + 1),
                                    b.getPlacedWalls());

                    System.out.print(blocked ? "||" : " ");
                }
            }

            System.out.println();

            // ---------- HORIZONTAL WALL ROW ----------
            if (i < 8) {

                System.out.print("   ");

                for (int j = 0; j < 9; j++) {

                    Point a = new Point(i, j);
                    Point b2 = new Point(i + 1, j);

                    boolean blocked = new MovementValidator()
                            .isWallBlocking(a, b2, b.getPlacedWalls());

                    System.out.print(blocked ? "==" : "----");
                }

                System.out.println();
            }
        }
    }

    // ---------------- TURN ----------------

    private static void printTurn() {
        Player p = engine.getCurrentPlayer();
        System.out.println("\nTurn: Player " + p.getId());
    }

    // ---------------- WIN ----------------

    private static void printWinner() {
        for (Player p : engine.getBoard().getPlayers()) {
            if (p.hasWon()) {
                System.out.println("\n🏆 PLAYER " + p.getId() + " WINS!");
            }
        }
    }
}