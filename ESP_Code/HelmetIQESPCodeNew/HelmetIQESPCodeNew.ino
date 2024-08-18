// HelmetIQ System Code for ESP32 with Blinker Lights Controlled by Touch Sensors
// This code integrates dynamic brake light control, touch sensor-controlled blinkers, collision detection, and Bluetooth communication
// All pin assignments are declared as global variables for easy mapping to a custom PCB

// Global Pin Assignments
int brakeLightPin = 25;          // Pin for controlling brake lights (used as tail lights when it's dark)
int headlightPin = 26;           // Pin for controlling headlight (should not flash during hazard mode)
int leftBlinkerPin = 13;         // Pin for left blinker light
int rightBlinkerPin = 12;        // Pin for right blinker light
int leftTouchSensorPin = 14;     // Pin for left touch sensor (TTP223)
int rightTouchSensorPin = 27;    // Pin for right touch sensor (TTP223)
int lightSensorPin = 32;         // Pin for VEML7700 light sensor
int mpu6050SDA = 21;             // SDA pin for MPU6050
int mpu6050SCL = 22;             // SCL pin for MPU6050
int tca9548aSDA = 33;            // SDA pin for TCA9548A I2C multiplexer
int tca9548aSCL = 34;            // SCL pin for TCA9548A I2C multiplexer

bool leftBlinkerOn = false;      // Variable to toggle left blinker
bool rightBlinkerOn = false;     // Variable to toggle right blinker
bool flash = false;              // Variable to toggle hazard flashing state

// Include necessary libraries
#include <Wire.h>
#include <Adafruit_DRV2605.h>
#include <Adafruit_MPU6050.h>
#include <Adafruit_VEML7700.h>
#include <TCA9548A.h>
#include <BluetoothSerial.h>
#include <esp_sleep.h>
#include <esp_system.h> // Library for system reset

// Bluetooth Serial object
BluetoothSerial BTSerial;

// Sensor and driver objects
Adafruit_MPU6050 mpu;
Adafruit_VEML7700 veml = Adafruit_VEML7700();
Adafruit_DRV2605 drv;
TCA9548A tca;

volatile bool collisionDetected = false;  // Collision detection flag
volatile bool brakeLightsOn = false;      // Brake light state flag
volatile bool hapticTouchDetected = false;// Haptic touch detection flag

unsigned long hapticTouchStartTime = 0;   // Variable to track the time haptic sensor is held

void IRAM_ATTR onBrakeLightInterrupt() {
  brakeLightsOn = true;
}

void IRAM_ATTR onCollisionInterrupt() {
  collisionDetected = true;
}

void IRAM_ATTR onLeftTouchInterrupt() {
  leftBlinkerOn = !leftBlinkerOn;  // Toggle the left blinker state
}

void IRAM_ATTR onRightTouchInterrupt() {
  rightBlinkerOn = !rightBlinkerOn;  // Toggle the right blinker state
}

void setup() {
  Serial.begin(115200);
  BTSerial.begin("HelmetIQ");

  // Initialize I2C communication
  Wire.begin(tca9548aSDA, tca9548aSCL);

  // Initialize MPU6050
  if (!mpu.begin()) {
    Serial.println("Failed to find MPU6050 chip");
    while (1) {
      delay(10);
    }
  }
  mpu.setAccelerometerRange(MPU6050_RANGE_8_G);
  mpu.setGyroRange(MPU6050_RANGE_500_DEG);
  mpu.setFilterBandwidth(MPU6050_BAND_21_HZ);

  // Initialize VEML7700 light sensor
  if (!veml.begin()) {
    Serial.println("Failed to initialize VEML7700 light sensor");
    while (1);
  }

  veml.setGain(VEML7700_GAIN_1);
  veml.setIntegrationTime(VEML7700_IT_800MS);

  // Initialize DRV2605L haptic driver
  drv.begin();
  drv.selectLibrary(1);
  drv.setMode(DRV2605_MODE_INTTRIG);

  // Initialize pins
  pinMode(brakeLightPin, OUTPUT);
  pinMode(headlightPin, OUTPUT);
  pinMode(leftBlinkerPin, OUTPUT);
  pinMode(rightBlinkerPin, OUTPUT);
  pinMode(leftTouchSensorPin, INPUT);
  pinMode(rightTouchSensorPin, INPUT);

  // Attach interrupts to the touch sensor pins
  attachInterrupt(digitalPinToInterrupt(leftTouchSensorPin), onLeftTouchInterrupt, CHANGE);
  attachInterrupt(digitalPinToInterrupt(rightTouchSensorPin), onRightTouchInterrupt, CHANGE);

  // Set the ESP32 to wake up on interrupts from the touch sensors
  esp_sleep_enable_ext1_wakeup(GPIO_NUM_14 | GPIO_NUM_27, ESP_EXT1_WAKEUP_ANY_HIGH);
}

void loop() {
  // Enter deep sleep mode until an interrupt is triggered
  esp_light_sleep_start();

  // Get accelerometer and gyro data
  sensors_event_t a, g, temp;
  mpu.getEvent(&a, &g, &temp);

  // Check if we should activate the brake light based on acceleration
  handleBrakeLight(a.acceleration.z);

  // Handle collision detection
  if (collisionDetected) {
    collisionDetected = false;
    handleCollision();
  }

  // Handle blinker lights based on touch sensor input
  handleBlinkers();

  // Adjust taillight brightness based on ambient light
  adjustTaillightBrightness();

  // Send sensor data via Bluetooth
  sendSensorData(a, g);
}

void handleBrakeLight(float zAccel) {
  if (zAccel < -1.5) {  // Deceleration threshold
    // Turn on brake light (you can adjust the brightness using PWM)
    analogWrite(brakeLightPin, 255); // Full brightness
  } else {
    // Turn off brake light or dim it for taillight mode
    adjustTaillightBrightness(); // Adjust based on ambient light
  }
}

void handleCollision() {
  // Flash all lights except the headlight as hazard lights
  for (int i = 0; i < 10; i++) { // Flash 10 times
    digitalWrite(brakeLightPin, HIGH);
    digitalWrite(leftBlinkerPin, HIGH);
    digitalWrite(rightBlinkerPin, HIGH);
    delay(200);
    digitalWrite(brakeLightPin, LOW);
    digitalWrite(leftBlinkerPin, LOW);
    digitalWrite(rightBlinkerPin, LOW);
    delay(200);
  }
}

void handleBlinkers() {
  // Control the left blinker light
  if (leftBlinkerOn) {
    digitalWrite(leftBlinkerPin, HIGH);
  } else {
    digitalWrite(leftBlinkerPin, LOW);
  }

  // Control the right blinker light
  if (rightBlinkerOn) {
    digitalWrite(rightBlinkerPin, HIGH);
  } else {
    digitalWrite(rightBlinkerPin, LOW);
  }
}

void adjustTaillightBrightness() {
  // Function to adjust the taillight brightness based on ambient light levels
  uint16_t als = veml.readALS(); // Read the ambient light level

  if (als <= 3000) {
    // It's dark, so use lower intensity for taillights
    analogWrite(brakeLightPin, 128); // 50% brightness for taillights
  } else {
    // It's bright, so turn off the taillights
    analogWrite(brakeLightPin, 0); // Lights off
  }
}

void sendSensorData(sensors_event_t a, sensors_event_t g) {
  BTSerial.print("Acceleration Z: ");
  BTSerial.println(a.acceleration.z);

  BTSerial.print("Gyroscope Z: ");
  BTSerial.println(g.gyro.z);
}
