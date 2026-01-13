import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Main {
    private static final Set<PrintWriter> clientWriters = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) {
        int port = 12345;
        System.out.println("Chat Server started on port " + port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientName;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                
                // Read the client's name as the first message
                clientName = in.readLine();
                if (clientName == null) return;
                
                clientWriters.add(out);
                broadcast(clientName + " has joined the chat.");

                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Received from " + clientName + ": " + message);
                    broadcast(clientName + ": " + message);
                }
            } catch (IOException e) {
                System.err.println("Handler error: " + e.getMessage());
            } finally {
                if (out != null) {
                    clientWriters.remove(out);
                    if (clientName != null) {
                        broadcast(clientName + " has left the chat.");
                    }
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }

        private void broadcast(String message) {
            for (PrintWriter writer : clientWriters) {
                writer.println(message);
            }
        }
    }
}
