import java.awt.Color;
import java.awt.Graphics;
import java.math.BigDecimal;

public class ArenaCell {
    public enum Status {
        UNEXPLORED, OBSTACLE, FREE, PHANTOM
    }

    public static final int CELL_WIDTH = 30;
    public static final int CELL_HEIGHT = 30;

    public static final double PHANTOM_MIN = 0.4;
    public static final double PHANTOM_MAX = 0.6;
    int x = 0, y = 0, visitedCount = 0;
    private Status status = Status.UNEXPLORED;
    private Status prevStatus = null;

    static final int SCORE_WINDOW = 7;
    MovingAverage obstacleScore = new MovingAverage(SCORE_WINDOW);

    public ArenaCell(int x, int y) {
        this.x = x;
        this.y = y;
    }

    private Color getColor() {
        // TODO: Improve getColor (Dynamic Status)
        switch (status) {
            case UNEXPLORED:
                return Color.ORANGE;
            case FREE:
                return Color.GREEN;
            case OBSTACLE:
                return Color.BLACK;
            case PHANTOM:
                return Color.LIGHT_GRAY;
        }
        return Color.WHITE;
    }

    public void setStatus(Status nStatus) {
        prevStatus = Status.valueOf(status.toString()) ;
        status = nStatus;
    }
    public boolean getHasChanged(){
    
        if (prevStatus == null || prevStatus == Status.UNEXPLORED) return false; //If changed from Unexplored to anything, count as no change
        return (prevStatus != status);
    }
    public Status getStatus() {
        return status;
    }

    public static int ToStartDrawX(int x) {
        return (x + (x * ArenaCell.CELL_WIDTH) + (ArenaPanel.OFFSET_WIDTH));
    }

    public static int ToStartDrawY(int y) {
        int draw_y = (ArenaPanel.NO_CELLS_HEIGHT - 1 - y);
        return (draw_y + (draw_y * ArenaCell.CELL_HEIGHT) + ArenaPanel.OFFSET_HEIGHT);
    }

    public void draw(Graphics g) {
        int startDraw_X = ToStartDrawX(x);
        int startDraw_Y = ToStartDrawY(y);
        Color lastColor = g.getColor();

        g.setColor(getColor());
        g.fillRect(startDraw_X, startDraw_Y, ArenaCell.CELL_WIDTH, ArenaCell.CELL_HEIGHT);
        g.setColor(Color.BLUE);
        g.drawString(x + "," + y, startDraw_X, startDraw_Y + 10);
        g.setColor(Color.DARK_GRAY);
        g.drawString("" + obstacleScore.getAverage(), startDraw_X + 10, startDraw_Y + 20);
        g.setColor(lastColor);
    }

    public static boolean isValidCoords(int x, int y) {
        if (x < 0 || x >= ArenaPanel.NO_CELLS_WIDTH || y < 0 || y >= ArenaPanel.NO_CELLS_HEIGHT)
            return false;
        return true;
    }

    public static boolean isValidCoords(Coordinate coords) {
        if (coords == null)
            return false;
        return isValidCoords(coords.getX(), coords.getY());
    }

    public boolean isObstacle() {
        return getStatus() == Status.OBSTACLE;
    }

    public void markObstacle() {
        if (IsInGoalZone(x, y) || IsInStartZone(x, y) || IsInWaypoint(x, y)) {
            markFree();
            return;
        }
        obstacleScore.add(new BigDecimal(1.0));
        updateStatus();
    }

    public void markFree() {
        obstacleScore.add(new BigDecimal(0.0));
        updateStatus();
    }

    public void preMark() {

    }

    private void updateStatus() {
        double obstacleScore = this.obstacleScore.getAverage().doubleValue();
        if (obstacleScore >= PHANTOM_MIN && obstacleScore <= PHANTOM_MAX)
            setStatus(Status.PHANTOM);
        else if (obstacleScore > 0.5)
            setStatus(Status.OBSTACLE);
        else if (obstacleScore < 0.5)
            setStatus(Status.FREE);
    }

    public void visit() {
        visitedCount++;
        // System.out.println("Visited " + x + ", " + y + " : " + visitedCount);
    }

    public boolean overVisited() {
        return (visitedCount > 2 && ((visitedCount - 2) % 3 != 0));
    }

    public static boolean IsInStartZone(int x, int y) {
        return (x <= 2 && y <= 2);
    }

    public static boolean IsInGoalZone(int x, int y) {
        return (x >= 12 && y >= 17);
    }

    public static boolean IsInWaypoint(int x, int y){
        Coordinate waypoint = ArenaViewer.GetWaypoint();
        if (waypoint == null){ return false; }
        return (x >= (waypoint.getX() -1) && x <= (waypoint.getX() + 1) && y >= (waypoint.getY() - 1) && y <= (waypoint.getY()+1));
    }
}
