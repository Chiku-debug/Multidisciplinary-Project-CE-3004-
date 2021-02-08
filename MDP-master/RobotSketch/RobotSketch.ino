//#include "DualVNH5019MotorShield.h"
#include "Motor.h"
#include "IRSensor.h"
#include "CommandQ.h"
#include <PinChangeInterrupt.h>

#define IS_DEBUGGING false
#define DO_PRINT_LOOP IS_DEBUGGING
#define PRINT_DEBUG_LINES IS_DEBUGGING

#define DEMO_MODE false
#define RANGING false

#define CMD_LENGTH 4 //CMD, LEFT_STR, RIGHT_STR, NULL

#define M1_A 19
#define M1_B 21
#define M2_A 3
#define M2_B 5

#define PS1 A0
#define PS2 A1
#define PS3 A2
#define PS4 A3
#define PS5 A4
#define PS6 A5

#define E_BRAKE_DIST 11.15
#define CALIBRATE_TIMEOUT 15000

byte cmd[CMD_LENGTH]; //= {0, 0, 0};
bool WATCHER = false;

Motor m1 = Motor(2, 4, 9, 6, A0, true);    //A-3, B-5
Motor m2 = Motor(7, 8, 10, 12, A1, false); //A-11, B-13

IRSensor sensors[6] = {
    IRSensor(PS1, 0.000073402135, 0, -20), IRSensor(PS2, 0.0002531327648, -80), IRSensor(PS3, 0.0001734804878, 0, -2.5),
    IRSensor(PS4, 0.0001856807832, -0.0002, -4.65), IRSensor(PS5, 0.0002412773293, -15, -2.8), IRSensor(PS6, 0.000215348665, -20)};

CommandQ cmdq = CommandQ();

unsigned long START_CALIBRATE = 0;

#define FrontLeft (sensors[3])
#define FrontRight (sensors[4])
#define FrontCenter (sensors[2])
#define Left (sensors[1])
#define BackRight (sensors[0])
#define Right (sensors[5])

#define CalibrateTimes (64) // + //random(0, 1024))
//#define Ranging (sensors[5])

#define SensorCount 6

#if DEMO_MODE
byte SEQUENCE_COUNTER = 0;
/*
  //90 Degrees Turn
  byte DEMO_SEQUENCE[][3] = {
    {0x40, 0x00, 0x00}, //Forward until Obstacle
    {0x04, 0x00, 0x5A}, //Rotate 90 Anti
    {0x10, 0x00, 0x1E}, //Forward 30
    {0x07, 0x00, 0x5A}, //Rotate 90 Clock
    {0x10, 0x00, 0x3C}, //Forward 60
    {0x07, 0x00, 0x5A}, //Rotate 90 Clock
    {0x10, 0x00, 0x1E}, //Forward 30
    {0x04, 0x00, 0x5A}, //Rotate 90 Anti
    (0x10, 0x00, 0x01), //Forward 1cm
  };
  */
/*
  //45 Degrees Turns
  byte DEMO_SEQUENCE[][3] = {
    {0x40, 0x00, 0x00}, //Forward until Obstacle
    {0x04, 0x00, 0x2D}, //Rotate 45 Anti
    {0x10, 0x00, 0x2B}, //Forward 43
    {0x07, 0x00, 0x5A}, //Rotate 90 Clock
    {0x10, 0x00, 0x2B}, //Forward 43
    {0x04, 0x00, 0x2D}, //Rotate 45 Anti
    (0x10, 0x00, 0x01), //Forward 1cm
  };
  */
