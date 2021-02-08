#include "Motor.h"

void Motor::init(){
  pinModeFast(IN_A, OUTPUT);
  pinModeFast(IN_B, OUTPUT);
  pinModeFast(PWM, OUTPUT);
  pinModeFast(EN, INPUT);
  pinModeFast(CS, INPUT);
}
Motor::Motor(byte IN_A, byte IN_B, byte PWM, byte EN, byte CS, bool IS_LEFT, byte MIN_SPEED){
  this->IN_A = IN_A;
  this->IN_B = IN_B;
  this->PWM = PWM;
  this->EN = EN;
  this->CS = CS;
  this->IS_LEFT = IS_LEFT;
  this->MIN_SPEED = IS_LEFT ? DEFAULT_MIN_SPEED : DEFAULT_MIN_SPEED_R;
  this->START_SPEED = IS_LEFT ? MOTOR_START_SPEED_L : MOTOR_START_SPEED_R;
  // if (IS_LEFT){
  //   this->MIN_SPEED = DEFAULT_MIN_SPEED;
  // } else{
  //   this->MIN_SPEED = DEFAULT_MIN_SPEED_R;
  // }
    
  //this->ENC_A = ENC_A;
  //this->ENC_B = ENC_B;
  
  this->init();
  //this->initInterrupt();
}
void Motor::setOtherMotor(Motor* om){
  otherMotor = om;
}
void Motor::onAFall(){
  //a_width = micros() - t_a_fall;
  t_a_fall = micros();
  onInt();
  //turnCheck();
}
void Motor::onBFall(){
  //b_width = micros() - t_b_fall;
  t_b_fall = micros();
  onInt();
  //turnCheck();
  rpmCheck();
}
void Motor::onARise(){
  a_width = ((micros() - t_a_fall) << 1); // * 2
  onInt();
  //rpmCheck();
}
void Motor::onBRise(){
  b_width = ((micros() - t_b_fall) << 1); // * 2
  onInt();
  //rpmCheck();
}

void Motor::onInt(){
  if (is_counting && turn_counter > 0) {
    turn_counter--;
    if (turn_counter == DEACCEL_TURN_COUNTER) rpmCheck();
    turnCheck();
  }
  //if (DO_DEBUG_PRINT) Serial.println((String)(IS_LEFT?"Motor LEFT: ":"Motor RIGHT: ") + turn_counter);
}

void Motor::setSpeed(byte speed, boolean is_reverse){
  if (speed != 0 && speed < MIN_SPEED) speed = MIN_SPEED;
  this->speed = speed;
  this->is_reverse = is_reverse;
  digitalWriteFast(PWM, 0);
  if (speed == 0){
    digitalWriteFast(IN_A, LOW);
    digitalWriteFast(IN_B, LOW);
  }else{
    digitalWriteFast(IN_A, is_reverse ? LOW : HIGH);
    digitalWriteFast(IN_B, is_reverse ? HIGH : LOW);
  }
  analogWrite(PWM, speed);
}

boolean Motor::getFault(){
  return !digitalReadFast(EN);
}

unsigned long Motor::getAWidth(){
  if (speed == 0) return 0;
  return a_width;
}
unsigned long Motor::getBWidth(){
  if (speed == 0) return 0;
  return b_width;
}
String Motor::toString(){
  //return (String)"A_Width: " + getAWidth() + "us, B_Width: " + getBWidth() + "us";
  //return (String)"RPM: " + otherMotor->getRPM() + "Diff: " + otherMotor->getDiff() + " Speed: " + speed;
  return (String)"turnCount: " + turn_counter;
  //return (String)"RPM"
}
double Motor::getWidth(){
  if (speed == 0) return 0;
  if (a_width != 0) return a_width;
  if (b_width != 0) return b_width;
  return ((a_width + b_width) / 2.0);
}
double Motor::getRPM(){
  //if (speed == 0) return 0;
  if (getWidth() == 0) return 0;
  return WIDTH_TO_RPM_CONST / getWidth();
} 

void Motor::setRPM(double rpm, boolean is_reverse){
  setRPM2(rpm, is_reverse);
  rpmCheck(); //Kick Start the loop
}

void Motor::setRPM2(double rpm, boolean is_reverse){
  is_braking = false;
  targetRPM = rpm;
  lastSync = micros();
  //this->is_reverse = is_reverse;
  setSpeed(START_SPEED, is_reverse);
}

