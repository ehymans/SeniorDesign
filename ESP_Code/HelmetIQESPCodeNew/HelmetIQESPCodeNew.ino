#include <Wire.h>
#include <Adafruit_VEML7700.h>
#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>

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

// Initialize VEML7700 object for the light sensor
Adafruit_VEML7700 veml = Adafruit_VEML7700();

// MPU6050 setup
Adafruit_MPU6050 mpu;

// Variables to store acceleration data
float accelX = 0, accelY = 0, accelZ = 0;
float prevAccelMagnitude = 0;  // To store the previous magnitude of acceleration

// Thresholds for detecting deceleration
const float decelThreshold = 0.5;  // Adjust this based on testing
bool isDecelerating = false;
unsigned long decelEndTime = 0;

// Function declarations
void handleBlinkers();
void blinkLED(int pin);
void delayWithSensorCheck(int delayTime);
void handleHeadlightAndTailLight();
void IRAM_ATTR leftTouchISR();
void IRAM_ATTR rightTouchISR();
void blinkBrakeLight(int blinks);
void handleDeceleration();

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

  Serial.println("System initialized.");
}

// Loop function
void loop() {
  // Handle blinker logic
  handleBlinkers();

  // Handle headlight and tail light logic based on the light sensor
  handleHeadlightAndTailLight();

  // Handle deceleration detection and brake light control
  handleDeceleration();
}

// ISR for left touch sensor
void IRAM_ATTR leftTouchISR() {
  unsigned long currentTime = millis();
  if (currentTime - lastLeftInterruptTime > debounceDelay) {
    leftFlash = !leftFlash;
    Serial.println("Left blinker toggled");
    lastLeftInterruptTime = currentTime;
  }
}

// ISR for right touch sensor
void IRAM_ATTR rightTouchISR() {
  unsigned long currentTime = millis();
  if (currentTime - lastRightInterruptTime > debounceDelay) {
    rightFlash = !rightFlash;
    Serial.println("Right blinker toggled");
    lastRightInterruptTime = currentTime;
  }
}

void handleBlinkers() {
  // If leftFlash is true, blink the left LED
  if (leftFlash) {
    Serial.println("Left blinker ON");
    blinkLED(leftLedPin);
  }

  // If rightFlash is true, blink the right LED
  if (rightFlash) {
    Serial.println("Right blinker ON");
    blinkLED(rightLedPin);
  }
}

void blinkLED(int pin) {
  digitalWrite(pin, HIGH);
  delayWithSensorCheck(200);  // Reduced delay for faster blinking
  digitalWrite(pin, LOW);
  delayWithSensorCheck(200);  // Reduced delay for faster blinking
}

void delayWithSensorCheck(int delayTime) {
  unsigned long startTime = millis();
  while (millis() - startTime < delayTime) {
    // Let the system remain responsive and check for sensor states
    // Insert code here if needed to handle other tasks
  }
}

void handleHeadlightAndTailLight() {
  // Read the ambient light from the VEML7700 sensor
  float lux = veml.readLux();
  Serial.print("Ambient Light (lux): ");
  Serial.println(lux);

  // Set a threshold for low light to turn on the headlight and tail light
  if (lux < 50.0) {  // Adjust threshold as needed
    digitalWrite(headlightPin, HIGH);  // Turn on headlight

    // Set the tail light to 50% brightness using PWM
    ledcWrite(tailLightPin, 50);  // 20% duty cycle (50 out of 255)
    Serial.println("Headlight ON and tail light at 50% brightness");
  } else {
    digitalWrite(headlightPin, LOW);   // Turn off headlight

    // Turn off the tail light
    ledcWrite(tailLightPin, 0);  // 0% duty cycle (tail light off)
    Serial.println("Headlight and tail light OFF");
  }

  delay(900);  // Wait for 1 second before checking again
}

void handleDeceleration() {
  // Read acceleration data from MPU6050
  sensors_event_t a, g, temp;
  mpu.getEvent(&a, &g, &temp);

  // Get acceleration values on X, Y, Z axes
  accelX = a.acceleration.x;
  accelY = a.acceleration.y;
  accelZ = a.acceleration.z;

  // Calculate the magnitude of the acceleration vector
  float accelMagnitude = sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ);

  // Print raw acceleration values
  Serial.print("Accel X: "); Serial.print(accelX);
  Serial.print(" Accel Y: "); Serial.print(accelY);
  Serial.print(" Accel Z: "); Serial.println(accelZ);

  // Print magnitude of the acceleration
  Serial.print("Accel Magnitude: ");
  Serial.println(accelMagnitude);

  // Calculate the change in acceleration (delta) to detect deceleration
  float accelDelta = prevAccelMagnitude - accelMagnitude;

  // Print delta (change in acceleration magnitude)
  Serial.print("Accel Delta: ");
  Serial.println(accelDelta);

  // Detect deceleration
  if (accelDelta > decelThreshold) {  // Deceleration is detected
    if (!isDecelerating) {
      isDecelerating = true;
      Serial.println("Deceleration detected, brake light blinking ON");
    }
    
    // Blink brake light while decelerating
    blinkBrakeLight(2);  // Blink once per iteration while decelerating
    decelEndTime = millis();  // Reset the deceleration end time
  } else {
    if (isDecelerating && millis() - decelEndTime > 500) {  // After deceleration stops
      isDecelerating = false;
      Serial.println("Deceleration stopped, extra blinks triggered");
      blinkBrakeLight(6);  // Blink brake light twice after deceleration stops
    }
    
    ledcWrite(tailLightPin, 0);  // Turn off brake light after blinking
  }

  // Update the previous acceleration magnitude
  prevAccelMagnitude = accelMagnitude;
}

void blinkBrakeLight(int blinks) {
  for (int i = 0; i < blinks; i++) {
    ledcWrite(tailLightPin, 250);  // Full brightness
    delay(100);  // Short blink duration
    ledcWrite(tailLightPin, 50);  // Dim to 50% brightness during pause
    delay(100);  // Short pause between blinks
  }
}