//Move forward 50 cm
byte DEMO_SEQUENCE[][3] = {
    //{0x80, 0x00, 0x01},
    //{0x40, 0x00, 0x00},
    //{0x04, 0x00, 0x5a},
    {0x10, 0x00, 0x0a},
    {0x10, 0x00, 0x0a},
    {0x10, 0x00, 0x0a},
    {0x10, 0x00, 0x0a},
    {0x10, 0x00, 0x0a},
    {0x10, 0x00, 0x0a},
    {0x10, 0x00, 0x0a},
    {0x10, 0x00, 0x0a},
    {0x10, 0x00, 0x0a},
    {0x10, 0x00, 0x0a},
    {0x10, 0x00, 0x0a},
    {0x10, 0x00, 0x0a},
    {0x10, 0x00, 0x0a},
    //{0x07, 0x00, 0x5a},
    {0xFF, 0x00, 0x00}};
#endif

bool auto_explore = false;

bool do_obstacle_check = false;
bool is_calibrating = false;
bool calibrate_done = false;
byte calibrate_mode = 0;
bool wall_calibrate_flag = false;
bool movement_flag = false;
bool purpose_turn_flag = false;
bool forward_flag = false;

void debugPrint(String x)
{
#if IS_DEBUGGING
  Serial.println(x);
#endif
  return;
}

void stopIfFault()
{
  if (m1.getFault())
  {
    while (1)
      Serial.println("Motor 1 Fault!");
  }
  if (m2.getFault())
  {
    while (1)
      Serial.println("Motor 2 Fault!");
  }
}
#pragma region INIT_INTERRUPT
void _INT_M1_A_FALL()
{
  attachPCINT((M1_A), _INT_M1_A_RISE, RISING);
  m1.onAFall();
}
void _INT_M1_A_RISE()
{
  attachPCINT((M1_A), _INT_M1_A_FALL, FALLING);
  m1.onARise();
}
void _INT_M1_B_FALL()
{
  attachPCINT((M1_B), _INT_M1_B_RISE, RISING);
  m1.onBFall();
}
void _INT_M1_B_RISE()
{
  attachPCINT((M1_B), _INT_M1_B_FALL, FALLING);
  m1.onBRise();
}
void _INT_M2_A_FALL()
{
  attachPCINT((M2_A), _INT_M2_A_RISE, RISING);
  m2.onAFall();
}
void _INT_M2_A_RISE()
{
  attachPCINT((M2_A), _INT_M2_A_FALL, FALLING);
  m2.onARise();
}
void _INT_M2_B_FALL()
{
  attachPCINT((M2_B), _INT_M2_B_RISE, RISING);
  m2.onBFall();
}
void _INT_M2_B_RISE()
{
  attachPCINT((M2_B), _INT_M2_B_FALL, FALLING);
  m2.onBRise();
}

void initInterrupt()
{
  pinModeFast(3, INPUT);
  pinModeFast(5, INPUT);
  pinModeFast(11, INPUT);
  pinModeFast(13, INPUT);
  attachPCINT((M1_A), _INT_M1_A_FALL, FALLING);
  attachPCINT((M1_B), _INT_M1_B_FALL, FALLING);
  attachPCINT((M2_A), _INT_M2_A_FALL, FALLING);
  attachPCINT((M2_B), _INT_M2_B_FALL, FALLING);
}
#pragma endregion
int bytesToInt(byte high, byte low)
{
  return ((((int)high) << 8) | low);
}

void setup()
{
  // put your setup code here, to run once:
  Serial.begin(115200);
  Serial.setTimeout(15); //15 ms timeout
  m1.setOtherMotor(&m2);
  m2.setOtherMotor(&m1);
  initInterrupt();
  //Calibrate Sensors
  double *values = NULL;
  (values) = (new double[3]{17, 27, 41.5});
  FrontLeft.calibrate(true, values);
  (values) = (new double[3]{17, 25, 39.5});
  FrontCenter.calibrate(true, values);
  (values) = (new double[3]{16, 25, 39.5});
  FrontRight.calibrate(true, values);
  (values) = (new double[7]{5, 15, 20, 33, 42, 52, 62});
  BackRight.calibrate(false, values);
  (values) = (new double[3]{14.5, 27, 31.65});
  Left.calibrate(true, values);
  (values) = (new double[3]{14.5, 27, 33});
  Right.calibrate(true, values);
  Serial.println("R");
#if DEMO_MODE
  for (byte i = 0; i < 3; i++)
  {
    cmd[i] = DEMO_SEQUENCE[SEQUENCE_COUNTER][i];
  }
  WATCHER = true;
  //serialEvent();
  decodeCommand();
#endif
}

