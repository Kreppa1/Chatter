import java.net.*;
import java.io.*;
import java.util.*;

ServerSocket serverSocket;
private List<Socket> clients = new ArrayList<>();

boolean running;
int port = 6767;

void main() {
    try {
        startServer();
        updateServer();
        stopServer();
    } catch (IOException e) {
        System.out.println(e.getMessage());
    }
}

void startServer() throws IOException {
    serverSocket = new ServerSocket(port);
    running = true;
    System.out.println("Server started on port " + port);
}

void updateServer() throws IOException {
    while (running) {
        Socket client = serverSocket.accept();
        clients.add(client);
        System.out.println("Client connected: " + client.getRemoteSocketAddress());

        // spawn a thread for each client
        new Thread(() -> handleClient(client)).start();
    }
}

void stopServer() throws IOException {
    running = false;

    for (Socket c : clients) {
        if (!c.isClosed()) c.close();
    }

    if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close();
    }

    System.out.println("Server stopped");
}

void handleClient(Socket client) {
    try (
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
    ) {
        String line;
        while ((line = in.readLine()) != null) {
            broadcast(line, client);
        }
    } catch (IOException e) {
        System.out.println("User disconnected from your channel");
    } finally {
        try {
            clients.remove(client);
            client.close();
        } catch (IOException ignored) {}
    }
}

void broadcast(String msg, Socket sender) {
    for (Socket c : clients) {
        try {
            PrintWriter out = new PrintWriter(c.getOutputStream(), true);
            out.println(msg);
        } catch (IOException ignore) {}
    }
}