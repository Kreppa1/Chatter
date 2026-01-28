import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

public class CounterStrike3Manager {

    public class Player {
        private int x;
        private int y;
        private int ID;
        private Color color;

        public Player(int ID, int x, int y, Color color) {
            this.ID = ID;
            this.x = x;
            this.y = y;
            this.color = color;
        }

        public int getX() { return x; }
        public int getY() { return y; }
        public int getID() { return ID; }
        public Color getColor() { return color; }
    }

    private JFrame frame;
    private PlayerPanel panel;
    private ClientApplication clientApp;
    private int windowWidth;
    private int windowHeight;
    private int myLastX = -1;
    private int myLastY = -1;
    private boolean hasReceivedInitialData = false;

    public CounterStrike3Manager(ClientApplication clientApp) {
        this.clientApp = clientApp;
    }

    public void open() {
        if (frame != null) return;

        frame = new JFrame("Players View");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                close();
            }
        });

        panel = new PlayerPanel();
        setupKeyBindings(panel);
        frame.add(panel, BorderLayout.CENTER);

        // Set default size, will be updated when we get window dimensions
        windowWidth = 800;
        windowHeight = 600;
        frame.setSize(windowWidth, windowHeight);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void processCounterStrike3Data(String playerData) {
        if (!hasReceivedInitialData) {
            // First message: "welcome you(id):c(X,Y)"
            processInitialWelcome(playerData);
        } else {
            // Subsequent messages: "all p(id,x,y,r,g,b):p(id,x,y,r,g,b):..."
            processPlayerUpdate(playerData);
        }
    }

    private void processInitialWelcome(String welcomeData) {
        // Format: "welcome you(id):c(X,Y)"
        try {
            // Parse the ID from "you(id)"
            int idStart = welcomeData.indexOf("you(");
            if (idStart != -1) {
                int idEnd = welcomeData.indexOf("):", idStart);
                String idStr = welcomeData.substring(idStart + 4, idEnd);
                clientApp.ID = Integer.parseInt(idStr);
            }

            // Parse window dimensions from "c(X,Y)"
            int cStart = welcomeData.indexOf("c(");
            if (cStart != -1) {
                int cEnd = welcomeData.indexOf(")", cStart);
                String cData = welcomeData.substring(cStart + 2, cEnd);
                String[] coords = cData.split(",");

                if (coords.length >= 2) {
                    windowWidth = Integer.parseInt(coords[0].trim());
                    windowHeight = Integer.parseInt(coords[1].trim());

                    // Update frame size
                    if (frame != null) {
                        frame.setSize(windowWidth, windowHeight);
                        panel.setWindowSize(windowWidth, windowHeight);
                    }
                }
            }

            hasReceivedInitialData = true;
            open(); // Make sure window is open

        } catch (Exception e) {
            System.err.println("Error parsing welcome data: " + welcomeData);
            e.printStackTrace();
        }
    }

    private void processPlayerUpdate(String playerData) {
        // Format: "all p(id,x,y,r,g,b):p(id,x,y,r,g,b):..."
        open(); // Make sure window is open

        List<Player> currentPlayers = new ArrayList<>();

        // Split by ":" to get each player
        String[] playerStrings = playerData.split(":");

        for (String playerStr : playerStrings) {
            if (playerStr.startsWith("p(") && playerStr.endsWith(")")) {
                try {
                    String playerInfo = playerStr.substring(2, playerStr.length() - 1);
                    String[] parts = playerInfo.split(",");

                    if (parts.length >= 6) {
                        int id = Integer.parseInt(parts[0].trim());
                        int x = Integer.parseInt(parts[1].trim());
                        int y = Integer.parseInt(parts[2].trim());
                        int r = Integer.parseInt(parts[3].trim());
                        int g = Integer.parseInt(parts[4].trim());
                        int b = Integer.parseInt(parts[5].trim());

                        Color color = new Color(r, g, b);
                        Player player = new Player(id, x, y, color);
                        currentPlayers.add(player);

                        // Store my position for movement reference
                        if (id == clientApp.ID) {
                            myLastX = x;
                            myLastY = y;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing player: " + playerStr);
                }
            }
        }

        // Update the panel
        if (panel != null) {
            panel.setPlayers(currentPlayers);
        }
    }

    public void close() {
        if (frame != null) {
            frame.dispose();
            frame = null;
            panel = null;
        }
    }

    private void moveLocalPlayer(int dx, int dy) {
        // Calculate new position based on my last known position
        if (myLastX == -1 || myLastY == -1) return;

        int newX = myLastX + dx;
        int newY = myLastY + dy;

        // Boundary checking
        if (newX < 0) newX = 0;
        if (newX > windowWidth - 20) newX = windowWidth - 20;
        if (newY < 0) newY = 0;
        if (newY > windowHeight - 40) newY = windowHeight - 40;

        // Update my last known position
        myLastX = newX;
        myLastY = newY;

        // Send only my new position to server
        sendPositionToServer(newX, newY);
    }

    private void sendPositionToServer(int x, int y) {
        // NEW FORMAT: /set cs3 position id x y (no parentheses)
        String msg = "/set cs3 position " +
                clientApp.ID + " " +
                x + " " +
                y;

        clientApp.sendPlayerData(msg);
    }

    // Optional: if you need to send color changes
    private void sendColorToServer(Color color) {
        // NEW FORMAT: /set cs3 color id r g b (no parentheses)
        String msg = "/set cs3 color " +
                clientApp.ID + " " +
                color.getRed() + " " +
                color.getGreen() + " " +
                color.getBlue();

        clientApp.sendPlayerData(msg);
    }

    private void setupKeyBindings(JComponent c) {
        InputMap im = c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = c.getActionMap();

        // WASD keys for movement
        im.put(KeyStroke.getKeyStroke("W"), "up");
        im.put(KeyStroke.getKeyStroke("S"), "down");
        im.put(KeyStroke.getKeyStroke("A"), "left");
        im.put(KeyStroke.getKeyStroke("D"), "right");

        // Arrow keys as alternative
        im.put(KeyStroke.getKeyStroke("UP"), "upArrow");
        im.put(KeyStroke.getKeyStroke("DOWN"), "downArrow");
        im.put(KeyStroke.getKeyStroke("LEFT"), "leftArrow");
        im.put(KeyStroke.getKeyStroke("RIGHT"), "rightArrow");

        // Movement speed
        int moveSpeed = 10;

        am.put("up", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                moveLocalPlayer(0, -moveSpeed);
            }
        });
        am.put("down", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                moveLocalPlayer(0, moveSpeed);
            }
        });
        am.put("left", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                moveLocalPlayer(-moveSpeed, 0);
            }
        });
        am.put("right", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                moveLocalPlayer(moveSpeed, 0);
            }
        });

        // Arrow key actions (same as WASD)
        am.put("upArrow", am.get("up"));
        am.put("downArrow", am.get("down"));
        am.put("leftArrow", am.get("left"));
        am.put("rightArrow", am.get("right"));
    }

    class PlayerPanel extends JPanel {

        private List<Player> currentPlayers;
        private int panelWidth;
        private int panelHeight;

        public PlayerPanel() {
            setBackground(Color.DARK_GRAY);
            setDoubleBuffered(true);
            panelWidth = 800;
            panelHeight = 600;
        }

        public void setWindowSize(int width, int height) {
            this.panelWidth = width;
            this.panelHeight = height;
            if (frame != null) {
                frame.setSize(width, height);
            }
            revalidate();
            repaint();
        }

        public void setPlayers(List<Player> players) {
            this.currentPlayers = players;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (currentPlayers == null || currentPlayers.isEmpty()) return;

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw all players from the current data
            for (Player p : currentPlayers) {
                g2.setColor(p.getColor());

                // Draw player as a circle
                int playerSize = 20;
                int x = p.getX();
                int y = p.getY();

                // Fill circle for player
                g2.fillOval(x, y, playerSize, playerSize);

                // Draw outline
                g2.setColor(Color.BLACK);
                g2.drawOval(x, y, playerSize, playerSize);

                // Draw player ID
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 10));
                String idText = String.valueOf(p.getID());
                FontMetrics fm = g2.getFontMetrics();
                int textWidth = fm.stringWidth(idText);
                int textHeight = fm.getHeight();
                g2.drawString(idText,
                        x + (playerSize - textWidth) / 2,
                        y + (playerSize + textHeight) / 2 - 2);

                // If this is the local player, draw a highlight
                if (clientApp != null && p.getID() == clientApp.ID) {
                    g2.setColor(Color.YELLOW);
                    g2.drawOval(x - 2, y - 2, playerSize + 4, playerSize + 4);
                }
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(panelWidth, panelHeight);
        }
    }
}