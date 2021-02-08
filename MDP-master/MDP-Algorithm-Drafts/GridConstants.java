import java.awt.*;
public final class GridConstants{
    //default map cell colors
    public static final Color outlineColor = Color.WHITE;
    public static final Color unexploredColor = Color.GREEN;
    public static final Color occupiedColor = Color.BLACK;
    public static final Color unoccupiedColor = Color.getHSBColor((float)0.6, (float)0.5,(float)0.9);
    public static final Color startAreaColor = Color.LIGHT_GRAY;
    public static final Color endAreaColor = Color.DARK_GRAY;
    //default robot colors
    public static final Color robotColor = Color.RED;
    public static final Color robotFill = Color.RED;
    public static final Color robotLine = Color.BLACK;
    public static final Color robotDirectionFill = Color.WHITE;
    //default map measurements
    public static final Integer tileWidth = 25;
    public static final Integer mapPadding = 10;

}