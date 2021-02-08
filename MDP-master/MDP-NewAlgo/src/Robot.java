import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class Robot {
    public enum Direction {
        NORTH, EAST, SOUTH, WEST
    }

    public enum MovementDirection {
        FORWARD, RIGHT, BACKWARDS, LEFT
    }

    public static final Color ROBOT_COLOR = Color.MAGENTA;
    public static final Color ROBOT_HEAD_COLOR = Color.WHITE;

    public static final String TOPIC_FROM_ROBOT = "/robot/laptop";
    public static final String TOPIC_TO_ROBOT = "/laptop/robot";

    public static final int DELAY = 50;

    private ArrayList<RobotCallback> callback_list = new ArrayList<RobotCallback>();

    private Boolean isReal;
    private int x = 1, y = 1;
    private int last_x = x, last_y = y;
    private Direction direction = Direction.NORTH;

    private boolean hasMovedForward = false;
    private MovementDirection lastTurn = null;

    private MqttClient spoofListener = null, spoofSender = null;
    private MqttClient listener = null, sender = null;


    private byte[] lastCmd = null;

    private Sensor RR = null, // RIGHT-RIGHT
            LR = null, // LEFT-RIGHT
            FC = null, // FRONT-CENTER
            FL = null, // FRONT-LEFT
            FR = null, // FRONT-RIGHT
            RL = null; // RIGHT-LEFT

    public Robot() {
        this.isReal = false;
        init();
    }

    public Robot(Boolean isReal) {
        this.isReal = isReal;
        init();
    }

    public boolean isReal(){
        return isReal;
    }
    private void init() {
        if (!isReal) { // Not real robot, Spoof Command
            try {
                spoofListener = new MqttClient("tcp://127.0.0.1:1883", UUID.randomUUID().toString(),
                        new MemoryPersistence());
                spoofSender = new MqttClient("tcp://127.0.0.1:1883", UUID.randomUUID().toString(),
                        new MemoryPersistence());
                spoofListener.connect();
                spoofSender.connect();
                spoofListener.subscribe(TOPIC_TO_ROBOT);
                spoofListener.setCallback(new MqttCallback() {
                    @Override
                    public void messageArrived(String arg0, MqttMessage arg1) throws Exception {
                        // System.out.println("On Received Cmd");
                        // return;
                        onReceiveCommand(arg1.getPayload());
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken arg0) {
                        // System.out.println("spoof delivery complete");
                    }

                    @Override
                    public void connectionLost(Throwable arg0) {
                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

        try {
            listener = new MqttClient(!isReal ? "tcp://127.0.0.1:1883" : "tcp://192.168.88.254",
                    UUID.randomUUID().toString(), new MemoryPersistence());
            sender = new MqttClient(!isReal ? "tcp://127.0.0.1:1883" : "tcp://192.168.88.254",
                    UUID.randomUUID().toString(), new MemoryPersistence());
            listener.connect();
            sender.connect();
            listener.subscribe(TOPIC_FROM_ROBOT);
            listener.setCallback(new MqttCallback() {
                @Override
                public void messageArrived(String arg0, MqttMessage arg1) throws Exception {
                    // System.out.println("On Received Reply");
                    onReceiveReply(arg1.getPayload());
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken arg0) {
                    // System.out.println("send delivery complete");
                }

                @Override
                public void connectionLost(Throwable arg0) {
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

        initSensors();
    }

    private void initSensors() {
        FL = new Sensor(0, 0, 1, Direction.NORTH, "FL");
        FC = new Sensor(0, 0, 1, Direction.NORTH, "FC");
        FR = new Sensor(0, 0, 0, Direction.NORTH, "FR");

        RR = new Sensor(0, 0, 3, Direction.EAST, "RR");
        RL = new Sensor(0, 0, 1, Direction.EAST, "RL");
        LR = new Sensor(0, 0, 2, Direction.WEST, "LR");

        updateSensorPosition();
    }

    public Direction getOrientation() {
        return direction;
    }

    private void updateSensorPosition() {
        switch (direction) {
            case NORTH:
                FC.update(x, y + 1, Direction.NORTH);
                FL.update(x - 1, y + 1, Direction.NORTH);
                FR.update(x + 1, y + 1, Direction.NORTH);
                RL.update(x + 1, y + 1, Direction.EAST);
                RR.update(x + 1, y - 1, Direction.EAST);
                LR.update(x - 1, y + 1, Direction.WEST);
                break;
            case EAST:
                FC.update(x + 1, y, Direction.EAST);
                FL.update(x + 1, y + 1, Direction.EAST);
                FR.update(x + 1, y - 1, Direction.EAST);
                RL.update(x + 1, y - 1, Direction.SOUTH);
                RR.update(x - 1, y - 1, Direction.SOUTH);
                LR.update(x + 1, y + 1, Direction.NORTH);
                break;
            case SOUTH:
                FC.update(x, y - 1, Direction.SOUTH);
                FL.update(x + 1, y - 1, Direction.SOUTH);
                FR.update(x - 1, y - 1, Direction.SOUTH);
                RL.update(x - 1, y - 1, Direction.WEST);
                RR.update(x - 1, y + 1, Direction.WEST);
                LR.update(x + 1, y - 1, Direction.EAST);
                break;
            case WEST:
                FC.update(x - 1, y, Direction.WEST);
                FL.update(x - 1, y - 1, Direction.WEST);
                FR.update(x - 1, y + 1, Direction.WEST);
                RL.update(x - 1, y + 1, Direction.NORTH);
                RR.update(x + 1, y + 1, Direction.NORTH);
                LR.update(x - 1, y - 1, Direction.SOUTH);
                break;
        }
    }

    public void draw(Graphics g) {
        Color lastColor = g.getColor();
        g.setColor(ROBOT_COLOR);
        g.fillOval(ArenaCell.ToStartDrawX(x - 1), ArenaCell.ToStartDrawY(y + 1), ArenaCell.CELL_WIDTH * 3,
                ArenaCell.CELL_HEIGHT * 3);
        g.setColor(lastColor);
        drawHead(g);
        for (Sensor s : getSensors()) {
            if (s != null)
                s.draw(g);
        }
    }

    private void drawHead(Graphics g) {
        Color lastColor = g.getColor();
        g.setColor(ROBOT_HEAD_COLOR);
        int head_x = x, head_y = y;
        switch (direction) {
            case NORTH:
                head_y += 1;
                break;
            case EAST:
                head_x += 1;
                break;
            case SOUTH:
                head_y -= 1;
                break;
            case WEST:
                head_x -= 1;
                break;
        }
        g.fillOval(ArenaCell.ToStartDrawX(head_x), ArenaCell.ToStartDrawY(head_y), ArenaCell.CELL_WIDTH,
                ArenaCell.CELL_HEIGHT);
        g.setColor(lastColor);
    }

    private void onReceiveCommand(byte[] cmd) {
        // For Virtual Robot
        if (isReal)
            return;
        // Return Sensor Value6
        byte[] ret = new byte[] { RR.getReading(), LR.getReading(), FC.getReading(), FL.getReading(), FR.getReading(),
                RL.getReading() };
        try {
            TimeUnit.MILLISECONDS.sleep(DELAY);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        try {
            spoofSender.publish(TOPIC_FROM_ROBOT, ret, 0, false);
        } catch (MqttPersistenceException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void onReceiveReply(byte[] reply) {
        if (isReal) {
            if (lastTurn != null)
                updateOrientation();
            if (hasMovedForward)
                updatePostion();
        }
        for (RobotCallback cb : callback_list) {
            cb.onCallback(reply);
        }
    }

    public int addCallback(RobotCallback callback) { // Returns the index
        System.out.println("Add Callback " + callback_list.size());
        if (callback_list.add(callback)) {
            System.out.println("Add Callback " + callback_list.size());
            return callback_list.size() - 1;
        }
        return -1;
    }

    public void removeCallback() {
        removeCallback(-1);
    }

    public RobotCallback removeCallback(int index) {
        System.out.println("Remove Callback");
        if (index < -1 || index > (callback_list.size() - 1))
            return null;
        if (index == -1)
            return callback_list.remove(callback_list.size() - 1); // pop last
        return callback_list.remove(index);
    }

    public boolean canMoveRight(ArenaCell[][] map) {
        // Step 1, find the middle cell
        Coordinate mid = getRightCoord();
        boolean temp = canMoveXP2(map, mid.getX(), mid.getY(), MovementDirection.RIGHT);
        return temp;
    }

    public boolean canMoveLeft(ArenaCell[][] map) {
        // Step 1, find the middle cell
        int mid_x = x, mid_y = y;
        switch (direction) {
            case NORTH:
                mid_x -= 2;
                break;
            case EAST:
                mid_y += 2;
                break;
            case SOUTH:
                mid_x += 2;
                break;
            case WEST:
                mid_y -= 2;
                break;
        }
        return canMoveXP2(map, mid_x, mid_y, MovementDirection.LEFT);
    }

    public boolean canMoveForward(ArenaCell[][] map) {
        Coordinate mid = getForwardCoord();
        return canMoveXP2(map, mid.getX(), mid.getY(), MovementDirection.FORWARD);
    }

    private Coordinate getForwardCoord() {
        int mid_x = x, mid_y = y;
        switch (direction) {
            case EAST:
                mid_x += 2;
                break;
            case SOUTH:
                mid_y -= 2;
                break;
            case WEST:
                mid_x -= 2;
                break;
            case NORTH:
                mid_y += 2;
                break;
        }
        return (new Coordinate(mid_x, mid_y));
    }

    private Coordinate getLeftCoord(){
        int mid_x = x, mid_y = y;
        switch (direction) {
            case NORTH:
                mid_x -= 2;
                break;
            case EAST:
                mid_y += 2;
                break;
            case SOUTH:
                mid_x += 2;
                break;
            case WEST:
                mid_y -= 2;
                break;
        }
        return (new Coordinate(mid_x, mid_y));
    }
    private Coordinate getRightCoord() {
        int mid_x = x, mid_y = y;
        switch (direction) {
            case NORTH:
                mid_x += 2;
                break;
            case EAST:
                mid_y -= 2;
                break;
            case SOUTH:
                mid_x -= 2;
                break;
            case WEST:
                mid_y += 2;
                break;
        }
        return (new Coordinate(mid_x, mid_y));
    }

    private boolean canMoveXP2(ArenaCell[][] map, int mid_x, int mid_y, MovementDirection movementDirection) { // Part 2
        ArrayList<ArenaCell> cells_to_check = new ArrayList<ArenaCell>();
        // Step 1.5, Check if is valid
        if (!ArenaCell.isValidCoords(mid_x, mid_y))
            return false;
        // Step 2, Add Cells to list
        cells_to_check.add(map[mid_y][mid_x]);
        if (movementDirection == MovementDirection.LEFT || movementDirection == MovementDirection.RIGHT) {
            switch (direction) {
                case NORTH:
                case SOUTH:
                    if (ArenaCell.isValidCoords(mid_x, mid_y + 1))
                        cells_to_check.add(map[mid_y + 1][mid_x]);
                    if (ArenaCell.isValidCoords(mid_x, mid_y - 1))
                        cells_to_check.add(map[mid_y - 1][mid_x]);
                    break;
                case EAST:
                case WEST:
                    if (ArenaCell.isValidCoords(mid_x + 1, mid_y))
                        cells_to_check.add(map[mid_y][mid_x + 1]);
                    if (ArenaCell.isValidCoords(mid_x - 1, mid_y))
                        cells_to_check.add(map[mid_y][mid_x - 1]);
                    break;
            }
        } else {
            switch (direction) {
                case EAST:
                case WEST:
                    if (ArenaCell.isValidCoords(mid_x, mid_y + 1))
                        cells_to_check.add(map[mid_y + 1][mid_x]);
                    if (ArenaCell.isValidCoords(mid_x, mid_y - 1))
                        cells_to_check.add(map[mid_y - 1][mid_x]);
                    break;
                case NORTH:
                case SOUTH:
                    if (ArenaCell.isValidCoords(mid_x + 1, mid_y))
                        cells_to_check.add(map[mid_y][mid_x + 1]);
                    if (ArenaCell.isValidCoords(mid_x - 1, mid_y))
                        cells_to_check.add(map[mid_y][mid_x - 1]);
                    break;
            }
        }

        // Step 3, Logic
        if (cells_to_check.size() != 3)
            return false;
        for (int i = 0; i < cells_to_check.size(); i++) {
            if ((cells_to_check.get(i).isObstacle()) /*|| (movementDirection == MovementDirection.RIGHT && cells_to_check.get(i).getHasChanged())*/ )
                return false;
        }
        return true;
    }

    public void turn(MovementDirection movementDirection) {
        byte[] cmd;
        switch (movementDirection) {
            case LEFT:
                cmd = Turn(MovementDirection.LEFT);
                break;
            case RIGHT:
                cmd = Turn(MovementDirection.RIGHT);
                break;
            case FORWARD:
                moveForward();
                return;
            default:
                return;
        }
        lastTurn = movementDirection;
        updateOrientation();
        sendCmd(cmd);
    }

    public static byte[] Turn(MovementDirection dir){
        switch (dir){
            case LEFT:
                return new byte[] { 0x07, 0x00, 0x5a };
            case RIGHT:
                return new byte[] { 0x04, 0x00, 0x5a };
            case FORWARD:
                return new byte[] {0x10, 0x00, 0x0a};
            default:
                break;
        }
        return null;
    }

    public void moveForward() {
        moveForward(10);
    }

    public void moveForward(int cm) {
        byte[] cmd = new byte[] { 0x10, 0x00, 0x00 };
        cmd[1] = (byte) ((cm >> 8) & 0xFF);
        cmd[2] = (byte) (cm & 0xFF);
        if (isReal) {
            hasMovedForward = true;
            sendCmd(cmd);
        } else {
            sendCmd(cmd);
            updatePostion();
        }
    }
    public static int FromFwdCmdToInt (byte[] cmd){
        int in1 = Byte.toUnsignedInt(cmd[1]);
        int in2 = Byte.toUnsignedInt(cmd[2]);
        return (in1 << 8) + in2;
    }
    public static byte[] FromIntToFwdCmd(int dist){
        byte[] cmd = new byte[] { 0x10, 0x00, 0x00 };
        cmd[1] = (byte) ((dist >> 8) & 0xFF);
        cmd[2] = (byte) (dist & 0xFF);
        return cmd;
    }

    public void sense() {
        byte[] cmd = new byte[] { 0x20, 0x00, 0x00 };
        sendCmd(cmd);
    }

    public void calibrate() {
        byte[] cmd = new byte[] { (byte) 0x80, 0x00, 0x00 };
        sendCmd(cmd);
    }
    public void sendCmd(byte[] cmd, boolean doMovement){
        if (doMovement){
            lastCmd = cmd.clone();
            if (cmd[0] == 0x11 || cmd[0] == 0x10){
                updatePostion();
            }else if (cmd[0] == 0x04 || cmd[0] == 0x07){
                lastTurn = (cmd[0] == 0x04 ? Robot.MovementDirection.RIGHT : Robot.MovementDirection.LEFT);
                updateOrientation();
            }
        }
        try {
            sender.publish(TOPIC_TO_ROBOT, cmd, 0, false);
            lastCmd = cmd;
        } catch (MqttPersistenceException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    public void sendCmd(byte[] cmd) {
        sendCmd(cmd, false);
        // System.out.println("Sending Cmd");
        
    }

    private void updatePostion() {
        hasMovedForward = false;
        int new_x = x, new_y = y;
        switch (direction) {
            case NORTH:
                new_y += 1;
                break;
            case EAST:
                new_x += 1;
                break;
            case SOUTH:
                new_y -= 1;
                break;
            case WEST:
                new_x -= 1;
                break;
        }
        x = new_x;
        y = new_y;
        ArenaViewer.GetMap()[y][x].visit();
        updateSensorPosition();
        // System.out.println("updatePosition [" + getX() + ", " + getY() + "]");
        int moveLength = FromFwdCmdToInt(lastCmd);
        if ((lastCmd[0] == 0x10 || lastCmd[0] == 0x11) && moveLength > 10){
            // System.out.println(moveLength);
            lastCmd = FromIntToFwdCmd(moveLength -= 10);
            updatePostion();
        }
    }

    private void updateOrientation() {
        // Update Turn Direction
        switch (direction) {
            case NORTH:
                if (lastTurn == MovementDirection.LEFT)
                    direction = Direction.WEST;
                else
                    direction = Direction.EAST;
                break;
            case EAST:
                if (lastTurn == MovementDirection.LEFT)
                    direction = Direction.NORTH;
                else
                    direction = Direction.SOUTH;
                break;
            case SOUTH:
                if (lastTurn == MovementDirection.LEFT)
                    direction = Direction.EAST;
                else
                    direction = Direction.WEST;
                break;
            case WEST:
                if (lastTurn == MovementDirection.LEFT)
                    direction = Direction.SOUTH;
                else
                    direction = Direction.NORTH;
                break;
        }
        lastTurn = null;
        updateSensorPosition();
    }

    public Sensor[] getSensors() {
        return (new Sensor[] { RR, LR, FC, FL, FR, RL });
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void delete() {
        try {
            sender.disconnect();
            listener.disconnect();
            if (!isReal) {
                spoofListener.disconnect();
                spoofSender.disconnect();
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    public boolean hasPhantom(ArenaCell[][] map) {
        Sensor[] sensors = getSensors();
        for (Sensor s : sensors) {
            if (s.hasPhantom(map))
                return true;
        }
        return false;
    }

    public boolean canCalibrateForward(ArenaCell[][] map) {
        Coordinate mid = getForwardCoord();
        byte null_count = 0, obstacle_count = 0;

        if (ArenaCell.isValidCoords(mid.getX(), mid.getY())) {
            if (map[mid.getY()][mid.getX()].isObstacle()) {
                obstacle_count += 1;
            }
        } else {
            null_count += 1;
        }

        switch (direction) {
            case NORTH:
            case SOUTH:
                if (ArenaCell.isValidCoords(mid.getX() + 1, mid.getY())) {
                    if (map[mid.getY()][mid.getX() + 1].isObstacle()) {
                        obstacle_count += 1;
                    }
                } else {
                    null_count += 1;
                }
                if (ArenaCell.isValidCoords(mid.getX() - 1, mid.getY())) {
                    if (map[mid.getY()][mid.getX() - 1].isObstacle()) {
                        obstacle_count += 1;
                    }
                } else {
                    null_count += 1;
                }
                break;
            case EAST:
            case WEST:
                if (ArenaCell.isValidCoords(mid.getX(), mid.getY() + 1)) {
                    if (map[mid.getY() + 1][mid.getX()].isObstacle()) {
                        obstacle_count += 1;
                    }
                } else {
                    null_count += 1;
                }
                if (ArenaCell.isValidCoords(mid.getX(), mid.getY() - 1)) {
                    if (map[mid.getY() - 1][mid.getX()].isObstacle()) {
                        obstacle_count += 1;
                    }
                } else {
                    null_count += 1;
                }
                break;
        }
        return (null_count == 3 || obstacle_count == 3);
    }

    public boolean canCalibrateRight(ArenaCell[][] map) {
        int[] result = rightWallObstacleCount(map);
        return (result[0] == 3 || result[1] == 3);
    }
    public boolean canTakePhotoRight(ArenaCell[][] map){
        int[] result = rightWallObstacleCount(map);
        return (result[1] > 0);
    }
    public boolean canCalibrateLeft(ArenaCell[][] map){
        Coordinate mid = getLeftCoord();
        byte null_count = 0, obstacle_count = 0;

        if (ArenaCell.isValidCoords(mid.getX(), mid.getY())) {
            if (map[mid.getY()][mid.getX()].isObstacle()) {
                obstacle_count += 1;
            }
        } else {
            null_count += 1;
        }

        switch (direction) {

            case EAST:
            case WEST:
                if (ArenaCell.isValidCoords(mid.getX() + 1, mid.getY())) {
                    if (map[mid.getY()][mid.getX() + 1].isObstacle()) {
                        obstacle_count += 1;
                    }
                } else {
                    null_count += 1;
                }
                if (ArenaCell.isValidCoords(mid.getX() - 1, mid.getY())) {
                    if (map[mid.getY()][mid.getX() - 1].isObstacle()) {
                        obstacle_count += 1;
                    }
                } else {
                    null_count += 1;
                }
                break;
            case NORTH:
            case SOUTH:
                if (ArenaCell.isValidCoords(mid.getX(), mid.getY() + 1)) {
                    if (map[mid.getY() + 1][mid.getX()].isObstacle()) {
                        obstacle_count += 1;
                    }
                } else {
                    null_count += 1;
                }
                if (ArenaCell.isValidCoords(mid.getX(), mid.getY() - 1)) {
                    if (map[mid.getY() - 1][mid.getX()].isObstacle()) {
                        obstacle_count += 1;
                    }
                } else {
                    null_count += 1;
                }
                break;
        }
        return (null_count == 3 || obstacle_count == 3);
    }
    private int[] rightWallObstacleCount(ArenaCell[][] map){ //Null, Obstacle
        Coordinate mid = getRightCoord();
        byte null_count = 0, obstacle_count = 0;

        if (ArenaCell.isValidCoords(mid.getX(), mid.getY())) {
            if (map[mid.getY()][mid.getX()].isObstacle()) {
                obstacle_count += 1;
            }
        } else {
            null_count += 1;
        }

        switch (direction) {

            case EAST:
            case WEST:
                if (ArenaCell.isValidCoords(mid.getX() + 1, mid.getY())) {
                    if (map[mid.getY()][mid.getX() + 1].isObstacle()) {
                        obstacle_count += 1;
                    }
                } else {
                    null_count += 1;
                }
                if (ArenaCell.isValidCoords(mid.getX() - 1, mid.getY())) {
                    if (map[mid.getY()][mid.getX() - 1].isObstacle()) {
                        obstacle_count += 1;
                    }
                } else {
                    null_count += 1;
                }
                break;
            case NORTH:
            case SOUTH:
                if (ArenaCell.isValidCoords(mid.getX(), mid.getY() + 1)) {
                    if (map[mid.getY() + 1][mid.getX()].isObstacle()) {
                        obstacle_count += 1;
                    }
                } else {
                    null_count += 1;
                }
                if (ArenaCell.isValidCoords(mid.getX(), mid.getY() - 1)) {
                    if (map[mid.getY() - 1][mid.getX()].isObstacle()) {
                        obstacle_count += 1;
                    }
                } else {
                    null_count += 1;
                }
                break;
        }
        return new int[]{null_count, obstacle_count};
    }

    public MovementDirection directionToNorth() {
        return directionToX(Direction.NORTH);
    }

    public boolean turnToNorth() {
        return turnToX(Direction.NORTH);
    }

    public MovementDirection directionToX(Direction targetDirection) {
        if (direction == targetDirection)
            return null;
        switch (direction) {
            case NORTH:
                switch (targetDirection) {
                    case EAST:
                        return MovementDirection.RIGHT;
                    case WEST:
                        return MovementDirection.LEFT;
                    default:
                        return null;
                }
            case EAST:
                switch (targetDirection) {
                    case NORTH:
                        return MovementDirection.LEFT;
                    case SOUTH:
                        return MovementDirection.RIGHT;
                    default:
                        return null;
                }
            case SOUTH:
                switch (targetDirection) {
                    case EAST:
                        return MovementDirection.LEFT;
                    case WEST:
                        return MovementDirection.RIGHT;
                    default:
                        return null;
                }
            case WEST:
                switch (targetDirection) {
                    case NORTH:
                        return MovementDirection.RIGHT;
                    case SOUTH:
                        return MovementDirection.LEFT;
                    default:
                        return null;
                }
        }
        return null;
    }
    public boolean turnToX(Direction targetDirection){
        MovementDirection turnDirection = directionToX(targetDirection);
        if (turnDirection == null) return false;
        turn(turnDirection);
        return true;
    }

    public static MovementDirection DirectionToX(Direction source_facing, Coordinate source, Coordinate target){
        int x_diff = target.getX() - source.getX();
        int y_diff = target.getY() - source.getY();
        switch(source_facing){
            case NORTH:
                if (y_diff == 0){
                    if (x_diff > 0){
                        return MovementDirection.RIGHT;
                    }else if (x_diff < 0){
                        return MovementDirection.LEFT;
                    }
                }
                break;
            case EAST:
                if (x_diff == 0){
                    if (y_diff > 0){
                        return MovementDirection.LEFT;
                    }else if (y_diff < 0){
                        return MovementDirection.RIGHT;
                    }
                }
                break;
            case SOUTH:
                if (y_diff == 0){
                    if (x_diff > 0){
                        return MovementDirection.LEFT;
                    }else if (x_diff < 0){
                        return MovementDirection.RIGHT;
                    }
                }
                break;
            case WEST:
                if (x_diff == 0){
                    if (y_diff > 0){
                        return MovementDirection.RIGHT;
                    }else if (y_diff < 0){
                        return MovementDirection.LEFT;
                    }
                }
                break;
        }
        return null;
    }
    public static boolean IsFacingTarget(Direction source_facing, Coordinate source, Coordinate target){
        int x_diff = target.getX() - source.getX();
        int y_diff = target.getY() - source.getY();
        switch (source_facing){
            case NORTH:
                return (x_diff == 0 && y_diff > 0);
            case SOUTH:
                return (x_diff == 0 && y_diff < 0);
            case EAST:
                return (x_diff > 0 && y_diff == 0);
            case WEST:
                return (x_diff < 0 && y_diff == 0);
        }
        return false;
    }
    public static Direction DirectionAfterTurning(Direction source, MovementDirection turn){
        switch(source){
            case NORTH:
                switch(turn){
                    case LEFT: return Direction.WEST;
                    case RIGHT: return Direction.EAST;
                    default: return null;
                }
            case EAST:
                switch(turn){
                    case LEFT: return Direction.NORTH;
                    case RIGHT: return Direction.SOUTH;
                    default: return null;
                }
            case SOUTH:
                switch(turn){
                    case LEFT: return Direction.EAST;
                    case RIGHT: return Direction.WEST;
                    default: return null;
                }
            case WEST:
                switch(turn){
                    case LEFT: return Direction.SOUTH;
                    case RIGHT: return Direction.NORTH;
                    default: return null;
                }
        }
        return null;
    }
    public static Direction Opposite(Direction dir){
        switch(dir){
            case NORTH: return Direction.SOUTH;
            case EAST: return Direction.WEST;
            case SOUTH: return Direction.NORTH;
            case WEST: return Direction.EAST;
        }
        return null;
    }
}
