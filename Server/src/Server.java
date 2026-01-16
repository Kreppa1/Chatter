import java.net.*;
import java.io.*;
import java.util.*;

ServerSocket serverSocket;
private List<ClientObject> clients = Collections.synchronizedList(new ArrayList<>());
private List<ChannelObject> channels = Collections.synchronizedList(new ArrayList<>());

int default_channel=-1;
boolean running;
int port = 6767;

void main() {
    try {
        initializeChannels();
        startServer();
        updateServer();
        stopServer();
    } catch (IOException e) {
        System.out.println(e.getMessage());
    }
}

void initializeChannels(){
    channels.add(new ChannelObject("Main",true,true));
    channels.add(new ChannelObject("Chat",true,false));
    channels.add(new ChannelObject("Fun",true,true));
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
            whisper("!! Invalid command.",client,null);
    }
}
public void switchClientChannel(ClientObject client, int newChannel){
    int oldChannel=client.clientChannel;
    ChannelObject channelObject;
    try{
        channelObject=channels.get(newChannel);
    }
    catch (IndexOutOfBoundsException e){
        whisper("!! Channel not initialized.", client, null);
        return;
    }
    String channelResult=channelObject.checkClient(client);
    if(channelResult!=""){
        whisper(channelResult, client, null);
        return;
    }
    client.clientChannel=newChannel;
    broadcast("// User disconnected from your channel: "+client.getDisplayName(),oldChannel,null,client);
    broadcast("// User joined your channel: "+client.getDisplayName(), client.clientChannel,null,client);
    whisper("// Channel switched: "+getChannelByID(client.clientChannel).channelName+" ("+client.clientChannel+")",client,null);
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
            whisper("[System]: You cannot send messages in this channel, please enter \"/switch <ID>\" to switch channel.",sender,null);
        }
        else{
            msg="[" + sender.getDisplayName() + "]: " +msg;
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


public ChannelObject getChannelByID(int channelID){
    return channels.get(channelID);
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

    public String getDisplayName(){
        if (clientName!=null) return clientName;
        else return "?";
    }
}

public class ChannelObject{
    boolean allowMessages=true;
    boolean allowAnonymous=false;
    String channelName;

    public ChannelObject(String channelName, boolean allowMessages, boolean allowAnonymous){
        this.allowMessages=allowMessages;
        this.channelName=channelName;
        this.allowAnonymous=allowAnonymous;
    }
    public String checkClient(ClientObject client){ //if client trying to join doesnt meet the requirements, the returning string will be the error message;
        String message="!! The channel you try to join requires the client to:";
        if (!allowAnonymous && client.clientName==null) message=message+" have a name";
        else return "";
        return message+".";
    }
}


