import java.awt.*;
import java.util.Map;

public class PixelChannel extends ChannelObject{
    Color[][] pixelGrid;
    private final Map<String, Color> COLORS = Map.ofEntries(
            Map.entry("black", Color.BLACK),
            Map.entry("blue", Color.BLUE),
            Map.entry("cyan", Color.CYAN),
            Map.entry("green", Color.GREEN),
            Map.entry("red", Color.RED),
            Map.entry("white", Color.WHITE),
            Map.entry("yellow", Color.YELLOW),
            Map.entry("gray", Color.GRAY),
            Map.entry("dark_gray", Color.DARK_GRAY),
            Map.entry("light_gray", Color.LIGHT_GRAY)
    );

    public PixelChannel(String channelName, boolean allowMessages, boolean allowAnonymous, int gridWidth, int gridHeight) {
        super(channelName, allowMessages, allowAnonymous);
        pixelGrid = new Color[gridWidth][gridHeight];
    }
    public void placePixel(int x, int y, Color color){
        pixelGrid[x][y]=color;
    }

    public boolean placePixel(int x, int y, String colorString){
        Color c = COLORS.get(colorString.toLowerCase());
        if(c==null){
            return false;
        }
        pixelGrid[x][y]=c;
        return true;
    }

    public String getPrintedGrid(){
        String print = "";
        for(int i=0;i<pixelGrid.length;i++){
            for(int j=0;i<pixelGrid[0].length;j++){
                print=print+pixelGrid[j][i].toString()+":";
            }
            print=print+"~";
            System.out.println(print);
        }
        return print;
    }
}
