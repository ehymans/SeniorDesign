#include <Wire.h>
#include <Adafruit_VEML7700.h>

// Define pins for touch sensors and LEDs (blinkers)
const int leftTouchPin = 2;   // Left touch sensor
const int rightTouchPin = 15; // Right touch sensor
const int leftLedPin = 19;    // Left blinker LED
const int rightLedPin = 5;    // Right blinker LED

// Define pins for the headlight and tail light
const int headlightPin = 23;  // LED headlight
const int tailLightPin = 18;  // LED tail light

// PWM settings for the tail light
const int pwmFrequency = 5000; // PWM frequency
const int pwmResolution = 8;   // 8-bit resolution (0-255 for duty cycle)

// Flash state variables for blinkers
volatile bool leftFlash = false;
volatile bool rightFlash = false;

// Last time touch sensors were triggered (for debouncing)
volatile unsigned long lastLeftInterruptTime = 0;
volatile unsigned long lastRightInterruptTime = 0;
const unsigned long debounceDelay = 300;  // 300ms debounce delay

// Initialize VEML7700 object for the light sensor
Adafruit_VEML7700 veml = Adafruit_VEML7700();

// Function declarations
void handleBlinkers();
void blinkLED(int pin);
void delayWithSensorCheck(int delayTime);
void handleHeadlightAndTailLight();
void IRAM_ATTR leftTouchISR();
void IRAM_ATTR rightTouchISR();

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

  // Set initial VEML7700 sensor parameters
  veml.setGain(VEML7700_GAIN_1);
  veml.setIntegrationTime(VEML7700_IT_100MS);
  veml.powerSaveEnable(true);

  // Attach hardware interrupts for touch sensors
  attachInterrupt(digitalPinToInterrupt(leftTouchPin), leftTouchISR, CHANGE);  // Trigger on any change
  attachInterrupt(digitalPinToInterrupt(rightTouchPin), rightTouchISR, CHANGE);  // Trigger on any change

  Serial.println("System initialized.");
}

void loop() {
  // Handle blinker logic
  handleBlinkers();

  // Handle headlight and tail light logic based on the light sensor
  handleHeadlightAndTailLight();
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
  int elapsedTime = 0;
  while (elapsedTime < delayTime) {
    delay(5); // Small delay to allow other operations
    elapsedTime += 5;
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

  delay(500);  // Wait for 1 second before checking again
}
