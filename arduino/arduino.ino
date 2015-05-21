
#include <SPI.h>
#include <MFRC522.h>
#include <Wire.h>
#include <dht.h>

// pins definition
#define D0        0
#define D1        1
#define D2        2
#define D3        3
#define D4        4
#define D5        5
#define D6        6
#define D7        7
#define D8        8 
#define D9        9
#define D10       10
#define D11       11
#define D12       12
#define D13       13
#define A0        0
#define A1        1
#define A2        2
#define A3        3
#define A4        4
#define A5        5
#define A6        6
#define A7        7

#define DIGITAL   0
#define ANALOG    1

// sensors type definition
#define UNDEFINED              0
#define TEMP                   1
#define RFID                   2
#define PIR                    3
#define BUZZER                 4
#define RELAY                  5
#define VOLTMETER              6

// msg definition
#define SETUP_MSG              '*'
#define SUPER_SEPARATOR        '|'
#define SEPARATOR              ';'
#define START_MSG              '@'
#define RESET_MSG              '#'
#define END_MSG                '!'
#define END_CHSUM              '^'

#define MAX_SENSORS            10
#define BUFFER_SIZE            128
#define BUFFER_SIZE_IN         32


struct sensor {
    int pin;
    int pins[5];
    int type;
    int id;
    
    int int_value;
    bool bool_value;
    char str_value[16];
    char char_value;
    unsigned long timestamp;
    
    // if buzzer
    int beep_num;
    int beep_type; // 0: normal, 1: fast, 2: slow
    
    // if rfid
    MFRC522* mfrc522;
};

void(* resetFunc) (void) = 0;

struct sensor *sensors;
boolean setup_done = false;
char out_msg[BUFFER_SIZE];
char curr_msg[BUFFER_SIZE];
char in_msg[BUFFER_SIZE];
int i2c_index = 0;
dht DHT;

#define SLAVE_ADDRESS   0x04

int test_count = 100;

void setup() {
    Serial.begin(9600);
    while (!Serial);
    
    sensors = (struct sensor*)malloc(sizeof(struct sensor) * MAX_SENSORS);
    reset_all_sensors();
    
    out_msg[0] = 0;
    curr_msg[0] = 0;
    SPI.begin(); // Init SPI bus
    Wire.begin(SLAVE_ADDRESS);
    Wire.onReceive(receive_data);
    Wire.onRequest(send_data);
}

void loop() {
    
    if (!setup_done)
        return;
    
    loop_sensors();
    
}

void reset_all_sensors() {
    for (int i = 0; i < MAX_SENSORS-1; i++) {
        reset_sensor(i);
    }
}

void reset_sensor(int index) {
    //free(sensors[index].mfrc522);
    sensors[index].type = UNDEFINED;
    sensors[index].pin = -1;
    sensors[index].int_value = 0;
    sensors[index].bool_value = LOW;
    sensors[index].beep_num = 0;
    sensors[index].beep_type = 0;
    sensors[index].timestamp = 0;
}

int get_free_sensor_index() {
    for (int i = 0; i < MAX_SENSORS; i++) {
        if (sensors[i].type == UNDEFINED) {
            reset_sensor(i);
            return i;
        }
    }
    return -1;
}

int set_sensor(int pin, int type, int id) {
    
    
    int index = get_free_sensor_index();
    
    
    if (index != -1 && get_sensor_by_id(id) == -1) {
        sensors[index].pin = pin;
        sensors[index].type = type;
        sensors[index].id = id;
        
        if (type == BUZZER || type == RELAY) {
            sensors[index].bool_value = LOW;
            pinMode(pin, OUTPUT);
        }
        else if (type == PIR) {
            pinMode(pin, INPUT);
        }
        else if (type == TEMP) {
            sensors[index].timestamp = 0;
        }
        return index;
    }
    return -1;
}

