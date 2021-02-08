#include "IRSensor.h"

/*
void IRSensor::calibrate(byte type, int value){
  if (IRSENSOR_DEBUG_MODE){
    Serial.println((String)"Calibrate: Type: "+ (int)type + " Value: " + (int)value); 
  }
  switch (type){
    case 1:
      V_10 = value; break;
    case 2:
      V_20 = value; break;
    case 3:
      V_30 = value; break;
    case 4:
      V_40 = value; break;
  }
}
*/

String IRSensor::toString(){
  String str = "";
  str += PIN;
  str += " PIN. POINTER: ";
  str += MEMORY_POINTER;
  return str;
}

int IRSensor::readAnalog(){
  return analogRead(PIN);
}

IRSensor::IRSensor(byte PIN, double GRADIENT, double OFFSET, double OFFSET_CM){
  pinModeFast(PIN, INPUT);
  this->PIN = PIN;
  this->GRADIENT = GRADIENT;
  this->OFFSET = OFFSET;
  this->OFFSET_CM = OFFSET_CM;
}
double IRSensor::read(){
  #if USE_CMA
    return CMA;
  #else
    double val = 0;
    byte MAX = (IS_MEMORY_COMPLETE ? MAX_MEMORY_COUNT : MEMORY_POINTER);
    // qsort(MEMORY, MAX, 1, &IRSensor::comparator);
    for (byte i = 0 ; i < MAX; i++){ val += MEMORY[i]; }
    // return (MEMORY[(MAX/2) + 1]);
    return (val / MAX);
  #endif
}
double IRSensor::readFast(){
  //byte MAX = (IS_MEMORY_COMPLETE ? MAX_MEMORY_COUNT : MEMORY_POINTER);
  byte MAX = (IS_MEMORY_COMPLETE ? MAX_MEMORY_COUNT : MEMORY_POINTER);
  byte index = MEMORY_POINTER == 0 ? MAX : MEMORY_POINTER - 1;
  return ((MEMORY[index] + MEMORY[MEMORY_POINTER]) / 2.0);
}
void IRSensor::readLoop(){
  MEMORY[MEMORY_POINTER++] = analogRead(PIN);
  #if USE_CMA
    CMA += ((MEMORY[MEMORY_POINTER-1] - CMA)/++CMA_COUNT);
  #endif
  if (!IS_MEMORY_COMPLETE){ if (MEMORY_POINTER >= MAX_MEMORY_COUNT) IS_MEMORY_COMPLETE = true; }
  //if (IS_MEMORY_COMPLETE == false && MEMORY_POINTER >= MAX_MEMORY_COUNT){ IS_MEMORY_COMPLETE = true; }
  MEMORY_POINTER = MEMORY_POINTER % MAX_MEMORY_COUNT;
}
double IRSensor::getCM(){
  return ((1/(read() + OFFSET))/GRADIENT) + OFFSET_CM;
}
double IRSensor::getCMFast(){
 return ((1/(readFast()))/GRADIENT); + OFFSET_CM; 
}
double IRSensor::getCMRaw(){
  return ((1/(read() + OFFSET))/GRADIENT);
}
double IRSensor::getCMRawFast(){
  return ((1/(readFast()))/GRADIENT);
}

byte IRSensor::getCMRawByte(){
  double raw = getCMRaw();
  if (raw > 255) return 255;
  else return ((byte)(round(raw)));
}
void IRSensor::calibrate(bool isShortRange, double *thresholds){
  IS_SHORT_RANGE = isShortRange;
  byte count = TRESHOLD_COUNT;
  //byte my_thresholds[count];// = byte[];
  for (byte i = 0 ; i < TRESHOLD_COUNT; i++){ 
    byte this_reading = *(thresholds+i);
    //Serial.println(String("[" + String(i)  + "]" + "CalibrateReading: ") + this_reading);
    //my_thresholds[i] = this_reading; 
    THRESHOLDS[i] = this_reading;
  }
  //if (THRESHOLDS != NULL){ delete THRESHOLDS; }
}

byte IRSensor::getUnitCM(){
  byte raw = getCMRawByte();
  //byte NUM_VALS = TRESHOLD_COUNT;
  byte count = TRESHOLD_COUNT;
  for (byte i=0; i<count; i++){
    //Serial.println("[" + String(i)  + "]" + String("Comparing ") + raw + " < " + (THRESHOLDS[i]) );
    if (raw < (THRESHOLDS[i])){ 
      if (IS_SHORT_RANGE)
        return ((i)*10); 
      else
        return (i < 3? 0 : (i-3)*10);
    }
  }
  return 255;
}
byte IRSensor::getUnitCM2(){
  double raw = getCMRaw();
  //byte NUM_VALS = TRESHOLD_COUNT;
  byte count = TRESHOLD_COUNT;
  for (byte i=0; i<count; i++){
    //Serial.println("[" + String(i)  + "]" + String("Comparing ") + raw + " < " + (THRESHOLDS[i]) );
    if (raw < (THRESHOLDS[i])){ 
      if (IS_SHORT_RANGE)
        return ((i)*10); 
      else
        return (i < 3? 0 : (i-3)*10);
    }
  }
  return 255;
}

int IRSensor::comparator(byte x, byte y){
  return (x < y ? -1 : x == y ? 0 : 1);
}