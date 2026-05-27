# Quoridor – Java/JavaFX Implementation

> CSE472s Artificial Intelligence · Spring 2026 · Term Project

---

## Team Members

•[Omnia Magdy Hamouda]  — ID: [2301146] 
•[Jana ahmed kassem]    — ID: [2300914] 
•[Marina Isaac Youssef] — ID: [2300813] 
•[Giovanni Boulis abdo] — ID: [2300063] 
•[Sama Alaa Abdelgwad ] — ID: [2300119] 
•[Youssef Amir Youssef] — ID: [2300489] 
---

## Table of Contents

1. [Game Description](#game-description)
2. [Screenshots](#screenshots)
3. [Installation & Running](#installation--running)
4. [Controls](#controls)
5. [Project Structure](#project-structure)
6. [AI Implementation](#ai-implementation)
7. [Bonus Features](#bonus-features)
8. [Demo Video](#demo-video)
9. [Team Members](#team-members)

---

## Game Description

Quoridor is a two-player abstract strategy board game invented by Mirko Marchesi (1997) and winner of the Mensa Mind Game award.

**Objective:** Be the first player to move your pawn from your starting edge to the opposite side of the 9×9 board.

**Rules summary:**

- Each player starts at the center of their baseline: BLUE at row 8, RED at row 0.
- On each turn a player must either **move their pawn** or **place a wall**.
- Pawns move one square orthogonally per turn.
- If your pawn is directly adjacent to the opponent, you may **jump over** them (straight jump if the path is clear, diagonal bypass if a wall blocks the straight jump).
- Each player has **10 walls** to place. Walls span two cell edges and block movement.
- A wall may **never** completely cut off a player's path to their goal — at least one route must always remain open.
- The first player whose pawn reaches any cell in the opposite baseline **wins**.

---

## Screenshots

> https://drive.google.com/drive/folders/11XtyZkj_Xw6fo-TzxERVxztMg3WxlemD?usp=sharing

```
screenshots/main_menu.png      – Main menu with difficulty selector
screenshots/gameplay.png       – Mid-game with walls placed
screenshots/win_screen.png     – Win overlay animation
```
---

## Installation & Running

### Prerequisites

| Requirement | Version |
|---|---|
| Java JDK | 17 or later |
| JavaFX SDK | 17 or later |
| Maven | 3.8 or later (optional, if using pom.xml) |

### Clone the repository

```bash
https://github.com/youssefamir508/Ai-project

### Run with Maven

```bash
mvn clean javafx:run
```

### Run manually (without Maven)

```bash
# Compile
javac --module-path /path/to/javafx-sdk/lib \
      --add-modules javafx.controls,javafx.fxml \
      -d out src/main/java/com/mycompany/game/*.java

# Run
java --module-path /path/to/javafx-sdk/lib \
     --add-modules javafx.controls,javafx.fxml \
     -cp out com.mycompany.game.App
```

> Replace `/path/to/javafx-sdk` with the actual path to your JavaFX SDK installation.

---

## Controls

### Main Menu

| Button | Action |
|---|---|
| Easy / Medium / Hard | Select AI difficulty before starting |
| Play vs AI | Start a Human vs. Computer game |
| 2 Players | Start a Human vs. Human game |
| Quit | Exit the application |

### In-Game

| Action | How |
|---|---|
| Move pawn | Click a highlighted green cell (valid moves shown automatically) |
| Enter wall-placement mode | Click the **🧱 Place Wall** toggle button |
| Choose wall orientation | Click **─ Horizontal** or **│ Vertical** |
| Place a wall | While in wall mode, hover to preview then click to confirm |
| Exit wall-placement mode | Click the **🧱 Place Wall** toggle again |
| Undo last move | Click **↩ Undo** |
| Redo | Click **↪ Redo** |
| Return to menu | Click **☰ Menu** |

---

## Project Structure

```
src/main/java/com/mycompany/game/
│
├── App.java               – JavaFX Application entry point; full GUI
├── GameEngine.java        – Core game loop, turn management, undo/redo
├── Board.java             – Board state: players and placed walls
├── Player.java            – Player state: position, wall count, win detection
├── Point.java             – Simple (row, col) coordinate
├── Walls.java             – Wall data: anchor position + orientation
├── Orientation.java       – Enum: HORIZONTAL / VERTICAL
├── MovementValidator.java – All pawn-move legality (normal, jump, diagonal)
├── Pathfinder.java        – BFS to verify a path to goal exists
├── BoardSnapshot.java     – Immutable snapshot for undo/redo stack
├── HeuristicProvider.java – Board evaluation function for the AI
├── Ai_player.java         – Minimax + Alpha-Beta AI player
└── AiMove.java            – Value object representing one AI candidate move
```

### Architecture overview

```
App (GUI / JavaFX)
    │
    └──► GameEngine
              │
              ├──► Board  ◄──── BoardSnapshot (undo stack)
              │       └──► Player, Walls
              │
              ├──► MovementValidator
              ├──► Pathfinder
              │
              └──► Ai_player
                       ├──► HeuristicProvider  (evaluate)
                       ├──► MovementValidator  (generate pawn moves)
                       └──► Pathfinder         (validate wall candidates)
```

**Key design decisions:**

- **Separation of concerns** — GUI (`App`), rules engine (`GameEngine`, `MovementValidator`), and AI (`Ai_player`, `HeuristicProvider`) are fully independent.
- **Undo/redo via snapshots** — `BoardSnapshot` captures the full game state before every move, enabling unlimited undo/redo without inverting logic.
- **AI runs on a background thread** — `triggerAI()` spawns a daemon thread so the JavaFX UI never freezes during search; `Platform.runLater` marshals the result back to the UI thread.

---

## AI Implementation

The computer opponent uses **Minimax search with Alpha-Beta pruning**.

### Difficulty levels

| Level | Search Depth | Behavior |
|---|---|---|
| Easy | 1 | Looks one move ahead; mostly greedy |
| Medium | 3 | Balanced; considers short-term tactics |
| Hard | 4 | Stronger positional play; slower |

### Algorithm details

**Evaluation function (`HeuristicProvider`):**

```
score = (opponent BFS distance to goal  –  my BFS distance to goal) × 10
      + (my walls remaining  –  opponent walls remaining) × 2
```

A higher score means the position is better for the AI. BFS is run on the actual board graph (respecting placed walls) to compute true shortest-path distances.

**Move generation (`Ai_player`):**

- Pawn moves are always generated (up to 12 offsets covering normal, jump, and diagonal bypass).
- Wall moves are only generated at the **top 2 levels** of the search tree (depth ≥ searchDepth − 2) to keep the branching factor tractable.
- Wall candidates are filtered to those that **increase the opponent's BFS distance** and capped at the top 6 by gain.
- Moves are ordered cheaply (pawn moves toward goal first) to maximise Alpha-Beta cutoffs.

**Correctness fix — maximising flag:**

After `engine.playTurn()` the engine internally calls `switchTurn()`, so querying `engine.getCurrentPlayer()` immediately after an applied move already returns the *next* player. The `maximising` flag is therefore set as:

```java
boolean nextIsAI = engine.getCurrentPlayer().getId() - 1 == aiIndex;
```

This correctly handles all turn transitions without manual alternation.

---

## Bonus Features

| Feature | Status |
|---|---|
| **Undo / Redo** (unlimited, snapshot-based) | ✅ Implemented |
| **AI difficulty levels** (Easy / Medium / Hard) | ✅ Implemented |
| Valid move highlighting | ✅ Implemented |
| Wall hover preview | ✅ Implemented |

---

## Demo Video

>https://drive.google.com/file/d/1fM06HjozXQn_iNctxT62D0-IIMvGeVYU/view?usp=sharing

The video covers:
1. Application launch and main menu
2. Human vs. Human gameplay walkthrough
3. Human vs. Computer on each difficulty level
4. Wall placement, jump moves, and win detection

---


## References

- Official Quoridor Rules – *Gigamic*
- Quoridor on BoardGameGeek – boardgamegeek.com
- Minimax with Alpha-Beta Pruning – *Stuart Russell & Peter Norvig, Artificial Intelligence: A Modern Approach*
- JavaFX Documentation – openjfx.io
- BFS Pathfinding – *Introduction to Algorithms, Cormen et al.*