int set_sensor(int pins[], int type, int id) {
    
    if (pins[0] != 0 && pins[1] == 0) {
        return set_sensor(pins[0], type, id);
    }
    
    int index = get_free_sensor_index();
    
    if (index != -1 && get_sensor_by_id(id) == -1) {
        sensors[index].type = type;
        sensors[index].id = id;
        
        if (type == RFID) {
            MFRC522* mfrc522 = new MFRC522(pins[0], pins[1]);
            sensors[index].mfrc522 = mfrc522;
            sensors[index].mfrc522->PCD_Init();
        }
        return index;
    }
    return -1;
}

int get_sensor_by_type(int type, int index) {
    
    for (int i = 0; i < MAX_SENSORS; i++) {
        if (sensors[i].type == type) {
            if (index == 0)
                return i;
            index--;
        }
    }
    
    return -1;
}

int get_sensor_by_id(int id) {
    
    for (int i = 0; i < MAX_SENSORS; i++) {
        if (sensors[i].id == id && sensors[i].type != UNDEFINED) {
            return i;
        }
    }
    
    return -1;
}

void loop_sensors() {
    for (unsigned int i = 0; i < MAX_SENSORS; i++) {
        switch (sensors[i].type) {
            case TEMP:
                temp_loop(i);
                break;
            case RFID:
                rfid_loop(i);
                break;
            case PIR:
                pir_loop(i);
                break;
            case BUZZER:
                buzzer_loop(i);
                break;
            case RELAY:
                relay_loop(i);
                break;
            case VOLTMETER:
                voltmeter_loop(i);
                break;
            default:
                break;
        }
    }
    prepare_data();
}

void relay_loop(int index) {
    digitalWrite(sensors[index].pin, sensors[index].bool_value);
}

void temp_loop(int index) {
    if (millis() - sensors[index].timestamp > 2000) {
        int chk = DHT.read11(sensors[index].pin);
        int tries = 3;
        if (chk == DHTLIB_OK) {
            
            int i = (int)floor(DHT.temperature);
            char integer_string[32];
            integer_string[0] = 0;
            snprintf(integer_string, 32, "%d", i);
            strncpy(sensors[index].str_value, integer_string, 16 - strlen(sensors[index].str_value) - 1);
            sensors[index].str_value[strlen(sensors[index].str_value)] = ';';
            sensors[index].str_value[strlen(sensors[index].str_value) + 1] = 0;
            i = (int)floor(DHT.humidity);
            integer_string[0] = 0;
            snprintf(integer_string, 32, "%d", i);
            strncat(sensors[index].str_value, integer_string, 16 - strlen(sensors[index].str_value) - 1);
            
            sensors[index].timestamp = millis();
            
        } 
        else {
            sensors[index].str_value[0] = 0;
            sensors[index].timestamp = 0;
        }
    }
}

void voltmeter_loop(int index) {
    sensors[index].int_value=analogRead(sensors[index].pin);
}

void pir_loop(int index) {
    sensors[index].int_value = digitalRead(sensors[index].pin);
    sensors[index].bool_value = (sensors[index].int_value == 1 ? true : false);
}

void buzzer_loop(int index) {
    
    if (sensors[index].beep_num == 0)
        return;
        
    int millis_high = 150;
    int millis_low = 300;
    
    if (sensors[index].beep_type == 2) {
        millis_high = 300;
        millis_low = 500;
    }
    if (sensors[index].beep_type == 1) {
        millis_high = 100;
        millis_low = 200;
    }
    
    if (sensors[index].bool_value == HIGH && (millis() - sensors[index].timestamp) > millis_high) {
        sensors[index].bool_value = LOW;
        
        if (sensors[index].beep_num != 0) {
            sensors[index].timestamp = millis();
            sensors[index].beep_num--;
        }
        else {
            sensors[index].timestamp = 0;
        }
        digitalWrite(sensors[index].pin, sensors[index].bool_value);
    }
    else if (sensors[index].bool_value == LOW && (millis() - sensors[index].timestamp) > millis_low) {
        if (sensors[index].beep_num != 0) {
            sensors[index].bool_value = HIGH;
            sensors[index].timestamp = millis();
        }
        else {
            sensors[index].bool_value = LOW;
            sensors[index].timestamp = 0;
            sensors[index].beep_num = 0;
        }
        digitalWrite(sensors[index].pin, sensors[index].bool_value);
    }
}

