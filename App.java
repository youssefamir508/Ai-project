package com.mycompany.game;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App extends Application {

    private static final int CELL   = 64;
    private static final int GAP    = 10;
    private static final int WTHICK = GAP;
    private static final int N      = 9;

    private static final int BPXW = N * CELL + (N - 1) * GAP;
    private static final int BPXH = N * CELL + (N - 1) * GAP;

    MovementValidator val = new MovementValidator();

    private GameEngine  engine;
    private Ai_player   aiPlayer;

    private boolean wallMode   = false;
    private boolean gameOver   = false;
    private boolean vsAI       = true;
    private int     difficulty  = 2;

    private Orientation pendingOrientation = Orientation.HORIZONTAL;
    private Walls hoverWall = null;

    // ── Cached move highlights so drawBoard() doesn't recompute them every time ──
    private boolean[][] validMoveCache = null;
    private boolean moveCacheDirty = true;

    // ── Reusable cell rectangles to avoid GC pressure ──
    private Rectangle[][] cellRects = new Rectangle[N][N];
    private boolean boardInitialized = false;

    // ── Thread pool for AI (avoids creating a new Thread every turn) ──
    private final ExecutorService aiExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AI-Worker");
        t.setDaemon(true);
        return t;
    });

    private Pane         boardPane;
    private Label        statusLabel;
    private Label        p1WallsLabel;
    private Label        p2WallsLabel;
    private ToggleButton wallToggle;
    private ToggleButton hToggle, vToggle;
    private Button       undoBtn, redoBtn;

    @Override public void start(Stage stage) { showMainMenu(stage); }

    // ══════════════════════════════════════════════════════════════════════
    //  MAIN MENU
    // ══════════════════════════════════════════════════════════════════════
    private void showMainMenu(Stage stage) {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.setStyle("-fx-background-color: #0f0f1e;");

        Text title = new Text("QUORIDOR");
        title.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 60));
        title.setFill(Color.web("#d0d0ff"));
        title.setEffect(new Glow(0.4));

        Text sub = new Text("Block your opponent · Reach the other side");
        sub.setFont(Font.font("Arial", 15));
        sub.setFill(Color.web("#7777aa"));

        Label diffLabel = new Label("AI Difficulty:");
        diffLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        diffLabel.setTextFill(Color.web("#9999cc"));

        ToggleGroup dg = new ToggleGroup();
        ToggleButton easy   = diffBtn("Easy",   dg);
        ToggleButton medium = diffBtn("Medium", dg);
        ToggleButton hard   = diffBtn("Hard",   dg);
        medium.setSelected(true);
        styleDiffBtn(easy,   false);
        styleDiffBtn(medium, true);
        styleDiffBtn(hard,   false);

        easy.setOnAction(e   -> { difficulty=1; styleDiffBtn(easy,true);  styleDiffBtn(medium,false); styleDiffBtn(hard,false); });
        medium.setOnAction(e -> { difficulty=2; styleDiffBtn(easy,false); styleDiffBtn(medium,true);  styleDiffBtn(hard,false); });
        hard.setOnAction(e   -> { difficulty=3; styleDiffBtn(easy,false); styleDiffBtn(medium,false); styleDiffBtn(hard,true);  });

        HBox diffBox = new HBox(8, easy, medium, hard);
        diffBox.setAlignment(Pos.CENTER);

        VBox diffSection = new VBox(8, diffLabel, diffBox);
        diffSection.setAlignment(Pos.CENTER);

        Button btnAI   = menuBtn("▶  Play vs AI",  "#3355cc", "#4466ee");
        Button btn2P   = menuBtn("▶  2 Players",   "#226644", "#338855");
        Button btnQuit = menuBtn("✕  Quit",         "#882222", "#aa3333");

        btnAI.setOnAction(e   -> startGame(stage, true));
        btn2P.setOnAction(e   -> startGame(stage, false));
        btnQuit.setOnAction(e -> Platform.exit());

        root.getChildren().addAll(title, sub, new Separator(),
            diffSection, new Separator(), btnAI, btn2P, btnQuit);

        stage.setScene(new Scene(root, 480, 560));
        stage.setTitle("Quoridor");
        stage.setResizable(false);
        stage.show();
    }

    private ToggleButton diffBtn(String text, ToggleGroup g) {
        ToggleButton b = new ToggleButton(text);
        b.setToggleGroup(g);
        b.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        b.setPrefWidth(90);
        return b;
    }
    private void styleDiffBtn(ToggleButton b, boolean on) {
        b.setStyle("-fx-background-color:"+(on?"#3355cc":"#222244")+
                   ";-fx-text-fill:"+(on?"white":"#8888bb")+
                   ";-fx-background-radius:8;-fx-cursor:hand;");
    }
    private Button menuBtn(String text, String base, String hover) {
        Button b = new Button(text);
        b.setFont(Font.font("Arial", FontWeight.BOLD, 17));
        b.setPrefWidth(260); b.setPrefHeight(50);
        b.setStyle("-fx-background-color:"+base+";-fx-text-fill:white;-fx-background-radius:10;-fx-cursor:hand;");
        b.setOnMouseEntered(e->b.setStyle("-fx-background-color:"+hover+";-fx-text-fill:white;-fx-background-radius:10;-fx-cursor:hand;"));
        b.setOnMouseExited (e->b.setStyle("-fx-background-color:"+base +";-fx-text-fill:white;-fx-background-radius:10;-fx-cursor:hand;"));
        return b;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  START GAME
    // ══════════════════════════════════════════════════════════════════════
    private void startGame(Stage stage, boolean ai) {
        vsAI      = ai;
        engine    = new GameEngine();
        aiPlayer  = ai ? new Ai_player(1, difficulty) : null;
        wallMode  = false;
        gameOver  = false;
        hoverWall = null;
        boardInitialized = false;
        moveCacheDirty = true;

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#0d0d1c;");

        boardPane = new Pane();
        boardPane.setPrefSize(BPXW, BPXH);
        boardPane.setStyle("-fx-background-color:#1a1a30;");

        StackPane boardWrapper = new StackPane(boardPane);
        boardWrapper.setPadding(new Insets(24));

        VBox sidebar = buildSidebar(stage);

        root.setCenter(boardWrapper);
        root.setRight(sidebar);

        boardPane.setOnMouseClicked(this::onBoardClick);
        boardPane.setOnMouseMoved(this::onBoardHover);
        // Single mouse-exit handler (was duplicated before — caused redundant redraws)
        boardPane.setOnMouseExited(e -> {
            if (hoverWall != null) {
                hoverWall = null;
                drawBoard();
            }
        });

        Scene scene = new Scene(root, BPXW + 260, BPXH + 48);
        stage.setScene(scene);
        String diffName = difficulty==1?"Easy":difficulty==2?"Medium":"Hard";
        stage.setTitle("Quoridor" + (ai?" – vs AI ("+diffName+")":"  – 2 Players"));
        drawBoard();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SIDEBAR
    // ══════════════════════════════════════════════════════════════════════
    private VBox buildSidebar(Stage stage) {
        VBox box = new VBox(12);
        box.setPadding(new Insets(22,16,22,12));
        box.setPrefWidth(230);
        box.setStyle("-fx-background-color:#12122a;");

        p1WallsLabel = info("🔵 BLUE  –  10 walls","#4499ff");
        p2WallsLabel = info("🔴 RED   –  10 walls","#ff5555");

        statusLabel = new Label("🔵 BLUE's turn");
        statusLabel.setFont(Font.font("Arial",FontWeight.BOLD,15));
        statusLabel.setTextFill(Color.web("#ccccff"));
        statusLabel.setWrapText(true);

        wallToggle = new ToggleButton("🧱 Place Wall");
        wallToggle.setFont(Font.font("Arial",FontWeight.BOLD,14));
        wallToggle.setPrefWidth(200);
        styleWallOff(wallToggle);
        wallToggle.setOnAction(e -> {
            wallMode = wallToggle.isSelected();
            if (wallMode) styleWallOn(wallToggle); else styleWallOff(wallToggle);
            moveCacheDirty = true;
            hoverWall = null; drawBoard();
        });

        hToggle = new ToggleButton("─ Horizontal");
        vToggle = new ToggleButton("│ Vertical");
        ToggleGroup og = new ToggleGroup();
        hToggle.setToggleGroup(og); vToggle.setToggleGroup(og);
        hToggle.setSelected(true);
        hToggle.setFont(Font.font("Arial",13)); vToggle.setFont(Font.font("Arial",13));
        hToggle.setPrefWidth(97); vToggle.setPrefWidth(97);
        styleOri(hToggle,true); styleOri(vToggle,false);
        hToggle.setOnAction(e->{ pendingOrientation=Orientation.HORIZONTAL; styleOri(hToggle,true);  styleOri(vToggle,false); hoverWall=null; drawBoard(); });
        vToggle.setOnAction(e->{ pendingOrientation=Orientation.VERTICAL;   styleOri(hToggle,false); styleOri(vToggle,true);  hoverWall=null; drawBoard(); });
        HBox oriBox = new HBox(6, hToggle, vToggle);

        undoBtn = sideBtn("↩ Undo","#2a3a55");
        redoBtn = sideBtn("↪ Redo","#2a3a55");
        undoBtn.setOnAction(e->{ engine.undo(); gameOver=false; moveCacheDirty=true; hoverWall=null; drawBoard(); });
        redoBtn.setOnAction(e->{ engine.redo(); gameOver=false; moveCacheDirty=true; hoverWall=null; drawBoard(); });
        HBox ur = new HBox(8, undoBtn, redoBtn);

        Button resetBtn = sideBtn("↺ Reset","#1a3a1a");
        resetBtn.setPrefWidth(200);
        resetBtn.setOnAction(e -> startGame(stage, vsAI));

        Button menuBtn = sideBtn("☰ Menu","#3a1a3a");
        menuBtn.setPrefWidth(200);
        menuBtn.setOnAction(e -> showMainMenu(stage));

        VBox legend = new VBox(5,
            secTitle("GOAL"),
            legendRow(Color.web("#4499ff"), "BLUE → reach row 0 (top)"),
            legendRow(Color.web("#ff5555"), "RED  → reach row 8 (bottom)")
        );

        box.getChildren().addAll(
            secTitle("PLAYERS"), p1WallsLabel, p2WallsLabel, sep(),
            secTitle("TURN"),    statusLabel, sep(),
            secTitle("CONTROLS"), wallToggle, oriBox, sep(),
            ur, sep(), legend, sep(), resetBtn, menuBtn
        );
        return box;
    }

    private Label info(String t, String c) {
        Label l=new Label(t); l.setFont(Font.font("Arial",FontWeight.BOLD,14)); l.setTextFill(Color.web(c)); return l;
    }
    private Label secTitle(String t) {
        Label l=new Label(t); l.setFont(Font.font("Arial",FontWeight.EXTRA_BOLD,10)); l.setTextFill(Color.web("#555588")); return l;
    }
    private Button sideBtn(String t, String bg) {
        Button b=new Button(t); b.setFont(Font.font("Arial",13)); b.setPrefWidth(96);
        b.setStyle("-fx-background-color:"+bg+";-fx-text-fill:#ccccff;-fx-background-radius:7;-fx-cursor:hand;"); return b;
    }
    private Separator sep() { Separator s=new Separator(); s.setStyle("-fx-background-color:#2a2a55;"); return s; }
    private void styleWallOn (ToggleButton b){ b.setStyle("-fx-background-color:#cc6600;-fx-text-fill:white;-fx-background-radius:8;-fx-cursor:hand;"); }
    private void styleWallOff(ToggleButton b){ b.setStyle("-fx-background-color:#2a3a55;-fx-text-fill:#ccccff;-fx-background-radius:8;-fx-cursor:hand;"); }
    private void styleOri(ToggleButton b,boolean on){ b.setStyle("-fx-background-color:"+(on?"#225577":"#1a1a35")+";-fx-text-fill:"+(on?"white":"#6666aa")+";-fx-background-radius:7;-fx-cursor:hand;"); }
    private HBox legendRow(Color c, String t) {
        Rectangle r=new Rectangle(12,12,c); r.setArcWidth(3); r.setArcHeight(3);
        Label l=new Label(t); l.setFont(Font.font("Arial",11)); l.setTextFill(Color.web("#9999bb"));
        HBox h=new HBox(7,r,l); h.setAlignment(Pos.CENTER_LEFT); return h;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DRAWING  (optimised: reuse nodes, cache move highlights)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Compute (once per turn) which cells are valid destinations.
     * Stored in validMoveCache[row][col]. Only recomputed when moveCacheDirty.
     */
    private void rebuildMoveCache() {
        validMoveCache = new boolean[N][N];
        if (wallMode || gameOver) return;
        Player cur = engine.getCurrentPlayer();
        Point  pos = cur.getPosition();
        int[][] offs = {{-1,0},{1,0},{0,-1},{0,1},{-2,0},{2,0},{0,-2},{0,2},{-1,-1},{-1,1},{1,-1},{1,1}};
        for (int[] d : offs) {
            int nr = pos.row + d[0], nc = pos.col + d[1];
            if (nr < 0 || nr >= N || nc < 0 || nc >= N) continue;
            if (val.validateMove(pos, new Point(nr, nc), engine.getBoard(), cur)) {
                validMoveCache[nr][nc] = true;
            }
        }
        moveCacheDirty = false;
    }

    private void drawBoard() {
        // Rebuild move cache only when the game state changed, not on every hover
        if (moveCacheDirty) rebuildMoveCache();

        boardPane.getChildren().clear();

        // ── Cells ──
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                Rectangle cell = new Rectangle(cx(c), cy(r), CELL, CELL);
                cell.setFill((r == 0 || r == 8) ? Color.web("#142847") : Color.web("#1e1e38"));
                cell.setArcWidth(4); cell.setArcHeight(4);
                cell.setStroke(Color.web("#2e2e55")); cell.setStrokeWidth(1);
                boardPane.getChildren().add(cell);
            }
        }

        addText("BLUE's goal", BPXW/2.0-32, cy(0)+CELL/2.0+5, 10, "#6699ff");
        addText("RED's goal",  BPXW/2.0-28, cy(8)+CELL/2.0+5, 10, "#ff7777");

        // ── Move highlights (use pre-computed cache) ──
        if (!wallMode && !gameOver && validMoveCache != null) {
            for (int r = 0; r < N; r++) {
                for (int c = 0; c < N; c++) {
                    if (validMoveCache[r][c]) {
                        Rectangle hi = new Rectangle(cx(c), cy(r), CELL, CELL);
                        hi.setFill(Color.color(0.2, 0.8, 0.3, 0.28));
                        hi.setStroke(Color.color(0.2, 1.0, 0.3, 0.7));
                        hi.setStrokeWidth(2); hi.setArcWidth(4); hi.setArcHeight(4);
                        boardPane.getChildren().add(hi);
                    }
                }
            }
        }

        // ── Placed walls ──
        for (Walls w : engine.getBoard().getPlacedWalls()) {
            drawWallRect(w, false);
        }

        // ── Hover wall preview ──
        if (wallMode && hoverWall != null) {
            drawWallRect(hoverWall, true);
        }

        // ── Pawns ──
        for (Player p : engine.getBoard().getPlayers()) drawPawn(p);

        // ── Sidebar labels ──
        List<Player> pl = engine.getBoard().getPlayers();
        p1WallsLabel.setText("🔵 BLUE  –  "+pl.get(0).getWallsRemaining()+" walls");
        p2WallsLabel.setText("🔴 RED   –  "+pl.get(1).getWallsRemaining()+" walls");

        if (!gameOver) {
            Player cur = engine.getCurrentPlayer();
            boolean blue = cur.getColor().equals("BLUE");
            statusLabel.setText((blue?"🔵 BLUE":"🔴 RED")+"'s turn");
            statusLabel.setTextFill(Color.web(blue?"#4499ff":"#ff5555"));
        }
    }

    private void addText(String t, double x, double y, int sz, String color) {
        Text tx = new Text(x,y,t);
        tx.setFont(Font.font("Arial",sz)); tx.setFill(Color.web(color));
        boardPane.getChildren().add(tx);
    }

    private void drawPawn(Player p) {
        double pcx = cx(p.getPosition().col) + CELL/2.0;
        double pcy = cy(p.getPosition().row) + CELL/2.0;
        double radius = CELL * 0.34;

        Circle sh = new Circle(pcx+2, pcy+3, radius);
        sh.setFill(Color.color(0,0,0,0.45));
        boardPane.getChildren().add(sh);

        boolean blue = p.getColor().equals("BLUE");
        Circle body = new Circle(pcx, pcy, radius,
            blue ? Color.web("#2266ee") : Color.web("#ee2222"));
        body.setStroke(blue ? Color.web("#88bbff") : Color.web("#ffaaaa"));
        body.setStrokeWidth(2.5);
        body.setEffect(new DropShadow(10, Color.color(0,0,0,0.5)));
        boardPane.getChildren().add(body);

        Text lbl = new Text(blue?"B":"R");
        lbl.setFont(Font.font("Arial",FontWeight.EXTRA_BOLD,(int)(radius*1.2)));
        lbl.setFill(Color.WHITE);
        lbl.setX(pcx - lbl.getLayoutBounds().getWidth()/2);
        lbl.setY(pcy + lbl.getLayoutBounds().getHeight()/4);
        boardPane.getChildren().add(lbl);
    }

    private void drawWallRect(Walls w, boolean preview) {
        Point anchor = w.getPosition1();
        int r = anchor.row, c = anchor.col;

        double wx, wy, ww, wh;

        if (w.getOrientation() == Orientation.HORIZONTAL) {
            wx = cx(c);
            wy = cy(r) + CELL;
            ww = CELL * 2 + GAP;
            wh = WTHICK;
        } else {
            wx = cx(c) + CELL;
            wy = cy(r);
            ww = WTHICK;
            wh = CELL * 2 + GAP;
        }

        Color fill = preview ? Color.color(1.0, 0.65, 0.0, 0.55)
                             : Color.web("#ffaa00");
        Rectangle rect = new Rectangle(wx, wy, ww, wh);
        rect.setFill(fill);
        rect.setArcWidth(4); rect.setArcHeight(4);
        if (!preview) rect.setEffect(new DropShadow(6, Color.web("#ff880066")));
        boardPane.getChildren().add(rect);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  MOUSE EVENTS
    // ══════════════════════════════════════════════════════════════════════

    private void onBoardHover(javafx.scene.input.MouseEvent e) {
        if (!wallMode || gameOver) return;

        Walls w = wallFromMouse(e.getX(), e.getY());

        // Only redraw if the hover wall actually changed — avoids redrawing on every pixel
        if (w == null && hoverWall == null) return;
        if (w != null && hoverWall != null
                && w.getPosition1().equals(hoverWall.getPosition1())
                && w.getOrientation() == hoverWall.getOrientation()) return;

        hoverWall = w;
        drawBoard();
    }

    private void onBoardClick(javafx.scene.input.MouseEvent e) {
        if (gameOver) return;
        if (vsAI && engine.getCurrentPlayer() == engine.getBoard().getPlayers().get(1)) return;

        if (wallMode) {
            Walls w = wallFromMouse(e.getX(), e.getY());
            if (w == null) return;
            boolean ok = engine.playTurn(w.getPosition1().row, w.getPosition1().col, true, w.getOrientation());
            if (ok) {
                moveCacheDirty = true;
                hoverWall = null; drawBoard();
                if (engine.checkForWin()) { showWin(); return; }
                if (vsAI) triggerAI();
            }
        } else {
            int col = snapCol(e.getX());
            int row = snapRow(e.getY());
            boolean ok = engine.playTurn(row, col, false, null);
            if (ok) {
                moveCacheDirty = true;
                hoverWall = null; drawBoard();
                if (engine.checkForWin()) { showWin(); return; }
                if (vsAI) triggerAI();
            }
        }
    }

    private Walls wallFromMouse(double mx, double my) {
        if (pendingOrientation == Orientation.HORIZONTAL) {
            int bestR = -1;
            double bestDist = Double.MAX_VALUE;
            for (int g = 0; g <= 7; g++) {
                double gapCenter = cy(g) + CELL + GAP / 2.0;
                double dist = Math.abs(my - gapCenter);
                if (dist < bestDist) { bestDist = dist; bestR = g; }
            }
            if (bestR < 0 || bestDist > CELL * 0.8) return null;
            int c = (int) Math.floor(mx / (CELL + GAP));
            c = Math.max(0, Math.min(7, c));
            return new Walls(bestR, c, Orientation.HORIZONTAL);
        } else {
            int bestC = -1;
            double bestDist = Double.MAX_VALUE;
            for (int g = 0; g <= 7; g++) {
                double gapCenter = cx(g) + CELL + GAP / 2.0;
                double dist = Math.abs(mx - gapCenter);
                if (dist < bestDist) { bestDist = dist; bestC = g; }
            }
            if (bestC < 0 || bestDist > CELL * 0.8) return null;
            int r = (int) Math.floor(my / (CELL + GAP));
            r = Math.max(0, Math.min(7, r));
            return new Walls(r, bestC, Orientation.VERTICAL);
        }
    }

    private int snapCol(double x) {
        return Math.max(0, Math.min(N-1, (int) Math.round(x / (CELL+GAP))));
    }
    private int snapRow(double y) {
        return Math.max(0, Math.min(N-1, (int) Math.round(y / (CELL+GAP))));
    }

    private double cx(int col) { return col * (CELL + GAP); }
    private double cy(int row) { return row * (CELL + GAP); }

    // ══════════════════════════════════════════════════════════════════════
    //  AI TURN  (reuses thread-pool instead of spawning a new Thread each turn)
    // ══════════════════════════════════════════════════════════════════════
    private void triggerAI() {
        String diffName = difficulty==1?"Easy":difficulty==2?"Medium":"Hard";
        statusLabel.setText("🤖 AI ("+diffName+") thinking…");
        statusLabel.setTextFill(Color.web("#ffaa44"));

        aiExecutor.submit(() -> {
            AiMove move = aiPlayer.getBestMove(engine);
            Platform.runLater(() -> {
                if (move != null)
                    engine.playTurn(move.row, move.col, move.isWall, move.orientation);
                moveCacheDirty = true;
                drawBoard();
                if (engine.checkForWin()) showWin();
            });
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    //  WIN OVERLAY
    // ══════════════════════════════════════════════════════════════════════
    private void showWin() {
        gameOver = true;
        Player winner = null;
        for (Player p : engine.getBoard().getPlayers())
            if (p.hasWon()) { winner = p; break; }

        String msg   = winner != null ? winner.getColor()+" WINS! 🎉" : "GAME OVER";
        String color = (winner!=null && winner.getColor().equals("BLUE")) ? "#4499ff" : "#ff5555";

        statusLabel.setText("🏆 "+msg);
        statusLabel.setTextFill(Color.web(color));

        Rectangle dim = new Rectangle(0,0,BPXW,BPXH);
        dim.setFill(Color.color(0,0,0,0.5));
        boardPane.getChildren().add(dim);

        Text tx = new Text(msg);
        tx.setFont(Font.font("Arial",FontWeight.EXTRA_BOLD,48));
        tx.setFill(Color.web(color));
        tx.setEffect(new DropShadow(16,Color.BLACK));
        tx.setX(BPXW/2.0 - 130);
        tx.setY(BPXH/2.0 + 16);
        tx.setOpacity(0);
        boardPane.getChildren().add(tx);

        FadeTransition ft = new FadeTransition(Duration.millis(700), tx);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    public static void main(String[] args) { launch(args); }
}