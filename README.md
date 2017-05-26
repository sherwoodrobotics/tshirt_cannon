# T-Shirt Cannon Files

## Info

T-Shirt Cannon connects from an Arduino to an Android device using Bluetooth Low Energy (Bluetooth LE, or BLE).  The Arduino is an Arduino 101 (also called Genuine 101). The code utilizes the CurieBLE library on that Arduino. The Android can be any 4.4 (API 19) phone with the ArduinoBluetooth app installed on it (source in here). 

***

## Configuration

### UUID List:

All UUIDs in this table look like "`0000####-0000-1000-8000-00805F9B34FB`" in full, 128-bit form. The 16-bit values in the table below replace the "`####`"

| 16-bit UUID  | Description |
| ------------- | ------------- |
| `0000`  | Bluetooth device  |
| `0001`  | Bluetooth service for driving  |
| `0011`  | Bluetooth characteristic for driving left wheels |
| `0021`  | Bluetooth characteristic for driving right wheels |

### Android Configuration

Save the following settings on a config named "tshirt_cannon":

| Control  | Command | UUID |
| ------------- | ------------- | ------------- |
| Left seek  | `%v`  | `C 0011 - S 0001` |
| Right seek  | `%v`  | `C 0021 - S 0001` |
| A  | `1` | `C 0011 - S 0001` |
| B  | `0` | `C 0011 - S 0001` |
| C  | `-1` | `C 0011 - S 0001` |
| 1  | `1` | `C 0021 - S 0001` |
| 2  | `0` | `C 0021 - S 0001` |
| 3  | `-1` | `C 0021 - S 0001` |
| Seekbar checkbox | checked | ---- |
| Seekbar delay | `500` | ---- |

### Arduino Pins

| Pin  | Action |
| ------------- | ------------- |
| 5  | Left wheel PWM |
| 6  | Right wheel PWM |

***

## Other useful links:
 - Arduino 101 Info Page: https://www.arduino.cc/en/Main/ArduinoBoard101
 - Arduino CurieBLE library: https://www.arduino.cc/en/Reference/CurieBLE
 - Android Bluetooth LE guide: https://developer.android.com/guide/topics/connectivity/bluetooth-le.html
 - nRF Toolbox app: https://play.google.com/store/apps/details?id=no.nordicsemi.android.nrftoolbox&hl=en
