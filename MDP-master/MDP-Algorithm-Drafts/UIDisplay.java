import java.awt.*;
import javax.swing.*;
import java.util.ArrayList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class UIDisplay{
    final static boolean shouldFill = true;
    final static boolean shouldWeightX = true;
    public GridMap currGridMap = new GridMap();
    public String loadHexString = "";
    public PaintDisplay paintList = new PaintDisplay();
    public PaintPanel currMapPanel = new PaintPanel(paintList);
    private ArrayList<String> moveList = new ArrayList<>(); //movements to animate
    public int timerCount;
    public String robotOrientation = "north";
    public GridCell robotElement = new GridCell(2,19,"robot");
    public GridCell robotHead = new GridCell(0,0,"circle");



    public UIDisplay(){
        
    }

    public UIDisplay(GridMap currGridMap){
        this.currGridMap = currGridMap;
        JFrame f = new JFrame(); 
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel,BoxLayout.Y_AXIS));
        mainPanel.setMinimumSize(mainPanel.getPreferredSize());
        f.getContentPane().add(mainPanel);
        f.setSize(600,610);
        f.setMinimumSize(f.getPreferredSize());
        f.setVisible(true);

        setPaintList(currGridMap);
        paintList.addCell(robotElement);
        paintList.addCell(robotHead);
        updateRobotOrientation("north");
        setCurrMapPanel();
        mainPanel.add(currMapPanel);
        //timer concept for map animation, reads off an array of moves up,up,left,left,etc.
        final Timer timerMainPanel = new Timer(120,null);
        timerMainPanel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("mainPanel revalidating and repainting...");
                for(String movement:moveList){
                    move(movement);
                    mainPanel.revalidate();
                    mainPanel.repaint();
                }
                timerMainPanel.stop();
                
            }
        });
        //timerMainPanel.start() 
        //to start animation of movelist.
        


    }

    public void setRobotOrientation(String orientation){
        this.robotOrientation = orientation;
    }
    public void setPaintList(GridMap newMap){
        // paintlist of gridcells with corresponding colorations.(to be painted)
        this.currGridMap = newMap;
        this.paintList.clearDisplay();
        ArrayList<GridCell> cellsArr = newMap.getCellsArr();
        for (GridCell cell: cellsArr){ 
            this.paintList.addCell(cell);
        }
        
    }
    public void setCurrGridMap(String pathName, boolean isBinary){
        //1. read file onto this.currGridMap
        ReadMap reader1 = new ReadMap(pathName);
        String fileStr = reader1.fileToString();
        //System.out.println("[setCurrGridMap]: "+currGridMap.binToHex(fileStr. replaceAll("\\s","")));
        
        fileStr = fileStr. replaceAll("\\s","");
        if(isBinary){
            this.currGridMap.setMapFromBin(fileStr);
        }
        else{
            this.currGridMap.setMapFromHex(fileStr);
        }
        
    }

    public void setCurrMapPanel(){
        //sets current panel based on object's paintlist.
       this.currMapPanel.setPaintList(this.paintList);
       this.currMapPanel.setPreferredSize(new Dimension(375+GridConstants.mapPadding,500+GridConstants.mapPadding+20));
    }

    public void updateRobotOrientation(String orientation){
        Integer robotX = this.robotElement.getX();
        Integer robotY = this.robotElement.getY();
        switch (orientation){
            case "north":
                robotY -= 1;
                break;
            case "south":
                robotY += 1;
                break;
            case "east":
                robotX += 1;
                break;
            case "west":
                robotX -= 1;
                break;
            default:
                break;
        }
        this.robotHead.setX(robotX);
        this.robotHead.setY(robotY);
        this.setRobotOrientation(orientation);
    }
    public void move(String direction){//visually moves robot on screen
        switch(direction){
            case "right":
                this.robotElement.setX(this.robotElement.getX()+1);
                updateRobotOrientation("east");
                break;

            case "left":
                this.robotElement.setX(this.robotElement.getX()-1);
                updateRobotOrientation("west");
                break;

            case "up":
                this.robotElement.setY(this.robotElement.getY()-1);
                updateRobotOrientation("north");
                break;
            
            case "down":
                this.robotElement.setY(this.robotElement.getY()+1);
                updateRobotOrientation("south");
                break;
            default:
                System.out.println("[move(direction)]: Invalid direction! ");

        }
          
    }

    
    public static void main(String[] args){
        //creates instance of JFrame
        JFrame f = new JFrame(); 
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel,BoxLayout.Y_AXIS));
        mainPanel.setMinimumSize(mainPanel.getPreferredSize());
        f.getContentPane().add(mainPanel);

        //read map from binary file to a PaintPanel (extend jpanel)
        String directoryPath = "C:\\Users\\whate\\Documents\\GitHub\\MDP\\MDP-Algorithm-Drafts\\MapFiles\\";
        UIDisplay loadArena1 = new UIDisplay();

        loadArena1.moveList.add("up");
        loadArena1.moveList.add("up");
        loadArena1.moveList.add("up");
        loadArena1.moveList.add("up");
        loadArena1.moveList.add("up");
        loadArena1.moveList.add("up");
        loadArena1.moveList.add("up");
        loadArena1.moveList.add("right");
        loadArena1.moveList.add("right");
        loadArena1.moveList.add("right");
        loadArena1.moveList.add("right");
        loadArena1.moveList.add("left");
        loadArena1.moveList.add("left");
        loadArena1.moveList.add("left");
        loadArena1.moveList.add("left");
        loadArena1.moveList.add("left");
        loadArena1.moveList.add("down");
        loadArena1.moveList.add("down");
        loadArena1.moveList.add("down");
        loadArena1.moveList.add("right");
        loadArena1.moveList.add("up");
        //read map file from disk
        loadArena1.setCurrGridMap(directoryPath+"sampleArena5.txt", true);

        System.out.println("[part1String]: "+loadArena1.currGridMap.binToHex(loadArena1.currGridMap.part1String()));
        System.out.println("[part2String]: "+loadArena1.currGridMap.binToHex(loadArena1.currGridMap.part2String()));
        

        //set the paint list from the Grid Map
        loadArena1.setPaintList(loadArena1.currGridMap);
        //add start and goal Areas to the paint list\
        loadArena1.paintList.addCell(new GridCell(2,19,"startEndArea"));
        //add robot to the paint list
        loadArena1.paintList.addCell(loadArena1.robotElement);
        loadArena1.paintList.addCell(loadArena1.robotHead);
        loadArena1.updateRobotOrientation("north");
        //loadArena1.paintList.addCell(new GridCell(2,18,"circle"));

        //paint items in list onto panel.
        loadArena1.setCurrMapPanel();
        //add painted panel onto the main panel.
        mainPanel.add(loadArena1.currMapPanel);

        //Options buttons
        String[] roboModes = new String[] {"Exploration", "Fastest",
                                    "Pacman", "Reset"};
        
        JPanel pane = new JPanel();
        mainPanel.add(pane);
        pane.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        if (shouldFill){
            c.fill = GridBagConstraints.HORIZONTAL;
        }
        if(shouldWeightX){
            c.weightx = 0.5;
        }
       
        JComboBox<String> modesCombo = new JComboBox<String>(roboModes);
        c.gridx = 1;
        c.gridy = 1;
        pane.add(modesCombo, c);
        
        //String strPart1 = loadArena1.currGridMap.part1String();
        JLabel label1 = new JLabel("Test label1");
        //JLabel label1 = new JLabel(loadArena1.currGridMap.binToHex(strPart1));
        c.gridx = 3;
        c.gridy = 1;
        pane.add(label1,c);

        JButton button;
        button = new JButton("Run");
        c.gridx = 0;
        c.gridy = 1;
        pane.add(button, c);

        //timer concept for map animation, reads off an array of moves up,up,left,left,etc.
        loadArena1.timerCount = 0;
        final Timer timerMainPanel = new Timer(120,null);
        timerMainPanel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("mainPanel revalidating and repainting...");
                loadArena1.move(loadArena1.moveList.get(loadArena1.timerCount));
                loadArena1.currGridMap.setCell(1,1,true,false); 
                //PaintPanel will redraw with the paintlist it points to (which points a currGridMap) (when mainPanel does repaint())
                mainPanel.revalidate();
                mainPanel.repaint();
                loadArena1.timerCount++;
                //check if movements are all done, if so, stop animating.
                if(loadArena1.timerCount>loadArena1.moveList.size()-1){
                    loadArena1.timerCount = 0;
                    timerMainPanel.stop();
                }
            }
        });

        button.addActionListener(new ActionListener() { 
            public void actionPerformed(ActionEvent e) { 
              System.out.println("Button Pressed!");
              String runMode = modesCombo.getSelectedItem().toString();  
              System.out.println("runMode: "+runMode);

              switch(runMode){
                  case "Exploration":
                    timerMainPanel.start(); //starts movesList animation.
                    break;
                  case "Fastest":
                    loadArena1.move("left");
                    mainPanel.revalidate();
                    mainPanel.repaint();
                    break;
                  case "Pacman":
                    loadArena1.move("up");
                    mainPanel.revalidate();
                    mainPanel.repaint();
                    break;
                  case "Reset":
                    loadArena1.move("down");
                    mainPanel.revalidate();
                    mainPanel.repaint();
                    break;
                  default:
                    System.out.println("[run button actionlistener]: Invalid runMode!");
                

              }
              
            } 
          });

        

        f.setSize(600,610); //400 width, 500 height
        f.setMinimumSize(f.getPreferredSize());
        //f.setLayout(null);//using no layout managers, MUST setSize(width,height) for ALL in frame.
        f.setVisible(true);//making the frame visible
    }

    
}