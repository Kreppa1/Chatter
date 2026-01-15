import java.net.*;
import java.io.*;
import java.util.*;

ServerSocket serverSocket;
private List<Socket> clientss = new ArrayList<>();
private List<ClientObject> clients = new ArrayList<>();

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
        Socket clientS = serverSocket.accept();
        ClientObject client = new ClientObject(clientS, null);
        clients.add(client);
        System.out.println("Client connected: " + client.clientSocket.getRemoteSocketAddress());

        // spawn a thread for each client
        new Thread(() -> handleClient(client)).start();
    }
}

void stopServer() throws IOException {
    running = false;

    for (ClientObject c : clients) {
        if (!c.clientSocket.isClosed()) c.clientSocket.close();
    }

    if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close();
    }

    System.out.println("Server stopped");
}

void handleClient(ClientObject client) {
    try (
            BufferedReader in = new BufferedReader(new InputStreamReader(client.clientSocket.getInputStream()));
    ) {
        String line;
        while ((line = in.readLine()) != null) {
            if (client.clientName!=null) broadcast("[" + client.clientName + "]: " + line, null);
            else{
                client.clientName=line;
                broadcast("// User joined your channel: "+client.clientName, client);
            }
        }
    } catch (IOException e) {
        System.out.println("Client disconnected: " + client.clientSocket.getRemoteSocketAddress());
        broadcast("// User disconnected from your channel: "+client.clientName, client);
    } finally {
        try {
            clients.remove(client);
            client.clientSocket.close();
        } catch (IOException ignored) {}
    }
}


void clientApproval(Socket client){
    try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));)
    {

    }
    catch (IOException e){
        System.out.println("Failed to verify client");
    }
}

void broadcast(String msg, ClientObject clientToHide) {
    System.out.println(msg);
    for (ClientObject c : clients) {
        if (c!=clientToHide){
            try {
                PrintWriter out = new PrintWriter(c.clientSocket.getOutputStream(), true);
                out.println(msg);
            } catch (IOException ignore) {}
        }
    }
}


public class ClientObject{
    public ClientObject(Socket clientSocket, String clientName){
        this.clientSocket=clientSocket;
        this.clientName=clientName;
    }
    public Socket clientSocket;
    public String clientName;
}