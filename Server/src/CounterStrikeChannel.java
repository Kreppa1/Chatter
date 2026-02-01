import java.awt.*;
import java.sql.Array;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class CounterStrikeChannel extends ChannelObject{
    Server serverParent;
    public int X;
    public int Y;
    public List<PlayerObject> players = new ArrayList<PlayerObject>(); //All players
    List<PlayerObject> playerCopy = new ArrayList<>(); //Just the real players, no chaser
    PlayerObject chaser = null; //chaser
    Color[][] mapColors;
    int mapGridSize = 32;
    int PLAYER_SIZE = 20;


    public CounterStrikeChannel(Server serverParent,String channelName, boolean allowMessages, boolean allowAnonymous,String[] welcomeMessages,boolean allowSpam, int width, int height, String mapPath) {
        super(channelName, allowMessages, allowAnonymous, welcomeMessages, allowSpam);
        this.serverParent=serverParent;
        X=width;
        Y=height;

        try {
            loadMapFromFile(mapPath);
        }
        catch (IOException e) {
            System.out.println("Couldn't load map: " + e.getMessage());
        }
    }

    public String joinGame() {
        PlayerObject p;
        if(players.isEmpty()){
            p = new PlayerObject(findValidSpawnPosition(), Color.red, true);
        }
        else p = new PlayerObject(findValidSpawnPosition(), Color.blue, false);
        players.add(p);
        updatePlayerLists();

        String welcomeString = "§cs3 welcome you("+players.indexOf(p)+"):c("+X+","+Y+")";
        String mapString = ":m(s(" + mapGridSize + ")" + "v(";

        for(int y = 0; y < mapGridSize; y++) {
            for(int x = 0; x < mapGridSize; x++){
                Color color = mapColors[y][x];
                // Encode color as 6-digit hex (RRGGBB)
                String hex = String.format("%02X%02X%02X",
                        color.getRed(), color.getGreen(), color.getBlue());
                mapString += hex;
            }
        }
        mapString += "))";
        return welcomeString + mapString;
    }

    private Point findValidSpawnPosition() {
        int tileSize = X / mapGridSize;
        Random rand = new Random();

        // Chaser spawn
        if (chaser == null) {
            for (int i = 0; i < 1000; i++) {
                int tx = rand.nextInt(mapGridSize);
                int ty = rand.nextInt(mapGridSize);

                // Check if the tile is walkable (pure white)
                Color tileColor = mapColors[ty][tx];
                if (tileColor.getRed() == 255 &&
                        tileColor.getGreen() == 255 &&
                        tileColor.getBlue() == 255) {

                    int px = tx * tileSize + tileSize / 2 - PLAYER_SIZE / 2;
                    int py = ty * tileSize + tileSize / 2 - PLAYER_SIZE / 2;
                    return new Point(px, py);
                }
            }
        } else { // Normal spawn 200px away from Chaser
            for (int i = 0; i < 1000; i++) {
                int tx = rand.nextInt(mapGridSize);
                int ty = rand.nextInt(mapGridSize);

                // Check if the tile is walkable (pure white)
                Color tileColor = mapColors[ty][tx];
                if (tileColor.getRed() == 255 &&
                        tileColor.getGreen() == 255 &&
                        tileColor.getBlue() == 255) {

                    int px = tx * tileSize + tileSize / 2 - PLAYER_SIZE / 2;
                    int py = ty * tileSize + tileSize / 2 - PLAYER_SIZE / 2;

                    double distance = Math.sqrt(Math.pow(px - chaser.x, 2) + Math.pow(py - chaser.y, 2));

                    if (distance >= 200 &&
                            px >= PLAYER_SIZE && px <= X - PLAYER_SIZE * 2 &&
                            py >= PLAYER_SIZE && py <= Y - PLAYER_SIZE * 2) {
                        return new Point(px, py);
                    }
                }
            }

            // If nothing found at least 200px away
            for (int i = 0; i < 1000; i++) {
                int tx = rand.nextInt(mapGridSize);
                int ty = rand.nextInt(mapGridSize);

                // Check if the tile is walkable (pure white)
                Color tileColor = mapColors[ty][tx];
                if (tileColor.getRed() == 255 &&
                        tileColor.getGreen() == 255 &&
                        tileColor.getBlue() == 255) {

                    int px = tx * tileSize + tileSize / 2 - PLAYER_SIZE / 2;
                    int py = ty * tileSize + tileSize / 2 - PLAYER_SIZE / 2;

                    if (px >= PLAYER_SIZE && px <= X - PLAYER_SIZE * 2 &&
                            py >= PLAYER_SIZE && py <= Y - PLAYER_SIZE * 2) {
                        return new Point(px, py);
                    }
                }
            }
        }

        // Fallback - search all tiles systematically
        for (int ty = 0; ty < mapGridSize; ty++) {
            for (int tx = 0; tx < mapGridSize; tx++) {
                // Check if the tile is walkable (pure white)
                Color tileColor = mapColors[ty][tx];
                if (tileColor.getRed() == 255 &&
                        tileColor.getGreen() == 255 &&
                        tileColor.getBlue() == 255) {

                    int px = tx * tileSize + tileSize / 2 - PLAYER_SIZE / 2;
                    int py = ty * tileSize + tileSize / 2 - PLAYER_SIZE / 2;
                    return new Point(px, py);
                }
            }
        }

        // Ultimate fallback to center (even if it's not walkable)
        return new Point(X/2 - PLAYER_SIZE/2, Y/2 - PLAYER_SIZE/2);
    }

    @Override
    public String[] getWelcomeMessages() {
        if (welcomeMessages == null) welcomeMessages = new String[0];
        String[] result = new String[welcomeMessages.length + 2];
        System.arraycopy(welcomeMessages, 0, result, 0, welcomeMessages.length);

        result[result.length - 2] = joinGame();
        result[result.length - 1] = "§cs3" + getAllPrinted();

        //System.out.println(result);
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
        PlayerObject player = players.get(ID);
        int oldX = player.x;
        int oldY = player.y;

        player.set(x, y);

        if(isSolid(x, y)) {
            resolveCollision(player);

            // If position was corrected, send update to all clients
            if (player.x != oldX || player.y != oldY) {
                System.out.println("Position corrected for player " + ID +
                        ": from (" + oldX + "," + oldY +
                        ") to (" + player.x + "," + player.y + ")");
            }
        }

        checkWin();

        serverParent.broadcast("§cs3" + getAllPrinted(), serverParent.getIDbyChannel(this), serverParent.serverDummy, null);
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
        //System.out.println("Checking win");
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
    public void loadMapFromFile(String path) throws IOException {
        BufferedImage img = ImageIO.read(new File(path));
        Color[][] tempMap = new Color[mapGridSize][mapGridSize];

        for(int y = 0; y < mapGridSize; y++){
            for(int x = 0; x < mapGridSize; x++){
                int rgb = img.getRGB(x, y);
                Color color = new Color(rgb);
                tempMap[y][x] = color;
            }
        }
        mapColors = tempMap;
    }
    boolean isSolid(int px, int py) {
        // Pure white (255,255,255) is walkable, everything else is solid
        int playerRadius = 10;
        int playerCenterX = px + playerRadius;
        int playerCenterY = py + playerRadius;
        int tileSize = X / mapGridSize;

        int minTileX = Math.max(0, (playerCenterX - playerRadius) / tileSize);
        int maxTileX = Math.min(mapGridSize - 1, (playerCenterX + playerRadius) / tileSize);
        int minTileY = Math.max(0, (playerCenterY - playerRadius) / tileSize);
        int maxTileY = Math.min(mapGridSize - 1, (playerCenterY + playerRadius) / tileSize);

        for (int tileY = minTileY; tileY <= maxTileY; tileY++) {
            for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
                Color tileColor = mapColors[tileY][tileX];
                // Check if NOT pure white (walkable)
                if (!(tileColor.getRed() == 255 &&
                        tileColor.getGreen() == 255 &&
                        tileColor.getBlue() == 255)) {

                    int tileLeft = tileX * tileSize;
                    int tileRight = tileLeft + tileSize;
                    int tileTop = tileY * tileSize;
                    int tileBottom = tileTop + tileSize;

                    int closestX = clamp(playerCenterX, tileLeft, tileRight);
                    int closestY = clamp(playerCenterY, tileTop, tileBottom);

                    int dx = playerCenterX - closestX;
                    int dy = playerCenterY - closestY;
                    int distanceSquared = dx * dx + dy * dy;

                    if (distanceSquared < playerRadius * playerRadius) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    void resolveCollision(PlayerObject p) {
        int playerRadius = 10;
        int playerCenterX = p.x + playerRadius;
        int playerCenterY = p.y + playerRadius;
        int tileSize = X / mapGridSize;

        for (int tileY = 0; tileY < mapGridSize; tileY++) {
            for (int tileX = 0; tileX < mapGridSize; tileX++) {
                Color tileColor = mapColors[tileY][tileX];
                // Check if NOT pure white (solid)
                if (!(tileColor.getRed() == 255 &&
                        tileColor.getGreen() == 255 &&
                        tileColor.getBlue() == 255)) {

                    int tileLeft = tileX * tileSize;
                    int tileRight = tileLeft + tileSize;
                    int tileTop = tileY * tileSize;
                    int tileBottom = tileTop + tileSize;

                    int closestX = clamp(playerCenterX, tileLeft, tileRight);
                    int closestY = clamp(playerCenterY, tileTop, tileBottom);

                    int dx = playerCenterX - closestX;
                    int dy = playerCenterY - closestY;
                    int distanceSquared = dx * dx + dy * dy;

                    if (distanceSquared < playerRadius * playerRadius) {
                        // Collision detected! Push player out
                        double distance = Math.sqrt(distanceSquared);
                        if (distance == 0) {
                            // Special case: player center is inside tile
                            // Push out in a random direction
                            p.x = tileX * tileSize - playerRadius;
                            p.y = tileY * tileSize - playerRadius;
                        } else {
                            // Normalize direction and push out
                            double pushX = dx / distance;
                            double pushY = dy / distance;

                            p.x = (int)(closestX + pushX * playerRadius - playerRadius);
                            p.y = (int)(closestY + pushY * playerRadius - playerRadius);
                        }
                        return;
                    }
                }
            }
        }
    }

    public class PlayerObject {
        public int x;
        public int y;
        public boolean chaser=false;
        public Color c;

        public PlayerObject(Point position, Color c, boolean chaser) {
            this.x = position.x;
            this.y = position.y;
            this.c = c;
            this.chaser = chaser;
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


