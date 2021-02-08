#include <Arduino.h>

#include "digitalWriteFast.h"

#define IRSENSOR_DEBUG_MODE true
#define MAX_MEMORY_COUNT 7

#define USE_CMA false

#ifndef IRSENSOR_H
#define IRSENSOR_H

#define SHORT_RANGE_COUNT 3
#define LONG_RANGE_COUNT 7
#define TRESHOLD_COUNT (IS_SHORT_RANGE ? SHORT_RANGE_COUNT : LONG_RANGE_COUNT)
class IRSensor{
  private:
    byte PIN;
    int MEMORY[MAX_MEMORY_COUNT];
    byte MEMORY_POINTER = 0;
    bool IS_MEMORY_COMPLETE = false;
    double GRADIENT = 0.0;
    double OFFSET = 0.0;
    double OFFSET_CM = 0.0;
    double CMA = 0.0;
    unsigned long CMA_COUNT = 0;
    bool IS_SHORT_RANGE = true;
    double THRESHOLDS[8];// = NULL;
  public:
    static int comparator(byte x, byte y);
    IRSensor(byte, double g = 0.0, double offset = 0.0, double cm_offset = 0.0);
    //IRSensor(byte, double);
    String toString();
    double read();
    int readAnalog();
    void readLoop();
    double getCM();
    double getCMFast();
    double getCMRaw();
    double readFast();
    double getCMRawFast();
    byte getCMRawByte();
    void calibrate(bool isShortRange, double *thresholds);
    byte getUnitCM();
    byte getUnitCM2();
};
#endif
