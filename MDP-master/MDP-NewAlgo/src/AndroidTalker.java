import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class AndroidTalker {
    private static final String ToAndroid = "/laptop/android";
    private static final String FromAndroid = "/android/laptop";
    private MqttClient sender, receiver;

    public boolean ignoreCalibrate = false, ignoreTurn = false, ignoreForward = false;

    public AndroidTalker(boolean isReal) {
        String connectionString = isReal ? "tcp://192.168.88.254" : "tcp://127.0.0.1";
        try {
            sender = new MqttClient(connectionString, MqttClient.generateClientId(), new MemoryPersistence());
            receiver = new MqttClient(connectionString, MqttClient.generateClientId(), new MemoryPersistence());

            sender.connect();
            receiver.connect();

            receiver.subscribe(FromAndroid);

            AndroidTalker talker = this;
            receiver.setCallback(new MqttCallback() {
                @Override
                public void messageArrived(String arg0, MqttMessage arg1) throws Exception {
                    onReceive(arg0, arg1.getPayload());
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken arg0) {
                }

                @Override
                public void connectionLost(Throwable arg0) {
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void handleInstruction(String arg0, String arg1){
        if (arg0.charAt(0) == 'X' || arg0.charAt(0) == 'x'){
            switch(arg1){
                case  "Explore":
                    ArenaViewer.RealExplore();
                    break;
                case "Fastest":
                    ArenaViewer.ShortestPath();
                    break;
                case "Calibrate":
                    Explorer.Init(true);
                    Explorer.DoPreCalibrate();
                    break;
            }
        }
    }
    private void handleLocation(String arg0, String arg1){
        //TODO: Handle This Properly, Ignoring for now, assuming Robot Starts at 1,1
    }
    private void handleWaypoint(String arg0, String arg1){
        ArenaViewer.SetWaypoint(arg0, arg1, false);
    }
    private void onReceive(String arg0, byte[] payload) {
        String msg = new String(payload);
        String[] cmd = msg.split("\\|");
        for (int i = 0 ; i < cmd.length; i+=2){
            switch (cmd[i]){
                case "INS":
                    handleInstruction(cmd[i+1], cmd[i+2]);
                    break;
                case "LOC":
                    handleLocation(cmd[i+1], cmd[i+2]);
                    break;
                case "WAY":
                    handleWaypoint(cmd[i+1], cmd[i+2]);
                    break;
            }
        }
    }

    public void send(String arg0) {
        try {
            sender.publish(ToAndroid, arg0.getBytes(), 0, false);
        } catch (MqttPersistenceException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        return;
    }
    public static int ToAndroidDirection(Robot.Direction dir){
        switch(dir){
            case NORTH:
                return 0;
            case WEST:
                return 1;
            case SOUTH:
                return 2;
            case EAST:
                return 3;
        }
        return -1;
    }
}