void handleSetRPM()
{
  //Execute Command
  //if (PRINT_DEBUG_LINES) Serial.println((String)"Setting M1, " + (double)cmd[1] + "RPM, " + ((cmd[0] & 0x01) ? "reversed" : "forward"));
  //if (PRINT_DEBUG_LINES) Serial.println((String)"Setting M2, " + (double)cmd[2] + "RPM, " + ((cmd[0] & 0x02) ? "reversed" : "forward"));
  m1.setRPM((double)cmd[1], (cmd[0] & 0x01));
  m2.setRPM((double)cmd[2], (cmd[0] & 0x02));
}
void handleTurn()
{
  int turnDeg = bytesToInt(cmd[1], cmd[2]);
  //if (PRINT_DEBUG_LINES) Serial.println((String)"Turning1: " + turnDeg);
  if (cmd[0] == 0x04) turnDeg = turnDeg * -1;
  if (cmd[0] == 0x04){
    m1.turnReady(turnDeg);
    m2.turnReady(turnDeg);
  }else{
    m1.turnReady_L(turnDeg);
    m2.turnReady_L(turnDeg);
  }
  
  m1.turnRun();
  m2.turnRun();
  WATCHER = true;
  movement_flag = true;
}
void handleTurn2()
{
  int noTurn = bytesToInt(cmd[1], cmd[2]);
  //if (PRINT_DEBUG_LINES) Serial.println((String)"Turning2: " + noTurn);
  m1.turn2(noTurn);
  m2.turn2(noTurn);
  WATCHER = true;
  movement_flag = true;
}
void handleForward(bool isFastest = false)
{
  int dist = bytesToInt(cmd[1], cmd[2]);
  //if (PRINT_DEBUG_LINES) Serial.println((String)"Forward: " + dist);
  if (dist >= 10)
    forward_flag = true;
  m1.forwardReady(dist);
  m2.forwardReady(dist);
  m1.forwardRun(isFastest);
  m2.forwardRun(isFastest);
  WATCHER = true;
  movement_flag = true;
}
void handleFastestForward()
{
  return handleForward(true);
}
void debug()
{
  //Set Speed
  m1.setSpeed(cmd[1], cmd[0] & 0x1);
  m2.setSpeed(cmd[2], cmd[0] & 0x2);
}
void handleSensorRead()
{
  for (unsigned long i = 0; i < CalibrateTimes; i++)
    sensorLoop();
  if (cmd[0] & 0x10)
  { //Print String
    Serial.println(
        String(sensors[0].getUnitCM()) + "," +
        String(sensors[1].getUnitCM()) + "," +
        String(sensors[2].getUnitCM()) + "," +
        String(sensors[3].getUnitCM()) + "," +
        String(sensors[4].getUnitCM()) + "," +
        String(sensors[5].getUnitCM()));
  }
  else
  { //Print in bytes
    byte buf[] = {
        (sensors[0].getUnitCM()),
        (sensors[1].getUnitCM()),
        (sensors[2].getUnitCM()),
        (sensors[3].getUnitCM()),
        (sensors[4].getUnitCM()),
        (sensors[5].getUnitCM()),
    };
    Serial.write(buf, SensorCount);
    Serial.println();
  }
}
void handleRunUntilObstacle()
{
  do_obstacle_check = true;
  forward_flag = true;
  m1.forwardReady(10000);
  m2.forwardReady(10000);
  m1.forwardRun();
  m2.forwardRun();
  WATCHER = true;
}
#define TOLOERANCE_DIST 0.1 //Tolerance distance
void handleCalibrateFront()
{
  //Serial.println("calibrate front");
  calibrate_mode = 1;
  calibrate_done = false;
  //Take Front-Middle as reference point
  if (purpose_turn_flag)
  {
    purpose_turn_flag = false;
    WATCHER = true;
    //handleCalibrateFront = false;
    byte myCmd[3] = {0x04, 0x00, 0x14};
    cmd[0] = myCmd[0];
    cmd[1] = myCmd[1];
    cmd[2] = myCmd[2];
    decodeCommand();
    return;
  }
  for (unsigned int i = 0; i < CalibrateTimes; i++)
  {
    sensorLoop();
  }
  double center = FrontCenter.getCM();
  double left = FrontLeft.getCM();
  double right = FrontRight.getCM();
#if IS_DEBUGGING
  Serial.println(String("Front: ") + center + " Left: " + left + " Right: " + right);
#endif
  //Check if |left - middle| >= 2
  //double diff = abs(left - center);
  double diff2 = abs(left - right);
  if (/*diff >= TOLOERANCE_DIST ||*/ diff2 >= TOLOERANCE_DIST)
  {
    double degree = 0;
    if (/*left < center || center < right ||*/ left < right)
    {               //left < center < right
      degree = 0.1; //Turn Right
    }
    else if (/*right < center || center < left ||*/ right < left)
    {                //right < center < left
      degree = -0.1; //Turn Left
    }
    WATCHER = true;
    movement_flag = true;
    m1.turnReady(degree);
    m2.turnReady(degree);
    m1.turnRun();
    m2.turnRun();
  }
  else
  {
    //Detected correct value, check again
    for (unsigned int i = 0; i < CalibrateTimes; i++)
    {
      sensorLoop();
    }
    left = FrontLeft.getCM();
    right = FrontRight.getCM();
    diff2 = abs(left - right);
    WATCHER = true;
    if (diff2 <= TOLOERANCE_DIST)
    {
      calibrate_mode = 11; //handleCalibrateFront3()
    }
  }
}
int calibrate_side_counter = 0;
#define TIMES_BEFORE_SWITCH 32
void handleCalibrateSide()
{
  calibrate_mode = 2;
  calibrate_done = false;
  /*
  if (calibrate_side_counter++ >= TIMES_BEFORE_SWITCH && wall_calibrate_flag == false){
    debugPrint("Switched!");
    calibrate_side_counter = 0;
    wall_calibrate_flag = true;
    cmd[0] = 0x07;
    cmd[1] = 0x00;
    cmd[2] = 35;
    return serialEvent();
  }
  */
  for (unsigned int i = 0; i < (CalibrateTimes); i++)
  {
    sensorLoop();
  }
  double left = Right.getCM();
  double right = BackRight.getCM();
  if (wall_calibrate_flag && left > 5)
    left = 5 + (left - 5) / 2;
  else if (wall_calibrate_flag == false)
  {
    //right += 2.5;
    //if (right > 20) right += 1.5;
    right += 2.5;
  }
  double diff = abs(left - right);
#if IS_DEBUGGING
  Serial.println(String("Left: ") + left + " Right: " + right);
#endif
  if (diff > TOLOERANCE_DIST)
  {
    if (left < right)
    {
      cmd[0] = 0x07;
    }
    else
    {
      cmd[0] = 0x04;
    }
    cmd[1] = 0;
    cmd[2] = 1;
    WATCHER = true;
    //serialEvent();
    decodeCommand();
  }
  else
  {
    calibrate_done = true;
    WATCHER = true;
    if (wall_calibrate_flag)
      wall_calibrate_flag = false;
    else
    {
      //cmd[0] = 0x04;
      //cmd[1] = 0x00;
      //cmd[2] = 0x07;
      //serialEvent();
      // cmd[0] = 0x10;
      // cmd[1] = 0xff;
      // cmd[2] = 5;
      // decodeCommand();
    }
  }
}

