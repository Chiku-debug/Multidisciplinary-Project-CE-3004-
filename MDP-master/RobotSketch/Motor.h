#include <Arduino.h>
#include "digitalWriteFast.h"

#define WIDTH_TO_RPM_CONST 106714.09515340152
#define RPM_TOL 0
#define SYNC_TOL 0
#define SYNC_DELAY 10000
#define SYNC_PENALTY 1
#define SPEED_STEP 1
#define WAVES_PER_REVOLUTION 562.25
//#define DEG_TO_WAVES_MULTIPLIER (4.564089432)
//#define DEG_TO_WAVES_MULTIPLIER (4.451572566)
//#define DEG_TO_WAVES_MULTIPLIER (4.47616688917)
// #define DEG_TO_WAVES_MULTIPLIER (4.271708500727093)
// #define DEG_TO_WAVES_MULTIPLIER (4.175) //Pretty good

// #define DEG_TO_WAVES_MULTIPLIER (4.405642448524345)
//#define DEG_TO_WAVES_MULTIPLIER (4.463767534906371)
//#define DEG_TO_WAVES_MULTIPLIER (4.47816688917)
// #define DEG_TO_WAVES_MULTIPLIER (4.1953125)

//#define CM_TO_WAVES_MULTIPLIER (29.605670103)
//#define CM_TO_WAVES_MULTIPLIER (30.41025641)
//#define CM_TO_WAVES_MULTIPLIER (31.858363858)
//#define CM_TO_WAVES_MULTIPLIER (27.645687645)
// #define CM_TO_WAVES_MULTIPLIER (28.15)
//#define CM_TO_WAVES_MULTIPLIER (29.5)
//#define CM_TO_WAVES_MULTIPLIER (29.78)
// #define CM_TO_WAVES_MULTIPLIER (29.65) //This worked in lounge
//#define CM_TO_WAVES_MULTIPLIER (27.192479651)

#define DEG_TO_WAVES_MULTIPLIER (4.09)
#define DEG_TO_WAVES_MULTIPLIER_L (4.10)
#define CM_TO_WAVES_MULTIPLIER (29.10)

#define TURN_SPEED 120
#define FORWARD_SPEED TURN_SPEED
#define FAST_FORWARD_SPEED 120
#define FAST_FORWARD_MULTI 1.5125

#define ROBOT_RADIUS 18 //CM
#define WHEEL_RADIUS 3
#define WHEEL_CIRCUM (28 + (31 / 113.0))

#define CM_TO_WAVES_MULTIPLIER_10 (1200)
//Working - Normal Speed
// #define DEFAULT_MIN_SPEED 152
// #define DEFAULT_MIN_SPEED_R 150

// #define DEFAULT_MIN_SPEED 163
// #define DEFAULT_MIN_SPEED_R 250

#define DEFAULT_MIN_SPEED 50
#define DEFAULT_MIN_SPEED_R 50

#define MOTOR_START_SPEED_L 215
#define MOTOR_START_SPEED_R 253
//#define PI (355/113)

#define DEACCEL_TURN_COUNTER 850
#define DEACCEL_TARGET_RPM 30

#define DO_DEBUG_PRINT false

#ifndef MOTOR_H
#define MOTOR_H
//#define byte unsigned char

class Motor
{
private:
  byte IN_A, IN_B, PWM, EN, CS;
  volatile byte speed, is_reverse;
  volatile byte MIN_SPEED;
  volatile byte START_SPEED;
  volatile bool IS_LEFT = true;
  volatile bool is_turning = true;
  volatile bool is_counting = false;
  volatile bool is_braking = false;
  volatile bool is_forward = false;
  volatile int targetRPM;
  volatile unsigned long t_a_fall = 0, t_b_fall = 0;
  volatile unsigned long a_width, b_width;
  void init();
  void initInterrupt();
  volatile unsigned long turn_counter;
  volatile unsigned long lastSync = 0;
  Motor *otherMotor = NULL;
  volatile unsigned int dist_counter = 0;

public:
  Motor(byte IN_A, byte IN_B, byte PWM, byte EN, byte CS, bool IS_LEFT = false, byte MIN_SPEED = DEFAULT_MIN_SPEED); //Constructor
  void setOtherMotor(Motor *otherMotor);
  unsigned long getTurnCounter();
  void setSpeed(byte speed, boolean is_reverse); //Set Speed (Motor will keep spinning after call)
  void setSpeedFast(byte speed);
  void brake(byte str = 255); //Set brakes, default to full strength brake
  void brake2();
  boolean getFault();        //Check if theres anything wrong
  void onAFall();            //Interrupt Call on A Fall Edge
  void onBFall();            //Interrupt Call on B Fall Edge
  void onARise();            //Interrupt Call on A Rising Edge
  void onBRise();            //Interrupt Call on B Rising Edge
  void onInt();              //Common method ran on all Falling and Rising Edge for both A and B
  unsigned long getAWidth(); //Returns a_width;
  unsigned long getBWidth(); //Returns b_width;
  double getWidth();         //Returns average of a_width and b_width
  double getRPM();           //Returns RPM calculated using width
  String toString();         //debug String
  double getTargetRPM();
  void setRPM(double rpm, boolean is_reverse);  //Sets RPM, motor will try to reach specified RPM after call
  void setRPM2(double rpm, boolean is_reverse); //Sets RPM, motor will try to reach specified RPM after call
  void loop();                                  //Main loop for Motor
  double getDiff();                             //targetRPM - currentRPM
  double getNewPower(double);
  void turnReady(double deg);                           //Prepare Motor to Turn.
  void turnReady_L(double deg);                           //Prepare Motor to Turn.
  void turnRun();                                       //Set Motor to Turn
  void turnCheck();                                     //Checks if turned finished, ran in loop();
  void rpmCheck();                                      //Checks if targetRPM is reached, ran in loop();
  void turn2(int count);                                //Turn function by number of waves
  void debugPrint();                                    //Prints debug messages
  void forwardReady(double leng, bool additive = true); //Prepare Motor to move forward leng CM
  void forwardRun(bool isFastest = false);              //Sets Motor to move forward
  void fastForwardReady(double, bool add = true);
  void fastForwardRun();
  bool getIsIdle();
  unsigned int getDistCounter();
};

#endif
