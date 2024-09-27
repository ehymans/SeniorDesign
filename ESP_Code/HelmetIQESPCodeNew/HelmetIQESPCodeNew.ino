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
void blinkLED(int pin);
void handleHeadlightAndTailLight(void *param);
void IRAM_ATTR leftTouchISR();
void IRAM_ATTR rightTouchISR();
void blinkBrakeLight(int blinks);
//void handleDeceleration(void *param);
void handleCollisionDetection(void *param);

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
  //xTaskCreatePinnedToCore(handleDeceleration, "DecelerationTask", 2048, NULL, 1, &decelerationTaskHandle, 0);
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
    if (lux < 90.0) {  // Adjust threshold as needed
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

void delayWithSensorCheck(int delayTime) {
  unsigned long startTime = millis();
  while (millis() - startTime < delayTime) {
    // Let the system remain responsive and check for sensor states
    // Insert code here if needed to handle other tasks
    vTaskDelay(1 / portTICK_PERIOD_MS);  // Let other tasks run during the delay
  }
}


// Threshold for collision detection (adjust as needed)
const float collisionThreshold = 12.0; // Adjust thresholds in all directions
//const float collisionThresholdY = 12.0; // Adjust thresholds in all directions
//const float collisionThresholdZ = 12.0; // Adjust thresholds in all directions
const float immobileThreshold = 8.0; //modify with testing

const float decelThreshold = 4.0; //Adjust when testing

// Task for collision detection
void handleCollisionDetection(void *param) {
  sensors_event_t a, g, temp;  // Accelerometer, Gyroscope, and Temperature data

  while (true) {
    // Get the accelerometer and gyroscope readings
    mpu.getEvent(&a, &g, &temp);

    // Calculate the delta for each axis
    float deltaX = (a.acceleration.x - accelX);
    float deltaY = (a.acceleration.y - accelY);
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
      Serial.println("Collision detected!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
      inCollision = true;
      if(inCollision){
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
    } 
    else if (deltaX > decelThreshold || deltaY > decelThreshold){
      Serial.println("Slow down detected!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
      
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
    }

    else {
      inCollision = false;
    }

    vTaskDelay(100 / portTICK_PERIOD_MS);  // Adjust delay as needed
  }
}


