// ChatUI.java
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

public class ChatUI {
    private JFrame frame;
    private JTextPane chatArea; // was JTextArea
    private JTextField messageField;
    private ClientApplication clientApp;

    public ChatUI(ClientApplication clientApp, String userName) {
        this.clientApp = clientApp;
        createUI(userName);
    }

    private void createUI(String userName) {
        frame = new JFrame("Chat - " + userName);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);
        frame.setLocation(100, 100);

        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        chatArea.setBackground(Color.WHITE);
        chatArea.setForeground(Color.BLACK);

        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        messageField = new JTextField();
        JButton sendButton = new JButton("Send");
        JButton disconnectButton = new JButton("Disconnect");

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        bottomPanel.add(disconnectButton, BorderLayout.WEST);
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        disconnectButton.addActionListener(e -> clientApp.disconnect());
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
    }

    private void sendMessage() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;

        if (text.startsWith("#")) {
            processClientCommand(text);
        } else {
            clientApp.sendMessage(text);
        }

        messageField.setText("");
        messageField.requestFocus();
    }

    private void processClientCommand(String command) {
        command = command.replaceAll("#", "");
        String[] param = command.split(" ");

        try{
            switch (param[0]){
                case "set":
                    switch (param[1]){
                        case "theme":
                            if (clientApp.swapUITheme(param[2])) {
                                appendMessage("Client --> Theme changed.\n", Color.blue,true);
                            } else {
                                appendMessage("Client --> Error changing theme.\n", Color.blue,true);
                            }
                            return;
                    }
                case "clear":
                case "cls":
                    clearChat();
                    return;
                default:
                    appendMessage("Client --> Command not found.\n", Color.blue,true);
            }
        }
        catch (Exception ignore){
            appendMessage("Client --> Invalid format.\n", Color.blue,true);
        }
    }

    public void appendMessage(String message) {
        if (message.startsWith("//")){
            message=message.substring(2);
            appendMessage(message, Color.black,true);
        }
        else if(message.startsWith("!!")){
            message=message.substring(2);
            appendMessage(message,Color.red,true);
        }
        else{
            appendMessage(message, chatArea.getForeground(), false);
        }
    }

    public void appendMessage(String message, Color color, boolean bold) {
        StyledDocument doc = chatArea.getStyledDocument();

        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setForeground(attrs, color);
        StyleConstants.setBold(attrs, bold);

        try {
            doc.insertString(doc.getLength(), message, attrs);
            chatArea.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public void clearChat(){
        chatArea.setText("");
    }

    public void show() {
        frame.setVisible(true);
        SwingUtilities.invokeLater(() -> messageField.requestFocusInWindow());
    }

    public void close() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    public JFrame getFrame() {
        return frame;
    }
}
