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