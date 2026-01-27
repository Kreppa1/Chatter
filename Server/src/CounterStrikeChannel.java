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


