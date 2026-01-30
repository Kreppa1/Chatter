// CounterStrike3Manager.java - Client-side Counter Strike 3 game manager
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

public class CounterStrike3Manager {
    private ClientApplication clientApp;
    private JFrame gameFrame;
    private GamePanel gamePanel;
    private int clientID = -1;
    private int canvasWidth = 1000;
    private int canvasHeight = 1000;
    private Map<Integer, PlayerData> players = new HashMap<>();
    private boolean isActive = false;

    // Player movement variables
    private boolean upPressed = false;
    private boolean downPressed = false;
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private final int MOVEMENT_SPEED = 5;

    public CounterStrike3Manager(ClientApplication clientApp) {
        this.clientApp = clientApp;
        initializeGameFrame();
    }

    private void initializeGameFrame() {
        gameFrame = new JFrame("Counter Strike 3 - " + clientApp.getName());
        gameFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        gameFrame.setSize(800, 600);
        gameFrame.setLocationRelativeTo(null);

        gamePanel = new GamePanel();
        gameFrame.add(gamePanel);

        setupKeyListeners();
    }

    private void setupKeyListeners() {
        InputMap inputMap = gamePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = gamePanel.getActionMap();

        // Key pressed actions
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, false), "upPressed");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0, false), "downPressed");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, false), "leftPressed");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0, false), "rightPressed");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, false), "upPressed");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, false), "downPressed");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false), "leftPressed");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false), "rightPressed");

        // Key released actions
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, true), "upReleased");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0, true), "downReleased");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, true), "leftReleased");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0, true), "rightReleased");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, true), "upReleased");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, true), "downReleased");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, true), "leftReleased");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, true), "rightReleased");

        // Pressed actions
        actionMap.put("upPressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                upPressed = true;
                updateMovement();
            }
        });

        actionMap.put("downPressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                downPressed = true;
                updateMovement();
            }
        });

        actionMap.put("leftPressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                leftPressed = true;
                updateMovement();
            }
        });

        actionMap.put("rightPressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rightPressed = true;
                updateMovement();
            }
        });

        // Released actions
        actionMap.put("upReleased", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                upPressed = false;
                updateMovement();
            }
        });

        actionMap.put("downReleased", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                downPressed = false;
                updateMovement();
            }
        });

        actionMap.put("leftReleased", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                leftPressed = false;
                updateMovement();
            }
        });

        actionMap.put("rightReleased", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rightPressed = false;
                updateMovement();
            }
        });

        // Movement update timer
        Timer movementTimer = new Timer(50, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isActive && (upPressed || downPressed || leftPressed || rightPressed)) {
                    updateMovement();
                }
            }
        });
        movementTimer.start();
    }

    private void updateMovement() {
        if (clientID == -1) return;

        PlayerData myPlayer = players.get(clientID);
        if (myPlayer == null) return;

        int newX = myPlayer.x;
        int newY = myPlayer.y;

        if (upPressed) newY -= MOVEMENT_SPEED;
        if (downPressed) newY += MOVEMENT_SPEED;
        if (leftPressed) newX -= MOVEMENT_SPEED;
        if (rightPressed) newX += MOVEMENT_SPEED;

        // Keep within bounds
        newX = Math.max(0, Math.min(canvasWidth - 20, newX));
        newY = Math.max(0, Math.min(canvasHeight - 20, newY));

        // Send update to server
        String command = "/set cs3 position " + clientID + " " + newX + " " + newY;
        clientApp.sendMessage(command);
    }

    public void processCounterStrike3Data(String data) {
        if (data.startsWith("welcome")) {
            // Parse welcome message: "welcome you(ID):c(X,Y)"
            parseWelcomeMessage(data);
            showGameWindow();
            isActive = true;
        }
        else if (data.startsWith("end")) {
            close();
        }
        else {
            // Parse player update: "p(ID,x,y,r,g,b):p(ID,x,y,r,g,b):..."
            parsePlayerUpdate(data);
            if (gamePanel != null) {
                gamePanel.repaint();
            }
        }
    }

    private void parseWelcomeMessage(String data) {
        try {
            // Extract ID: "welcome you(0):c(1000,1000)"
            int idStart = data.indexOf("you(") + 4;
            int idEnd = data.indexOf(")", idStart);
            clientID = Integer.parseInt(data.substring(idStart, idEnd));

            // Extract canvas size: "c(1000,1000)"
            int sizeStart = data.indexOf("c(") + 2;
            int sizeEnd = data.indexOf(")", sizeStart);
            String[] sizeParts = data.substring(sizeStart, sizeEnd).split(",");
            canvasWidth = Integer.parseInt(sizeParts[0]);
            canvasHeight = Integer.parseInt(sizeParts[1]);

            // Add initial player data
            players.put(clientID, new PlayerData(0, 0, Color.GREEN));

        } catch (Exception e) {
            System.err.println("Error parsing welcome message: " + e.getMessage());
        }
    }

    private void parsePlayerUpdate(String data) {
        players.clear();

        // Split by "):" to get individual player data
        String[] playerStrings = data.split(":");

        for (String playerStr : playerStrings) {
            if (playerStr.trim().isEmpty()) continue;

            // Remove "p(" prefix and ")" suffix
            if (playerStr.startsWith("p(")) {
                playerStr = playerStr.substring(2);
                playerStr = playerStr.substring(0, playerStr.length() - 1);
            }

            // Parse: "ID,x,y,r,g,b"
            String[] parts = playerStr.split(",");
            if (parts.length >= 6) {
                try {
                    int id = Integer.parseInt(parts[0]);
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    int r = Integer.parseInt(parts[3]);
                    int g = Integer.parseInt(parts[4]);
                    int b = Integer.parseInt(parts[5]);

                    Color color = new Color(r, g, b);
                    players.put(id, new PlayerData(x, y, color));

                } catch (NumberFormatException e) {
                    System.err.println("Error parsing player data: " + playerStr);
                }
            }
        }
    }

    public void showGameWindow() {
        if (!gameFrame.isVisible()) {
            gameFrame.setVisible(true);
            gamePanel.requestFocusInWindow();
        }
    }

    public void close() {
        if (gameFrame != null) {
            gameFrame.dispose();
        }
        isActive = false;
    }

    // Inner class for game panel
    private class GamePanel extends JPanel {
        private final int PLAYER_SIZE = 20;

        public GamePanel() {
            setBackground(Color.BLACK);
            setPreferredSize(new Dimension(800, 600));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (players.isEmpty()) return;

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Calculate scale to fit all players
            int panelWidth = getWidth();
            int panelHeight = getHeight();

            double scaleX = (double) panelWidth / canvasWidth;
            double scaleY = (double) panelHeight / canvasHeight;
            double scale = Math.min(scaleX, scaleY);

            // Draw grid (optional)
            drawGrid(g2d, scale);

            // Draw all players
            for (Map.Entry<Integer, PlayerData> entry : players.entrySet()) {
                PlayerData player = entry.getValue();
                int scaledX = (int) (player.x * scale);
                int scaledY = (int) (player.y * scale);
                int scaledSize = (int) (PLAYER_SIZE * scale);

                // Draw player
                g2d.setColor(player.color);
                g2d.fillOval(scaledX, scaledY, scaledSize, scaledSize);

                // Draw player ID
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, (int)(12 * scale)));
                String idText = String.valueOf(entry.getKey());
                if (entry.getKey() == clientID) {
                    idText += " (You)";
                }
                g2d.drawString(idText, scaledX, scaledY - 5);

                // Draw player info
                String posText = "(" + player.x + "," + player.y + ")";
                g2d.drawString(posText, scaledX, scaledY + scaledSize + 15);
            }

            // Draw HUD
            drawHUD(g2d);
        }

        private void drawGrid(Graphics2D g2d, double scale) {
            g2d.setColor(new Color(50, 50, 50));
            int gridSize = 50;

            for (int x = 0; x <= canvasWidth; x += gridSize) {
                int scaledX = (int) (x * scale);
                g2d.drawLine(scaledX, 0, scaledX, (int)(canvasHeight * scale));
            }

            for (int y = 0; y <= canvasHeight; y += gridSize) {
                int scaledY = (int) (y * scale);
                g2d.drawLine(0, scaledY, (int)(canvasWidth * scale), scaledY);
            }
        }

        private void drawHUD(Graphics2D g2d) {
            int panelWidth = getWidth();

            // Draw info panel
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(10, 10, 200, 120);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString("Counter Strike 3", 15, 30);

            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            g2d.drawString("Your ID: " + clientID, 15, 50);
            g2d.drawString("Players: " + players.size(), 15, 70);
            g2d.drawString("Canvas: " + canvasWidth + "x" + canvasHeight, 15, 90);
            g2d.drawString("Controls: WASD or Arrow Keys", 15, 110);

            // Draw controls reminder in bottom right
            String controls = "Movement: WASD/Arrows | Close: ESC";
            int textWidth = g2d.getFontMetrics().stringWidth(controls);
            g2d.drawString(controls, panelWidth - textWidth - 10, getHeight() - 10);
        }
    }

    // Inner class for player data
    private static class PlayerData {
        int x, y;
        Color color;

        PlayerData(int x, int y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
        }
    }
}