void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200);
}
void loop(){
  //Serial.println("Waiting...");
}
void serialEvent(){
  while (Serial.available()){
    //Serial.println("Received!");
    Serial.write(Serial.read()+1);
    Serial.println();
  }
}
