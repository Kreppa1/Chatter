import java.awt.*;
import java.sql.Array;
import java.util.ArrayList;
import java.util.List;

public class CounterStrikeChannel extends ChannelObject{
    public int X;
    public int Y;
    public List<PlayerObject> players = new ArrayList<PlayerObject>();

    public CounterStrikeChannel( String channelName, boolean allowMessages, boolean allowAnonymous,String[] welcomeMessages, int gridWidth, int gridHeight) {
        super(channelName, allowMessages, allowAnonymous, welcomeMessages);
    }

    public void joinGame(ClientObject client){
        players.add(new PlayerObject(0,0, Color.red));

    }

    public String[] getWelcomeMessages(){
        if (welcomeMessages == null) welcomeMessages = new String[0];

        String[] result = new String[welcomeMessages.length + 1];
        System.arraycopy(welcomeMessages, 0, result, 0, welcomeMessages.length);

        result[result.length - 1] = "§cs3" + getAllPrinted();
        return result;
    }

    public String getAllPrinted(){
        String r="";
        r=r+"c("+X+","+Y+"):";
        for(PlayerObject p : players){
            r=r+"p("+players.indexOf(p)+","+p.x+","+p.y+","+p.c.getRed()+","+p.c.getGreen()+","+p.c.getBlue()+")";
        }
        return r;
    }

    public class PlayerObject {
        public int x;
        public int y;
        public Color c;

        public PlayerObject(int x,int y, Color c) {
            this.x = x;
            this.y = y;
        }

        public void move(int x, int y) {
            this.x = this.x + x;
            this.y = this.y + y;
        }
    }
}


