import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

public class PaintDisplay {
    /* 
    PaintDisplay Class that keeps an array list of GridCells
    */
    private List<GridCell> cellsList = new ArrayList<>();

    public PaintDisplay() {
        this.cellsList = new ArrayList<GridCell>();
    }

    public void addCell(GridCell cell){
        this.cellsList.add(cell);
    }

    public void removeCell(GridCell cell){
        this.cellsList.remove(cell);
    }
    public void addRectangle(int x, int y, int w, int h, Color lineColor, Color fillColor){
        GridCell cell = new GridCell(x,y,w,h,lineColor,fillColor,"rectangle");
        this.cellsList.add(cell);
    }

    public void addCircle(int x, int y, int w, int h, Color lineColor, Color fillColor){
        GridCell cell = new GridCell(x,y,w,h,lineColor,fillColor,"circle");
        this.cellsList.add(cell);
    }

    //standard map elements
    
    public GridCell getCellByIndex(Integer index){
        return this.cellsList.get(index);
    }

    public void clearDisplay(){
        this.cellsList.clear();
    }

	public void draw(Graphics g ){
        
        for (GridCell cell : cellsList) {
            switch(cell.getShape()){
                case "rectangle":
                    g.setColor(cell.getFillColor());//fill
                    g.fillRect(cell.getX()*GridConstants.tileWidth,cell.getY()*GridConstants.tileWidth,GridConstants.tileWidth,GridConstants.tileWidth);
                    g.setColor(cell.getLineColor());//outline
                    g.drawRect(cell.getX()*GridConstants.tileWidth,cell.getY()*GridConstants.tileWidth,GridConstants.tileWidth,GridConstants.tileWidth);
                    break;
                case "circle":
                    g.setColor(Color.white);
                    g.fillOval(cell.getX()*GridConstants.tileWidth,cell.getY()*GridConstants.tileWidth,GridConstants.tileWidth,GridConstants.tileWidth);
                    break;
                case "robot":
                    g.setColor(GridConstants.robotColor);
                    g.fillOval((cell.getX()-1)*GridConstants.tileWidth,(cell.getY()-1)*GridConstants.tileWidth,3*GridConstants.tileWidth,3*GridConstants.tileWidth);
                    break;
                case "startEndArea":
                    g.setColor(GridConstants.startAreaColor);//fill
                    g.fillRect((cell.getX()-1)*GridConstants.tileWidth,(cell.getY()-1)*GridConstants.tileWidth,GridConstants.tileWidth*3,GridConstants.tileWidth*3);
                    g.setColor(GridConstants.outlineColor);//outline
                    g.drawRect((cell.getX()-1)*GridConstants.tileWidth,(cell.getY()-1)*GridConstants.tileWidth,GridConstants.tileWidth*3,GridConstants.tileWidth*3);
                    break;
                default:
                    System.out.println("No match found! (switch)");
            }
            
        }
        
	}

}

