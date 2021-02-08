import java.awt.BorderLayout;
import java.awt.LayoutManager;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;

import javax.swing.*;

public class ArenaViewer {
    public final static int WIDTH = 512;
    public final static int HEIGHT = 1024;
    private static JFrame mainFrame = null;
    private static File selectedFile = null;
    private static ArenaPanel arenaPanel = null;
    private static Coordinate waypoint = null;
    private static ArenaCell[][] loadedMap;

    private static JTextField txt_x = new JTextField("XXXXX");
    private static JTextField txt_y = new JTextField("YYYYY");
    private static JButton wp_btn = new JButton("Set Waypoint");

    private static AndroidTalker android = new AndroidTalker(true);

    public static void Init() {
        // Setting up Frame
        mainFrame = new JFrame("Arena");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(WIDTH, HEIGHT);
        // mainFrame.setVisible(true);
        SetupMenuBar();
        SetupArenaTiles();
        SetupBottomButtons();
        mainFrame.setVisible(true);
        SetWaypoint("13", "18", false);
        // mainFrame.getContentPane().paintAll(mainFrame.getGraphics());
    }

    public static void Notify(String message) {
        JOptionPane.showMessageDialog(null, message);
    }

    public static void SetupMenuBar() {
        JMenuBar mb = new JMenuBar();
        JMenu m1 = new JMenu("File");
        mb.add(m1);
        JMenuItem item1 = new JMenuItem("Open Arena File");
        m1.add(item1);
        item1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser file = new JFileChooser("Open Arena File (File Picker)");
                file.setCurrentDirectory(new File(System.getProperty("user.dir")));
                int result = file.showOpenDialog(item1);
                if (result == JFileChooser.APPROVE_OPTION) {
                    selectedFile = file.getSelectedFile();
                    LoadMap();
                }
            }
        });
        mainFrame.getContentPane().add(BorderLayout.NORTH, mb);
    }

    public static void SetupArenaTiles() {
        // Draw Cells
        ArenaCell[][] cells = new ArenaCell[ArenaPanel.NO_CELLS_HEIGHT][ArenaPanel.NO_CELLS_WIDTH];
        for (int y = 0; y < ArenaPanel.NO_CELLS_HEIGHT; y++) {
            for (int x = 0; x < ArenaPanel.NO_CELLS_WIDTH; x++) {
                cells[y][x] = new ArenaCell(x, y);
            }
        }
        // Robot
        arenaPanel = new ArenaPanel(cells);
        arenaPanel.paint(mainFrame.getGraphics());
        // MyPanel panel = new MyPanel();
        mainFrame.getContentPane().add(BorderLayout.CENTER, arenaPanel);
    }

    public static void SetupBottomButtons() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 3));
        JPanel waypoint_panel = new JPanel(new BorderLayout());
        JPanel waypoint_text_panel = new JPanel(new GridLayout(0,2));
        waypoint_text_panel.add(txt_x);
        waypoint_text_panel.add(txt_y);
        waypoint_panel.add(BorderLayout.NORTH, waypoint_text_panel);
        waypoint_panel.add(BorderLayout.SOUTH, wp_btn);
        wp_btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int x = Integer.parseInt(txt_x.getText());
                    int y = Integer.parseInt(txt_y.getText());
                    waypoint = new Coordinate(x, y);
                    Notify("Waypoint set to: " + waypoint.toString());
                } catch (NumberFormatException error) {
                    Notify("Error While Setting Waypoint!");
                }
            }
        });
        JButton btn_simulate = new JButton("Run Simulator!");
        btn_simulate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SimulateExplore();
            }

        });
        JButton btn_real_explore = new JButton("Real Explore");
        btn_real_explore.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RealExplore();
            }
        });
        JButton btn_shortest = new JButton("Run Shortest Path!");
        btn_shortest.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ShortestPath();
            }

        });
        JButton btn_mapstr = new JButton("Display Map String");
        JTextField mapstring = new JTextField("Map String Displayed here");
        mapstring.setEditable(false);
        btn_mapstr.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                mapstring.setText(ToMapString());
            }
        });
        JButton btn_calibrate = new JButton("Pre-Calibrate");
        btn_calibrate.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                PreCalibrate(false);
            }
        });
        panel.add(waypoint_panel);
        panel.add(btn_simulate);
        panel.add(btn_real_explore);
        panel.add(btn_shortest);
        JPanel map_str_panel = new JPanel(new GridLayout(0,1));
        map_str_panel.add(btn_mapstr);
        map_str_panel.add(mapstring);
        panel.add(map_str_panel);
        panel.add(btn_calibrate);
        // mainFrame.getContentPane().add(BorderLayout.SOUTH, button); // Adds Button to
        // content pane of frame
        // mainFrame.getContentPane().add(BorderLayout.SOUTH, btn_simulate);
        mainFrame.getContentPane().add(BorderLayout.SOUTH, panel);
    }

    public static void PreCalibrate(){
        PreCalibrate(true);
    }
    public static void PreCalibrate(boolean isReal){
        Explorer.Init(isReal);
        Explorer.DoPreCalibrate();
    }
    public static void RedrawArena() {
        if (arenaPanel != null) {
            arenaPanel.repaint();
        }
        return;
    }

    private static void LoadMap() {
        String inputStr = "";
        try {
            FileInputStream in = new FileInputStream(selectedFile.getAbsolutePath());

            while (in.available() > 0) {
                char nextChar = (char) in.read();
                if (nextChar != '\n' && nextChar != '\r')
                    inputStr += nextChar;
            }
            // System.out.println(inputStr);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
            Notify("Something went wrong while reading file");
        }
        if (inputStr.length() != 608) {
            Notify("Invalid map file");
            return;
        }
        // Start from index 2 + (300) + 2 = 304;
        int counter = 0;
        loadedMap = new ArenaCell[ArenaPanel.NO_CELLS_HEIGHT][ArenaPanel.NO_CELLS_WIDTH];
        for (int i = 304; i < 604; i++) { // 608-4 = 604
            int x = (counter % ArenaPanel.NO_CELLS_WIDTH);
            int y = (counter / ArenaPanel.NO_CELLS_WIDTH);
            loadedMap[y][x] = new ArenaCell(x, y);
            // arenaPanel.getMap()[y][x] = new ArenaCell(x, y);
            //arenaPanel.getMap()[y][x].setStatus(inputStr.charAt(i) == '1' ? ArenaCell.Status.OBSTACLE : ArenaCell.Status.FREE);
            //loadedMap[y][x].setStatus(arenaPanel.getMap()[y][x].getStatus());
            loadedMap[y][x].setStatus(inputStr.charAt(i) == '1' ? ArenaCell.Status.OBSTACLE : ArenaCell.Status.FREE);
            counter++;
        }
        RedrawArena();
        // loadedMap = arenaPanel.getMap().clone();
    }

    public static ArenaCell[][] GetMap() {
        if (arenaPanel == null)
            return null;
        return arenaPanel.getMap();
    }

    public static ArenaCell[][] GetLoadedMap() {
        return loadedMap;
    }

    public static ArenaPanel GetArenaPanel() {
        return arenaPanel;
    }

    public static void SimulateExplore() {
        if (GetLoadedMap() == null) {
            Notify("No Map loaded!");
            return;
        }
        /*
        ArenaCell[][] map = GetMap();
        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[y].length; x++) {
                map[y][x].setStatus(ArenaCell.Status.UNEXPLORED);
            }
        }
        */
        SetStartPointExplored();
        SetGoalPointExplored();
        Explorer.Init(false);
        Explorer.Explore();
    }

    public static void ShortestPath() {
        //PathFinder.DoFindPath(waypoint);
        PathFinder.RobotPathFinderLoop();
    }

    private static void SetStartPointExplored() {
        int xs[] = { 0, 1, 2, 0, 1, 2, 0, 1, 2 };
        int ys[] = { 0, 0, 0, 1, 1, 1, 2, 2, 2 };
        for (int i = 0; i < xs.length; i++) {
            GetMap()[ys[i]][xs[i]].setStatus(ArenaCell.Status.FREE);
            GetMap()[ys[i]][xs[i]].setStatus(ArenaCell.Status.FREE);
            GetMap()[ys[i]][xs[i]].setStatus(ArenaCell.Status.FREE);
        }
    }

    private static void SetGoalPointExplored() {
        int xs[] = { 12, 13, 14, 12, 13, 14, 12, 13, 14 };
        int ys[] = { 17, 17, 17, 18, 18, 18, 19, 19, 19 };
        for (int i = 0; i < xs.length; i++) {
            GetMap()[ys[i]][xs[i]].setStatus(ArenaCell.Status.FREE);
            GetMap()[ys[i]][xs[i]].setStatus(ArenaCell.Status.FREE);
            GetMap()[ys[i]][xs[i]].setStatus(ArenaCell.Status.FREE);
            GetMap()[ys[i]][xs[i]].setStatus(ArenaCell.Status.FREE);
        }
    }

    public static void RealExplore() {
        SetStartPointExplored();
        SetGoalPointExplored();
        Explorer.Init(true);
        Explorer.Explore();
    }

    public static void SetWaypoint(String x, String y) {
        SetWaypoint(x, y, true);
    }

    public static void SetWaypoint(String x, String y, boolean doPopUp) {
        txt_x.setText(x);
        txt_y.setText(y);
        if (doPopUp)
            wp_btn.doClick();
        else
            waypoint = new Coordinate(Integer.parseInt(x), Integer.parseInt(y));
    }

    public static String ToMapString(){
        String mapStr = "11";
        String mapStr2 = "";
        for (int y = 0; y < ArenaPanel.NO_CELLS_HEIGHT ; y++){
            for (int x = 0; x < ArenaPanel.NO_CELLS_WIDTH; x++){
                ArenaCell curCell = GetMap()[y][x];
                if (curCell.getStatus() == ArenaCell.Status.UNEXPLORED){
                    mapStr += "0";
                }else{
                    mapStr += "1";
                    if (curCell.isObstacle()){
                        mapStr2 += "1";
                    }else{
                        mapStr2 += "0";
                    }
                }
            }
        }
        mapStr += "11";
        String hexString = GetHexString(mapStr);
        String hexString2 = GetHexString(mapStr2);
        return hexString + "|" + hexString2;
    }
    private static String GetHexString(String binaryString){
        String hexStr = "";
        for (int i = 0 ; i < binaryString.length(); i+=4){
            String curString = "";
            if (i+3 >= binaryString.length()){
                curString = "0"+curString;
            }else{
                curString = binaryString.charAt(i+3) + curString;
            }
            if (i+2 >= binaryString.length()){
                curString = "0"+curString;
            }else{
                curString = binaryString.charAt(i+2) + curString;
            }
            if (i+1 >= binaryString.length()){
                curString = "0"+curString;
            }else{
                curString = binaryString.charAt(i+1) + curString;
            }
            curString = binaryString.charAt(i) + curString;
            //Convert to int
            hexStr +=  Integer.toHexString(Integer.parseInt(curString, 2));
        }
        return hexStr;
    }
    public static AndroidTalker GetAndroid(){
        return android;
    }
    public static boolean IsWaypointValid(){
        try{
            int x = Integer.parseInt(txt_x.getText());
            int y = Integer.parseInt(txt_y.getText());
            return ArenaCell.isValidCoords(x, y);
        }catch (NumberFormatException e){
            return false;
        }
    }
    public static Coordinate GetWaypoint(){
        return waypoint;
    }
}
