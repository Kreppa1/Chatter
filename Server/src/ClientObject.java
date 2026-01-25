import java.net.Socket;

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
    public int messageStress;

    public String getDisplayName(){
        if (clientName!=null) return clientName;
        else return "?";
    }
}