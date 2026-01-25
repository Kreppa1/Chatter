// ConnectForm.java
import javax.swing.*;
import java.awt.*;

public class ConnectForm {
    private JFrame frame;
    private ClientApplication clientApp;

    public ConnectForm(ClientApplication clientApp) {
        this.clientApp = clientApp;
        createUI();
    }

    private void createUI() {
        frame = new JFrame("Connect");
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

        frame.getRootPane().setDefaultButton(connectButton);
        connectButton.addActionListener(e -> {
            String host = hostField.getText().trim();
            int port = Integer.parseInt(portField.getText().trim());
            String name = nameField.getText().trim();

            if (name.isEmpty()) return;

            frame.dispose();
            clientApp.connect(host, port, name);
        });

        frame.setLocationRelativeTo(null);
    }

    public void show() {
        frame.setVisible(true);
    }

    public JFrame getFrame() {
        return frame;
    }
}