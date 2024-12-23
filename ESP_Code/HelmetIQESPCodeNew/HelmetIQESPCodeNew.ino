#include <Wire.h>
#include <Adafruit_VEML7700.h>
#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>
#include "BluetoothSerial.h"
#include <Sparkfun_DRV2605L.h>

#define TCA9548A_ADDRESS 0x70  // Address of the TCA9548A mux

BluetoothSerial SerialBT;
SFE_HMD_DRV2605L HMD1;  // Haptic motor 1
SFE_HMD_DRV2605L HMD2;  // Haptic motor 2

bool isHeadlightOn = true; // Track the headlight status

// Define pins for touch sensors and LEDs (blinkers)
const int leftTouchPin = 2;   // Left touch sensor
const int rightTouchPin = 15; // Right touch sensor
const int leftLedPin = 19;    // Left blinker LED
const int rightLedPin = 5;    // Right blinker LED

// Define pins for the headlight and tail light
const int headlightPin = 23;  // LED headlight
const int tailLightPin = 18;  // LED tail light
const int brakeLightPin = 18; // Brake light (same as tail light for simplicity)

// PWM settings for the tail light
const int pwmFrequency = 5000; // PWM frequency
const int pwmResolution = 8;   // 8-bit resolution (0-255 for duty cycle)

// Flash state variables for blinkers
volatile bool leftFlash = false;
volatile bool rightFlash = false;

// Last time touch sensors were triggered (for debouncing)
volatile unsigned long lastLeftInterruptTime = 0;
volatile unsigned long lastRightInterruptTime = 0;
const unsigned long debounceDelay = 600;  // 600ms debounce delay

// Collision state variables
bool inCollision = false;
unsigned long collisionBlinkEndTime = 0;

bool deccel = false;

// Initialize VEML7700 object for the light sensor
Adafruit_VEML7700 veml = Adafruit_VEML7700();

// MPU6050 setup
Adafruit_MPU6050 mpu;

// Variables to store acceleration data
float accelX = 0, accelY = 0, accelZ = 0;
float prevAccelMagnitude = 0;  // To store the previous magnitude of acceleration

// Function declarations
void handleBlinkers(void *param);
void blinkLeftLED();
void blinkRightLED();
void handleHeadlightAndTailLight(void *param);
void IRAM_ATTR leftTouchISR();
void IRAM_ATTR rightTouchISR();
void blinkBrakeLight(int blinks);
//void handleDeceleration(void *param);
void handleCollisionDetection(void *param);
void handleBluetooth(void *param);
void tcaSelect(uint8_t channel);  // Function to select the mux channel

// FreeRTOS Task Handles
TaskHandle_t blinkerTaskHandle;
TaskHandle_t headlightTaskHandle;
//TaskHandle_t decelerationTaskHandle;
TaskHandle_t collisionTaskHandle;

