import java.util.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.List;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

public class PathFinder {
    // private static ArrayList<Robot.MovementDirection> movements = null;
    private static ArrayList<byte[]> movements = null;
    private static int SubscriptionIndex = -1;
    public static boolean Switch = false;
    public static class Node extends Coordinate{
        private Node[] prevNode;
        public Node(int x ,int y, Node[] prevNode){
            super(x,y);
            this.prevNode = prevNode;
        }
        public Node[] getPrevNode(){
            return prevNode;
        }
    }
    public static class Node2 implements Comparable<Node2> { 
        public Coordinate coordinate; 
        public int cost; 
        public Robot.Direction direction;
        public Node2(Coordinate coord, int cost, Robot.Direction direction) 
        { 
            this.coordinate = coord; 
            this.cost = cost; 
            this.direction = direction;
        } 
      
        @Override
        public int compareTo(Node2 o) {
            if (this.cost < o.cost) 
                return -1; 
            if (this.cost > o.cost) 
                return 1; 
            return 0; 
        }
    } 
    public static boolean[][] toBooleanArray(ArenaCell[][] map) {
        //Converts map into boolean array, 1 = free, 0 = obstacle
        boolean[][] bmap = new boolean[map.length][map[0].length];
        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[y].length; x++) {
                bmap[y][x] = (!map[y][x].isObstacle() && map[y][x].getStatus() != ArenaCell.Status.UNEXPLORED);
            }
        }
        return bmap;
    }
    private static boolean isBotValidCell(boolean[][] map, Coordinate cell) {
        // This checks if bot can move to this cell
        final int x_offset[] = { -1, 0, 1, 1, 1, 0, -1, -1 };
        final int y_offset[] = { 1, 1, 1, 0, -1, -1, -1, 0 };
        // Check surrounding 8 cells
        
        for (int i = 0; i < 8; i++) {
            if (ArenaCell.isValidCoords(cell.getX() + x_offset[i], cell.getY() + y_offset[i])) {
                if (map[cell.getY() + y_offset[i]][cell.getX() + x_offset[i]] == false) { return false; }
            } else {
                return false;
            }
        }
        return true;
    }
    public static ArrayList<Coordinate> BFS(ArenaCell[][] map, Coordinate src, Coordinate dest){
        return BFS(toBooleanArray(map), src, dest);
    }
    public static ArrayList<Coordinate> BFS(boolean[][] map, Coordinate src, Coordinate dest) {
        // These arrays are used to get row and column
        // numbers of 4 neighbours of a given cell
        final int rowNum[] = { -1, 0, 0, 1 };
        final int colNum[] = { 0, -1, 1, 0 };
        // Error Checking
        if ((!ArenaCell.isValidCoords(src) || !ArenaCell.isValidCoords(dest))
                || (map[src.getY()][src.getX()] == false || map[dest.getY()][dest.getX()] == false)) {
            return null;
        }
        // array to mark visited cells
        boolean[][] visited = new boolean[map.length][map[0].length];
        visited[src.getY()][src.getX()] = true;
        // Create a queue for BFS 
        Queue<Node> q = new LinkedList<>(); 
        q.add(new Node(src.getX(), src.getY(), new Node[0]));

        boolean isPathFound = false;
        while (!q.isEmpty()){
            Node current = q.peek();
            if (current.getX() == dest.getX() && current.getY() == dest.getY()){ //Destination Reached. returns queue
                isPathFound = true;
                break;
            }
            q.remove();
            for (int i = 0 ; i < 4; i++){
                int row = current.getX() + rowNum[i]; 
                int col = current.getY() + colNum[i]; 

                //Check if bot can move to this cell && cell is free && have not visited this cell yet
                if (isBotValidCell(map, new Coordinate(row, col)) && map[col][row] == true && visited[col][row] == false){
                    visited[col][row] = true;
                    Node[] oldPrevNodes = current.getPrevNode().clone();
                    Node[] prevNodes = new Node[oldPrevNodes.length + 1];
                    int j;
                    for (j = 0 ; j < oldPrevNodes.length; j++){
                        prevNodes[j] = oldPrevNodes[j];
                    }
                    prevNodes[j] = current;
                    Node adjNode = new Node(row, col, prevNodes);
                    q.add(adjNode);
                }
            }
        }
        if (isPathFound){
            ArrayList<Coordinate> ret_list = new ArrayList<>();
            Node lastNode = q.remove();
            for (Node node : lastNode.getPrevNode()){ ret_list.add(node); }
            ret_list.add(lastNode);
            return ret_list;
        }else{
            return null;
        }
    }
    private static ArrayList<Coordinate> anyPath(ArenaCell[][] map, Coordinate src, Coordinate target){
        ArrayList<Coordinate> result1 = AStar(map, src, target);
        if (result1 == null){
            result1 = BFS(map, src, target);
        }
        return result1;
    }
    private static boolean HasSubbed = false;
    public static void SubscribeToRobot(){
        if (HasSubbed) return;
        HasSubbed = true;
        //Step 1 - Subscribe to Robot
        SubscriptionIndex = ArenaViewer.GetArenaPanel().getRobot().addCallback(new RobotCallback(){
            @Override
            public void onCallback(byte[] sensor_data) {
                OnRobotCallback(sensor_data);
            }
        });
    }
    public static void DoFindPath(Coordinate waypoint){
        //AStar(ArenaViewer.GetMap(), new Coordinate(1, 1), waypoint);
        ArrayList<Coordinate> result1 = anyPath(ArenaViewer.GetMap(), new Coordinate(1, 1), waypoint);
        if (result1 != null){
            ArrayList<Coordinate> result2 = anyPath(ArenaViewer.GetMap(), waypoint, new Coordinate(13, 18));
            if (result2 != null){
                result2.remove(0);
                result1.addAll(result2);
                //Step 2 - Set the movements
                movements = ToCmd(ToMovement(result1)) ;
                //Step 3 - Start the loop
                //RobotPathFinderLoop();
                System.out.println("Ready For Fastest Path");
                isFirstLoop = true;
                return;
            }
        }
        ArenaViewer.Notify("No Path Found!");
    }
    private static boolean isFirstLoop = true;
    private static long PathFinderStart = 0;
    public static void RobotPathFinderLoop(){
        if (isFirstLoop){
            isFirstLoop = false;
            PathFinder.Switch = true;
            System.out.println("Starting Fastest Path\nCurrent Time Stamp: "+(new Timestamp((new Date()).getTime())));
            PathFinderStart = (new Date()).getTime();
        }
        if (movements == null){
            return;
        }
        if (movements.size() > 0){
            byte[] move = movements.remove(0);
            ArenaViewer.GetArenaPanel().getRobot().sendCmd(move, true);
        }else{
            //Robot Finished!
            System.out.println("Finished Fastest Path!");
            //ArenaViewer.GetArenaPanel().getRobot().removeCallback(SubscriptionIndex);
            System.out.println("Current Time Stamp: "+(new Timestamp((new Date()).getTime())));
            double totalTime_seconds = ((new Date()).getTime() - PathFinderStart) / 1000.0;
            long minutes = Math.round(totalTime_seconds) / 60;
            long seconds = Math.round(totalTime_seconds) % 60;
            System.out.println("Total run time: " + minutes + " minutes, " + seconds + " seconds.");
        }
        
    }
    private static void OnRobotCallback(byte[] sensor_data){
        if (!Switch) return;

        //We are ignoring sensor_data here
        ArenaViewer.RedrawArena();
        RobotPathFinderLoop();
        ArenaViewer.RedrawArena();
        Explorer.UpdateAndroid();
    }
    private static ArrayList<Robot.MovementDirection> ToMovement(ArrayList<Coordinate> path){
        ArrayList<Robot.MovementDirection> directions = new ArrayList<Robot.MovementDirection>();
        Robot.Direction curDirection = Robot.Direction.NORTH;
        Coordinate curCoord = path.get(0);
        for (int i = 1 ; i < path.size(); i++){
            Coordinate nextPath = path.get(i);
            if (!Robot.IsFacingTarget(curDirection, curCoord, nextPath)){
                Robot.MovementDirection turnDir = Robot.DirectionToX(curDirection, curCoord, nextPath);
                if (turnDir != null){
                    directions.add(turnDir);
                    curDirection = Robot.DirectionAfterTurning(curDirection, turnDir);
                }else{
                    directions.add(Robot.MovementDirection.RIGHT);
                    curDirection = Robot.DirectionAfterTurning(curDirection, Robot.MovementDirection.RIGHT);
                    directions.add(Robot.MovementDirection.RIGHT);
                    curDirection = Robot.DirectionAfterTurning(curDirection, Robot.MovementDirection.RIGHT);
                }
            }
            directions.add(Robot.MovementDirection.FORWARD);
            curCoord = path.get(i);
        }
        return directions;
    }

    private static ArrayList<byte[]> ToCmd(ArrayList<Robot.MovementDirection> movements){
        ArrayList<byte[]> cmdList = new ArrayList<byte[]>();
        for (Robot.MovementDirection dir : movements){
            if (cmdList.size() > 0){
                if (cmdList.get(cmdList.size() - 1)[0] == 0x10 && dir == Robot.MovementDirection.FORWARD){
                    cmdList.get(cmdList.size() - 1)[2] += 0x0a;
                    continue;
                }else if (cmdList.get(cmdList.size() - 1)[0] == 0x10){
                    cmdList.get(cmdList.size() - 1)[0] = 0x11;
                }
            }
            cmdList.add(Robot.Turn(dir)); 
        }
        if (cmdList.get(cmdList.size() - 1)[0] == 0x10){ cmdList.get(cmdList.size() - 1)[0] = 0x11; }
        return cmdList;
    }

    private static ArrayList<Coordinate> AStar(ArenaCell[][] map, Coordinate src, Coordinate dest){
        return AStar(toBooleanArray(map), src, dest);
    }
    private static int XYtoSingle(Coordinate c){
        return XYtoSingle(c.getX(), c.getY());
    }
    private static int XYtoSingle(int x, int y){
        return (ArenaPanel.NO_CELLS_WIDTH * y) + x;
    }
    private static boolean IsXInQ(Node2 X, PriorityQueue<Node2> Q){
        for (Node2 n : Q){
            if (X.coordinate.getX() == n.coordinate.getX() && X.coordinate.getY() == n.coordinate.getY()){
                return true;
            }
        }
        return false;
    }
    private static ArrayList<Coordinate> AStar(boolean[][] map, Coordinate src, Coordinate dest){
        final int rowNum[] = { -1, 1, 0, 0 };
        final int colNum[] = { 0, 0, 1, -1 };
        PriorityQueue<Node2> openSet = new PriorityQueue<>();

        Coordinate[] cameFrom = new Coordinate[map.length*map[0].length];
        int[] gScore = new int[map.length*map[0].length];
        Arrays.fill(gScore, Integer.MAX_VALUE);
        gScore[XYtoSingle(src)] = 0;
        int[] fScore = new int[map.length*map[0].length];
        boolean[] visited = new boolean[map.length*map[0].length];
        visited[XYtoSingle(src)] = true;
        Arrays.fill(fScore, Integer.MAX_VALUE);
        openSet.add(new Node2(src, 0, Robot.Direction.NORTH));
        while (!openSet.isEmpty()){
            Node2 current = openSet.peek();
            Coordinate curCoord = current.coordinate;
            Robot.Direction curDirection = current.direction;
            if (curCoord.getX() == dest.getX() && curCoord.getY() == dest.getY()){                
                ArrayList<Coordinate> path = new ArrayList<>();
                path.add(curCoord);
                Coordinate thisParent = cameFrom[XYtoSingle(curCoord)];
                while (thisParent != null){
                    path.add(0, thisParent);
                    thisParent = cameFrom[XYtoSingle(thisParent)];
                }
                //path.add(0, src);
                return path;
            }

            current = openSet.remove();
            for (int i = 0; i < 4; i++){
                int x = curCoord.getX() + rowNum[i];
                int y = curCoord.getY() + colNum[i];
                Coordinate nextCoordinate = new Coordinate(x, y);
                if (ArenaCell.isValidCoords(x, y) && isBotValidCell(map, nextCoordinate) && !visited[XYtoSingle(x, y)]){
                    int tenativeScore = gScore[XYtoSingle(curCoord)] + AStar_d(curDirection, curCoord, nextCoordinate);
                    if (tenativeScore < gScore[XYtoSingle(x, y)]){
                        cameFrom[XYtoSingle(x, y)] = curCoord;
                        gScore[XYtoSingle(x, y)] = tenativeScore;
                        gScore[XYtoSingle(x, y)] = gScore[XYtoSingle(x, y)] + AStar_h(curCoord, nextCoordinate);
                        Robot.Direction nextDirection = curDirection;
                        if (!Robot.IsFacingTarget(curDirection, curCoord, nextCoordinate)){
                            Robot.MovementDirection turnMove = Robot.DirectionToX(curDirection, curCoord, nextCoordinate);
                            if (turnMove == null){
                                nextDirection = Robot.Opposite(curDirection);
                            }else{
                                nextDirection = Robot.DirectionAfterTurning(curDirection, turnMove);
                            }
                        }
                        Node2 nextNode = new Node2(nextCoordinate, tenativeScore, nextDirection);
                        if (!IsXInQ(nextNode, openSet)){
                            visited[XYtoSingle(nextCoordinate)] = true;
                            openSet.add(nextNode);
                        }
                    }
                }
            }
        }
        return null;
    }
    
    private static int AStar_d(Robot.Direction curDir, Coordinate current, Coordinate target){
        final int FORWARD_D = -1;
        final int REVERSE_D = Integer.MAX_VALUE / 2;
        final int TURN_ANY_D = 3;
        int x_diff = target.getX() - current.getX();
        int y_diff = target.getY() - current.getY();
        switch (curDir) {
            case NORTH:
                if (x_diff == 0){
                    if (y_diff > 0) return FORWARD_D;
                    if (y_diff < 0) return REVERSE_D; 
                }else{
                    return TURN_ANY_D;
                }
                break;
            case EAST:
                if (y_diff == 0){
                    if (x_diff > 0) return FORWARD_D;
                    if (x_diff < 0) return REVERSE_D;
                }else{
                    return TURN_ANY_D;
                }
                break;
            case SOUTH:
                if (x_diff == 0){
                    if (y_diff < 0) return FORWARD_D;
                    if (y_diff > 0) return REVERSE_D; 
                }else{
                    return TURN_ANY_D;
                }
                break;
            case WEST:
                if (y_diff == 0){
                    if (x_diff < 0) return FORWARD_D;
                    if (x_diff > 0) return REVERSE_D;
                }else{
                    return TURN_ANY_D;
                }
                break;
        }
        return Integer.MAX_VALUE;
    }
    
    private static int AStar_h(Coordinate current, Coordinate target){
        return ((Math.abs(target.getX() - current.getX()))+(Math.abs(target.getY() - current.getY())));
    }
}