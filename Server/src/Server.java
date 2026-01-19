import java.net.*;
import java.io.*;
import java.util.*;

ServerSocket serverSocket;
private List<ClientObject> clients = Collections.synchronizedList(new ArrayList<>());
private List<ChannelObject> channels = Collections.synchronizedList(new ArrayList<>());
private List<Thread> clientThreads= Collections.synchronizedList(new ArrayList<>());
private ClientObject serverDummy;

//special channels
int default_channel=-1;
int system_channel=-2;

//sepcial rols /set by initializeRoles
List<RoleObject> roles = new ArrayList<>();
int default_role_ID;
int admin_role_ID;
int system_role_ID;

boolean running;
int port = 6767;

void main() {
    try {
        initializeRoles();
        initializeChannels();
        startServer();
        updateServer();
        stopServer();
    } catch (IOException e) {
        System.out.println(e.getMessage());
    }
}

void initializeRoles(){
    roles.add(new RoleObject(0,"0","User"));
    roles.add(new RoleObject(-1,"35890673905693465394672","Administrator"));
    roles.add(new RoleObject(-2,null,"System"));
    default_role_ID=0;
    admin_role_ID=-1;
    system_role_ID=-2;
}
void initializeChannels(){
    serverDummy=new ClientObject(null,"System",-2,getRoleByID(-2));
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
        ClientObject client = new ClientObject(clientS, null,-1, getRoleByID(0));
        clients.add(client);
        System.out.println("Client connected: " + client.clientSocket.getRemoteSocketAddress());
        whisper("// Connected", client, serverDummy);

        clientThreads.add(new Thread(() -> handleClient(client)));
        clientThreads.getLast().start();
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
        broadcast("// User disconnected from your channel: "+client.clientName,client.clientChannel,serverDummy, client);
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

    try{
        switch (param[0]){
            case "set":
                switch (param[1]){
                    case "name":
                        changeClientName(client,param[2]);
                        return;
                    case "role":
                        changeClientRole(client, Integer.parseInt(param[2]), param[3]);
                        return;
                }
                return;
            case "switch":
                switchClientChannel(client, Integer.parseInt(param[1]));
                return;
            case "msg":
            case "whisper":
                String message = String.join(" ",
                        java.util.Arrays.copyOfRange(param, 2, param.length));
                whisper(message.trim(),getClientByName(param[1]),client);
                return;
        }
    }
    catch (Exception ignore){
        whisper("!! Invalid format.",client,serverDummy);
    }

    if (client.clientRole.roleID==admin_role_ID){
        try{
            switch (param[0]){
                case "kick":
                    if(param[1].equals("id")){
                        String message = String.join(" ",
                                java.util.Arrays.copyOfRange(param, 3, param.length));
                        kickClient(clients.get(Integer.parseInt(param[2])),message.trim(),client);
                        return;
                    }
                    String message = String.join(" ",
                            java.util.Arrays.copyOfRange(param, 2, param.length));
                    kickClient(getClientByName(param[1]),message.trim(),client);
                    return;
            }
        }
        catch (Exception ignore){
            whisper("!! Invalid format.",client,serverDummy);
        }
    }
    whisper("!! Invalid command.",client,serverDummy);
}
public void switchClientChannel(ClientObject client, int newChannel){
    int oldChannel=client.clientChannel;
    ChannelObject channelObject;
    try{
        channelObject=channels.get(newChannel);
    }
    catch (IndexOutOfBoundsException e){
        whisper("!! Channel not initialized.", client, serverDummy);
        return;
    }
    String channelResult=channelObject.checkClient(client);
    if(channelResult!=""){
        whisper(channelResult, client, serverDummy);
        return;
    }
    client.clientChannel=newChannel;
    broadcast("// User disconnected from your channel: "+client.getDisplayName(),oldChannel,serverDummy,client);
    broadcast("// User joined your channel: "+client.getDisplayName(), client.clientChannel,serverDummy,client);
    whisper("// Channel switched: "+getChannelByID(client.clientChannel).channelName+" ("+client.clientChannel+")",client,serverDummy);
}
public void changeClientName(ClientObject client, String newName){
    System.out.println("nigger");
    if (null == getClientByName(newName)){
        System.out.println("nigger2");
        client.clientName=newName;
        whisper("// Username edited: "+client.clientName,client,serverDummy);
    }
    else whisper("// Username allready taken, no changes apply", client, serverDummy);
}
public void changeClientRole(ClientObject client, int roleID, String token){
    RoleObject targetRole=getRoleByID(roleID);
    if (targetRole==null){
        whisper("// The user group you specified is not initialized",client,serverDummy);
        return;
    }
    if (targetRole.roleToken==null) {
        whisper("// The user group you specified is a system role only.",client,serverDummy);
        return;
    }
    if (token.equals(targetRole.roleToken)){
        client.clientRole=targetRole;
        whisper("// User group assigned: "+client.clientRole.roleName+" ("+client.clientRole.roleID+")",client,serverDummy);
    }
    else{
        whisper("// The token you entered is invalid, no changes apply.",client,serverDummy);
    }
}
public void kickClient(ClientObject target, String reason, ClientObject client) throws IOException {
    String targetName=target.getDisplayName();
    int targetID=clients.indexOf(target);
    whisper("!! You have been kicked from the server: "+reason,target,serverDummy);
    target.clientSocket.close();
    clients.remove(target);
    whisper("// Client kicked: "+targetName+" ("+targetID+")", client, serverDummy);
}


void broadcast(String msg, int channelID, ClientObject sender ,ClientObject clientToHideFrom) {
    if(sender.getDisplayName()!="System"){
        if (channelID==default_channel){
            whisper("[System]: You cannot send messages in this channel, please enter \"/switch <ID>\" to switch channel.",sender, serverDummy);
        }
        else{
            msg="[" + sender.getDisplayName() + "]: " +msg;
        }
    }
    System.out.println("("+channelID+") "+msg);
    for (ClientObject c : clients) {
        if ((c!=clientToHideFrom && c.clientChannel==channelID && c.clientChannel!=default_channel) || channelID==system_channel){
            try {
                PrintWriter out = new PrintWriter(c.clientSocket.getOutputStream(), true);
                out.println(msg);
            } catch (IOException ignore) {}
        }
    }
}
void whisper(String msg, ClientObject target, ClientObject sender){

    if(sender!=serverDummy) msg="("+sender.getDisplayName()+" whispers to you: "+msg+")";

    try{
        PrintWriter out= new PrintWriter(target.clientSocket.getOutputStream(), true);
        out.println(msg);
        System.out.println("(WHISPER from:\""+sender.getDisplayName()+"\", to:\""+target.getDisplayName()+"\") "+msg);
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
public RoleObject getRoleByID(int ID){
    for(RoleObject r : roles){
        if(r.roleID==ID){
            return r;
        }
    }
    return null;
}

public class ClientObject{
    public ClientObject(Socket clientSocket, String clientName, int clientChannel, RoleObject clientRole){
        this.clientSocket=clientSocket;
        this.clientName=clientName;
        this.clientChannel=clientChannel;
        this.clientRole=clientRole;
    }
    public Socket clientSocket;
    public String clientName;
    public int clientChannel;
    public RoleObject clientRole;

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

public class RoleObject{
    public RoleObject(int roleID, String roleToken, String roleName){
        this.roleID=roleID;
        this.roleToken=roleToken;
        this.roleName=roleName;
    }
    int roleID;
    String roleToken;
    String roleName;
}