void rfid_loop(int index) {

    if (sensors[index].mfrc522->PICC_IsNewCardPresent() && sensors[index].mfrc522->PICC_ReadCardSerial()) {
        
        
        sensors[index].timestamp = millis();
        
        String last_card = "";
        for (byte i = 0; i < sensors[index].mfrc522->uid.size; i++) {
            if (i!=0) last_card += String(sensors[index].mfrc522->uid.uidByte[i] < 0x10 ? "0" : "");
            last_card += String(sensors[index].mfrc522->uid.uidByte[i], HEX);
        }
        
        
        last_card.toCharArray(sensors[index].str_value, 16);
        
        sensors[index].mfrc522->PICC_HaltA();      // Halt PICC
        sensors[index].mfrc522->PCD_StopCrypto1(); // Stop encryption on PCD
    }
    
    if (millis() - sensors[index].timestamp > 1000) {
        sensors[index].timestamp = 0;
    }
}

void receive_data(int byteCount) {
    
    int numOfBytes = Wire.available();
    for(int i=0; i < numOfBytes && i < BUFFER_SIZE_IN - 1; i++) {
        in_msg[i] = Wire.read();
    }
    in_msg[min(numOfBytes, BUFFER_SIZE_IN - 1)] = 0;
    parse_data();
    
}

void parse_data() {
    
    if (in_msg[0] == START_MSG && in_msg[strlen(in_msg) - 1] == END_CHSUM) {
        
        char real_msg[strlen(in_msg)];
        char chsum_msg[strlen(in_msg)];
        bool chsum = false;
        for (int i = 0, j = 0, k = 0; i < strlen(in_msg); i++) {
            if (in_msg[i] == END_CHSUM) {
                break;
            }
            
            if (!chsum) {
                real_msg[j++] = in_msg[i];
                real_msg[j] = 0;
            }
            else {
                chsum_msg[k++] = in_msg[i];
                chsum_msg[k] = 0;
            }
            
            if (in_msg[i] == END_MSG)
                chsum = true;
        }
        
        
        int remote_chsum = 0;
        for (int i = 0; i < strlen(chsum_msg); i++) {
            remote_chsum = remote_chsum * 10 + (chsum_msg[i] - 48);
        }
        int local_chsum = get_checksum(real_msg);
        
        int argument = 0;
        int sensor_id = 0;
        int arguments[5] = { 0 };
        bool is_setup = false;
        int sensor_type = 0;
        if (local_chsum == remote_chsum) {
            for (int i = 0; i < strlen(real_msg); i++) {
                if (real_msg[i] != START_MSG && real_msg[i] != END_MSG) {
                    if (i == 1) {
                        
                        if (real_msg[i] == RESET_MSG) {
                            resetFunc();
                        }
                        
                        if (!setup_done && real_msg[i] != SETUP_MSG)
                            return;
                        
                        if (real_msg[i] == SETUP_MSG) {
                            // parse setup
                            is_setup = true;
                            continue;
                        }
                    }
                    if (real_msg[i] == SEPARATOR) {
                        // prepare for next argument
                        argument++;
                        arguments[argument - 1] = 0;
                        continue;
                    }
                    else if (real_msg[i] == SUPER_SEPARATOR) {
                        // reset all variables
                        if (is_setup) {
                            if (set_sensor(arguments,sensor_type,sensor_id) != -1) {
                                setup_done = true;
                            }
                        }
                        else {
                            sensor_put(sensor_id, arguments);
                        }
                        
                        argument = 0;
                        sensor_id = 0;
                        sensor_type = 0;
                        arguments[0] = 0;
                        arguments[1] = 0;
                        arguments[2] = 0;
                        arguments[3] = 0;
                        arguments[4] = 0;
                        continue;
                    }
                    
                    if (argument == 0) {
                        // sensor id
                        sensor_id = sensor_id * 10 + (real_msg[i] - 48);
                    }
                    else if (argument < 5) {
                        // arguments
                        if (is_setup && argument == 1) {
                            sensor_type = sensor_type * 10 + (real_msg[i] - 48);
                        }
                        else if (is_setup)
                            arguments[argument - 2] = arguments[argument - 2] * 10 + (real_msg[i] - 48);
                        else
                            arguments[argument - 1] = arguments[argument - 1] * 10 + (real_msg[i] - 48);
                    }
                }
                if (real_msg[i] == END_MSG) {
                    if (is_setup) {
                        if (set_sensor(arguments,sensor_type,sensor_id) != -1) {
                            setup_done = true;
                        }
                    }
                    else {
                        sensor_put(sensor_id, arguments);
                    }
                }
            }
        }
    }
}