// Setup function
void setup() {
  // Initialize the LED pins for blinkers as output
  pinMode(leftLedPin, OUTPUT);
  pinMode(rightLedPin, OUTPUT);

  // Initialize the LED pins for the headlight as output
  pinMode(headlightPin, OUTPUT);

  // Initialize PWM for the tail light (kept as in your original code)
  ledcAttach(tailLightPin, pwmFrequency, pwmResolution);  // Use new API to attach pin and set frequency/resolution

  // Start serial communication for debugging
  Serial.begin(115200);

  // Initialize I2C for Haptic
  Wire.begin();  // Initialize I2C communication

  // Initialize Haptic Motor 1
  tcaSelect(0);  // Select mux channel 0
  if (!HMD1.begin()) {
    Serial.println("Failed to initialize DRV2605L on channel 0");
    while (1);
  }
  HMD1.Mode(0);  // Internal trigger mode
  HMD1.MotorSelect(0x36);  // ERM motor selected
  HMD1.Library(1);  // Use library 1 (for ERM motors)
  Serial.println("Haptic motor 1 initialized on channel 0");

  // Initialize Haptic Motor 2
  tcaSelect(1);  // Select mux channel 1
  if (!HMD2.begin()) {
    Serial.println("Failed to initialize DRV2605L on channel 1");
    while (1);
  }
  HMD2.Mode(0);  // Internal trigger mode
  HMD2.MotorSelect(0x36);  // ERM motor selected
  HMD2.Library(1);  // Use library 1 (for ERM motors)
  Serial.println("Haptic motor 2 initialized on channel 1");

  // Initialize I2C communication with the VEML7700 sensor
  if (!veml.begin()) {
    Serial.println("VEML7700 not found");
    while (1);
  }

  // Initialize MPU6050
  if (!mpu.begin()) {
    Serial.println("Failed to find MPU6050 chip");
    while (1);
  }

  // Set initial MPU6050 and VEML7700 sensor parameters
  veml.setGain(VEML7700_GAIN_1);
  veml.setIntegrationTime(VEML7700_IT_100MS);
  veml.powerSaveEnable(true);

  // Set accelerometer range
  mpu.setAccelerometerRange(MPU6050_RANGE_2_G);

  SerialBT.begin("HelmetIQ"); // Name for the Bluetooth device
  Serial.println("Bluetooth Started! Waiting for connection...");

  // Attach hardware interrupts for touch sensors
  attachInterrupt(digitalPinToInterrupt(leftTouchPin), leftTouchISR, CHANGE);  // Trigger on any change
  attachInterrupt(digitalPinToInterrupt(rightTouchPin), rightTouchISR, CHANGE);  // Trigger on any change

  // Create FreeRTOS tasks
  xTaskCreatePinnedToCore(handleBlinkers, "BlinkerTask", 4096, NULL, 1, &blinkerTaskHandle, 0);
  xTaskCreatePinnedToCore(handleHeadlightAndTailLight, "HeadlightTask", 4096, NULL, 1, &headlightTaskHandle, 0);
  //xTaskCreatePinnedToCore(handleDeceleration, "DecelerationTask", 2048, NULL, 1, &decelerationTaskHandle, 0);
  xTaskCreatePinnedToCore(handleCollisionDetection, "CollisionTask", 4096, NULL, 1, &collisionTaskHandle, 0);
  xTaskCreatePinnedToCore(handleBluetooth, "BluetoothTask", 4096, NULL, 1, NULL, 0);

  Serial.println("System initialized.");
}

void loop() {
  // The logic is now handled within FreeRTOS tasks
}

// Function to select the mux channel
void tcaSelect(uint8_t channel) {
  if (channel > 7) return;
  Wire.beginTransmission(TCA9548A_ADDRESS);
  Wire.write(1 << channel);
  Wire.endTransmission();
}

// ISR for left touch sensor (simplified)
void IRAM_ATTR leftTouchISR() {
  unsigned long currentTime = millis();
  if (currentTime - lastLeftInterruptTime > debounceDelay) {
    leftFlash = !leftFlash;  // Toggle left blinker flag
    lastLeftInterruptTime = currentTime;  // Update debounce time
  }
}

// ISR for right touch sensor (simplified)
void IRAM_ATTR rightTouchISR() {
  unsigned long currentTime = millis();
  if (currentTime - lastRightInterruptTime > debounceDelay) {
    rightFlash = !rightFlash;  // Toggle right blinker flag
    lastRightInterruptTime = currentTime;  // Update debounce time
  }
}

// Task to handle blinker logic and debounce
void handleBlinkers(void *param) {
  while (true) {
    unsigned long currentTime = millis();

    // If leftFlash is true, blink the left LED
    if (leftFlash) {
      if (currentTime - lastLeftInterruptTime > debounceDelay) {
        Serial.println("Left blinker ON");
        blinkLeftLED();
      }
    }

    // If rightFlash is true, blink the right LED
    if (rightFlash) {
      if (currentTime - lastRightInterruptTime > debounceDelay) {
        Serial.println("Right blinker ON");
        blinkRightLED();
      }
    }

    vTaskDelay(100 / portTICK_PERIOD_MS);  // Task delay to prevent rapid toggling
  }
}

