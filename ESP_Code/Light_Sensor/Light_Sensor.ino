#include <Wire.h>
#include <Adafruit_VEML7700.h>

Adafruit_VEML7700 veml = Adafruit_VEML7700();
const int diodePin = 5;

void setup() {
  Serial.begin(115200);
  while (!Serial) {
    delay(1000);
  }

  pinMode(diodePin, OUTPUT);  // Set the GPIO pin as an output
  Serial.println("Adafruit VEML7700 Test");


  // Initialize I2C communication
  if (!veml.begin()) {
    Serial.println("Failed to communicate with VEML7700 sensor, check wiring!");
    while (1);
  }

  Serial.println("Found VEML7700 sensor");

  veml.setGain(VEML7700_GAIN_1);
  veml.setIntegrationTime(VEML7700_IT_800MS);

  Serial.print(F("Gain: "));
  switch (veml.getGain()) {
    case VEML7700_GAIN_1: Serial.println(F("1")); break;
    case VEML7700_GAIN_2: Serial.println(F("2")); break;
    case VEML7700_GAIN_1_8: Serial.println(F("1/8")); break;
    case VEML7700_GAIN_1_4: Serial.println(F("1/4")); break;
  }

  Serial.print(F("Integration Time (ms): "));
  switch (veml.getIntegrationTime()) {
    case VEML7700_IT_25MS: Serial.println(F("25")); break;
    case VEML7700_IT_50MS: Serial.println(F("50")); break;
    case VEML7700_IT_100MS: Serial.println(F("100")); break;
    case VEML7700_IT_200MS: Serial.println(F("200")); break;
    case VEML7700_IT_400MS: Serial.println(F("400")); break;
    case VEML7700_IT_800MS: Serial.println(F("800")); break;
  }
}

void loop() {
  Serial.print(F("Lux: ")); Serial.println(veml.readLux());
  Serial.print(F("White: ")); Serial.println(veml.readWhite());
  Serial.print(F("Raw Ambient Light Sensing: ")); Serial.println(veml.readALS());
  
  if(veml.readALS()<=3000){
  digitalWrite(diodePin, HIGH);  // Turn the diode on
  }
else digitalWrite(diodePin, LOW);   // Turn the diode off

}
