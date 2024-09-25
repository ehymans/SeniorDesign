#include <Wire.h>
#include <Adafruit_VEML7700.h>
#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>

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
const unsigned long debounceDelay = 600;  // 300ms debounce delay

// Collision state variables
bool inCollision = false;
unsigned long collisionBlinkEndTime = 0;

// Initialize VEML7700 object for the light sensor
Adafruit_VEML7700 veml = Adafruit_VEML7700();

// MPU6050 setup
Adafruit_MPU6050 mpu;

// Variables to store acceleration data
float accelX = 0, accelY = 0, accelZ = 0;
float prevAccelMagnitude = 0;  // To store the previous magnitude of acceleration


// Function declarations
void handleBlinkers(void *param);
void blinkLED(int pin);
void handleHeadlightAndTailLight(void *param);
void IRAM_ATTR leftTouchISR();
void IRAM_ATTR rightTouchISR();
void blinkBrakeLight(int blinks);
void handleDeceleration(void *param);
void handleCollisionDetection(void *param);

// FreeRTOS Task Handles
TaskHandle_t blinkerTaskHandle;
TaskHandle_t headlightTaskHandle;
TaskHandle_t decelerationTaskHandle;
TaskHandle_t collisionTaskHandle;

// Setup function
void setup() {
  // Initialize the LED pins for blinkers as output
  pinMode(leftLedPin, OUTPUT);
  pinMode(rightLedPin, OUTPUT);

  // Initialize the LED pins for the headlight as output
  pinMode(headlightPin, OUTPUT);

  // Initialize PWM for the tail light
  ledcAttach(tailLightPin, pwmFrequency, pwmResolution);  // Use new API to attach pin and set frequency/resolution

  // Start serial communication for debugging
  Serial.begin(115200);

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

  // Attach hardware interrupts for touch sensors
  attachInterrupt(digitalPinToInterrupt(leftTouchPin), leftTouchISR, CHANGE);  // Trigger on any change
  attachInterrupt(digitalPinToInterrupt(rightTouchPin), rightTouchISR, CHANGE);  // Trigger on any change

  // Create FreeRTOS tasks
  xTaskCreatePinnedToCore(handleBlinkers, "BlinkerTask", 2048, NULL, 1, &blinkerTaskHandle, 0);
  xTaskCreatePinnedToCore(handleHeadlightAndTailLight, "HeadlightTask", 2048, NULL, 1, &headlightTaskHandle, 0);
  xTaskCreatePinnedToCore(handleDeceleration, "DecelerationTask", 2048, NULL, 1, &decelerationTaskHandle, 0);
  xTaskCreatePinnedToCore(handleCollisionDetection, "CollisionTask", 2048, NULL, 1, &collisionTaskHandle, 0);

  Serial.println("System initialized.");
}

void loop() {
  // The logic is now handled within FreeRTOS tasks
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
        blinkLED(leftLedPin);
      }
    }

    // If rightFlash is true, blink the right LED
    if (rightFlash) {
      if (currentTime - lastRightInterruptTime > debounceDelay) {
        Serial.println("Right blinker ON");
        blinkLED(rightLedPin);
      }
    }

    vTaskDelay(100 / portTICK_PERIOD_MS);  // Task delay to prevent rapid toggling
  }
}

// Function to blink the LED
void blinkLED(int pin) {
  digitalWrite(pin, HIGH);
  vTaskDelay(200 / portTICK_PERIOD_MS);  // Reduced delay for faster blinking
  digitalWrite(pin, LOW);
  vTaskDelay(200 / portTICK_PERIOD_MS);  // Reduced delay for faster blinking
}


// FreeRTOS task for handling the headlight and tail light
void handleHeadlightAndTailLight(void *param) {
  while (true) {
    // Read the ambient light from the VEML7700 sensor
    float lux = veml.readLux();
    Serial.print("Ambient Light (lux): ");
    Serial.println(lux);

    // Set a threshold for low light to turn on the headlight and tail light
    if (lux < 170.0) {  // Adjust threshold as needed
      digitalWrite(headlightPin, HIGH);  // Turn on headlight
      ledcWrite(tailLightPin, 50);  // 50% brightness for tail light
      Serial.println("Headlight ON and tail light at 50% brightness");
    } else {
      digitalWrite(headlightPin, LOW);  // Turn off headlight
      ledcWrite(tailLightPin, 0);  // Turn off tail light
      Serial.println("Headlight and tail light OFF");
    }

    vTaskDelay(2000 / portTICK_PERIOD_MS);  // Check every 2 seconds
  }
}

