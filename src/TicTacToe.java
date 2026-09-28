import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class TicTacToe extends JPanel {

    private static final int WINDOW_SIZE = 600;
    private static final int CELL_SIZE   = WINDOW_SIZE / 3;
    private static final int LINE_THICK  = 6;
    private static final int MARK_THICK  = 10;
    private static final int PADDING     = 35;
    private static final int AI_DELAY_MS = 400;

    private enum Player { NONE, X, O }
    private enum GameState { SIDE_SELECT, PLAYING }

    private final Player[][] board = new Player[3][3];
    private Player currentPlayer = Player.X;
    private Player humanPlayer = Player.NONE;
    private Player aiPlayer = Player.NONE;
    private boolean gameOver = false;
    private GameState state = GameState.SIDE_SELECT;

    private final JFrame frame;
    private final Random rng = new Random();

    public TicTacToe(JFrame frame) {
        this.frame = frame;
        setPreferredSize(new Dimension(WINDOW_SIZE, WINDOW_SIZE));
        setBackground(new Color(245, 245, 245));
        resetBoard();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleClick(e.getX(), e.getY());
            }
        });

        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_R) {
                    state = GameState.SIDE_SELECT;
                    frame.setTitle("Tic Tac Toe - Click a side: X (left) or O (right)");
                    repaint();
                }
            }
        });
    }

    // ---------- game logic ----------

    private void resetBoard() {
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                board[r][c] = Player.NONE;
        currentPlayer = Player.X; // X always moves first
        gameOver = false;
    }

    private void handleClick(int x, int y) {
        if (state == GameState.SIDE_SELECT) {
            humanPlayer = (x < WINDOW_SIZE / 2) ? Player.X : Player.O;
            aiPlayer = (humanPlayer == Player.X) ? Player.O : Player.X;
            resetBoard();
            state = GameState.PLAYING;
            if (currentPlayer == aiPlayer) {
                startAiTurn();
            } else {
                updateTitle();
            }
            repaint();
        } else if (state == GameState.PLAYING && !gameOver && currentPlayer == humanPlayer) {
            int col = x / CELL_SIZE;
            int row = y / CELL_SIZE;
            if (row >= 0 && row < 3 && col >= 0 && col < 3 && board[row][col] == Player.NONE) {
                board[row][col] = humanPlayer;

                if (checkWin(humanPlayer)) {
                    gameOver = true;
                    frame.setTitle("You win! Press R to choose sides again");
                } else if (checkDraw()) {
                    gameOver = true;
                    frame.setTitle("It's a draw! Press R to try again");
                } else {
                    startAiTurn();
                }
                repaint();
            }
        }
    }

    private void startAiTurn() {
        currentPlayer = aiPlayer;
        frame.setTitle("Computer is thinking...");

        Timer timer = new Timer(AI_DELAY_MS, e -> {
            int move = aiPickMove();
            if (move >= 0) board[move / 3][move % 3] = aiPlayer;

            if (checkWin(aiPlayer)) {
                gameOver = true;
                frame.setTitle("Computer wins! Press R to try again");
            } else if (checkDraw()) {
                gameOver = true;
                frame.setTitle("It's a draw! Press R to try again");
            } else {
                currentPlayer = humanPlayer;
                updateTitle();
            }
            repaint();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void updateTitle() {
        if (gameOver) return; // end-of-game titles are set directly where the game ends
        if (currentPlayer == humanPlayer) {
            frame.setTitle("Your turn (" + (humanPlayer == Player.X ? "X" : "O") + ") - R to change sides");
        } else {
            frame.setTitle("Computer is thinking...");
        }
    }

    private boolean checkWin(Player player) {
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == player && board[i][1] == player && board[i][2] == player) return true;
            if (board[0][i] == player && board[1][i] == player && board[2][i] == player) return true;
        }
        if (board[0][0] == player && board[1][1] == player && board[2][2] == player) return true;
        if (board[0][2] == player && board[1][1] == player && board[2][0] == player) return true;
        return false;
    }

    private boolean checkDraw() {
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                if (board[r][c] == Player.NONE) return false;
        return true;
    }

    // returns a 0-8 cell index where `player` would win immediately, or -1 if none
    private int findWinningMove(Player player) {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (board[r][c] == Player.NONE) {
                    board[r][c] = player;
                    boolean wins = checkWin(player);
                    board[r][c] = Player.NONE;
                    if (wins) return r * 3 + c;
                }
            }
        }
        return -1;
    }

    // simple heuristic AI: take a win, else block, else center, else corner, else edge
    private int aiPickMove() {
        int move = findWinningMove(aiPlayer);
        if (move != -1) return move;

        move = findWinningMove(humanPlayer);
        if (move != -1) return move;

        if (board[1][1] == Player.NONE) return 4;

        int[] corners = {0, 2, 6, 8};
        int start = rng.nextInt(4);
        for (int i = 0; i < 4; i++) {
            int idx = corners[(start + i) % 4];
            if (board[idx / 3][idx % 3] == Player.NONE) return idx;
        }

        int[] edges = {1, 3, 5, 7};
        start = rng.nextInt(4);
        for (int i = 0; i < 4; i++) {
            int idx = edges[(start + i) % 4];
            if (board[idx / 3][idx % 3] == Player.NONE) return idx;
        }

        return -1;
    }

    // ---------- drawing ----------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (state == GameState.SIDE_SELECT) {
            drawSideSelect(g2);
        } else {
            drawGrid(g2);
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    if (board[r][c] == Player.X) drawX(g2, r, c);
                    else if (board[r][c] == Player.O) drawO(g2, r, c);
                }
            }
        }
    }

    private void drawGrid(Graphics2D g2) {
        g2.setColor(new Color(40, 40, 40));
        g2.setStroke(new BasicStroke(LINE_THICK));
        g2.drawLine(CELL_SIZE, 0, CELL_SIZE, WINDOW_SIZE);
        g2.drawLine(2 * CELL_SIZE, 0, 2 * CELL_SIZE, WINDOW_SIZE);
        g2.drawLine(0, CELL_SIZE, WINDOW_SIZE, CELL_SIZE);
        g2.drawLine(0, 2 * CELL_SIZE, WINDOW_SIZE, 2 * CELL_SIZE);
    }

    private void drawXMark(Graphics2D g2, int x0, int y0, int size, int pad) {
        g2.setColor(new Color(220, 60, 60));
        g2.setStroke(new BasicStroke(MARK_THICK, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x0 + pad, y0 + pad, x0 + size - pad, y0 + size - pad);
        g2.drawLine(x0 + size - pad, y0 + pad, x0 + pad, y0 + size - pad);
    }

    private void drawOMark(Graphics2D g2, int cx, int cy, int radius) {
        g2.setColor(new Color(60, 100, 220));
        g2.setStroke(new BasicStroke(MARK_THICK));
        g2.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);
    }

    private void drawX(Graphics2D g2, int row, int col) {
        drawXMark(g2, col * CELL_SIZE, row * CELL_SIZE, CELL_SIZE, PADDING);
    }

    private void drawO(Graphics2D g2, int row, int col) {
        drawOMark(g2, col * CELL_SIZE + CELL_SIZE / 2, row * CELL_SIZE + CELL_SIZE / 2, CELL_SIZE / 2 - PADDING);
    }

    private void drawSideSelect(Graphics2D g2) {
        g2.setColor(new Color(40, 40, 40));
        g2.setStroke(new BasicStroke(LINE_THICK));
        g2.drawLine(WINDOW_SIZE / 2, 0, WINDOW_SIZE / 2, WINDOW_SIZE);

        int bigPad = 90;
        drawXMark(g2, 0, 0, WINDOW_SIZE / 2, bigPad);
        drawOMark(g2, WINDOW_SIZE / 2 + WINDOW_SIZE / 4, WINDOW_SIZE / 2, WINDOW_SIZE / 4 - bigPad);
    }

    // ---------- entry point ----------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Tic Tac Toe - Click a side: X (left) or O (right)");
            TicTacToe panel = new TicTacToe(frame);

            frame.add(panel);
            frame.pack();
            frame.setResizable(false);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            panel.requestFocusInWindow();
        });
    }
}