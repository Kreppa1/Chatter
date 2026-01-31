import java.awt.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class Server {
    ServerSocket serverSocket;
    private List<ClientObject> clients = Collections.synchronizedList(new ArrayList<>());
    private List<ChannelObject> channels = Collections.synchronizedList(new ArrayList<>());
    private List<Thread> clientThreads= Collections.synchronizedList(new ArrayList<>());
    public ClientObject serverDummy;

    //special channels
    int default_channel=-1;
    String[] default_channel_welcome; //Server welcome messages, displayed for the abstract channel -1 uppon joining the server (works like the welcomeMessages array of normal channels)s
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
        System.out.println("Roles initialized.");
    }
    void initializeChannels(){
        serverDummy=new ClientObject(null,"System",-2,getRoleByID(-2));
        default_channel_welcome = new String[]{"Welcome to this server! You are now in the void channel, please enter the command: \"/switch <ChannelID>\", to join a real channel."};
        channels.add(new ChannelObject("Main",true,true, new String[]{"Welcome to the Main channel, everyone can chat here.","!! Beware off diddy bluds!"}));
        channels.add(new ChannelObject("Chat",true,false, new String[]{"This is a channel can only be used by non anonym users"}));
        channels.add(new PixelChannel("R/Placeoderso",false,false,new String[]{"This is a Pixel-Channel, here you can place pixels on the canvas, as long as your client supports it."},26,20));
        channels.add(new CounterStrikeChannel(this,"CS",false,false,null,true,640,640, "Server/src/maps/map.png"));
        System.out.println("Channels initialized.");
    }

    void startServer() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        System.out.println("Server started on port " + port+".");
    }

    void updateServer() throws IOException {
        while (running) {
            Socket clientS = serverSocket.accept();
            ClientObject client = new ClientObject(clientS, null,-1, getRoleByID(0));
            clients.add(client);
            System.out.println("Client connected: " + client.clientSocket.getRemoteSocketAddress());
            whisper("// Connected.", client, serverDummy);
            for (String msg : default_channel_welcome){
                whisper(msg, client,serverDummy);
            }

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
        Thread destressThread=null;
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(client.clientSocket.getInputStream()));
        ) {
            String line;
            while ((line = in.readLine()) != null) {
                try {
                    if(!getChannelByID(client.clientChannel).allowSpam) client.messageStress++;
                }
                catch (IndexOutOfBoundsException ignore){
                    client.messageStress++;
                }
                if (client.messageStress>=10){
                    whisper("!! Action currently not possible due to spam protection. Please wait a few seconds and try again.",client,serverDummy);
                    continue;
                }
                if (client.messageStress>1){
                    if(destressThread==null){
                        destressThread=new Thread(() -> handleClientDestress(client));
                        destressThread.start();
                    }
                }
                else{
                    destressThread=null;
                }
                if(line.startsWith("/")) processClientCommand(line, client);
                else{
                    broadcast(line,client.clientChannel,client, null);
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + client.clientSocket.getRemoteSocketAddress());
            try{
                disconnectClient(client);
            }catch (IOException ignore){}
            broadcast("// User disconnected from your channel: "+client.clientName,client.clientChannel,serverDummy, client);
        } finally {
            try {
                clients.remove(client);
                client.clientSocket.close();
            } catch (IOException ignored) {}
        }
    }
    void handleClientDestress(ClientObject client){
        while (client.messageStress>0){
            try{
                System.out.println(client.messageStress);
                Thread.sleep(1000);
                client.messageStress--;
            }
            catch (InterruptedException e){
                return;
            }
        }
    }




    public void processClientCommand(String command, ClientObject client){
        command=command.replaceAll("/","");
        String param[]=command.split(" ");
        //System.out.println(client.getDisplayName()+","+command);
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
                        case "pixel":
                            PixelChannel p;
                            try{
                                p = (PixelChannel) getChannelByID(client.clientChannel);
                            }
                            catch (ClassCastException ignore){
                                whisper("// The command you entered only works for Pixel-Channels",client,serverDummy);
                                return;
                            }
                            boolean answer;
                            if(param[2].equals("stream")){
                                answer=p.pixelStream(param[3]);
                            }
                            else if(param.length==5) answer=p.placePixel(Integer.parseInt(param[2]),Integer.parseInt(param[3]),param[4]);
                            else answer=p.placePixel(Integer.parseInt(param[2]),Integer.parseInt(param[3]),Integer.parseInt(param[4]),Integer.parseInt(param[5]),Integer.parseInt(param[6]));

                            if (answer){
                                whisper("// Pixel-changes sent.",client,serverDummy);
                                broadcast("§pixel"+p.getPrintedGrid(),client.clientChannel,serverDummy,null);
                            }
                            else whisper("// Failed to place pixel, invalid color?",client,serverDummy);
                            return;
                        case "cs3":
                            CounterStrikeChannel c;
                            try{
                                c = (CounterStrikeChannel) getChannelByID(client.clientChannel);
                            }
                            catch (ClassCastException ignore){
                                whisper("// The command you entered only works for CS3-Channels",client,serverDummy);
                                return;
                            }
                            if(param[2].equals("position")){
                                c.setPlayerPosition(Integer.parseInt(param[3]), Integer.parseInt(param[4]), Integer.parseInt(param[5]));
                            }
                            broadcast("§cs3"+c.getAllPrinted(),client.clientChannel,serverDummy,null);
                            return;
                        default: break;
                    }
                case "switch":
                    switchClientChannel(client, Integer.parseInt(param[1]));
                    return;
                case "msg":
                case "whisper":
                    String message = String.join(" ",
                            java.util.Arrays.copyOfRange(param, 2, param.length));
                    whisper(message.trim(),getClientByName(param[1]),client);
                    return;
                case "exit":
                case "disconnect":
                case "quit":
                    disconnectClient(client);
                    return;
            }
        }
        catch (Exception ignore){
            whisper("!! Invalid format.",client,serverDummy);
        }

        boolean isAdmin=false;
        boolean triggered=false;
        if (client.clientRole.roleID==admin_role_ID) isAdmin=true;
        try{
            switch (param[0]){
                case "kick":
                    if(!isAdmin){
                        triggered=true;
                        break;
                    }
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
                case "remove":
                    if(!isAdmin){
                        triggered=true;
                        break;
                    }
                    if(param[1].equals("id")){
                        String message2 = String.join(" ",
                                java.util.Arrays.copyOfRange(param, 3, param.length));
                        kickClientFromChannel(clients.get(Integer.parseInt(param[2])),message2);
                        return;
                    }
                    String message2 = String.join(" ",
                            java.util.Arrays.copyOfRange(param, 2, param.length));
                    kickClientFromChannel(getClientByName(param[1]),message2);
                    return;
                case "move":
                    if(!isAdmin){
                        triggered=true;
                        break;
                    }
                    if(param[1].equals("id")){
                        moveClientToChannel(client,clients.get(Integer.parseInt(param[2])),Integer.parseInt(param[3]));
                        return;
                    }
                    moveClientToChannel(client,getClientByName(param[1]),Integer.parseInt(param[2]));
                    return;
            }
            if(triggered) whisper("!! Insufficient permissions.",client,serverDummy);
        }
        catch (Exception ignore){
            whisper("!! Invalid format.",client,serverDummy);
        }
        whisper("!! Invalid command: "+command,client,serverDummy);
    }

    public void changeClientName(ClientObject client, String newName){
        if (null == getClientByName(newName)){
            client.clientName=newName;
            whisper("// Username edited: "+client.clientName,client,serverDummy);
        }
        else whisper("!! Username allready taken", client, serverDummy);
    }
    public void changeClientRole(ClientObject client, int roleID, String token){
        RoleObject targetRole=getRoleByID(roleID);
        if (targetRole==null){
            whisper("!! The server group you specified is not initialized",client,serverDummy);
            return;
        }
        if (targetRole.roleToken==null) {
            whisper("!! The server group you specified is a system role only.",client,serverDummy);
            return;
        }
        if (token.equals(targetRole.roleToken)){
            client.clientRole=targetRole;
            whisper("// Server group assigned: "+client.clientRole.roleName+" ("+client.clientRole.roleID+")",client,serverDummy);
        }
        else{
            whisper("!! The token you entered is invalid.",client,serverDummy);
        }
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
        broadcast("// User entered your channel: "+client.getDisplayName(), client.clientChannel,serverDummy,client);
        whisper("// Channel switched: "+getChannelByID(client.clientChannel).channelName+" ("+client.clientChannel+")",client,serverDummy);
        String[] welcomeMessages=channelObject.getWelcomeMessages();
        if(welcomeMessages!=null){
            for (String welcomeMessage : welcomeMessages) {
                if (welcomeMessage != null) {
                    System.out.println(welcomeMessage);
                    whisper(welcomeMessage, client, serverDummy);
                }
            }
        }
    }
    public void kickClientFromChannel(ClientObject client, String msg){
        int oldChannel=client.clientChannel;
        client.clientChannel=default_channel;
        broadcast("// User was kicked out of your channel: "+client.getDisplayName(),oldChannel,serverDummy,client);
        broadcast("// User was kicked to your channel: "+client.getDisplayName(), client.clientChannel,serverDummy,client);
        if (msg!=null) whisper("!! You were kicked from the channel: "+msg,client,serverDummy);
        else whisper("!! You were kicked from the channel.",client,serverDummy);
        String[] welcomeMessages=default_channel_welcome;
        if(welcomeMessages!=null){
            for (String welcomeMessage : welcomeMessages) {
                if (welcomeMessage != null) {
                    System.out.println(welcomeMessage);
                    whisper(welcomeMessage, client, serverDummy);
                }
            }
        }
    }
    public void moveClientToChannel(ClientObject client,ClientObject target, int newChannel){
        int oldChannel=target.clientChannel;
        ChannelObject channelObject;
        try{
            channelObject=channels.get(newChannel);
        }
        catch (IndexOutOfBoundsException e){
            whisper("!! Channel not initialized.", client, serverDummy);
            return;
        }
        target.clientChannel=newChannel;
        broadcast("// User disconnected from your channel: "+target.getDisplayName(),oldChannel,serverDummy,target);
        broadcast("// User entered your channel: "+target.getDisplayName(), client.clientChannel,serverDummy,target);
        whisper("// You were moved: "+getChannelByID(target.clientChannel).channelName+" ("+target.clientChannel+")",target,serverDummy);
        String[] welcomeMessages=channelObject.getWelcomeMessages();
        if(welcomeMessages!=null){
            for (String welcomeMessage : welcomeMessages) {
                if (welcomeMessage != null) {
                    System.out.println(welcomeMessage);
                    whisper(welcomeMessage, target, serverDummy);
                }
            }
        }
    }
    public void emptyChannel(int channelID,String msg){
        for(ClientObject c : clients){
            if(c.clientChannel==channelID) kickClientFromChannel(c,msg);
        }
    }

    public void kickClient(ClientObject target, String reason, ClientObject client) throws IOException {
        String targetName=target.getDisplayName();
        int targetID=clients.indexOf(target);
        int targetChannel=target.clientChannel;
        whisper("!! You have been kicked from the server: "+reason,target,serverDummy);
        disconnectClient(target);
        whisper("// Client kicked: "+targetName+" ("+targetID+")", client, serverDummy);
        broadcast("// User in your channel was kicked from the server.", targetChannel,serverDummy,null);
    }
    public void disconnectClient(ClientObject target) throws IOException{
        whisper("// Disconnected.",target,serverDummy);

        target.clientSocket.close();
        clients.remove(target);
        System.out.println("Client removed");
    }


    void broadcast(String msg, int channelID, ClientObject sender ,ClientObject clientToHideFrom) {
        if(!Objects.equals(sender.getDisplayName(), serverDummy.clientName)){
            if (channelID==default_channel || !getChannelByID(channelID).allowMessages){
                whisper("!! You cannot send messages in this channel, please enter \"/switch <ChannelID>\" to switch channel.",sender, serverDummy);
                return;
            }
            else{
                msg="[" + sender.getDisplayName() + "]: " +msg;
            }
        }
        //System.out.println("("+channelID+") "+msg);
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
    public int getIDbyChannel(ChannelObject channelObject){
        return channels.indexOf(channelObject);
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
}