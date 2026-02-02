// CounterStrike3Manager.java - Client-side Counter Strike 3 game manager
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
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

    private double moveX = 0;
    private double moveY = 0;
    private final int MOVEMENT_SPEED = 5;
    int PLAYER_SIZE = 20;

    private int mapGridSize;
    private Color[][] mapColors;

    public CounterStrike3Manager(ClientApplication clientApp) {
        this.clientApp = clientApp;
    }

    private void initializeGameFrame() {
        gameFrame = new JFrame("Counter Strike 3 - " + clientApp.getName());
        gameFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Create panel first
        gamePanel = new GamePanel();

        // Add panel first
        gameFrame.add(gamePanel);

        // Pack to preferred size
        gameFrame.pack();

        // Then set size to ensure exact dimensions
        gameFrame.setSize(canvasWidth, canvasHeight);

        setupKeyListeners();
    }

    private void setupKeyListeners() {
        InputMap inputMap = gamePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = gamePanel.getActionMap();

        // Key pressed actions - set direction components
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, false), "upPressed");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0, false), "downPressed");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, false), "leftPressed");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0, false), "rightPressed");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, false), "upPressed");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, false), "downPressed");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false), "leftPressed");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false), "rightPressed");

        // Key released actions - clear direction components
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
                moveY = -1;
                updateMovement();
            }
        });

        actionMap.put("downPressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveY = 1;
                updateMovement();
            }
        });

        actionMap.put("leftPressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveX = -1;
                updateMovement();
            }
        });

        actionMap.put("rightPressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveX = 1;
                updateMovement();
            }
        });

        // Released actions
        actionMap.put("upReleased", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveY = moveY < 0 ? 0 : moveY;
                updateMovement();
            }
        });

        actionMap.put("downReleased", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveY = moveY > 0 ? 0 : moveY;
                updateMovement();
            }
        });

        actionMap.put("leftReleased", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveX = moveX < 0 ? 0 : moveX;
                updateMovement();
            }
        });

        actionMap.put("rightReleased", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveX = moveX > 0 ? 0 : moveX;
                updateMovement();
            }
        });

        // Movement update timer
        Timer movementTimer = new Timer(50, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isActive && (moveX != 0 || moveY != 0)) {
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

        // Normalize the movement vector if moving diagonally
        double moveMagnitude = Math.sqrt(moveX * moveX + moveY * moveY);
        double normalizedX = 0;
        double normalizedY = 0;

        if (moveMagnitude > 0) {
            normalizedX = moveX / moveMagnitude;
            normalizedY = moveY / moveMagnitude;
        }

        // Calculate new position
        int newX = myPlayer.x;
        int newY = myPlayer.y;

        // Check X movement first (slide along walls)
        if (normalizedX != 0) {
            int tempX = myPlayer.x + (int)(normalizedX * MOVEMENT_SPEED);
            if (isValidPosition(tempX, myPlayer.y)) {
                newX = tempX;
            }
        }

        // Check Y movement
        if (normalizedY != 0) {
            int tempY = myPlayer.y + (int)(normalizedY * MOVEMENT_SPEED);
            if (isValidPosition(newX, tempY)) {
                newY = tempY;
            }
        }

        // If we couldn't move in either direction, try the other axis first
        if (newX == myPlayer.x && newY == myPlayer.y) {
            // Try Y then X
            if (normalizedY != 0) {
                int tempY = myPlayer.y + (int)(normalizedY * MOVEMENT_SPEED);
                if (isValidPosition(myPlayer.x, tempY)) {
                    newY = tempY;
                }
            }
            if (normalizedX != 0) {
                int tempX = myPlayer.x + (int)(normalizedX * MOVEMENT_SPEED);
                if (isValidPosition(tempX, newY)) {
                    newX = tempX;
                }
            }
        }

        // Send update if position changed
        if (newX != myPlayer.x || newY != myPlayer.y) {
            String command = "/set cs3 position " + clientID + " " + newX + " " + newY;
            clientApp.sendMessage(command);
            // Optimistically update local position
            players.put(clientID, new PlayerData(newX, newY, myPlayer.color));
        }

        // Repaint to show movement
        if (gamePanel != null) {
            gamePanel.repaint();
        }
    }

    private boolean isValidPosition(int x, int y) {
        int playerCenterX = x + PLAYER_SIZE / 2;
        int playerCenterY = y + PLAYER_SIZE / 2;
        int playerRadius = PLAYER_SIZE / 2;

        if (playerCenterX < playerRadius || playerCenterX >= canvasWidth - playerRadius ||
                playerCenterY < playerRadius || playerCenterY >= canvasHeight - playerRadius) {
            return false;
        }

        if (mapColors != null && mapGridSize > 0) {
            int tileSize = canvasWidth / mapGridSize;

            int minTileX = Math.max(0, (playerCenterX - playerRadius) / tileSize);
            int maxTileX = Math.min(mapGridSize - 1, (playerCenterX + playerRadius) / tileSize);
            int minTileY = Math.max(0, (playerCenterY - playerRadius) / tileSize);
            int maxTileY = Math.min(mapGridSize - 1, (playerCenterY + playerRadius) / tileSize);

            for (int tileY = minTileY; tileY <= maxTileY; tileY++) {
                for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
                    Color tileColor = mapColors[tileY][tileX];
                    // Check if NOT pure white (solid)
                    if (!(tileColor.getRed() == 255 &&
                            tileColor.getGreen() == 255 &&
                            tileColor.getBlue() == 255)) {

                        int tileLeft = tileX * tileSize;
                        int tileRight = tileLeft + tileSize;
                        int tileTop = tileY * tileSize;
                        int tileBottom = tileTop + tileSize;

                        int closestX = clamp(playerCenterX, tileLeft, tileRight);
                        int closestY = clamp(playerCenterY, tileTop, tileBottom);

                        int dx = playerCenterX - closestX;
                        int dy = playerCenterY - closestY;
                        int distanceSquared = dx * dx + dy * dy;

                        if (distanceSquared < playerRadius * playerRadius) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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

            System.out.println("Canvas size received: " + canvasWidth + "x" + canvasHeight);

            // Now initialize game frame with correct size
            if (gameFrame == null) {
                initializeGameFrame();
            } else {
                // Update existing frame size
                gameFrame.setSize(canvasWidth + 16, canvasHeight + 39);
                gamePanel.setPreferredSize(new Dimension(canvasWidth, canvasHeight));
                gameFrame.pack();
            }

            if (data.contains(":m(")) {
                int mapStart = data.indexOf(":m(") + 3;
                String mapData = data.substring(mapStart);

                // Extract grid size
                int gridStart = mapData.indexOf("s(") + 2;
                int gridEnd = mapData.indexOf(")", gridStart);
                mapGridSize = Integer.parseInt(mapData.substring(gridStart, gridEnd));

                // Extract color values
                int valuesStart = mapData.indexOf("v(") + 2;
                int valuesEnd = mapData.indexOf(")", valuesStart);
                String colorValues = mapData.substring(valuesStart, valuesEnd);

                mapColors = new Color[mapGridSize][mapGridSize];
                int index = 0;
                for (int y = 0; y < mapGridSize; y++) {
                    for (int x = 0; x < mapGridSize; x++) {
                        if (index + 5 < colorValues.length()) {
                            String hex = colorValues.substring(index, index + 6);
                            int rgb = Integer.parseInt(hex, 16);
                            mapColors[y][x] = new Color(rgb);
                            index += 6;
                        }
                    }
                }
            }

            // Add initial player data
            players.put(clientID, new PlayerData(0, 0, Color.GREEN));

            showGameWindow();
            isActive = true;

        } catch (Exception e) {
            System.err.println("Error parsing welcome message: " + e.getMessage());
        }
    }
    private void parsePlayerUpdate(String data) {

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



    private class GamePanel extends JPanel {
        private final int PLAYER_SIZE = 20;

        public GamePanel() {
            setBackground(Color.BLACK);
            // Set panel to actual canvas size
            setPreferredSize(new Dimension(canvasWidth, canvasHeight));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // NO SCALING - draw at 1:1 pixel ratio
            // Draw map
            drawMap(g2d);

            // Draw players
            for (Map.Entry<Integer, PlayerData> entry : players.entrySet()) {
                PlayerData player = entry.getValue();
                int playerRadius = PLAYER_SIZE / 2;
                int playerCenterX = player.x + playerRadius;
                int playerCenterY = player.y + playerRadius;

                // Draw player as circle from center (NO SCALING)
                g2d.setColor(player.color);
                g2d.fillOval(playerCenterX - playerRadius, playerCenterY - playerRadius,
                        PLAYER_SIZE, PLAYER_SIZE);

                // Draw player ID
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 12));
                String idText = String.valueOf(entry.getKey());
                if (entry.getKey() == clientID) {
                    idText += " (You)";
                }
                g2d.drawString(idText, player.x, player.y - 5);
            }

            drawHUD(g2d);
        }

        private void drawMap(Graphics2D g2d) {
            if (mapColors == null || mapGridSize <= 0) return;

            // Create a buffered image to draw the map once
            if (mapBuffer == null || mapBuffer.getWidth() != canvasWidth || mapBuffer.getHeight() != canvasHeight) {
                mapBuffer = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics2D bufferG2d = mapBuffer.createGraphics();
                bufferG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw the entire map to the buffer
                double tileSizeX = (double) canvasWidth / mapGridSize;
                double tileSizeY = (double) canvasHeight / mapGridSize;

                for (int y = 0; y < mapGridSize; y++) {
                    for (int x = 0; x < mapGridSize; x++) {
                        int tileX = (int) Math.round(x * tileSizeX);
                        int tileY = (int) Math.round(y * tileSizeY);
                        int nextTileX = (int) Math.round((x + 1) * tileSizeX);
                        int nextTileY = (int) Math.round((y + 1) * tileSizeY);
                        int width = nextTileX - tileX;
                        int height = nextTileY - tileY;

                        Color tileColor = mapColors[y][x];
                        bufferG2d.setColor(tileColor);
                        bufferG2d.fillRect(tileX, tileY, width, height);

                        // Add slight overlap to eliminate gaps
                        if (x < mapGridSize - 1) {
                            bufferG2d.fillRect(tileX + width - 1, tileY, 1, height);
                        }
                        if (y < mapGridSize - 1) {
                            bufferG2d.fillRect(tileX, tileY + height - 1, width, 1);
                        }
                    }
                }
                bufferG2d.dispose();
            }

            // Draw the buffered map
            g2d.drawImage(mapBuffer, 0, 0, null);
        }

        // Add this as a class field
        private BufferedImage mapBuffer;

        private void drawHUD(Graphics2D g2d) {
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