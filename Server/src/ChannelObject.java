public class ChannelObject{
    boolean allowMessages=true;
    boolean allowAnonymous=false;
    boolean allowSpam=false;
    String channelName;
    String[] welcomeMessages;


    public ChannelObject(String channelName, boolean allowMessages, boolean allowAnonymous, String[] welcomeMessages){
        this.allowMessages=allowMessages;
        this.channelName=channelName;
        this.allowAnonymous=allowAnonymous;
        this.welcomeMessages = welcomeMessages;
    }

    public ChannelObject(String channelName, boolean allowMessages, boolean allowAnonymous, String[] welcomeMessages, boolean allowSpam){
        this.allowMessages=allowMessages;
        this.channelName=channelName;
        this.allowAnonymous=allowAnonymous;
        this.welcomeMessages = welcomeMessages;
        this.allowSpam=allowSpam;
    }
    public String checkClient(ClientObject client){ //if client trying to join doesnt meet the requirements, the returning string will be the error message;
        String message="!! The channel you try to join requires the client to:";
        if (!allowAnonymous && client.clientName==null) message=message+" have a name";
        else return "";
        return message+".";
    }
    public String[] getWelcomeMessages(ClientObject clientObject){
        return welcomeMessages;
    }
}