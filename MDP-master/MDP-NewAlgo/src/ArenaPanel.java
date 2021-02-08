import javax.swing.JPanel;
import java.awt.Graphics;

public class ArenaPanel extends JPanel {
    public final static int NO_CELLS_WIDTH = 15;
    public final static int NO_CELLS_HEIGHT = 20;
    final static int OFFSET_HEIGHT = 15;
    final static int OFFSET_WIDTH = 15;
    private static final long serialVersionUID = -5387605596372822238L;


    private ArenaCell[][] cells;// = new ArenaCell[20][15];
    private Robot robot = new Robot(false);

    public ArenaPanel(ArenaCell[][] cells) {
        super();
        this.cells = cells;
    }

    public ArenaCell[][] getMap(){
        return this.cells;
    }
    public Robot getRobot(){
        return this.robot;
    }
    public void setRobot(Robot newBot){
        if (robot != null){
            robot.delete();
            robot = null;
        }
        robot = newBot;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // draw the rectangle here
        for (int y = 0; y < cells.length; y++) {
            for (int x = 0; x < cells[y].length; x++) {
                cells[y][x].draw(g);
            }
        }
        robot.draw(g);
    }
}
