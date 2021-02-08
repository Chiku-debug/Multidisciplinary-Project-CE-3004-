import paho.mqtt.client as mqtt
import threading

def onConnect(client, userdata, flags, rc):
    print("Connected with result code "+str(rc))
    client.subscribe("/pi/laptop")

def onMessage(client, userdata, msg):
    bytes = msg.payload
    print(msg.topic+":", (" ".join(["{:02x}".format(x) for x in bytes])))

def msgLoop():
    client.loop_forever()

def commandListener():
    #print ("Command: ", end="")
    while True:
        try:
            cmd = input("Enter to send stuff: ")
            cmd = "CAMERA|6|5|0|"
            payload = cmd
            client.publish('/laptop/pi', payload)
        except Exception as e:
            print (e)
            print ("Invalid Input!")
            pass
        


client = mqtt.Client()
client.on_connect = onConnect
client.on_message = onMessage
client.connect("192.168.88.254", 1883, 10)

threading.Thread(group=None, target=msgLoop, daemon=False).start()
threading.Thread(group=None, target=commandListener, daemon=False).start()