// Thresholds for detecting deceleration
const float decelThreshold = 1.25;  // Adjust this based on testing
bool isDecelerating = false;
unsigned long decelEndTime = 0;
// FreeRTOS task for handling deceleration and brake light control
void handleDeceleration(void *param) {
  while (true) {
    // Read acceleration data from MPU6050
    sensors_event_t a, g, temp;
    mpu.getEvent(&a, &g, &temp);

    // Get acceleration values on X, Y, Z axes
    accelX = a.acceleration.x;
    accelY = a.acceleration.y;
    accelZ = a.acceleration.z;

    // Calculate the magnitude of the acceleration vector
    float accelMagnitude = sqrt(accelX * accelX + accelY * accelY);
    // Smooth out the acceleration using exponential smoothing (optional)
  const float alpha = 0.1;  // Smoothing factor (between 0 and 1)
  float smoothedAccelMagnitude = (alpha * accelMagnitude) + ((1 - alpha) * prevAccelMagnitude);

    // Calculate the change in acceleration (delta) to detect deceleration
    float accelDelta = prevAccelMagnitude - smoothedAccelMagnitude;
    prevAccelMagnitude = accelMagnitude;
    Serial.println("Accel reading: ");
    Serial.println(smoothedAccelMagnitude);
    Serial.println("Accel Delta: ");
    Serial.println(accelDelta);

    // Detect deceleration
    if (accelDelta > decelThreshold) {
      if (!isDecelerating) {
        isDecelerating = true;
        Serial.println("Deceleration detected, brake light blinking ON");
        blinkBrakeLight(10);  // Blink brake light
      }
      decelEndTime = millis();  // Reset deceleration end time
    } else if (isDecelerating && millis() - decelEndTime > 200) {
      isDecelerating = false;
      Serial.println("Deceleration stopped, extra blinks triggered");
      blinkBrakeLight(6);  // Extra blinks after deceleration
    }

    prevAccelMagnitude = accelMagnitude;

    vTaskDelay(200 / portTICK_PERIOD_MS);  // Task delay
  }
}


// Threshold for collision detection
const float collisionThreshold = 12;  // Adjust this based on testing for collision
const float resumeMotionThreshold = 12;  // Threshold to exit hazard mode (when movement resumes)
// FreeRTOS task for handling collision detection
// FreeRTOS task for handling collision detection
void handleCollisionDetection(void *param) {
  while (true) {
    // Read accelerometer and gyroscope data from MPU6050
    sensors_event_t a, g, temp;
    mpu.getEvent(&a, &g, &temp);

    // Get acceleration and gyroscope values
    float accelMagnitude = sqrt(a.acceleration.x * a.acceleration.x + a.acceleration.y * a.acceleration.y + a.acceleration.z * a.acceleration.z);
    float gyroMagnitude = sqrt(g.gyro.x * g.gyro.x + g.gyro.y * g.gyro.y + g.gyro.z * g.gyro.z);

    // Detect collision and hazard lights
    if (accelMagnitude > collisionThreshold || gyroMagnitude > 5) {
      if (!inCollision) {
        inCollision = true;
        Serial.println("Collision detected, hazard lights ON");
      }
      blinkHazardLights(3);
      collisionBlinkEndTime = millis();
    } else if (inCollision && accelMagnitude > resumeMotionThreshold && millis() - collisionBlinkEndTime > 500) {
      inCollision = false;
      Serial.println("Motion resumed after collision, hazard lights OFF");
    }

    vTaskDelay(200 / portTICK_PERIOD_MS);  // Task delay
  }
}

// Other existing functions (e.g., blinkBrakeLight, blinkHazardLights) remain unchanged
// Function to blink the brake light
void blinkBrakeLight(int blinks) {
  for (int i = 0; i < blinks; i++) {
    ledcWrite(tailLightPin, 250);  // Full brightness
    vTaskDelay(100 / portTICK_PERIOD_MS);  // Short blink duration
    ledcWrite(tailLightPin, 50);  // Dim to 50% brightness during pause
    vTaskDelay(100 / portTICK_PERIOD_MS);  // Short pause between blinks
    ledcWrite(tailLightPin, 250);  // Full brightness
  }
}

// Function to blink all lights as hazard lights
void blinkHazardLights(int blinks) {
  for (int i = 0; i < blinks; i++) {
    // Turn on all lights (headlight, tail light, both blinkers)
    ledcWrite(tailLightPin, 250);  // Full brightness for tail light and brake light
    digitalWrite(headlightPin, HIGH);
    digitalWrite(leftLedPin, HIGH);
    digitalWrite(rightLedPin, HIGH);
    vTaskDelay(600 / portTICK_PERIOD_MS);  // Hazard blink duration

    // Turn off all lights
    ledcWrite(tailLightPin, 0);  // Turn off tail light
    digitalWrite(headlightPin, LOW);
    digitalWrite(leftLedPin, LOW);
    digitalWrite(rightLedPin, LOW);
    vTaskDelay(600 / portTICK_PERIOD_MS);  // Hazard blink off duration
  }
}

void delayWithSensorCheck(int delayTime) {
  unsigned long startTime = millis();
  while (millis() - startTime < delayTime) {
    // Let the system remain responsive and check for sensor states
    // Insert code here if needed to handle other tasks
    vTaskDelay(1 / portTICK_PERIOD_MS);  // Let other tasks run during the delay
  }
}
