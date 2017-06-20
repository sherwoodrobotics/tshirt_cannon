# T-Shirt Cannon Files

## Info

T-Shirt Cannon connects from an Arduino to an Android device using Bluetooth Low Energy (Bluetooth LE, or BLE).  The Arduino is an Arduino 101 (also called Genuino 101). The code utilizes the CurieBLE library on that Arduino. The Android can be any 4.4 (API 19) phone with the ArduinoBluetooth app installed on it (source in here). 

***

## Configuration

### UUID List:

All UUIDs in this table are actually `0000####-0000-1000-8000-00805F9B34FB` in full, 128-bit form. The 16-bit values in the table below replace the `####`

| 16-bit UUID  | Description |
| ------------- | ------------- |
| `0000` | Bluetooth service device |
| `0001` | Bluetooth service for driving |
| `0011` | Bluetooth characteristic for driving left wheels |
| `0021` | Bluetooth characteristic for driving right wheels |
| `0002` | Bluetooth service for launching |
| `0012` | Bluetooth characteristic for launching cannon 1 |
| `0022` | Bluetooth characteristic for launching cannon 2 |
| `0032` | Bluetooth characteristic for launching cannon 3 |

### Android Configuration

Save the following settings on a config named "tshirt_cannon":

| Control  | Command | UUID | Notes |
| ------------- | ------------- | ------------- | ------------- |
| A  | `1` | `C 0012 - S 0002` | Cannon 1 |
| B  | `1` | `C 0022 - S 0002` | Cannon 2 |
| C  | `1` | `C 0032 - S 0002` | Cannon 3 |
| Left seek  | `%v`  | `C 0011 - S 0001` | Left wheels |
| Right seek  | `%v`  | `C 0021 - S 0001` | Right wheels |
| Seekbar checkbox | checked | ---- | Wheels reset |
| Seekbar delay | `500` | ---- | Sends instruction every 500ms |

### Arduino Pins

| Pin  | Action |
| ------------- | ------------- |
| 2 | Cannon 1 launch |
| 3 | Cannon 2 launch |
| 4 | Cannon 3 launch |
| 14 (A0) | Left wheel PWM |
| 15 (A1) | Right wheel PWM |

***

## Keystore

Current provided APK signed by Andy Nguyen.
Valid for next 2 years as of 6/20/17.
Keystore password: `robotics`
key0 password: `arduinobluetooth`

***

## Other useful links:
 - Arduino 101 Info Page: https://www.arduino.cc/en/Main/ArduinoBoard101
 - Arduino CurieBLE library: https://www.arduino.cc/en/Reference/CurieBLE
 - Android Bluetooth LE guide: https://developer.android.com/guide/topics/connectivity/bluetooth-le.html
 - nRF Toolbox app: https://play.google.com/store/apps/details?id=no.nordicsemi.android.nrftoolbox&hl=en