void sensor_put(int sensor_id, int* params) {
    
    int index = get_sensor_by_id(sensor_id);
    if (index == -1)
        return;
        
    switch (sensors[index].type) {
        case BUZZER:
            if (params[0] > 0 && (params[1] == 0 || params[1] == 1 || params[1] == 2)) {
                sensors[index].beep_num = params[0];
                sensors[index].beep_type = params[1];
            }
            break;
        case RELAY:
            if (params[0] == 0 || params[0] == 1)
                sensors[index].bool_value = (params[0] == 0 ? false : true);
            break;        
    }
}


void send_data() {
    
    if (i2c_index == 0) {
        if (strlen(in_msg) > 0) {
            strncpy(curr_msg, in_msg, BUFFER_SIZE);
            in_msg[0] = 0;
        }
        else if (strlen(out_msg) > 0)
            strncpy(curr_msg, out_msg, BUFFER_SIZE);
        else {
            curr_msg[0] = '@';
            curr_msg[1] = 'n';
            curr_msg[2] = 'o';
            curr_msg[3] = 's';
            curr_msg[4] = 'e';
            curr_msg[5] = 't';
            curr_msg[6] = 'u';
            curr_msg[7] = 'p';
            curr_msg[8] = '!';
            curr_msg[9] = 0;
            
            char end_msg[2] = {END_CHSUM,0};
            char integer_string[32];
            integer_string[0] = 0;
            snprintf(integer_string, 32, "%d", get_checksum(curr_msg));
            strncat(curr_msg, integer_string, BUFFER_SIZE - strlen(curr_msg) - 1);
            strncat(curr_msg, end_msg, BUFFER_SIZE - strlen(curr_msg) - 1);
        }
    }

    Wire.write(curr_msg[i2c_index++]);
    
    if (i2c_index >= strlen(curr_msg)) {
         i2c_index = 0;
    }
}

