// ClientApplication.java - Main controller class
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientApplication {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String host;
    private int port;
    private String name;
    public int ID;

    private ConnectForm connectForm;
    private ChatUI chatUI;
    private CanvasManager canvasManager;
    private CounterStrike3Manager counterStrike3Manager;

    private final Map<String, String> LAF_MAP = new HashMap<>();
    private List<JFrame> frames;

    public ClientApplication() {
        initializeLookAndFeelMap();
        frames = new ArrayList<>();
        showConnectForm();
    }

    private void initializeLookAndFeelMap() {
        LAF_MAP.put("metal", "javax.swing.plaf.metal.MetalLookAndFeel");
        LAF_MAP.put("nimbus", "javax.swing.plaf.nimbus.NimbusLookAndFeel");
        LAF_MAP.put("system", UIManager.getSystemLookAndFeelClassName());
        LAF_MAP.put("motif", "com.sun.java.swing.plaf.motif.MotifLookAndFeel");
    }

    private void showConnectForm() {
        connectForm = new ConnectForm(this);
        frames.add(connectForm.getFrame());
        connectForm.show();
    }

    public void connect(String host, int port, String name) {
        this.host = host;
        this.port = port;
        this.name = name;

        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println("/set name " + name);

            // Initialize UI components
            chatUI = new ChatUI(this, name);
            frames.add(chatUI.getFrame());
            canvasManager = new CanvasManager(this, name);
            counterStrike3Manager = new CounterStrike3Manager(this);
            // Show chat UI
            chatUI.show();

            // Start listening for messages
            startMessageListener();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Failed to connect to: " + host + ":" + port);
        }
    }

    private void startMessageListener() {
        new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    final String message = msg.trim();
                    SwingUtilities.invokeLater(() -> processIncomingMessage(message));
                }
                new Thread(this::closeConnection).start();
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    new Thread(this::closeConnection).start();
                });
            }
        }).start();
    }

    private void processIncomingMessage(String message) {
        if (message.startsWith("§pixel")) {
            canvasManager.processCanvasData(message.substring(6));
        } else if (message.startsWith("§cs3")) {
            counterStrike3Manager.processCounterStrike3Data(message.substring(3));
        }
        else {
            if (chatUI != null) {
                chatUI.appendMessage(message + "\n");
            }
        }
    }

    public void sendMessage(String text) {
        if (out != null && text != null && !text.trim().isEmpty()) {
            out.println(text.trim());
        }
    }

    public void sendPixelData(String pixelData) {
        if (out != null && pixelData != null && !pixelData.trim().isEmpty()) {
            out.println(pixelData);
        }
    }

    public void sendPlayerData(String playerData) {
        if (out != null && playerData != null && !playerData.trim().isEmpty()) {
            out.println(playerData);
        }
    }

    public void disconnect() {
        sendMessage("/exit");
    }

    private void closeConnection() {
        if (chatUI != null) {
            SwingUtilities.invokeLater(() ->chatUI.appendMessage("Client --> Connection lost.\n", Color.blue,true));
        }
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Thread.sleep(1300);
        } catch (InterruptedException ignore){}
        if (canvasManager != null) {
            canvasManager.close();
        }
        if (counterStrike3Manager != null) {
            counterStrike3Manager.close();
        }
        if (chatUI!=null){
            chatUI.close();
        }
        new Thread(() -> {
            new ClientApplication();
        }).start();
    }

    public boolean swapUITheme(String themeName) {
        String laf = LAF_MAP.get(themeName.toLowerCase());
        if (laf == null) return false;

        try {
            UIManager.setLookAndFeel(laf);
            for (JFrame frame : frames) {
                SwingUtilities.updateComponentTreeUI(frame);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getName() {
        return name;
    }

    public ChatUI getChatUI() {
        return chatUI;
    }
}