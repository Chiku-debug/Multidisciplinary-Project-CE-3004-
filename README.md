# MDP 2019/20 S2 Group 14

The folder, MDP-Master, contains the code implementation of constructing and programming an autonomous robot which explores an arena filled with obstacles, generates a map of the explored arena and determines the fastest path from the start to the end goal of the selected arena.

# Table of Contents
1. [Arduino](#Arduino)
2. [Raspberry Pi](#RPI)
3. [Desktop Client](#Desktop)
4. [Android](#Android)

---
## Arduino <a name="Arduino"></a>

Arduino to accept "commands" from it's Serial Port. (TX/RX <-> PIN 0/1)
It will expect a command of *3 bytes*, ending with `'\n'` or `0x20`, totalling to *4 bytes*.

#### Command Format
```
Supposed command is received as: [Byte 0] [Byte 1] [Byte 2]
Byte 0:
    bit 7 - set for "Calibration" command (Byte 2 used for calibration mode).
    bit 6 - set for "Forward until Obstacle" command.
    bit 5 - set for "Sense" command (Byte 1 & Byte 2 not used, will return all 6 sensors data)
    bit 4 - set for "Forward" command (Move forward. Byte 1 & 2 will be used as a signed short int)
    bit 3 - set for "Turning2" command (Turn by number of waves, Used for debugging)
    bit 2 - set for "Turning" command (Turn by degrees. Byte 1 & 2 will be used as a signed short int)
    bit 1 - motor2 reversed? (True if reversed)
    bit 0 - motor1 reversed? (True if reversed)

Byte 1 - Motor 1's RPM (0-255) (May not be used)

Byte 2 - Motor 2's RPM (0-255) (May not be used)
```

#### Example:
```
Command: 03 FF FF
Description: Set Motor1's RPM to 255 & reverse rotation; Set Motor2's RPM to 255 & reverse rotation.

Command: 02 64 64
Description: Set Motor1's RPM to 100 & forward rotation; Set Motor2's RPM to 100 & reverse rotation.

Command: 04 00 5A
Description: Command Robot to rotate anti-clockwise by 90 degrees.

Command: 07 00 5A
Description: Command Robot to rotate clockwise by 90 degrees.

Command: 10 00 64
Description: Command Robot to move forward by 100 CM.

Command: 10 FF 9C
Description: Command Robot to move forward by -100 CM.

Command: 20 00 00
Description: Command Robot to return sensors data.

Command: 40 00 00 
Description: Command Robot to execute "Forward until obstacles detected"

Command: 80 00 00
Description: Command Robot to execute Calibration, Type 1 (Calibrate to the front)

Command: 80 00 01
Description: Command Robot to execute Calibration, Type 2 (Calibrate to the right)
```

#### Required Library
1. [PinChangeInterrupt](https://github.com/NicoHood/PinChangeInterrupt)
2. [DigitalWriteFast (Already Included in source)](https://github.com/watterott/Arduino-Libs/tree/master/digitalWriteFast)