void handleAutoExplore()
{
  auto_explore = true;
  if (Right.getCM() > 15 && BackRight.getCM() > 15)
  { //Right is Empty
    cmd[0] = 0x07;
    cmd[1] = 0;
    cmd[2] = 90;
  }
  else if (false)
  {
  }
  serialEvent();
}

void handleRunCalibrate()
{
  is_calibrating = true;
  calibrate_done = false;
  movement_flag = true;
  START_CALIBRATE = millis();
  switch (cmd[2])
  {
  case 0x02:
    return handleAutoExplore();
  case 0x03:
    wall_calibrate_flag = true;
  case 0x01:
    return handleCalibrateSide();
  case 0x00:
  default:
    purpose_turn_flag = false;
    calibrate_mode = 1;
    return handleCalibrateFront();
  }
}
void decodeCommand()
{
#if IS_DEBUGGING
  Serial.print("Command: ");
  Serial.println(String(cmd[0]) + " " + cmd[1] + " " + cmd[2]);
#endif
  if ((cmd[0] & 0xFF) == 0xFF)
  {
    calibrate_done = true;
    is_calibrating = true;
    return;
  }
  if (cmd[0] & 0x80)
  {
    return handleRunCalibrate();
  }
  if (cmd[0] & 0x40)
  {
    return handleRunUntilObstacle();
  }
  if (cmd[0] & 0x20)
  {
    return handleSensorRead();
  }
  if (cmd[0] == 0x11)
  {
    return handleFastestForward();
  }
  if (cmd[0] == 0x10)
  {
    return handleForward();
  }
  if (cmd[0] & 0x08)
  {
    //if (cmd[0] == 0x07) cmd[0] = 0x04;
    //if (cmd[0] == 0x04) cmd[0] = 0x07;
    return handleTurn();
  }
  if ((cmd[0] & 0x04) == false)
  {
    return handleSetRPM();
  }
  if (cmd[0] & 0x04)
  {
    return handleTurn();
  }
}