void prepare_data() {
    
    char setup_msg[2] = {SETUP_MSG,0};
    char super_separator[2] = {SUPER_SEPARATOR,0};
    char separator[2] = {SEPARATOR,0};
    char start_msg[2] = {START_MSG,0};
    char end_msg[2] = {END_MSG,0};
    char end_chsum[2] = {END_CHSUM,0};

    out_msg[0] = START_MSG;
    out_msg[1] = 0;
    
    for (int i = 0; i < MAX_SENSORS; i++) {
        char integer_string[32];
        switch (sensors[i].type) {
            case TEMP:
                if (strlen(sensors[i].str_value) > 0) {
                    integer_string[0] = 0;
                    snprintf(integer_string, 32, "%d", sensors[i].id);
                    
                    strncat(out_msg, integer_string, BUFFER_SIZE - strlen(out_msg) - 1);
                    strncat(out_msg, separator, BUFFER_SIZE - strlen(out_msg) - 1);
                    strncat(out_msg, sensors[i].str_value, BUFFER_SIZE - strlen(out_msg) - 1);
                    strncat(out_msg, super_separator, BUFFER_SIZE - strlen(out_msg) - 1);
                }
                break;
            case RFID:
                if (sensors[i].timestamp != 0) {
                    integer_string[0] = 0;
                    snprintf(integer_string, 32, "%d", sensors[i].id);
                    
                    strncat(out_msg, integer_string, BUFFER_SIZE - strlen(out_msg) - 1);
                    strncat(out_msg, separator, BUFFER_SIZE - strlen(out_msg) - 1);
                    strncat(out_msg, sensors[i].str_value, BUFFER_SIZE - strlen(out_msg) - 1);
                    strncat(out_msg, super_separator, BUFFER_SIZE - strlen(out_msg) - 1);
                }
                break;
            case PIR:
                integer_string[0] = 0;
                snprintf(integer_string, 32, "%d", sensors[i].id);
                
                strncat(out_msg, integer_string, BUFFER_SIZE - strlen(out_msg) - 1);
                strncat(out_msg, separator, BUFFER_SIZE - strlen(out_msg) - 1);
                
                integer_string[0] = 0;
                snprintf(integer_string, 32, "%d", sensors[i].int_value);
                strncat(out_msg, integer_string, BUFFER_SIZE - strlen(out_msg) - 1);
                strncat(out_msg, super_separator, BUFFER_SIZE - strlen(out_msg) - 1);
                break;
            case BUZZER:
                // no values to return
                break;
            case RELAY:
                integer_string[0] = 0;
                snprintf(integer_string, 32, "%d", sensors[i].id);
                
                strncat(out_msg, integer_string, BUFFER_SIZE - strlen(out_msg) - 1);
                strncat(out_msg, separator, BUFFER_SIZE - strlen(out_msg) - 1);
                
                integer_string[0] = 0;
                snprintf(integer_string, 32, "%d", sensors[i].bool_value ? 1 : 0);
                strncat(out_msg, integer_string, BUFFER_SIZE - strlen(out_msg) - 1);
                strncat(out_msg, super_separator, BUFFER_SIZE - strlen(out_msg) - 1);
                break;
            case VOLTMETER:
                char integer_string[32];
                snprintf(integer_string, 32, "%d", sensors[i].id);
                
                strncat(out_msg, integer_string, BUFFER_SIZE - strlen(out_msg) - 1);
                strncat(out_msg, separator, BUFFER_SIZE - strlen(out_msg) - 1);
                
                integer_string[0] = 0;
                snprintf(integer_string, 32, "%d", sensors[i].int_value);
                strncat(out_msg, integer_string, BUFFER_SIZE - strlen(out_msg) - 1);
                strncat(out_msg, super_separator, BUFFER_SIZE - strlen(out_msg) - 1);
                break;
            default:
                break;
        }
    }
    
    if (out_msg[strlen(out_msg) - 1] == SUPER_SEPARATOR)
        out_msg[strlen(out_msg) - 1] = 0;
    
    strncat(out_msg, end_msg, BUFFER_SIZE - strlen(out_msg) - 1);
    
    char integer_string[32];
    integer_string[0] = 0;
    snprintf(integer_string, 32, "%d", get_checksum(out_msg));
    strncat(out_msg, integer_string, BUFFER_SIZE - strlen(out_msg) - 1);
    strncat(out_msg, end_chsum, BUFFER_SIZE - strlen(out_msg) - 1);
    
}

int freeRam () {
  extern int __heap_start, *__brkval; 
  int v; 
  return (int) &v - (__brkval == 0 ? (int) &__heap_start : (int) __brkval); 
}

int get_checksum(char *string) {
    
    int i;
    int XOR = 0;
    int c;
    for (int i = 0; i < strlen(string); i++) {
        XOR ^= (int)string[i];
    }
    return XOR;
}