// Function to blink the left LED and trigger haptic motor 1
void blinkLeftLED() {
  digitalWrite(leftLedPin, HIGH);
  tcaSelect(0);  // Select mux channel 0
  HMD1.Waveform(0, 1);  // Play effect #1
  HMD1.go();  // Trigger the haptic motor
  vTaskDelay(200 / portTICK_PERIOD_MS);  // Delay for blinking
  digitalWrite(leftLedPin, LOW);
  vTaskDelay(200 / portTICK_PERIOD_MS);  // Delay for blinking
}

// Function to blink the right LED and trigger haptic motor 2
void blinkRightLED() {
  digitalWrite(rightLedPin, HIGH);
  tcaSelect(1);  // Select mux channel 1
  HMD2.Waveform(0, 1);  // Play effect #1
  HMD2.go();  // Trigger the haptic motor
  vTaskDelay(200 / portTICK_PERIOD_MS);  // Delay for blinking
  digitalWrite(rightLedPin, LOW);
  vTaskDelay(200 / portTICK_PERIOD_MS);  // Delay for blinking
}

// FreeRTOS task for handling the headlight and tail light
void handleHeadlightAndTailLight(void *param) {
  while (true) {
    // Read the ambient light from the VEML7700 sensor
    float lux = veml.readLux();
    Serial.print("Ambient Light (lux): ");
    Serial.println(lux);

    // Set a threshold for low light to turn on the headlight and tail light
    if (lux < 90.0 && isHeadlightOn) {
      // Adjust threshold as needed and check Bluetooth override
      digitalWrite(headlightPin, HIGH);  // Turn on headlight
      ledcWrite(tailLightPin, 50);  // 50% brightness for tail light
      Serial.println("Headlight ON and tail light at 50% brightness");
    } else {
      digitalWrite(headlightPin, LOW);  // Turn off headlight
      ledcWrite(tailLightPin, 0);  // Turn off tail light
      Serial.println("Headlight and tail light OFF");
    }

    vTaskDelay(1000 / portTICK_PERIOD_MS);  // Check every 1 second
  }
}

// Threshold for collision detection (adjust as needed)
const float collisionThreshold = 14.0; // Adjust thresholds in all directions
const float immobileThreshold = 8.0; // Modify with testing
const float decelThreshold = 6.0; // Adjust when testing

