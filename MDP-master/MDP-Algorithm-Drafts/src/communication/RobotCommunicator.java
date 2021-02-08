package src.communication;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import src.constants.CommunicatorConstants;
import src.enums.DIRECTION;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;


/*
Supposed command is received as: [Byte 0] [Byte 1] [Byte 2]
Special case for Sensors Data Request --> 20 00 00
Byte 0:
    bit 7 - unused
    bit 6 - unused
    bit 5 - unused
    bit 4 - set for "Forward" command (Move forward. Byte 1 & 2 will be used as a signed short int)
    bit 3 - set for "Turning2" command (Turn by number of waves, Used for debugging)
    bit 2 - set for "Turning" command (Turn by degrees. Byte 1 & 2 will be used as a signed short int)
    bit 1 - motor2 reversed? (True if reversed)
    bit 0 - motor1 reversed? (True if reversed)
Byte 1: for input..
Byte 2: for input...
 */
public class RobotCommunicator{

    /*
    Byte 0:
    bit 7 - unused
    bit 6 - unused
    bit 5 - unused
    bit 4 - set for "Forward" command (Move forward. Byte 1 & 2 will be used as a signed short int)
    bit 3 - set for "Turning2" command (Turn by number of waves, Used for debugging)
    bit 2 - set for "Turning" command (Turn by degrees. Byte 1 & 2 will be used as a signed short int)
    bit 1 - motor2 reversed? (True if reversed)
    bit 0 - motor1 reversed? (True if reversed)
    */
    private static String hexStr = "";
    private static byte[] byteCmd = new byte[]{};
    private static HashMap<String, Integer> values = new HashMap<String, Integer>();
    private static boolean calibrateMode = false;
    private static long timeStr = System.currentTimeMillis();
    public static byte[] getCommandBytes(String functionName, Integer stepSize) {
        return hexStringToByteArray(getCommandStr(functionName, stepSize));
    }

    public static String getCommandStr(String functionName, Integer stepSize) {
        String strByte0 = getByte0(functionName);
        if(functionName=="SENSE"){
            return "200000";
        }
        else if(functionName=="FRONTCALIBRATE"){
            return "800000"; //calibrate right --> "800001"
        }
        else if (functionName=="RIGHTCALIBRATE"){
            return "800001";
        }
        else if (functionName=="WALLCALIBRATE"){
            return "800003";
        }
        else{
            String strByte1 = getByte1();
            String strByte2 = getByte2(stepSize);
            String commandStr = "";
            if (strByte2.length() > 2) {
                commandStr = strByte0 + strByte2;
            } else {
                commandStr = strByte0 + strByte1 + strByte2;
            }
            // String commandStr = getByte0(functionName)+getByte1()+getByte2(stepSize);
            return commandStr;
        }
        
    }

    public static String getByte0(String functionName) { // function name
        String newStr = "";
        switch (functionName) {
            case "forward": // bin
                newStr = "00010000";
                break;
            case "turn": // bin
                newStr = "00000100";
                break;
            default:
                newStr = "00000000";
                break;
        }
        newStr = binToHex(newStr);
        return newStr;
    }

    public static String getByte1() { // argument 1
        return "00";
    }

    public static String getByte2(Integer stepSize) { // argument 2
        String newStr = "";
        if (stepSize < 0) { // negative turn
            newStr = bin2Complement(-1 * stepSize);
            newStr = binToHex(newStr);
            newStr = newStr.substring(newStr.length() - 4);

        } else { // forward and postive turn
            newStr = decimalToBin(Integer.toString(stepSize));
            newStr = String.format("%08d", Integer.parseInt(newStr));
            newStr = binToHex(newStr);
        }
        return newStr;
    }

    public static String bin2Complement(Integer number) {
        String newStr = Integer.toBinaryString(flip(number) + 1);
        return newStr;
    }

    public static Integer flip(Integer i) {
        Integer newNum = ~i;
        return newNum;
    }

    public static ArrayList<String> splitBytes(String inputString, Integer splitSize) {
        ArrayList<String> splitList = new ArrayList<>();
        for (int i = 0; i < inputString.length(); i += splitSize) {
            splitList.add(inputString.substring(i, Math.min(inputString.length(), i + splitSize)));
        }
        return splitList;
    }

    public static String binToHex(String inputStr) { // converts given binary string to hex.
        ArrayList<String> splitList = splitBytes((inputStr), 4);
        String finalStr = "";
        for (String str : splitList) {
            finalStr += new BigInteger(str, 2).toString(16);
        }
        return finalStr;
    }

    public static String hexToBin(String hexStr) {
        String binStr = new BigInteger(hexStr, 16).toString(2);
        return binStr;
    }

    public static String decimalToBin(String decimalStr) {
        String newStr = new BigInteger(decimalStr).toString(2);
        return newStr;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String byteArrayToHexString(byte[] byteArr) {
        String newStr = "";
        String hold = "";
        for (byte b : byteArr) {
            hold += b;
            hold = Integer.toHexString(Integer.parseInt(hold));
            hold = hold.substring(hold.length() - 2);
            newStr += hold;
            hold = "";
        }
        return newStr;
    }

    public static ArrayList<Integer> byteArrayToIntegerArray(byte[] byteArr) {
        ArrayList<Integer> newArr = new ArrayList<Integer>();
        for (byte b : byteArr) {
            newArr.add((int) b);
        }
        return newArr;

    }

    public static DIRECTION directionAndroidToLaptop(Integer givenAndroidDir){
        DIRECTION laptopDirection = DIRECTION.NORTH;
        switch(givenAndroidDir){
            case 0:
                laptopDirection = DIRECTION.NORTH;
                break;
            case 1:
                laptopDirection = DIRECTION.WEST;
                break;
            case 2:
                laptopDirection = DIRECTION.SOUTH;
                break;
            case 3:
                laptopDirection = DIRECTION.EAST;
                break;
        }
        return laptopDirection;
    }

    public static Integer directionLaptopToAndroid(DIRECTION givenDir){
        Integer androidDirection = 0;
        switch(givenDir){
            case NORTH:
                androidDirection = 0;
                break;
            case WEST:
                androidDirection = 1;
                break;
            case SOUTH:
                androidDirection = 2;
                break;
            case EAST:
                androidDirection = 3;
                break;
        }
        return androidDirection;
    }

    public static void main(String[] args) {
        Publisher pub02 = new Publisher("pub02");
        
        Subscriber sub02 = new Subscriber("sub02");
        //sub02.subscribeTopic(CommunicatorConstants.fromRobot);
        //pub02.publishBytes(CommunicatorConstants.toRobot, new byte[] {32, 0x00, 0x00});
//        String part2Str = "abc";
//        part2Str = String.format("%1$-" + (76) + "s", part2Str).replace(' ', '0');
//        System.out.println(part2Str);

        hexStr = RobotCommunicator.getCommandStr("forward", 10);
        byteCmd = RobotCommunicator.hexStringToByteArray(hexStr);

        //sub02.subscribeTopic("/robot/laptop");
        sub02.setOnReceive(new MqttCallback() {
            @Override
            public void connectionLost(Throwable throwable) {

            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                System.out.println(System.currentTimeMillis()-timeStr);
                timeStr = System.currentTimeMillis();
                //pub02.publishBytes("/laptop/robot",new byte[]{0});
                //pub02.publishBytes(CommunicatorConstants.toRobot, byteCmd);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
        timeStr = System.currentTimeMillis();
        //pub02.publishBytes("/laptop/robot",new byte[]{0});
        //pub02.publishBytes(CommunicatorConstants.toRobot, byteCmd);
        

    }

}