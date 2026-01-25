// CanvasManager.java
import javax.swing.*;
import java.awt.*;

public class CanvasManager {
    private ClientApplication clientApp;
    private String userName;

    private Color[][] pixelGrid;
    private JFrame canvasFrame;
    private CanvasPanel canvasPanel;
    private int gridRows = 0;
    private int gridCols = 0;
    private static final int CELL_SIZE = 30;

    private Color selectedColor = Color.BLACK;
    private JButton colorPickerButton;

    public CanvasManager(ClientApplication clientApp, String userName) {
        this.clientApp = clientApp;
        this.userName = userName;
    }

    public void processCanvasData(String canvasData) {
        try {
            String[] rows = canvasData.split("~");
            gridRows = rows.length;

            // Find max columns
            int maxCols = 0;
            for (String row : rows) {
                String[] pixelColors = row.split(":");
                maxCols = Math.max(maxCols, pixelColors.length);
            }
            gridCols = maxCols;


            // Create grid
            pixelGrid = new Color[gridRows][gridCols];

            // Parse colors
            for (int i = 0; i < gridRows; i++) {
                String[] pixelColors = rows[i].split(":");
                for (int j = 0; j < Math.min(pixelColors.length, gridCols); j++) {
                    pixelGrid[i][j] = parseRGB(pixelColors[j]);
                }
                // Fill remaining columns with black
                for (int j = pixelColors.length; j < gridCols; j++) {
                    pixelGrid[i][j] = Color.BLACK;
                }
            }

            SwingUtilities.invokeLater(this::showCanvas);

        } catch (Exception e) {
            System.err.println("Error parsing canvas data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Color parseRGB(String rgbStr) {
        try {
            String content = rgbStr.substring(4, rgbStr.length() - 1);
            String[] parts = content.split(",");

            if (parts.length == 3) {
                int r = Integer.parseInt(parts[0].trim());
                int g = Integer.parseInt(parts[1].trim());
                int b = Integer.parseInt(parts[2].trim());
                return new Color(r, g, b);
            }
        } catch (Exception e) {
            System.err.println("Failed to parse color: " + rgbStr);
        }
        return Color.BLACK;
    }

    private void showCanvas() {
        if (canvasFrame == null) {
            createCanvasWindow();
        } else {
            updateCanvasWindow();
        }

        if (canvasPanel != null) {
            canvasPanel.updateCanvas(pixelGrid, gridRows, gridCols);
            canvasPanel.repaint();
        }
    }

    private void createCanvasWindow() {
        canvasFrame = new JFrame("Canvas - " + userName + " [" + gridRows + "x" + gridCols + "]");
        canvasFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        canvasFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                canvasFrame.setVisible(false);
                canvasFrame.dispose();
                canvasFrame = null;
                canvasPanel = null;
            }
        });

        canvasFrame.setLayout(new BorderLayout());

        // Create canvas panel
        canvasPanel = new CanvasPanel();

        // Create scroll pane for canvas
        JScrollPane canvasScrollPane = new JScrollPane(canvasPanel);
        canvasScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        canvasScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        canvasScrollPane.setBorder(BorderFactory.createEmptyBorder());

        // Set preferred viewport size
        int viewportWidth = Math.min(gridCols * CELL_SIZE + 50, 800);
        int viewportHeight = Math.min(gridRows * CELL_SIZE + 50, 600);
        canvasScrollPane.getViewport().setPreferredSize(new Dimension(viewportWidth, viewportHeight));

        canvasFrame.add(canvasScrollPane, BorderLayout.CENTER);

        // Color picker panel
        JPanel bottomPanel = createColorPickerPanel();
        canvasFrame.add(bottomPanel, BorderLayout.SOUTH);

