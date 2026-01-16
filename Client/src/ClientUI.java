import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class ClientUI {

    // Networking
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    // User info
    private String host;
    private int port;
    private String name;

    // UI
    private JFrame chatFrame;
    private JTextArea chatArea;
    private JTextField messageField;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientUI::new);
    }

    public ClientUI() {
        showConnectForm();
    }


    // Connect form
    private void showConnectForm() {
        JFrame frame = new JFrame("Connect");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);
        frame.setLayout(new GridLayout(4, 2, 5, 5));

        JTextField hostField = new JTextField("localhost");
        JTextField portField = new JTextField("6767");
        JTextField nameField = new JTextField();

        JButton connectButton = new JButton("Connect");

        frame.add(new JLabel("Address:"));
        frame.add(hostField);
        frame.add(new JLabel("Port:"));
        frame.add(portField);
        frame.add(new JLabel("Name:"));
        frame.add(nameField);
        frame.add(new JLabel());
        frame.add(connectButton);

        connectButton.addActionListener(e -> {
            host = hostField.getText().trim();
            port = Integer.parseInt(portField.getText().trim());
            name = nameField.getText().trim();

            if (name.isEmpty()) return;

            frame.dispose();
            startClient();
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    //Chat UI
    private void showChatUI() {
        chatFrame = new JFrame("Chat - " + name);
        chatFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        chatFrame.setSize(500, 400);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);

        JScrollPane scrollPane = new JScrollPane(chatArea);

        messageField = new JTextField();
        JButton sendButton = new JButton("Send");
        JButton disconnectButton = new JButton("Disconnect");

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(disconnectButton, BorderLayout.WEST);
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        chatFrame.add(scrollPane, BorderLayout.CENTER);
        chatFrame.add(bottomPanel, BorderLayout.SOUTH);

        disconnectButton.addActionListener(e -> disconnect());
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

        chatFrame.setLocationRelativeTo(null);
        chatFrame.setVisible(true);
    }



    //Connect to Server
    private void startClient() {
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            //out.println("/set name "+name);

            showChatUI();
            listenForMessages();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Failed to resolve hostname: "+host+":"+port);
        }
    }


    //Message listen thread
    private void listenForMessages() {
        new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    String finalMsg = msg;
                    SwingUtilities.invokeLater(() ->
                            chatArea.append(finalMsg + "\n")
                    );
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() ->
                        chatArea.append("// Disconnected\n")
                );
            }
        }).start();
    }

    //Send function triggered by send button
    private void sendMessage() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;

        out.println(text);
        messageField.setText("");
    }


    //Not done yet, triggerd by disconnect button
    private void disconnect(){
        chatArea.append("// Disconnected\n");
        System.exit(0);
    }
}
