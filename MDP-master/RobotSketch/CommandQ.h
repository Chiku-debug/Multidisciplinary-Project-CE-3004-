#ifndef COMMAND_Q_H
#define COMMAND_Q_H

#define IS_DEBUGGING false
class CommandQUnit{
    private:
        byte cmd[5] = {0, 0, 0, 0, 0};
        CommandQUnit *next = NULL;
    public:
        CommandQUnit(byte * cmd){
            #if IS_DEBUGGING
                Serial.print("Creating new Command Unit: ");
                Serial.print(cmd[0]);
                Serial.print(" ");
                Serial.print(cmd[1]);
                Serial.print(" ");
                Serial.print(cmd[2]);
                Serial.println(" ");
            #endif
            this->cmd[0] = cmd[0];
            this->cmd[1] = cmd[1];
            this->cmd[2] = cmd[2];
        }
        byte * getCmd(){return cmd;}
        void setNext(CommandQUnit *next){ this->next = next; }
        CommandQUnit *getNext() {return this->next;}
};

class CommandQ{
    private:
        CommandQUnit *front = NULL;
        CommandQUnit *back = NULL;
    public:
        CommandQ(){}
        CommandQUnit *enqueue(byte *cmd){
            CommandQUnit *unit = new CommandQUnit(cmd);
            if (front == NULL) {
                front = unit;
                back = unit;
            }else{
                back->setNext(unit);
                back = unit;
            }
            return unit;
        }
        CommandQUnit *dequeue(){
            if (isEmpty()) return NULL;
            CommandQUnit *unit = front;
            front = front->getNext();
            if (front == NULL) back = NULL;
            return unit;
        }
        bool isEmpty(){
            return (this->front == NULL);
        }
};
#endif