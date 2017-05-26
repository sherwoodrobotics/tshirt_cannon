#include <CurieBLE.h>
#include <Servo.h>

/**
 * Written by Andy 5/9/17
 * 
 * T-Shirt drive base code for Bluetooth Arduino (Arduino/Genuino 101)
 * 
 * The built-in light indicates whether a client is connected
 * 
 * Sending "l%v;" and "r%v;" where "%v" is the value will control the wheels 
 * This is set on the Android client
 */

/* Editable */

//Pins for the wheels motors
const int leftMotors = 6;
const int rightMotors = 5;

/**
 * Convention:
 * 0000 will always be the UUID of the Bluetooth Device
 * 000X will always be the UUID of a service 
 * 00YX will always be the UUID of a characteristic
 * 
 * Examples of services include "T-Shirt Drive" "T-Shirt Launch"
 * Characteristic includes things like "Left-Wheels" "Right-Wheels"
 * 
 * All characteristics have an associated service
 */

const char* bluetoothUUID = "0000";
const char* driveServiceUUID = "0001";
const char* leftWheelsUUID = "0011";
const char* rightWheelsUUID = "0021";

//Constants defining forward and backward on Servo (Arduino server) vs Seekbar (Android client)
const int fullForward = 180;
const int fullBackward = 0;
const int clientFullForward = 1;
const int clientFullBackward = -1;

//If this is > 0, bluetooth polling will check input every pollDelay millis
const int pollDelay = 0; // ms

/* End editable*/

/* Do not touch these */

//Servos
Servo leftServo;
Servo rightServo;

//BT connection
BLEPeripheral blePeripheral; 
BLEService driveService(driveServiceUUID); 
BLEFloatCharacteristic leftWheelsChar(leftWheelsUUID, BLERead | BLEWrite);
BLEFloatCharacteristic rightWheelsChar(rightWheelsUUID, BLERead | BLEWrite);

/* End Do not touch these*/

void setup() {
  //Init
  Serial.begin(9600);
  leftServo.attach(leftMotors);
  rightServo.attach(rightMotors);

  //Setup Bluetooth Peripheral (server)
  blePeripheral.setLocalName("BluetoothLED");
  blePeripheral.setAdvertisedServiceUuid(bluetoothUUID);
  blePeripheral.addAttribute(driveService);
  blePeripheral.addAttribute(leftWheelsChar);
  blePeripheral.addAttribute(rightWheelsChar);

  //Setup callbacks to call when events happen
  blePeripheral.setEventHandler(BLEConnected, peripheralConnected);
  blePeripheral.setEventHandler(BLEDisconnected, peripheralDisconnected);
  leftWheelsChar.setEventHandler(BLEWritten, leftWheelsWritten);
  rightWheelsChar.setEventHandler(BLEWritten, rightWheelsWritten);

  //Ready
  leftWheelsChar.setValue(0);
  rightWheelsChar.setValue(0);
  blePeripheral.begin();
  Serial.println("Arduino ready to go");
}

void loop() {
  //Continuously check
  blePeripheral.poll();
  if (pollDelay > 0) {
    delay(pollDelay);
  }
}

void leftWheelsWritten(BLECentral& central, BLECharacteristic& characteristic) {
  Serial.print("Got: ");
  Serial.println(leftWheelsChar.value());
  
  leftServo.write(clip(leftWheelsChar.value()));
}

void rightWheelsWritten(BLECentral& central, BLECharacteristic& characteristic) {
  Serial.print("Got: ");
  Serial.println(rightWheelsChar.value());
  
  rightServo.write(clip(rightWheelsChar.value()));
}

float clip(float num) {
  //Convert [-1, 1] [0, 180]
  
  float num1 = (fullForward - fullBackward) * (num - clientFullBackward);
  num1 = num1 /  (clientFullForward - clientFullBackward);
  return num1;
}

void peripheralConnected(BLECentral& central) {
  Serial.print("Connected to central with address: ");
  Serial.println(central.address());
 
  digitalWrite(LED_BUILTIN, HIGH);
}

void peripheralDisconnected(BLECentral& central) {
  Serial.print("Disconnected from central with address: ");
  Serial.println(central.address());
  
  digitalWrite(LED_BUILTIN, LOW);
}
