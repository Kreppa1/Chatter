import java.awt.*;
import java.sql.Array;
import java.util.ArrayList;
import java.util.List;

import static jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyles.copy;

public class CounterStrikeChannel extends ChannelObject{
    Server serverParent;
    public int X;
    public int Y;
    public List<PlayerObject> players = new ArrayList<PlayerObject>(); //All players
    List<PlayerObject> playerCopy = new ArrayList<>(); //Just the real players, no chaser
    PlayerObject chaser = null; //chaser


    public CounterStrikeChannel(Server serverParent,String channelName, boolean allowMessages, boolean allowAnonymous,String[] welcomeMessages,boolean allowSpam, int gridWidth, int gridHeight) {
        super(channelName, allowMessages, allowAnonymous, welcomeMessages, allowSpam);
        this.serverParent=serverParent;
        X=gridWidth;
        Y=gridHeight;
    }

    public String joinGame(){
        PlayerObject p;
        if(players.isEmpty()){
            p = new PlayerObject(X-11,Y-11,Color.red,true);
        }
        else p = new PlayerObject(1,1,Color.blue,false);
        players.add(p);
        updatePlayerLists();
        return "§cs3 welcome you("+players.indexOf(p)+"):c("+X+","+Y+")";
    }

    @Override
    public String[] getWelcomeMessages(){
        if (welcomeMessages == null) welcomeMessages = new String[0];
        String[] result = new String[welcomeMessages.length + 2];
        System.arraycopy(welcomeMessages, 0, result, 0, welcomeMessages.length);

        result[result.length - 2] = joinGame();
        result[result.length - 1] = "§cs3" + getAllPrinted();

        System.out.println(result);
        return result;
    }

    public String getAllPrinted(){
        String r="";
        for(PlayerObject p : players){
            r=r+"p("+players.indexOf(p)+","+p.x+","+p.y+","+p.c.getRed()+","+p.c.getGreen()+","+p.c.getBlue()+"):";
        }
        return r;
    }

    public void setPlayerPosition(int ID, int x, int y){
        players.get(ID).set(x,y);
        checkWin();
    }


    public void updatePlayerLists(){
        for (PlayerObject i : players) {
            if (!i.chaser) {   // keep only if boolean is false
                playerCopy.add(i);
            }
            else chaser=i;
        }
    }

    public void checkWin(){
        System.out.println("Checking win");
        for(PlayerObject p : playerCopy){
            if(isTouching(p.x,p.y,chaser.x,chaser.y)) {
                serverParent.broadcast("§cs3 end",serverParent.getIDbyChannel(this),serverParent.serverDummy,null);
                serverParent.broadcast("// Chaser wins!",serverParent.getIDbyChannel(this),serverParent.serverDummy,null);
                serverParent.emptyChannel(serverParent.getIDbyChannel(this),"Game Over!");
                reset();
            }
        }
    }

    public static boolean isTouching(int x1, int y1, int x2, int y2) {
        int halfSize = 10; // 10 / 2

        return Math.abs(x1 - x2) <= halfSize * 2 &&
                Math.abs(y1 - y2) <= halfSize * 2;
    }

    public void reset(){
        playerCopy.clear();
        chaser=null;
        players.clear();
    }

    public class PlayerObject {
        public int x;
        public int y;
        public boolean chaser=false;
        public Color c;

        public PlayerObject(int x,int y, Color c, boolean chaser) {
            this.x = x;
            this.y = y;
            this.c = c;
            this.chaser=chaser;
        }
        public void move(int x, int y) {
            this.x = this.x + x;
            this.y = this.y + y;
        }
        public void set(int x, int y){
            this.x=x;
            this.y=y;
        }
    }
}