bool isIn = false;
unsigned short int serialEventCounter = 0;
void serialEvent()
{
  if (isIn)
  {
    serialEventCounter++;
    return;
  }
  isIn = true;
  if (Serial.available())
  {
    Serial.readBytes(cmd, 4);
    cmdq.enqueue(&cmd[0]);
    WATCHER = true;
  }
  isIn = false;
  if (serialEventCounter != 0)
  {
    serialEventCounter -= 1;
    serialEvent;
  }
}
#pragma region printLoop
unsigned long last_printed;
unsigned int print_delay = 250;

double sensor_min = 9999;
double sensor_max = 0;

double s2_min = 9999;
double s2_max = 0;
void printLoop()
{
#if DO_PRINT_LOOP

  if (DO_PRINT_LOOP == false)
    return;
  if (millis() - last_printed < print_delay)
    return;

  /*
  double sensor_val = Ranging.read();
  if (sensor_min > sensor_val) sensor_min = sensor_val;
  if (sensor_max < sensor_val) sensor_max = sensor_val;
  Serial.println("Left: " + String(sensor_val) + ", Min: " + String(sensor_min) + ", Max: " + String(sensor_max));
  */
  /*
  double v2 = FrontRight.read();
  if (s2_min > v2) s2_min = v2;
  if (s2_max < v2) s2_max = v2;
  Serial.println("Right: " + String(v2) + ", Min: " + String(s2_min) + ", Max: " + String(s2_max));
  */

  //Serial.print("M1 [" + m1.toString() + "], ");
  //Serial.println("M2 [" + m2.toString() + "]");
  /*
  Serial.print("Left: ");
  Serial.println(String(FrontLeft.read()) + "units, " + String(FrontLeft.getCM()) + "CM");
  Serial.print("Right: ");
  Serial.println(String(FrontRight.read()) + "units, " + String(FrontRight.getCM()) + "CM");
  */
  last_printed = millis();

#endif
}
#pragma endregion