// Task for collision detection
void handleCollisionDetection(void *param) {
  sensors_event_t a, g, temp;  // Accelerometer, Gyroscope, and Temperature data

  while (true) {
    // Get the accelerometer and gyroscope readings
    mpu.getEvent(&a, &g, &temp);

    // Calculate the delta for each axis
    float deltaX = abs(a.acceleration.x - accelX);
    float deltaY = abs(a.acceleration.y - accelY);
    float deltaZ = abs(a.acceleration.z - accelZ);

    // Update previous acceleration values
    accelX = a.acceleration.x;
    accelY = a.acceleration.y;
    accelZ = a.acceleration.z;

    // Print the current acceleration values and the deltas
    Serial.print("Accel X: "); Serial.print(a.acceleration.x);
    Serial.print(", Delta X: "); Serial.println(deltaX);

    Serial.print("Accel Y: "); Serial.print(a.acceleration.y);
    Serial.print(", Delta Y: "); Serial.println(deltaY);

    Serial.print("Accel Z: "); Serial.print(a.acceleration.z);
    Serial.print(", Delta Z: "); Serial.println(deltaZ);

    // Check if any delta exceeds the collision threshold
    if (deltaX > collisionThreshold || deltaY > collisionThreshold || deltaZ > collisionThreshold) {
      Serial.println("Collision detected!");
      SerialBT.println("69");
      inCollision = true;
      if (inCollision) {
        // Blink all lights in hazard mode for collision indication
        digitalWrite(leftLedPin, HIGH);
        digitalWrite(rightLedPin, HIGH);
        digitalWrite(headlightPin, HIGH);
        ledcWrite(tailLightPin, 255); // Full brightness for tail light
        vTaskDelay(300 / portTICK_PERIOD_MS);
        digitalWrite(leftLedPin, LOW);
        digitalWrite(rightLedPin, LOW);
        digitalWrite(headlightPin, LOW);
        ledcWrite(tailLightPin, 0);
        vTaskDelay(300 / portTICK_PERIOD_MS);
        digitalWrite(leftLedPin, HIGH);
        digitalWrite(rightLedPin, HIGH);
        digitalWrite(headlightPin, HIGH);
        ledcWrite(tailLightPin, 255); // Full brightness for tail light
        vTaskDelay(300 / portTICK_PERIOD_MS);
        digitalWrite(leftLedPin, LOW);
        digitalWrite(rightLedPin, LOW);
        digitalWrite(headlightPin, LOW);
        ledcWrite(tailLightPin, 0);
        vTaskDelay(300 / portTICK_PERIOD_MS);
        digitalWrite(leftLedPin, HIGH);
        digitalWrite(rightLedPin, HIGH);
        digitalWrite(headlightPin, HIGH);
        ledcWrite(tailLightPin, 255); // Full brightness for tail light
        vTaskDelay(300 / portTICK_PERIOD_MS);
        digitalWrite(leftLedPin, LOW);
        digitalWrite(rightLedPin, LOW);
        digitalWrite(headlightPin, LOW);
        ledcWrite(tailLightPin, 0);
        vTaskDelay(300 / portTICK_PERIOD_MS);
      }
    } else if (deltaX > decelThreshold || deltaY > decelThreshold) {
      Serial.println("Slow down detected!");
      // Blink brake light
      ledcWrite(tailLightPin, 255); // Full brightness for tail light
      vTaskDelay(300 / portTICK_PERIOD_MS);

      ledcWrite(tailLightPin, 0);
      vTaskDelay(300 / portTICK_PERIOD_MS);

      ledcWrite(tailLightPin, 255); // Full brightness for tail light
      vTaskDelay(200 / portTICK_PERIOD_MS);

      ledcWrite(tailLightPin, 0);
      vTaskDelay(200 / portTICK_PERIOD_MS);

      ledcWrite(tailLightPin, 255); // Full brightness for tail light
      vTaskDelay(200 / portTICK_PERIOD_MS);

      ledcWrite(tailLightPin, 0);
      vTaskDelay(200 / portTICK_PERIOD_MS);
    } else {
      inCollision = false;
    }

    vTaskDelay(100 / portTICK_PERIOD_MS);  // Adjust delay as needed
  }
}

void handleBluetooth(void *param) {
  unsigned long lastHeartbeatTime = 0; // Store the time of the last heartbeat signal
  const unsigned long heartbeatInterval = 5000; // 5 seconds heartbeat interval

  while (true) {
    // Check if data is available from the Android device
    if (SerialBT.available()) {
      String incomingData = "";

      // Read incoming characters until a newline or return character is detected
      while (SerialBT.available()) {
        char c = SerialBT.read();
        if (c == '\n' || c == '\r') break; // Terminate on newline or carriage return
        incomingData += c;
      }

      // Process the received data
      if (incomingData.length() > 0) {
        Serial.print("Received Data: ");
        Serial.println(incomingData);

        // Toggle the headlight based on the received message
        if (incomingData.equalsIgnoreCase("ON") || incomingData.equalsIgnoreCase("1")) {
          isHeadlightOn = true;
          Serial.println("Headlight turned ON");
        } else if (incomingData.equalsIgnoreCase("OFF") || incomingData.equalsIgnoreCase("2")) {
          isHeadlightOn = false;
          Serial.println("Headlight turned OFF");
        } else {
          Serial.println("Unrecognized command.");
        }
      }
    }

    // Send heartbeat signal every 5 seconds to keep the connection alive
    unsigned long currentTime = millis();
    if (currentTime - lastHeartbeatTime >= heartbeatInterval) {
      SerialBT.println("HB"); // Send "HB" as a heartbeat signal
      lastHeartbeatTime = currentTime; // Update the last heartbeat time
      Serial.println("Heartbeat sent");
    }

    // Delay to prevent overwhelming the Bluetooth communication
    vTaskDelay(100 / portTICK_PERIOD_MS);
  }
}
