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
    public PixelChannel(String channelName, boolean allowMessages, boolean allowAnonymous,String[] welcomeMessages, int gridWidth, int gridHeight) {
        super(channelName, allowMessages, allowAnonymous, welcomeMessages);
        pixelGrid = new Color[gridWidth][gridHeight];
        fillGrid(Color.BLACK);
        getPrintedGrid();
    }
    public PixelChannel(String channelName, boolean allowMessages, boolean allowAnonymous,String[] welcomeMessages,boolean allowSpam, int gridWidth, int gridHeight) {
        super(channelName, allowMessages, allowAnonymous, welcomeMessages, allowSpam);
        pixelGrid = new Color[gridWidth][gridHeight];
        fillGrid(Color.BLACK);
        getPrintedGrid();
    }


    public void fillGrid(Color color){
        for(int i=0;i<pixelGrid[0].length;i++){
            for(int j=0;j<pixelGrid.length;j++){
                pixelGrid[j][i]=color;
            }
        }
    }
    public void placePixel(int x, int y, Color color){
        pixelGrid[x][y]=color;
    }

    public boolean placePixel(int x, int y, int r, int g, int b){
        Color c = new Color(r,g,b);
        if(c==null){
            return false;
        }
        pixelGrid[x][y]=c;
        return true;
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
        for(int i=0;i<pixelGrid[0].length;i++){
            for(int j=0;j<pixelGrid.length;j++){
                print=print+colorToString(pixelGrid[j][i])+":";

            }
            print=print+"~";
        }
        return print;
    }
    public String colorToString(Color c){
        if(c==null){
            System.err.println("Critical Error: Color grid failed to initialize, check pixel channels!");
            System.exit(0);
        }
        return "rgb(" + c.getRed() + ", " + c.getGreen() + ", " + c.getBlue() + ")";
    }

    @Override
    public String[] getWelcomeMessages(ClientObject clientObject){
        if (welcomeMessages == null) welcomeMessages = new String[0];

        String[] result = new String[welcomeMessages.length + 1];
        System.arraycopy(welcomeMessages, 0, result, 0, welcomeMessages.length);

        result[result.length - 1] = "§pixel" + getPrintedGrid();
        return result;
    }




    //used speficicly by the client to send multible pixel changes at once
    public boolean pixelStream(String input) {
        if (input == null || input.isEmpty()) return false;

        try {
            // Split each pixel entry
            String[] pixels = input.split(":");

            for (String p : pixels) {
                if (p.isEmpty()) continue;

                // Remove parentheses
                if (!p.startsWith("(") || !p.endsWith(")")) return false;
                p = p.substring(1, p.length() - 1);

                String[] values = p.split(",");
                if (values.length != 5) return false;

                int x = Integer.parseInt(values[0].trim());
                int y = Integer.parseInt(values[1].trim());
                int r = Integer.parseInt(values[2].trim());
                int g = Integer.parseInt(values[3].trim());
                int b = Integer.parseInt(values[4].trim());

                // Bounds check for canvas
                if (y < 0 || y >= pixelGrid[0].length) continue;
                if (x < 0 || x >= pixelGrid.length) continue;

                // Clamp color values (optional but smart)
                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));

                pixelGrid[x][y] = new Color(r, g, b);
            }

            return true;

        } catch (Exception e) {
            return false;
        }
    }
}