void Motor::loop(){
  rpmCheck();
  turnCheck();
}
void Motor::rpmCheck(){
  if (is_braking) return; 
  double diff = getDiff();
  if (abs(diff) > RPM_TOL){
    int newSpeed = (int)speed + getNewPower(diff)/*round(diff)*//*(diff>0?1:-1)*/;
    if (newSpeed > 255) newSpeed = 255;
    else if (newSpeed < 0) newSpeed = 0;
    setSpeedFast(newSpeed);
  }
}
void Motor::setSpeedFast(byte newSpeed){
  this->speed = newSpeed;
  analogWrite(PWM, speed);
}
double Motor::getDiff(){
  return getTargetRPM() - getRPM();
}
double Motor::getTargetRPM(){
  if (!is_forward) return targetRPM;
  if (turn_counter <= DEACCEL_TURN_COUNTER)
    return DEACCEL_TARGET_RPM;
  return targetRPM;
} 
double Motor::getNewPower(double diff){
  unsigned now_micros = micros();
  if (now_micros - lastSync > SYNC_DELAY){
    lastSync = now_micros;
    long t_diff = getTurnCounter();
    t_diff -= otherMotor->getTurnCounter();
    //return round(t_diff);
    if (t_diff > SYNC_TOL) return SYNC_PENALTY;
    else if (t_diff < -SYNC_TOL) return -SYNC_PENALTY;
  }
  return (diff>0?SPEED_STEP:-SPEED_STEP);
  //return t;
  //return round(diff);

  //double t = (diff*diff*diff/200000.0);
  //double t = (diff*diff/500.0)*(diff>0?1:-1);
  //double t = (diff/20);
  //Serial.println((String)"newPower: " + t + "diff: " + diff);
}
void Motor::turnCheck(){
  if (is_turning == false) return;
  // if (turn_counter % CM_TO_WAVES_MULTIPLIER_10 == 0){
  //   dist_counter++;
  // }
  if (turn_counter == 0){ 
    is_turning = false;
    is_counting = false;
    is_forward = false;
    brake(); 
    //otherMotor->brake();
    //Serial.print(IS_LEFT ? "LEFT: " : "RIGHT: ");
    //Serial.println(dist_counter);
  }
}
void Motor::turnReady(double deg){ //Right
  brake();
  is_reverse = deg < 0;
  deg = abs(deg);
  turn_counter = ceil(4 * deg * DEG_TO_WAVES_MULTIPLIER);
}
void Motor::turnReady_L(double deg){ //Left
  brake();
  is_reverse = deg < 0;
  deg = abs(deg);
  //if (deg >= 90) deg -= 3;
  turn_counter = ceil(4 * deg * DEG_TO_WAVES_MULTIPLIER_L);
}
void Motor::turnRun(){
  is_turning = true;
  is_counting = true;
  setRPM((TURN_SPEED), is_reverse);
  turnCheck(); //Kick start the loop
}
void Motor::debugPrint(){
  if (DO_DEBUG_PRINT == false) return;
  Serial.print(this->toString());
}
void Motor::brake(byte strength){
  //setRPM(0, true);
  targetRPM = 0;
  turn_counter = 0;
  is_braking = true;
  is_turning = false;
  is_counting = false;
  digitalWriteFast(IN_A, LOW);
  digitalWriteFast(IN_B, LOW);
  //analogWrite(PWM, strength);//
  digitalWriteFast(PWM, HIGH);
}

void Motor::brake2(){
  targetRPM = 0;
  //is_braking = true;
  //digitalWriteFast(IN_A, LOW);
  //digitalWriteFast(IN_B, LOW);
  analogWrite(PWM, 0);
}

void Motor::turn2(int count){
  bool is_reverse = (count < 0);
  brake();
  is_turning = true;
  turn_counter = count;
  is_counting = true;
  setRPM((TURN_SPEED), is_reverse);
}
void Motor::forwardReady(double length, bool additive){ //Move forward length CM
  long turn_counter = ((long)((CM_TO_WAVES_MULTIPLIER * length)) << 2); 
  //if (DO_DEBUG_PRINT) Serial.println((String)"Turn Counter: " + turn_counter);
  if (additive){
    this->turn_counter += abs(turn_counter);  
  }else{
    this->turn_counter = abs(turn_counter); 
  }
  if (!IS_LEFT) turn_counter *= -1;
  is_reverse = (turn_counter < 0);
  dist_counter = 0;
}
void Motor::fastForwardReady(double length, bool additive){
  return forwardReady(length * FAST_FORWARD_MULTI, additive);
}
void Motor::forwardRun(bool isFastest){
  is_counting = true;
  is_turning = true;
  is_forward = true;
  setRPM(isFastest ? FAST_FORWARD_SPEED : FORWARD_SPEED, is_reverse);
}
void Motor::fastForwardRun(){
  forwardRun(true);
}
unsigned long Motor::getTurnCounter(){
  return turn_counter;
}
bool Motor::getIsIdle(){
  return !is_counting;
}
unsigned int Motor::getDistCounter(){
  return dist_counter;
}