        canvasFrame.pack();
        canvasFrame.setVisible(true);
    }

    private void updateCanvasWindow() {
        canvasFrame.setTitle("Canvas - " + userName + " [" + gridRows + "x" + gridCols + "]");

        Component centerComp = ((BorderLayout)canvasFrame.getContentPane().getLayout())
                .getLayoutComponent(BorderLayout.CENTER);
        if (centerComp instanceof JScrollPane) {
            JScrollPane scrollPane = (JScrollPane) centerComp;
            int viewportWidth = Math.min(gridCols * CELL_SIZE + 50, 800);
            int viewportHeight = Math.min(gridRows * CELL_SIZE + 50, 600);
            scrollPane.getViewport().setPreferredSize(new Dimension(viewportWidth, viewportHeight));
        }

        canvasFrame.pack();
        canvasFrame.toFront();
    }

    private JPanel createColorPickerPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(Color.DARK_GRAY);

        colorPickerButton = new JButton("Pick Color");
        updateColorPickerButton();

        colorPickerButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(
                    canvasFrame,
                    "Choose Color",
                    selectedColor
            );

            if (newColor != null) {
                selectedColor = newColor;
                updateColorPickerButton();
            }
        });

        panel.add(colorPickerButton, BorderLayout.CENTER);

        // Info label showing grid size
        String sizeInfo = gridRows + "x" + gridCols + " grid";
        JLabel infoLabel = new JLabel(sizeInfo);
        infoLabel.setForeground(Color.WHITE);
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        panel.add(infoLabel, BorderLayout.SOUTH);

        return panel;
    }

    private void updateColorPickerButton() {
        colorPickerButton.setBackground(selectedColor);
        colorPickerButton.setForeground(getContrastColor(selectedColor));
        colorPickerButton.setFocusPainted(false);
        colorPickerButton.setFont(new Font("Arial", Font.BOLD, 14));
        colorPickerButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 2),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
    }

    private Color getContrastColor(Color color) {
        double luminance = (0.299 * color.getRed() +
                0.587 * color.getGreen() +
                0.114 * color.getBlue()) / 255;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    public void close() {
        if (canvasFrame != null) {
            canvasFrame.dispose();
            canvasFrame = null;
        }
    }

    // Inner class for Canvas Panel
    class CanvasPanel extends JPanel {
        private Color[][] currentGrid;
        private int currentRows = 0;
        private int currentCols = 0;
        private final StringBuilder pixelStreamBuffer = new StringBuilder();
        private int lastCellCol = -1;
        private int lastCellRow = -1;

        public CanvasPanel() {
            setBackground(Color.DARK_GRAY);
            setDoubleBuffered(true);
            setLayout(null);

            MouseHandler mouseHandler = new MouseHandler();
            addMouseListener(mouseHandler);
            addMouseMotionListener(mouseHandler);
        }

        public void updateCanvas(Color[][] grid, int rows, int cols) {
            System.out.println("UPDATE CANVAS: " + rows + "x" + cols);
            this.currentGrid = grid;
            this.currentRows = rows;
            this.currentCols = cols;

            setPreferredSize(new Dimension(cols * CELL_SIZE, rows * CELL_SIZE));
            setMinimumSize(getPreferredSize());
            setMaximumSize(getPreferredSize());

            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (currentGrid == null || currentRows == 0 || currentCols == 0) {
                g.setColor(Color.DARK_GRAY);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.WHITE);
                g.drawString("Waiting for canvas data...", 10, 20);
                return;
            }

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Clear background
            g2d.setColor(Color.DARK_GRAY);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // Draw grid
            for (int row = 0; row < currentRows; row++) {
                for (int col = 0; col < currentCols; col++) {
                    Color color = currentGrid[row][col];
                    if (color == null) color = Color.BLACK;

                    g2d.setColor(color);
                    g2d.fillRect(col * CELL_SIZE, row * CELL_SIZE, CELL_SIZE, CELL_SIZE);

                    // Draw grid lines
                    g2d.setColor(Color.GRAY);
                    g2d.drawRect(col * CELL_SIZE, row * CELL_SIZE, CELL_SIZE, CELL_SIZE);

                    // Draw coordinates if cell is big enough
                    if (CELL_SIZE > 20) {
                        g2d.setColor(Color.WHITE);
                        g2d.setFont(new Font("Arial", Font.PLAIN, 9));
                        g2d.drawString(col + "," + row, col * CELL_SIZE + 3, row * CELL_SIZE + 12);
                    }
                }
            }

            // Debug: mark total size
            g2d.setColor(Color.RED);
            int totalWidth = currentCols * CELL_SIZE;
            int totalHeight = currentRows * CELL_SIZE;
            g2d.drawRect(0, 0, totalWidth - 1, totalHeight - 1);
        }

        @Override
        public Dimension getPreferredSize() {
            if (currentCols == 0 || currentRows == 0) {
                return new Dimension(400, 300);
            }
            return new Dimension(
                    currentCols * CELL_SIZE,
                    currentRows * CELL_SIZE
            );
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        // Mouse handler inner class
        class MouseHandler extends java.awt.event.MouseAdapter {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                handlePaint(e);
            }

            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                handlePaint(e);
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                flushPixelStream();
            }

            private void handlePaint(java.awt.event.MouseEvent e) {
                if (currentGrid == null || currentRows == 0 || currentCols == 0) {
                    return;
                }

                int col = e.getX() / CELL_SIZE;
                int row = e.getY() / CELL_SIZE;

                // Ignore repeats while dragging inside same cell
                if (col == lastCellCol && row == lastCellRow) return;

                if (col < 0 || col >= currentCols || row < 0 || row >= currentRows) {
                    return;
                }

                lastCellCol = col;
                lastCellRow = row;

                int r = selectedColor.getRed();
                int g = selectedColor.getGreen();
                int b = selectedColor.getBlue();

                // Buffer pixel
                pixelStreamBuffer
                        .append("(")
                        .append(col).append(",")
                        .append(row).append(",")
                        .append(r).append(",")
                        .append(g).append(",")
                        .append(b)
                        .append("):");

                // Update local grid
                currentGrid[row][col] = selectedColor;
                repaint();
            }

            private void flushPixelStream() {
                if (pixelStreamBuffer.isEmpty()) return;

                String command = "/set pixel stream " + pixelStreamBuffer;
                clientApp.sendPixelData(command);

                pixelStreamBuffer.setLength(0);
                lastCellCol = -1;
                lastCellRow = -1;
            }
        }
    }
}