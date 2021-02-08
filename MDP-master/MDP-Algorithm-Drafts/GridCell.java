import java.awt.*;
import java.util.HashMap;
public class GridCell{
    //fields to describe map
    private Integer x;
    private Integer y;
    private boolean explored = false;
    private boolean occupied = false;   
    private boolean isVirtualWall = false; 
    //fields to paint map
    private Integer w = GridConstants.tileWidth;
    private Integer h = GridConstants.tileWidth;
    private String shape = "rectangle";
    private Color lineColor;
    private Color fillColor;
    
    public GridCell(){

    }
    public GridCell(int x, int y){ //minimal constructor
        this.x = x;
        this.y = y;
    }
    public GridCell(int x, int y, String shape){
        this.x = x;
        this.y = y;
        this.shape = shape;
    }
    public GridCell(int x, int y, int w, int h,Color lineColor, Color fillColor, String shape){
        this.w = w;
        this.h = h;
        this.x = x;
        this.y = y;
        this.lineColor = lineColor;
        this.fillColor = fillColor;
        this.shape = shape;
    }

    public Integer getX(){
        return this.x;
    }

    public Integer getY(){
        return this.y;
    }

    public void setX(Integer x){
        this.x = x;
    }

    public void setY(Integer y){
        this.y = y;
    }

    public void setW(Integer w){
        this.w = w;
    }

    public void setH(Integer h){
        this.h = h;
    }
    
    public void setLineColor(Color lineColor){
        this.lineColor = lineColor;
    }
    public void setFillColor(Color fillColor){
        this.fillColor = fillColor;
    }

    public void setShape(String shape){
        this.shape = shape;
    }

    public boolean isOccupied(){
        return this.occupied;
    }

    public boolean isExplored(){
        return this.explored;
    }

    public void setOccupied(){
        if(this.explored){
            setLineColor(GridConstants.outlineColor);
            setFillColor(GridConstants.occupiedColor);
            this.occupied = true;
        }
    }

    public void setUnoccupied(){
        if(this.explored){
            setLineColor(GridConstants.outlineColor);
            setFillColor(GridConstants.unoccupiedColor);
            this.occupied = false;
        }
    }

    public void setExplored(){
        this.explored = true;
    }

    public void setUnexplored(){
        setLineColor(GridConstants.outlineColor);
        setFillColor(GridConstants.unexploredColor);
        this.explored = false;
    }

    public int getW(){
        return this.w;
    }

    public int getH(){
        return this.h;
    }
    public Color getLineColor(){
        return this.lineColor;
    }
    public Color getFillColor(){
        return this.fillColor;
    }

    public String getShape(){
        return this.shape;
    }


    public static void main(String[] args) {
        HashMap<String,HashMap> moveDict = new HashMap<String,HashMap>();
        HashMap<String,String> southDict = new HashMap<String,String>();
        HashMap<String,String> eastDict = new HashMap<String,String>();
        HashMap<String,String> westDict = new HashMap<String,String>();
        moveDict.put("south",southDict);
        moveDict.put("east",eastDict);
        moveDict.put("west",westDict);

        String givenMove = "for";
        System.out.println("givenMove: "+givenMove);
        System.out.println("north-facing robot's move: "+givenMove);
        southDict.put("for","back");
        southDict.put("back","for");
        southDict.put("right","left");
        southDict.put("left","right");
        System.out.println("south-facing robot's move: "+southDict.get(givenMove));
        eastDict.put("for","right");
        eastDict.put("back","left");
        eastDict.put("left","for");
        eastDict.put("right","back");
        System.out.println("east-facing robot's move: "+eastDict.get(givenMove));
        westDict.put("for","left");
        westDict.put("back", "right");
        westDict.put("left","back");
        westDict.put("right","for");
        System.out.println("west-facing robot's move: "+westDict.get(givenMove));

        //given robot's coord, given robot's orientation, determine robotDirection coord.
        //n:y+1, s:y-1, e:x+1, w:x-1
        String robotOrientation = "south";
        Integer robotX = 2;
        Integer robotY = 19;
        
        System.out.println("given robot coords: "+robotX+","+robotY);
        switch (robotOrientation){
            case "north":
                robotY += 1;
                break;
            case "south":
                robotY -= 1;
                break;
            case "east":
                robotX += 1;
            case "west":
                robotX -= 1;
        }

        System.out.println("robot's head coords: "+robotX+","+robotY);
    }
}