unsigned long sensor_counter = 0;
unsigned long sensorLastRead = 0;
#define SENSOR_READ_DELAY 500
void sensorLoop()
{
  sensors[sensor_counter % SensorCount].readLoop();
  sensor_counter = (sensor_counter + 1) % SensorCount;
}
unsigned int MIN_DIST = 15;
void obstacleCheckLoop()
{
  if (do_obstacle_check == false)
    return;
  if (FrontCenter.getCM() <= MIN_DIST || FrontLeft.getCM() <= MIN_DIST || FrontRight.getCM() <= MIN_DIST)
  {
    m1.brake();
    m2.brake();
    do_obstacle_check = false;
  }
}

void handleCalibrateFront2()
{ //Not used
  is_calibrating = true;
  calibrate_done = false;
  calibrate_mode = 1; //Switch to handleCalibrateFront
  double center = FrontCenter.getCM();
  cmd[0] = 0x10;
  cmd[1] = 0xff;
  cmd[2] = ((center > 9) ? 0xff : 0xfe); // 0xfe;
  decodeCommand();
}

void handleCalibrateFront3()
{
  calibrate_mode = 11;
  calibrate_done = false;
  WATCHER = true;
  double center = FrontCenter.getCM();

  // if (round(center) == 9){
  //   calibrate_done = true;
  // }else{
  //   // double offset = center - 9;
  //   double offset = center > 9 ? 0.01 : -0.01;
  //   movement_flag = true;
  //   m1.forwardReady(offset);
  //   m2.forwardReady(offset);
  //   m1.forwardRun();
  //   m2.forwardRun();
  // }

  if (center < 9.0)
  {
    calibrate_mode = 1;
    // cmd[0] = 0x10;cmd[1] = 0xFF;cmd[2] = 0xFF;decodeCommand();
    movement_flag = true;
    m1.forwardReady(-0.1);
    m2.forwardReady(-0.1);
    m1.forwardRun();
    m2.forwardRun();
  }
  else if (center > 9.25)
  {
    calibrate_mode = 1;
    movement_flag = true;
    // cmd[0] = 0x10;cmd[1] = 0x00;cmd[2] = 0x01;decodeCommand();
    m1.forwardReady(0.1);
    m2.forwardReady(0.1);
    m1.forwardRun();
    m2.forwardRun();
  }
  else
  {
    calibrate_done = true;
  }
  //calibrate_done = true;
  //cmd[0] = 0x10;
  //cmd[1] = 0xff;
  //cmd[2] = 0xff;
  //decodeCommand();
}

void continueCalibrate()
{
  //Serial.println("Continue Calibrate");
  switch (calibrate_mode)
  {
  case 1:
    return handleCalibrateFront();
  case 2:
    return handleCalibrateSide();
  case 10: //Run till 8cm
    return handleCalibrateFront2();
  case 11: //Run till 10cm / Reverse 5cm
    return handleCalibrateFront3();
  }
  return;
}

void ebrake()
{
  if (!forward_flag)
    return;
  // Serial.println("E BRAKE");
  double front = FrontCenter.getCMRawFast();
  // Serial.println(front);
  if (front <= E_BRAKE_DIST)
  {

    m1.brake();
    m2.brake();
    WATCHER = true;
  }
}

