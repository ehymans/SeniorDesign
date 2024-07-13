// Define pins
const int touchPin = 13; // TTP223 OUT pin connected to GPIO 13
const int ledPin = 12;   // LED connected to GPIO 12
bool flash = false;

void setup() {
  // Initialize the touch sensor pin as input
  pinMode(touchPin, INPUT);
  // Initialize the LED pin as output
  pinMode(ledPin, OUTPUT);
}

void loop() {
  // Read the state of the touch sensor
  int touchState = digitalRead(touchPin);

  // Toggle the flash state if the touch sensor is pressed
  if (touchState == HIGH) {
    flash = !flash;
    delay(200); // Debounce delay
  }

  // If flash is true, blink the LED
  while (flash) {
    digitalWrite(ledPin, HIGH);
    delayWithSensorCheck(500);
    digitalWrite(ledPin, LOW);
    delayWithSensorCheck(500);

    // Check the touch sensor state again to stop flashing
    touchState = digitalRead(touchPin);
    if (touchState == HIGH) {
      flash = !flash;
      delay(200); // Debounce delay
    }
  }
}

void delayWithSensorCheck(int delayTime) {
  int elapsedTime = 0;
  while (elapsedTime < delayTime) {
    if (digitalRead(touchPin) == HIGH) {
      flash = !flash;
      delay(200); // Debounce delay
      break;
    }
    delay(10); // Check the sensor every 10ms
    elapsedTime += 10;
  }
}
