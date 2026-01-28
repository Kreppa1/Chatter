import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class CounterStrike3Manager {

    public class Player {
        private int x;
        private int y;
        private int ID;
        private Color color;

        public Player(int x, int y, int ID, Color color) {
            this.x = x;
            this.y = y;
            this.ID = ID;
            this.color = color;
        }

        public int getX() { return x; }
        public int getY() { return y; }
        public int getID() { return ID; }
        public Color getColor() { return color; }

        public void setX(int x) { this.x = x; }
        public void setY(int y) { this.y = y; }
    }

    private JFrame frame;
    private PlayerPanel panel;
    private ClientApplication clientApp;
    private List<Player> players;

    public static final int CELL_SIZE = 30;

    public CounterStrike3Manager(ClientApplication clientApp) {
        this.clientApp = clientApp;
    }

    public void open(String playerData) {
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

        JScrollPane scrollPane = new JScrollPane(panel);
        frame.add(scrollPane, BorderLayout.CENTER);

        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void processCounterStrike3Data(String playerData) {
        open(String playerData);


        List<Player> newPlayers = null;

        if (panel != null) {
            panel.setPlayers(newPlayers);
        }
    }

    public void close() {
        if (frame != null) {
            frame.dispose();
            frame = null;
            panel = null;
        }
    }

    private Player getLocalPlayer() {
        if (players == null) return null;

        for (Player p : players) {
            if (p.getID() == clientApp.ID) {
                return p;
            }
        }
        return null;
    }

    private void moveLocalPlayer(int dx, int dy) {
        Player p = getLocalPlayer();
        if (p == null) return;

        p.setX(p.getX() + dx);
        p.setY(p.getY() + dy);

        panel.repaint();
        sendLocalPlayerToServer(p);
    }

    private void sendLocalPlayerToServer(Player p) {
        String msg = "/set cs3 position (" +
                p.getX() + "," +
                p.getY() + "," +
                p.getID() + "," +
                p.getColor().getRed() + "," +
                p.getColor().getGreen() + "," +
                p.getColor().getBlue() +
                ")";

        clientApp.sendPlayerData(msg);
    }

    private void setupKeyBindings(JComponent c) {
        InputMap im = c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = c.getActionMap();

        im.put(KeyStroke.getKeyStroke("W"), "up");
        im.put(KeyStroke.getKeyStroke("S"), "down");
        im.put(KeyStroke.getKeyStroke("A"), "left");
        im.put(KeyStroke.getKeyStroke("D"), "right");

        am.put("up", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                moveLocalPlayer(0, -1);
            }
        });
        am.put("down", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                moveLocalPlayer(0, 1);
            }
        });
        am.put("left", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                moveLocalPlayer(-1, 0);
            }
        });
        am.put("right", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                moveLocalPlayer(1, 0);
            }
        });
    }

    class PlayerPanel extends JPanel {

        private List<Player> players;

        public PlayerPanel() {
            setBackground(Color.DARK_GRAY);
            setDoubleBuffered(true);
        }

        public void setPlayers(List<Player> players) {
            this.players = players;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (players == null) return;

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            for (Player p : players) {
                g2.setColor(p.getColor());
                g2.fillRect(
                        p.getX() * CELL_SIZE,
                        p.getY() * CELL_SIZE,
                        CELL_SIZE,
                        CELL_SIZE
                );

                g2.setColor(Color.BLACK);
                g2.drawRect(
                        p.getX() * CELL_SIZE,
                        p.getY() * CELL_SIZE,
                        CELL_SIZE,
                        CELL_SIZE
                );
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(2000, 2000);
        }
    }
}
