import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter server IP and port (e.g., localhost:12345): ");
        String input = scanner.nextLine();
        String[] parts = input.split(":");
        if (parts.length != 2) {
            System.out.println("Invalid format. Use IP:PORT");
            return;
        }

        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        System.out.print("Enter your name: ");
        String name = scanner.nextLine();

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Send name to server
            out.println(name);

            System.out.println("Connected");

            // Thread to listen for messages from server
            Thread listenerThread = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        System.out.println(serverMessage);
                    }
                } catch (IOException e) {
                    System.out.println("Connection closed by server.");
                }
            });
            listenerThread.start();

            // Main loop to send messages
            while (true) {
                String message = scanner.nextLine();
                if (message.equalsIgnoreCase("exit")) {
                    break;
                }
                out.println(message);
            }

        } catch (IOException e) {
            System.err.println("Could not connect to server: " + e.getMessage());
        }
    }
}
