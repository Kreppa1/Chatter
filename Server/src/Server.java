import java.net.*;
import java.io.*;
import java.util.*;

ServerSocket serverSocket;
private List<ClientObject> clients = Collections.synchronizedList(new ArrayList<>());

int default_channel=-1;
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
        ClientObject client = new ClientObject(clientS, null,-1);
        clients.add(client);
        System.out.println("Client connected: " + client.clientSocket.getRemoteSocketAddress());
        whisper("// Connected", client, null);

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
            if(line.startsWith("/")) processClientCommand(line, client);
            else{
                broadcast(line,client.clientChannel,client, null);
            }
        }
    } catch (IOException e) {
        System.out.println("Client disconnected: " + client.clientSocket.getRemoteSocketAddress());
        broadcast("// User disconnected from your channel: "+client.clientName,client.clientChannel,null, client);
    } finally {
        try {
            clients.remove(client);
            client.clientSocket.close();
        } catch (IOException ignored) {}
    }
}
public void processClientCommand(String command, ClientObject client){
    command=command.replaceAll("/","");
    String param[]=command.split(" ");

    switch (param[0]){
        case "set":
            switch (param[1]){
                case "name":
                    changeClientName(client,param[2]);
                    break;
            }
            break;
        case "switch":
            switchClientChannel(client, Integer.parseInt(param[1]));
            break;
        case "msg":
        case "whisper":
            String message = String.join(" ",
                    java.util.Arrays.copyOfRange(param, 2, param.length));
            whisper(message.trim(),getClientByName(param[1]),client);
            break;
        default:
            whisper("[System] Invalid command.",client,null);
    }
}
public void switchClientChannel(ClientObject client, int newChannel){
    client.clientChannel=newChannel;
    broadcast("// User joined your channel: "+client.clientName, client.clientChannel,null,client);
    whisper("// Channel switched: "+client.clientChannel,client,null);
}
public void changeClientName(ClientObject client, String newName){
    System.out.println("nigger");
    if (null == getClientByName(newName)){
        System.out.println("nigger2");
        client.clientName=newName;
        whisper("// Username edited: "+client.clientName,client,null);
    }
    else whisper("// Username allready taken, no changes apply", client, null);
}


void broadcast(String msg, int channelID, ClientObject sender ,ClientObject clientToHideFrom) {
    if(sender!=null){
        if (channelID==default_channel){
            whisper("[System]: You cannot send messages in this cannel, please enter \"/switch <ID>\" to switch channel.",sender,null);
        }
        if(sender.clientName!=null){
            msg="[" + sender.clientName + "]: " +msg;
        }
        else{
            msg="[?]: " +msg;
        }
    }
    System.out.println(msg);
    for (ClientObject c : clients) {
        if (c!=clientToHideFrom && c.clientChannel==channelID && c.clientChannel!=default_channel){
            try {
                PrintWriter out = new PrintWriter(c.clientSocket.getOutputStream(), true);
                out.println(msg);
            } catch (IOException ignore) {}
        }
    }
}
void whisper(String msg, ClientObject target, ClientObject sender){
    if(sender!=null){
        msg="("+sender.clientName+" whispers to you: "+msg+")";
    }
    try{
        PrintWriter out= new PrintWriter(target.clientSocket.getOutputStream(), true);
        out.println(msg);
    }
    catch (IOException ignore){}
}

public ClientObject getClientByName(String clientName) {
    for (ClientObject c : clients) {
        if (c.clientName==null) continue;
        if (c.clientName.equals(clientName)) return c;
    }
    return null;
}

public class ClientObject{
    public ClientObject(Socket clientSocket, String clientName, int clientChannel){
        this.clientSocket=clientSocket;
        this.clientName=clientName;
        this.clientChannel=clientChannel;
    }
    public Socket clientSocket;
    public String clientName;
    public int clientChannel;
}

