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
const int leftMotors = 14;
const int rightMotors = 15;

const int cannon1 = 2;
const int cannon2 = 3;
const int cannon3 = 4;

const int millisBeforeLaunch = 5000;
const int triggerTime = 250;

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
const char* launchServiceUUID = "0002";
const char* cannon1UUID = "0012";
const char* cannon2UUID = "0022";
const char* cannon3UUID = "0032";

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

//Drive
BLEService driveService(driveServiceUUID); 
BLEFloatCharacteristic leftWheelsChar(leftWheelsUUID, BLERead | BLEWrite);
BLEFloatCharacteristic rightWheelsChar(rightWheelsUUID, BLERead | BLEWrite);

//Launch
BLEService launchService(launchServiceUUID);
BLEFloatCharacteristic cannon1Char(cannon1UUID, BLERead | BLEWrite);
BLEFloatCharacteristic cannon2Char(cannon2UUID, BLERead | BLEWrite);
BLEFloatCharacteristic cannon3Char(cannon3UUID, BLERead | BLEWrite);

int cannon1Time = 0;
int cannon2Time = 0;
int cannon3Time = 0;

/* End Do not touch these*/

void setup() {
  //Init
  Serial.begin(9600);
  leftServo.attach(leftMotors);
  rightServo.attach(rightMotors);
  pinMode(cannon1, OUTPUT);
  pinMode(cannon2, OUTPUT);
  pinMode(cannon3, OUTPUT);

  //Setup Bluetooth Peripheral (server)
  blePeripheral.setLocalName("BluetoothLED");
  blePeripheral.setAdvertisedServiceUuid(bluetoothUUID);
  blePeripheral.addAttribute(driveService);
  blePeripheral.addAttribute(leftWheelsChar);
  blePeripheral.addAttribute(rightWheelsChar);
  blePeripheral.addAttribute(launchService);
  blePeripheral.addAttribute(cannon1Char);
  blePeripheral.addAttribute(cannon2Char);
  blePeripheral.addAttribute(cannon3Char);

  //Setup callbacks to call when events happen
  blePeripheral.setEventHandler(BLEConnected, peripheralConnected);
  blePeripheral.setEventHandler(BLEDisconnected, peripheralDisconnected);
  leftWheelsChar.setEventHandler(BLEWritten, leftWheelsWritten);
  rightWheelsChar.setEventHandler(BLEWritten, rightWheelsWritten);
  cannon1Char.setEventHandler(BLEWritten, cannon1Written);
  cannon2Char.setEventHandler(BLEWritten, cannon2Written);
  cannon3Char.setEventHandler(BLEWritten, cannon3Written);

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

void cannon1Written(BLECentral& central, BLECharacteristic& characteristic) {
  if (cannon1Time >= millisBeforeLaunch + millis()) {
      digitalWrite(cannon1, HIGH);
      delay(triggerTime);
      digitalWrite(cannon1, LOW);
      cannon1Time = millis();
  }
}

void cannon2Written(BLECentral& central, BLECharacteristic& characteristic) {
  if (cannon2Time >= millisBeforeLaunch + millis()) {
      digitalWrite(cannon2, HIGH);
      delay(triggerTime);
      digitalWrite(cannon2, LOW);
      cannon2Time = millis();
  }
  
}

void cannon3Written(BLECentral& central, BLECharacteristic& characteristic) {

  if (cannon3Time >= millisBeforeLaunch + millis()) {
      digitalWrite(cannon3, HIGH);
      delay(triggerTime);
      digitalWrite(cannon3, LOW);
      cannon3Time = millis();
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
