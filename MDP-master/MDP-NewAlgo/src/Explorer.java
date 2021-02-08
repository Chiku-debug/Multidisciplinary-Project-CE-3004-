import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class Explorer {

    public interface PreAlgo {
        public void InitFlags();

        public boolean onPreAlgo(); // Return true to break
    }

    public interface PostAlgo {
        public boolean onPostAlgo(); // Return true to break
    }

    private static int callback_index = -1;

    private static ArrayList<PreAlgo> listPreAlgo = new ArrayList<>();
    private static ArrayList<PostAlgo> listPostAlgo = new ArrayList<>();

    private static HashMap<String, Integer> flags = new HashMap<String, Integer>();

    private static long startMillis = 0;

    private static boolean hasInit = false;
    private static boolean isDoingPreCalibrate = false;
    public static void DoPreCalibrate(){
        isDoingPreCalibrate = true;
        // flags.put("PRE_ALGO_CALIBRATE_LEFT", 0);
        // flags.put("PRE_ALGO_CALIBRATE_CALIBRATE", 0);
        // flags.put("PRE_ALGO_CALIBRATE_RIGHT", 0);
        flags.put("PRE_ALGO_STEP_COUNTER", 0);
        ExploreLoop();
    }

    public static void ExploreLoop() {
        for (PreAlgo preAlgo : listPreAlgo) {
            if (preAlgo.onPreAlgo())
                return;
        }
        // TODO Check if looped finished
        if (flags.get("hasTurnedRight") != 0 && GetRobot().canMoveForward(GetMap())) {
            flags.put("hasTurnedRight", 0);
            GetRobot().moveForward();
        } else if (GetRobot().canMoveRight(GetMap())) {
            flags.put("hasTurnedRight", 1);
            GetRobot().turn(Robot.MovementDirection.RIGHT);
        } else if (GetRobot().canMoveForward(GetMap())) {
            GetRobot().moveForward();
        } else if (GetRobot().canMoveLeft(GetMap())) {
            GetRobot().turn(Robot.MovementDirection.LEFT);
        } else {
            System.out.println("No Choice Turn Right");
            // Nowhere else to move, turn right anyways
            flags.put("forceTurnRight", 1);
            GetRobot().turn(Robot.MovementDirection.RIGHT);
        }

        for (PostAlgo postAlgo : listPostAlgo) {
            if (postAlgo.onPostAlgo())
                return;
        }
    }
    public static void Init(boolean isReal){
        if (hasInit) return;
        hasInit = true;
        ArenaViewer.GetArenaPanel().setRobot(new Robot(isReal));
        callback_index = GetRobot().addCallback(new RobotCallback() {
            @Override
            public void onCallback(byte[] sensor_data) {
                ObstacleCheck(sensor_data);
            }
        });
        listPreAlgo.clear();
        AddPreCalibrate();
        AddRightWallPicture(); //Important!! Put First!!
        AddSenseIfPhantom();
        AddCheckCalibrate();
        AddForceTurn();
        AddStopAtHome();
        // AddFirstCalibrate();
                
        AddCheckVisited();
        AddMapDescriptor();
        InitFlags();
        PathFinder.SubscribeToRobot();
    }

    public static void Explore() {
        if (!ArenaViewer.IsWaypointValid()){ ArenaViewer.Notify("Waypoint Invalid!"); return; }
        System.out.println("Current Time Stamp: "+(new Timestamp((new Date()).getTime())));
        startMillis = (new Date()).getTime();
        InitFlags();
        ExploreLoop();
    }

    private static void InitFlags() {
        flags.put("hasTurnedRight", 0);
        for (PreAlgo pre : listPreAlgo) {
            pre.InitFlags();
        }
    }
    
    private static void AddPreCalibrate(){
        listPreAlgo.add(new PreAlgo(){

            @Override
            public void InitFlags() {
                // flags.put("PRE_ALGO_CALIBRATE_LEFT", 0);
                // flags.put("PRE_ALGO_CALIBRATE_CALIBRATE", 0);
                // flags.put("PRE_ALGO_CALIBRATE_RIGHT", 0);

                flags.put("PRE_ALGO_STEP_COUNTER", 0);
            }

            @Override
            public boolean onPreAlgo() {
                if (isDoingPreCalibrate == false) return false;
                if (flags.get("PRE_ALGO_STEP_COUNTER") >= 0){
                    switch(flags.get("PRE_ALGO_STEP_COUNTER")){
                        case 0:
                        case 1:
                            GetRobot().turn(Robot.MovementDirection.LEFT);
                            break;
                        case 2:
                            GetRobot().calibrate();
                            break;
                        case 3:
                            GetRobot().turn(Robot.MovementDirection.RIGHT);
                            break;
                        case 4:
                            GetRobot().calibrate();
                            break;
                        case 5:
                            GetRobot().turn(Robot.MovementDirection.RIGHT);
                            break;

                    }
                    flags.put("PRE_ALGO_STEP_COUNTER", flags.get("PRE_ALGO_STEP_COUNTER") + 1);
                    return true;
                }
                // if (flags.get("PRE_ALGO_CALIBRATE_LEFT") == 0){
                //     flags.put("PRE_ALGO_CALIBRATE_LEFT", 1);
                //     GetRobot().turn(Robot.MovementDirection.LEFT);
                //     return true;
                // }else if (flags.get("PRE_ALGO_CALIBRATE_CALIBRATE") == 0){
                //     flags.put("PRE_ALGO_CALIBRATE_CALIBRATE", 1);
                //     GetRobot().calibrate();
                //     return true;
                // }else if (flags.get("PRE_ALGO_CALIBRATE_RIGHT") == 0){
                //     flags.put("PRE_ALGO_CALIBRATE_RIGHT", 1);
                //     GetRobot().turn(Robot.MovementDirection.RIGHT);
                //     return true;
                // }
                return false;
            }
            
        });
    }

    private static void AddForceTurn() {
        listPreAlgo.add(new PreAlgo() {
            @Override
            public boolean onPreAlgo() {
                if (flags.get("forceTurnRight") == 0 && flags.get("forceTurnLeft") == 0)return false;
                if (flags.get("forceTurnRight") > 0) {
                    flags.put("forceTurnRight", flags.get("forceTurnRight") - 1);
                    GetRobot().turn(Robot.MovementDirection.RIGHT);
                    return true;
                }else {
                    flags.put("forceTurnLeft", flags.get("forceTurnLeft") - 1);
                    GetRobot().turn(Robot.MovementDirection.LEFT);
                    return true;
                }
                
                
            }

            @Override
            public void InitFlags() {
                flags.put("forceTurnRight", 0);
                flags.put("forceTurnLeft", 0);
            }
        });
    }

    private static void AddStopAtHome() {
        // Check has re-entered home
        listPreAlgo.add(new PreAlgo() {
            @Override
            public boolean onPreAlgo() {
                if (flags.get("hasCompletedExploration") == 1) return true;
                if (flags.get("hasLeftHome") == 0) {
                    flags.put("hasLeftHome", (GetRobot().getX() != 1 && GetRobot().getY() != 1) ? 1 : 0);
                }
                if (flags.get("hasLeftHome") == 0)
                    return false;
                if (flags.get("hasEnteredHome") == 1){
                    System.out.println("Finished Exploration!");
                    if (flags.get("hasFacedNorthAfterExplore") == 0){
                        System.out.println("Facing North");
                        if (GetRobot().getOrientation() != Robot.Direction.NORTH) {
                            boolean canTurnNorth = GetRobot().turnToNorth();
                            if (!canTurnNorth) { // Need to do u-turn
                                flags.put("forceTurnRight", 1);
                                GetRobot().turn(Robot.MovementDirection.RIGHT);
                            }
                        }
                        flags.put("hasFacedNorthAfterExplore", 1);
                        return true;
                    }
                    if (flags.get("hasCompletedExploration") == 0){
                        flags.put("hasCompletedExploration", 1);
                        OnFinishExploration();
                    }                    
                    return true; // This line stops everything
                }
                flags.put("hasEnteredHome", (GetRobot().getX() == 1 && GetRobot().getY() == 1) ? 1 : 0);
                // TODO: Wait for other stuff before stopping
                if (flags.get("hasEnteredHome") == 1) {
                    if (GetClearPercent() <= 0.5){
                        System.out.println("Reentered home with < 0.5, continuing");
                        flags.put("hasEnteredHome", 0);
                    }else{
                        //Entered home, do Calibration
                        System.out.println("Doing Home Calibration");
                        GetRobot().calibrate();
                    }
                }
                return flags.get("hasEnteredHome") == 1;
            }

            @Override
            public void InitFlags() {
                flags.put("hasLeftHome", 0);
                flags.put("hasEnteredHome", 0);
                flags.put("hasCompletedExploration", 0);
                flags.put("hasFacedNorthAfterExplore", 0);
            }
        });
    }

    private static void AddSenseIfPhantom() {
        listPreAlgo.add(new PreAlgo() {
            @Override
            public boolean onPreAlgo() {
                if (GetRobot().hasPhantom(GetMap())) {
                    GetRobot().sense();
                    return true;
                }
                return false;
            }

            @Override
            public void InitFlags() {
            }
        });
    }

    private static void AddCheckVisited() {
        listPreAlgo.add(new PreAlgo() {
            @Override
            public boolean onPreAlgo() {
                if (GetMap()[GetRobot().getY()][GetRobot().getX()].overVisited()) {
                    if (flags.get("CHECK_VISITED_TURN_LEFT") == 0 && GetRobot().canMoveLeft(GetMap())) {
                        GetRobot().turn(Robot.MovementDirection.LEFT);
                        flags.put("CHECK_VISITED_TURN_LEFT", 1);
                    }else{
                        flags.put("CHECK_VISITED_TURN_LEFT", 0);
                        if (GetRobot().canMoveForward(GetMap())) {
                            GetRobot().moveForward();
                        } else {
                            GetRobot().turn(Robot.MovementDirection.LEFT);
                        }
                    }
                    return true;
                }
                return false;
            }

            @Override
            public void InitFlags() {
                flags.put("CHECK_VISITED_TURN_LEFT", 0);
            }
        });
    }

    private static void AddCheckCalibrate() {
        listPreAlgo.add(new PreAlgo() {
            final int THRESHOLD_FRONT = 1;
            final int THRESHOLD_SIDE = 6;

            @Override
            public boolean onPreAlgo() {

                if ((flags.get("isCalibrateLeft") != 0 || flags.get("isCalibrateRight") != 0) && flags.get("movesWithoutCalibrate") < THRESHOLD_SIDE) {
                    if (flags.get("isCalibrateRight") != 0) flags.put("isCalibrateRight", 0);
                    if (flags.get("isCalibrateLeft") != 0) flags.put("isCalibrateLeft", 0);
                    GetRobot().turnToX((Robot.Direction.values()[flags.get("originalDirection")]));
                    return true;
                }
                if (flags.get("isCalibrateLeft") != 0 || flags.get("isCalibrateRight") != 0 || GetRobot().canCalibrateForward(GetMap())
                        && flags.get("movesWithoutCalibrate") >= THRESHOLD_FRONT) {
                    GetRobot().calibrate();
                    flags.put("movesWithoutCalibrate", 0);
                    // System.out.println("moves: " + flags.get("movesWithoutCalibrate"));
                    return true;
                } else if (GetRobot().canCalibrateRight(GetMap())
                        && flags.get("movesWithoutCalibrate") >= THRESHOLD_SIDE) {
                    flags.put("isCalibrateRight", 1);
                    flags.put("originalDirection", GetRobot().getOrientation().ordinal());
                    GetRobot().turn(Robot.MovementDirection.RIGHT);
                    return true;
                }else if (GetRobot().canCalibrateLeft(GetMap()) && flags.get("movesWithoutCalibrate") >= THRESHOLD_SIDE){
                    flags.put("isCalibrateLeft", 1);
                    flags.put("originalDirection", GetRobot().getOrientation().ordinal());
                    GetRobot().turn(Robot.MovementDirection.LEFT);
                    return true;
                }

                flags.put("movesWithoutCalibrate", flags.get("movesWithoutCalibrate") + 1);
                // System.out.println("moves: " + flags.get("movesWithoutCalibrate"));
                return false;
            }

            @Override
            public void InitFlags() {
                flags.put("isCalibrateRight", 0);
                flags.put("isCalibrateLeft", 0);
                flags.put("movesWithoutCalibrate", 0);
                flags.put("originalDirection", 0);
            }
        });
    }

    private static void AddFirstCalibrate() {
        listPreAlgo.add(new PreAlgo() {
            @Override
            public boolean onPreAlgo() {
                if (flags.get("PRE_HAS_TURN_LEFT") == 1 && flags.get("PRE_HAS_CALIBRATE") == 1
                        && flags.get("PRE_HAS_TURN_RIGHT") == 1)
                    return false;
                if (flags.get("PRE_HAS_TURN_LEFT") == 0) {
                    flags.put("PRE_HAS_TURN_LEFT", 1);
                    GetRobot().turn(Robot.MovementDirection.LEFT);
                    return true;
                }
                if (flags.get("PRE_HAS_CALIBRATE") == 0) {
                    flags.put("PRE_HAS_CALIBRATE", 1);
                    GetRobot().calibrate();
                    return true;
                }
                if (flags.get("PRE_HAS_TURN_RIGHT") == 0) {
                    flags.put("PRE_HAS_TURN_RIGHT", 1);
                    GetRobot().turn(Robot.MovementDirection.RIGHT);
                    return true;
                }
                return false;
            }

            @Override
            public void InitFlags() {
                flags.put("PRE_HAS_TURN_LEFT", 0);
                flags.put("PRE_HAS_CALIBRATE", 0);
                flags.put("PRE_HAS_TURN_RIGHT", 0);
            }
        });
    }

    private static void AddRightWallPicture() {
        try {
            listPreAlgo.add(new PreAlgo() {
                MqttClient sender = new MqttClient(Explorer.GetRobot().isReal() ? "tcp://192.168.88.254" : "tcp://127.0.0.1", MqttClient.generateClientId(), new MemoryPersistence());
                final String ToRpi = "/laptop/pi";                

                @Override
                public boolean onPreAlgo() {
                    if (!Explorer.GetRobot().isReal()) return false;
                    if (sender.isConnected() && GetRobot().canTakePhotoRight(GetMap())) {
                        String str = "CAMERA|"+GetRobot().getX()+"|"+GetRobot().getY()+"|"+AndroidTalker.ToAndroidDirection(GetRobot().getOrientation())+"|";
                        try {
                            sender.publish(ToRpi, str.getBytes(), 0, false);
                        } catch (MqttPersistenceException e) { e.printStackTrace(); } catch (MqttException e) { e.printStackTrace(); }
                    }
                    return false;
                }

                @Override
                public void InitFlags() {
                    try {
                        if (!sender.isConnected()) sender.connect();
                    } catch (MqttSecurityException e) { e.printStackTrace(); } catch (MqttException e) { e.printStackTrace(); }
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    private static void AddMapDescriptor(){
        listPreAlgo.add(new PreAlgo(){

            @Override
            public void InitFlags() {

            }

            @Override
            public boolean onPreAlgo() {
                UpdateAndroid();
                return false;
            }
        });
    }
    private static Robot GetRobot() {
        return ArenaViewer.GetArenaPanel().getRobot();
    }

    private static ArenaCell[][] GetMap() {
        return ArenaViewer.GetMap();
    }

    public static void OnFinishExploration() {
        System.out.println("Current Time Stamp: "+(new Timestamp((new Date()).getTime())));
        double totalTime_seconds = ((new Date()).getTime() - startMillis) / 1000.0;
        long minutes = Math.round(totalTime_seconds) / 60;
        long seconds = Math.round(totalTime_seconds) % 60;
        System.out.println("Total run time: " + minutes + " minutes, " + seconds + " seconds.");
        // ArenaViewer.GetArenaPanel().getRobot().removeCallback(callback_index);
        //Check if 90% cleared
        System.out.println("Clear Percent: " + GetClearPercent());
        if (GetClearPercent() >= 0.5){
            //Color Unexplored to green
            ArenaCell[][] map = GetMap();    
            for (int y = 0; y < map.length; y++){
                for (int x = 0 ; x < map[y].length; x++){
                    if (map[y][x].getStatus() == ArenaCell.Status.UNEXPLORED){
                        map[y][x].markFree(); map[y][x].markFree(); map[y][x].markFree(); map[y][x].markFree(); map[y][x].markFree();
                    }
                        
                }
            }
        }
        UpdateAndroid();
        ArenaViewer.GetAndroid().send("INS|X|Explore|ACK|");
        PathFinder.DoFindPath(ArenaViewer.GetWaypoint());
        DoPreCalibrate();
        // System.out.println("Pathfinder set to true");
    }

    private static double GetClearPercent(){
        ArenaCell[][] map = GetMap();
        int cleared_count = 0;
        for (int y = 0; y < map.length; y++){
            for (int x = 0 ; x < map[y].length; x++){
                if (map[y][x].getStatus() != ArenaCell.Status.UNEXPLORED)
                    cleared_count += 1;
            }
        }
        return cleared_count / 300.0;
    }

    public static void ObstacleCheck(byte[] sensor_data) {
        
        if (isDoingPreCalibrate == false && flags.get("hasCompletedExploration") == 1){
            return;
        }
        // Update Obstacles
        if (flags.get("hasCompletedExploration") == 0){
            Sensor[] sensors = GetRobot().getSensors();
            for (int i = 0; i < sensors.length; i++) {
                sensors[i].updateMap(sensor_data[i], GetMap());
            }
        }
        ArenaViewer.RedrawArena();
        UpdateAndroid();
        if (isDoingPreCalibrate && flags.get("PRE_ALGO_STEP_COUNTER") == 6){
            isDoingPreCalibrate = false;
            return;
        }
        // if (isDoingPreCalibrate && flags.get("PRE_ALGO_CALIBRATE_LEFT") == 1 && flags.get("PRE_ALGO_CALIBRATE_CALIBRATE") == 1 && flags.get("PRE_ALGO_CALIBRATE_RIGHT") == 1){
        //     isDoingPreCalibrate = false;
        //     return;
        // }
        ExploreLoop();
    }
    public static void UpdateAndroid(){
        String str[] = ArenaViewer.ToMapString().split("\\|");
        String str1 = "MAP|1|"+str[0]+"|";
        String str2 = "MAP|2|"+str[1]+"|";
        String str3 = "BOT|"+GetRobot().getX()+"|"+GetRobot().getY()+"|"+AndroidTalker.ToAndroidDirection(GetRobot().getOrientation())+"|";
        ArenaViewer.GetAndroid().send(str1);
        ArenaViewer.GetAndroid().send(str2);
        ArenaViewer.GetAndroid().send(str3);
    }
}