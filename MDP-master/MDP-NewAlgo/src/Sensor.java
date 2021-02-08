import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

public class Sensor {
    int range = 3; // Default Short Range
    int x = 0, y = 0;
    Robot.Direction direction = Robot.Direction.NORTH;
    String name;

    public Sensor(int x, int y, int range, Robot.Direction direction) {
        this.x = x;
        this.y = y;
        this.range = range;
        this.direction = direction;
        this.name = null;
    }

    public Sensor(int x, int y, int range, Robot.Direction direction, String name) {
        this.x = x;
        this.y = y;
        this.range = range;
        this.direction = direction;
        this.name = name;
    }

    public void draw(Graphics g) {
        Color lastColor = g.getColor();
        g.setColor(Color.CYAN);
        int draw_x = ArenaCell.ToStartDrawX(x) + 5, draw_y = ArenaCell.ToStartDrawY(y) + 5;
        g.fillOval(draw_x, draw_y, 20, 20);
        g.setColor(lastColor);

    }

    public byte getReading() {
        ArenaCell[][] map = ArenaViewer.GetLoadedMap();
        ArenaCell tempCell;
        for (int i = 0; i <= range; i++) {
            tempCell = getCell(map, i);
            if (tempCell == null || tempCell.getStatus() == ArenaCell.Status.OBSTACLE) {
                return (byte) ((i) * 10);
            }
        }
        return -1;
    }

    public void update(int x, int y, Robot.Direction direction) {
        this.x = x;
        this.y = y;
        this.direction = direction;
    }

    public void updateMap(byte reading, ArenaCell[][] map) {
        // return;

        int mreading = Byte.toUnsignedInt(reading) / 10;
        int min = mreading > range ? range : mreading;
        for (int i = 0; i < min; i++) {
            ArenaCell cell = getCell(map, i);
            if (cell != null) {
                cell.markFree();
            }
        }
        ArenaCell cell2 = getCell(map, min);
        if (cell2 != null) {
            if (min < range || mreading == range) {
                cell2.markObstacle();
                // for (int i = min+1; i < range; i++){
                //     System.out.println("In Extra!");
                //     ArenaCell cell3 = getCell(map, i);
                //     if (cell3 != null){
                //         cell3.markFree();
                //     }
                // }
            } else if (mreading > range) {
                cell2.markFree();
            }
        }
    }

    private ArenaCell getCell(ArenaCell[][] map, int units) {
        switch (direction) {
            case NORTH:
                if (ArenaCell.isValidCoords(x, y + (1 + units)))
                    return map[y + 1 + units][x];
                break;
            case EAST:
                if (ArenaCell.isValidCoords(x + (1 + units), y))
                    return map[y][x + (1 + units)];
                break;
            case SOUTH:
                if (ArenaCell.isValidCoords(x, y - (1 + units)))
                    return map[y - (1 + units)][x];
                break;
            case WEST:
                if (ArenaCell.isValidCoords(x - (1 + units), y))
                    return map[y][x - (1 + units)];
                break;
        }
        return null;
    }

    public boolean hasPhantom(ArenaCell[][] map) {
        for (int i = 0; i <= range; i++) {
            ArenaCell cell = getCell(map, i);
            if (cell == null || cell.getStatus() == ArenaCell.Status.OBSTACLE)
                return false;
            if (cell.getStatus() == ArenaCell.Status.PHANTOM)
                return true;
        }
        return false;
    }
}