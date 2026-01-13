import java.io.*;
import java.net.*;
import java.util.*;

public class Server {

    private static Set<ClientHandler> clientHandlers = new HashSet<>();

    public static void main(String[] args) {
        int port = 12345; // Default port, you can change this

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                clientHandlers.add(handler);
                new Thread(handler).start();
            }

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    // Broadcast message to all clients
    public static void broadcast(String message, ClientHandler exclude) {
        for (ClientHandler client : clientHandlers) {
            if (client != exclude) {
                client.sendMessage(message);
            }
        }
    }

    // Remove client from the set
    public static void removeClient(ClientHandler client) {
        clientHandlers.remove(client);
    }

    // Inner class to handle each client
    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private String name;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        @Override
        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
            ) {
                out = new PrintWriter(socket.getOutputStream(), true);

                // First message is the client's name
                name = in.readLine();
                System.out.println(name + " has connected.");
                broadcast(name + " has joined the chat.", this);

                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println(name + ": " + message);
                    broadcast(name + ": " + message, this);
                }

            } catch (IOException e) {
                System.out.println(name + " disconnected.");
            } finally {
                removeClient(this);
                broadcast(name + " has left the chat.", this);
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }
}