void watcher()
{
  if (WATCHER == false)
    return;
  //Check for m1 & m2 is turning finished
  if (m1.getIsIdle() && m2.getIsIdle())
  {
    WATCHER = false;
    m1.brake();
    m2.brake();
    if (is_calibrating && !calibrate_done)
    {
      if (millis() - START_CALIBRATE >= CALIBRATE_TIMEOUT)
      {
        calibrate_done = true;
      }
    }
    //Serial.println("Done!");
    if (is_calibrating && calibrate_done)
    {
      is_calibrating = false;
      calibrate_done = false;
      WATCHER = true;
      //movement_flag = true;
      // cmd[0] = 0x20;
      // handleSensorRead();
    }
    else if (is_calibrating)
    {
      return continueCalibrate();
    }
#if IS_DEBUGGING
    Serial.println("Motors IDLE");
#endif
    if (forward_flag && movement_flag)
    {
      forward_flag = false;
      movement_flag = true;
      // WATCHER = true;
      // m1.turnReady(0.75);
      // m2.turnReady(0.75);
      // m1.turnRun();
      // m2.turnRun();
      // }else if (!forward_flag && movement_flag){
    }
    if (!forward_flag && movement_flag)
    {
      movement_flag = false;
      //Serial.write(m1.getDistCounter() > m2.getDistCounter() ? m1.getDistCounter() : m2.getDistCounter() );
      //Serial.println();
      cmd[0] = 0x20;
      handleSensorRead();
    }
    if (cmdq.isEmpty() == false)
    {
      CommandQUnit *nextCmd = cmdq.dequeue();
      byte *temp = nextCmd->getCmd();
      cmd[0] = temp[0];
      cmd[1] = temp[1];
      cmd[2] = temp[2];
      delete (temp);
      delete (nextCmd);
      decodeCommand();
    }
#if DEMO_MODE
    //Load Next Sequence
    SEQUENCE_COUNTER++;
    for (byte i = 0; i < 3; i++)
    {
      cmd[i] = DEMO_SEQUENCE[SEQUENCE_COUNTER][i];
    }
    WATCHER = true;
    decodeCommand();
    //serialEvent();
#endif
  }
}

void queueLoop() {}

void ranging()
{
  // Serial.print("Modified-> ");
  Serial.print(String("FrontLeft: ") + FrontLeft.getUnitCM2());
  Serial.print(String("FrontCenter: ") + FrontCenter.getUnitCM2());
  Serial.print(String("FrontRight: ") + FrontRight.getUnitCM2());

  // Serial.print(String("FrontLeft: ") + FrontLeft.getCM());
  // Serial.print(String("FrontCenter: ") + FrontCenter.getCM());
  // Serial.print(String("FrontRight: ") + FrontRight.getCM());
  // Serial.print(String("FrontRight: ") + FrontRight.getUnitCM());
  // Serial.print(String("LeftRight: ") + Left.getUnitCM());
  // Serial.print(String("RightLeft: ") + Right.getUnitCM());
  // Serial.print(String("RightRight: ") + BackRight.getUnitCM());
  Serial.println();
  Serial.print("Raw-> ");
  Serial.print(String("FrontLeft: ") + FrontLeft.getCMRaw());
  Serial.print(String("FrontCenter: ") + FrontCenter.getCMRaw());
  Serial.print(String("FrontRight: ") + FrontRight.getCMRaw());
  // Serial.print(String("FrontLeft: ") + FrontLeft.getCMRawByte());
  // Serial.print(String("FrontRight: ") + FrontRight.getCMRawByte());
  // Serial.print(String("LeftRight: ") + Left.getCMRaw());
  // Serial.print(String("RightLeft: ") + Right.getCMRaw());
  // Serial.print(String("RightRight: ") + BackRight.getCMRawByte());
  // Serial.print(String("FrontLeft: ") + FrontLeft.getCMRaw());
  // Serial.print(String("FrontCentert: ") + FrontCenter.getCMRaw());
  // Serial.print(String("FrontRight: ") + FrontRight.getCMRaw());
  Serial.println();
  //FrontCenter.getUnitCM();
  delay(250);
}

void loop()
{

//queueLoop();
#if RANGING
  for (long i = 0; i < CalibrateTimes; i++)
    sensorLoop();
  ranging();
#else
  ebrake();
  // sensorLoop();
  // m1.loop();
  // sensorLoop();
  // m2.loop();
  // sensorLoop();
  //sensors[5].readLoop();
  // obstacleCheckLoop();
  // sensorLoop();
  //stopIfFault();
  // printLoop();
  // sensorLoop();
  watcher();
  sensorLoop();

/*
  if (Serial.available()) {
    Serial.readBytes(cmd, 4);
    cmdq.enqueue(&cmd[0]);
    WATCHER = true;
  }
  */
#endif
}
