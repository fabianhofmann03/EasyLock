/**
  Generated Main Source File

  Company:
    Microchip Technology Inc.

  File Name:
    main.c

  Summary:
    This is the main file generated using PIC10 / PIC12 / PIC16 / PIC18 MCUs

  Description:
    This header file provides implementations for driver APIs for all modules selected in the GUI.
    Generation Information :
        Product Revision  :  PIC10 / PIC12 / PIC16 / PIC18 MCUs - 1.81.7
        Device            :  PIC16F1827
        Driver Version    :  2.00
 */

/*
    (c) 2018 Microchip Technology Inc. and its subsidiaries. 
    
    Subject to your compliance with these terms, you may use Microchip software and any 
    derivatives exclusively with Microchip products. It is your responsibility to comply with third party 
    license terms applicable to your use of third party software (including open source software) that 
    may accompany Microchip software.
    
    THIS SOFTWARE IS SUPPLIED BY MICROCHIP "AS IS". NO WARRANTIES, WHETHER 
    EXPRESS, IMPLIED OR STATUTORY, APPLY TO THIS SOFTWARE, INCLUDING ANY 
    IMPLIED WARRANTIES OF NON-INFRINGEMENT, MERCHANTABILITY, AND FITNESS 
    FOR A PARTICULAR PURPOSE.
    
    IN NO EVENT WILL MICROCHIP BE LIABLE FOR ANY INDIRECT, SPECIAL, PUNITIVE, 
    INCIDENTAL OR CONSEQUENTIAL LOSS, DAMAGE, COST OR EXPENSE OF ANY KIND 
    WHATSOEVER RELATED TO THE SOFTWARE, HOWEVER CAUSED, EVEN IF MICROCHIP 
    HAS BEEN ADVISED OF THE POSSIBILITY OR THE DAMAGES ARE FORESEEABLE. TO 
    THE FULLEST EXTENT ALLOWED BY LAW, MICROCHIP'S TOTAL LIABILITY ON ALL 
    CLAIMS IN ANY WAY RELATED TO THIS SOFTWARE WILL NOT EXCEED THE AMOUNT 
    OF FEES, IF ANY, THAT YOU HAVE PAID DIRECTLY TO MICROCHIP FOR THIS 
    SOFTWARE.
 */

#include "mcc_generated_files/mcc.h"
#include <stdio.h>
#include <stdlib.h>

#define data_storage_size 127
#define password_position 4

#define receive_timer_time 50
#define cmd_timer_time 1000
#define continue_time 12000
#define button_timer_time 50


/*
                         Main application
 */

enum lockstates {
    IDLE, OPENLOCK, WAITFOROPEN, WAITFORCLOSE
};
enum lockstates working_state = IDLE;

enum commandcontroll {
    WAITFORCMD, READCMD, READREQ, READDATLEN, READDATVAR, READDAT, READSTAT, COMP
};
enum commandcontroll cmd_state = WAITFORCMD;

enum commandexecution {
    WAITTOSTART, EXECMD
};
enum commandexecution cmd_exe = WAITTOSTART;

enum command {
    CHANGE_PASSWORD = 1, OPEN_LOCK = 2, START_CONFIG = 3
};

enum variable {
    PASSWORD = 1, NEW_PASSWORD = 2, LOCK_STATUS = 3
};

enum status_message {
    COMMAND_EXECUTED = 1, RECEIVED = 2, DENY = 3, TOO_MUCH_DATA = 4, MESSAGE_CORRUPTED = 5, LOCK_OPEND = 6, LOCK_CLOSED = 7, CANCELD = 8, CONTINUE = 9
};

uint8_t cmd;
uint8_t data_storage[data_storage_size + 2];                //First byte:   [xxxxxxxx]
                                                            //              [x-------] 1: Data completly loaded, 0: Data still loading
                                                            //              [-xxxxxxx] What variable is stored
                                                            //Second byte:  [xxxxxxxx] Length of Data (max 127)

uint8_t request_storage;                                    //  [x-------] 1: Request has come, 0: No request
                                                            //  [-xxxxxxx] Requested Variable

uint8_t status_storage;                                     //  [x-------] 1: State has come, 0: No state
                                                            //  [-xxxxxxx] Last State

uint8_t hall_codes[] = {127, 103};                          // 1: Open door
                                                            // 2: Closed door

int cmd_exe_counter = 0;
bool lock_status = false;

uint8_t new_hall_code_open = 0;
uint8_t new_hall_code_close = 0;

void del_array(uint8_t *array, int16_t len);

void send_msg_stopping(uint8_t *msg, uint8_t len);
void send_cmd(uint8_t cmd);
void send_req(uint8_t var);
void send_dat(uint8_t *dat, uint8_t len, uint8_t var);
void send_stat(uint8_t stat);
bool compare_pw(uint8_t *pw, uint8_t len);
void save_pw(uint8_t *pw, uint8_t len);
void read_hall_codes(void);
void save_hall_codes(uint8_t open, uint8_t close);
bool wait_for_requested(uint8_t req);
void stop_cmd(void);
int seek_confirmation(int wait_num, int continue_num, int retry_num, int cancel_num);
bool door_status_changed(bool open_or_close, int16_t val);

long receive_time = 0;
long receive_timer_len = 0;
long cmd_time = 0;
long cmd_timer_len = 0;
long button_time = 0;
bool button_status = false;
bool button = false;

void timer_up() {
    if(receive_timer_len > 0) receive_time++;
    if(cmd_timer_len > 0) cmd_time++;
    if(button_status) button_time++;
    button = !BUTTON_GetValue();
    if(button != button_status) {
        if(button_status && button_time < button_timer_time) {
            if(lock_status) {
                working_state = WAITFORCLOSE;
            }else {
                working_state = OPENLOCK;
            }
        }else {
            button_time = 0;
        }
    }
    button_status = button;
}

void main(void) {
    // initialize the device
    SYSTEM_Initialize();
    EUSART_Initialize();
    ADC_Initialize();
    TMR2_Initialize();

    // When using interrupts, you need to set the Global and Peripheral Interrupt Enable bits
    // Use the following macros to:

    // Enable the Global Interrupts
    INTERRUPT_GlobalInterruptEnable();

    // Enable the Peripheral Interrupts
    INTERRUPT_PeripheralInterruptEnable();

    ADC_StartConversion();
    TMR2_SetInterruptHandler(timer_up);
    
    read_hall_codes();
    
    while (1) {
        if(receive_timer_len > 0 && receive_time > receive_timer_len) {
            receive_time = 0;
            receive_timer_len = 0;
            stop_cmd();
        }
        if(cmd_timer_len > 0 && cmd_time > cmd_timer_len) {
            send_stat(CANCELD);
            stop_cmd();
        }
        if (EUSART_is_rx_ready()) {
            receive_timer_len = receive_timer_time;
            receive_time = 0;
            uint8_t input = EUSART_Read();
            static uint8_t control_byte;
            static uint8_t data_storage_counter = 0;
            static enum commandcontroll last_state = WAITFORCMD;
            switch (cmd_state) {
                case WAITFORCMD:
                    last_state = cmd_state;
                    if (input == 0b10000001) cmd_state = READCMD;
                    else if (input == 0b01000010) cmd_state = READREQ;
                    else if (input == 0b00100100) cmd_state = READDATVAR;
                    else if (input == 0b00011000) cmd_state = READSTAT;
                    control_byte = input;
                    break;
                case READCMD:
                    cmd = input;
                    last_state = cmd_state;
                    cmd_state = COMP;
                    break;
                case READREQ:
                    request_storage = input;
                    last_state = cmd_state;
                    cmd_state = COMP;
                    break;
                case READDATVAR:
                    data_storage[0] = input;
                    last_state = cmd_state;
                    cmd_state = READDATLEN;
                    break;
                case READDATLEN:
                    data_storage[1] = input;
                    last_state = cmd_state;
                    cmd_state = READDAT;
                    if(data_storage[1] == 0) cmd_state = COMP;
                    break;
                case READDAT:
                    if (data_storage[1] <= data_storage_size) data_storage[data_storage_counter + 2] = input;
                    data_storage_counter++;
                    if (data_storage_counter == (data_storage[1])) {
                        data_storage_counter = 0;
                        last_state = cmd_state;
                        cmd_state = COMP;
                    }
                    break;
                case READSTAT:
                    status_storage = input;
                    last_state = cmd_state;
                    status_storage = status_storage | (1<<7);
                    cmd_state = WAITFORCMD;
                    receive_timer_len = 0;
                    break;
                case COMP:
                    if (control_byte == input) {
                        switch (last_state) {
                            case READCMD:
                                if (control_byte != input) {
                                    send_stat(MESSAGE_CORRUPTED);
                                } else {
                                    stop_cmd();
                                    send_stat(RECEIVED);
                                    cmd_exe = EXECMD;
                                }
                                break;
                            case READREQ:
                                if (control_byte != input) {
                                    send_stat(MESSAGE_CORRUPTED);
                                } else {
                                    send_stat(RECEIVED);
                                    request_storage = request_storage | (1 << 7);
                                }
                                break;
                            case READDAT:
                                if (data_storage[1] > data_storage_size) {
                                    send_stat(TOO_MUCH_DATA);
                                } else if (control_byte != input) {
                                    send_stat(MESSAGE_CORRUPTED);
                                } else {
                                    send_stat(RECEIVED);
                                    data_storage[0] = data_storage[0] | (1 << 7);
                                }
                                break;
                            case READDATLEN:
                                if (data_storage[1] > data_storage_size) {
                                    send_stat(TOO_MUCH_DATA);
                                } else if (control_byte != input) {
                                    send_stat(MESSAGE_CORRUPTED);
                                } else {
                                    send_stat(RECEIVED);
                                    data_storage[0] = data_storage[0] | (1 << 7);
                                }
                                break;
                        }
                    }
                    last_state = cmd_state;
                    cmd_state = WAITFORCMD;
                    receive_timer_len = 0;
                    receive_time = 0;
                    break;
            }
            if (last_state != WAITFORCMD) control_byte = control_byte ^ input;
        }
        
        if((status_storage & (1<<7)) > 0) {
            switch(status_storage & (~(1<<7))) {
                case CANCELD:
                    if(lock_status == true) {
                        ADC_StartConversion();
                        working_state = WAITFORCLOSE;
                    }
                    stop_cmd();
                    status_storage = status_storage & (~(1<<7));
                    break;
            }
        }
        if((request_storage & (1<<7)) > 0) {
            switch(request_storage & (~(1<<7))) {
                case LOCK_STATUS:
                    if(lock_status) {
                        send_stat(LOCK_OPEND);
                    }else {
                        send_stat(LOCK_CLOSED);
                    }
                    request_storage = status_storage & (~(1<<7));
                    break;
            }
        }
        static int old_cmd_exe_counter = 0;
        switch (cmd_exe) {
            case WAITTOSTART:
                    cmd_timer_len = 0;
               break;
            case EXECMD:
                if(old_cmd_exe_counter != cmd_exe_counter) {
                    cmd_time = 0;
                    cmd_timer_len = cmd_timer_time;
                }
                old_cmd_exe_counter = cmd_exe_counter;
                switch (cmd) {
                    case CHANGE_PASSWORD:
                        switch (cmd_exe_counter) {
                            case 0:
                                send_req(PASSWORD);
                                cmd_exe_counter++;
                                break;
                            case 1:
                                cmd_exe_counter = seek_confirmation(1, 2, 0, 255);
                                break;
                            case 2:
                                if(wait_for_requested(PASSWORD)) {
                                    if(compare_pw(&data_storage[2], data_storage[1]) || !BUTTON_GetValue()) {
                                        cmd_exe_counter++;
                                    }else {
                                        send_stat(DENY);
                                        stop_cmd();
                                    }
                                }
                                break;
                            case 3:
                                send_req(NEW_PASSWORD);
                                cmd_exe_counter++;
                                break;
                            case 4:
                                cmd_exe_counter = seek_confirmation(4, 5, 3, 255);
                                break;
                            case 5:
                                if(wait_for_requested(NEW_PASSWORD)) {
                                    save_pw(&data_storage[2], data_storage[1]);
                                    send_stat(COMMAND_EXECUTED);
                                    stop_cmd();
                                }
                                break;
                            case 255:
                                send_stat(CANCELD);
                                stop_cmd();
                                break;
                        }
                        break; 
                    case OPEN_LOCK:
                        switch (cmd_exe_counter) {
                            case 0:
                                if(lock_status == true) {
                                    working_state = WAITFORCLOSE;
                                    send_stat(COMMAND_EXECUTED);
                                    stop_cmd();
                                }else {
                                    send_req(PASSWORD);
                                    cmd_exe_counter++;
                                }
                                break;
                            case 1:
                                cmd_exe_counter = seek_confirmation(1, 2, 0, 255);
                                break;
                            case 2:
                                if(wait_for_requested(PASSWORD)) {
                                    if(compare_pw(&data_storage[2], data_storage[1]) || !BUTTON_GetValue()) {
                                        working_state = OPENLOCK;
                                        send_stat(COMMAND_EXECUTED);
                                    }else {
                                        send_stat(DENY);
                                    }
                                    stop_cmd();
                                }
                                break;
                            case 255:
                                send_stat(CANCELD);
                                stop_cmd();
                                break;
                        }
                        break;
                    
                    case START_CONFIG:
                        switch (cmd_exe_counter) {
                            case 0:
                                send_req(PASSWORD);
                                cmd_exe_counter++;
                                break;
                            case 1:
                                cmd_exe_counter = seek_confirmation(1, 2, 0, 255);
                                break;
                            case 2:
                                if(wait_for_requested(PASSWORD)) {
                                    if(compare_pw(&data_storage[2], data_storage[1]) || !BUTTON_GetValue()) {
                                        working_state = IDLE;
                                        LOCK_SetHigh();
                                        send_stat(LOCK_OPEND);
                                        lock_status = true;
                                        send_stat(CONTINUE);
                                        cmd_exe_counter ++;
                                    }else {
                                        send_stat(DENY);
                                        stop_cmd();
                                    }
                                    
                                }
                                break;
                            case 3:
                                cmd_timer_len = continue_time;
                                if((status_storage & (1<<7)) > 0 && (status_storage & (~(1<<7))) == CONTINUE) {
                                    ADC_StartConversion();
                                    cmd_exe_counter ++;
                                }
                                status_storage = status_storage & (~(1<<7));
                                break;
                            case 4:
                                if(ADC_IsConversionDone()) {
                                    new_hall_code_close = ADC_GetConversionResult();
                                    send_stat(CONTINUE);
                                    cmd_exe_counter ++;
                                }
                                break;
                            case 5:
                                if((status_storage & (1<<7)) > 0 && (status_storage & (~(1<<7))) == CONTINUE) {
                                    ADC_StartConversion();
                                    cmd_exe_counter ++;
                                }
                                status_storage = status_storage & (~(1<<7));
                                break;
                            case 6:
                                if(ADC_IsConversionDone()) {
                                    new_hall_code_open = ADC_GetConversionResult();
                                    send_stat(COMMAND_EXECUTED);
                                    save_hall_codes(new_hall_code_open, new_hall_code_close);
                                    read_hall_codes();
                                    working_state = OPENLOCK;
                                    stop_cmd();
                                    new_hall_code_open = 0;
                                    new_hall_code_close = 0;
                                }
                                break;
                            case 255:
                                send_stat(CANCELD);
                                stop_cmd();
                                new_hall_code_open = 0;
                                new_hall_code_close = 0;
                                break;
                        }
                        break;
                    
                }
                break;
        }

        switch (working_state) {
            case IDLE:
                
                break;
            case OPENLOCK:
                LOCK_SetHigh();
                send_stat(LOCK_OPEND);
                lock_status = true;
                working_state = WAITFOROPEN;
                ADC_StartConversion();
                break;
            case WAITFOROPEN:
                if(ADC_IsConversionDone()) {
                    uint8_t res = ADRESH;
                    if(door_status_changed(true, res)) {
                        working_state = WAITFORCLOSE;
                    }
                    ADC_StartConversion();
                }
                break;
            case WAITFORCLOSE:
                if(ADC_IsConversionDone()) {
                    uint8_t res = ADRESH;
                    if(door_status_changed(false, res)) {
                        send_stat(LOCK_CLOSED);
                        lock_status = false;
                        working_state = IDLE;
                        LOCK_SetLow();
                    }
                    ADC_StartConversion();
                }
                break;
        }
    }
}

void del_array(uint8_t *array, int16_t len) {
    for (int16_t x = 0; x < len; x++) *(array + x) = 0;
}

void send_msg_stopping(uint8_t *msg, uint8_t len) {
    for (int x = 0; x < len; x++) {
        while (!EUSART_is_tx_done());
        EUSART_Write(*(msg + x));
    }
}

void send_cmd(uint8_t cmd) {
    uint8_t command_protocol[] = {0b10000001, cmd, 0};
    command_protocol[2] = command_protocol[0] ^ command_protocol[1];
    send_msg_stopping(command_protocol, 3);
    status_storage = 0;
}

void send_req(uint8_t var) {
    del_array(data_storage, data_storage_size + 2);
    uint8_t request_protocol[] = {0b01000010, var, 0};
    request_protocol[2] = request_protocol[0] ^ request_protocol[1];
    send_msg_stopping(request_protocol, 3);
    status_storage = 0;
}

void send_dat(uint8_t *dat, uint8_t len, uint8_t var) {
    uint8_t control_byte = 0b00100100;
    control_byte = (control_byte ^ len) ^ var;
    send_msg_stopping(0b00100100, 1);
    for (int x = 0; x < len; x++) {
        control_byte = control_byte ^ *(dat + x);
    }
    send_msg_stopping(&len, 1);
    send_msg_stopping(&var, 1);
    send_msg_stopping(dat, len);
    send_msg_stopping(&control_byte, 1);
    status_storage = 0;
}

void send_stat(uint8_t stat) {
    uint8_t status_protocol[] = {0b00011000, stat};
    send_msg_stopping(status_protocol, 2);
}

bool compare_pw(uint8_t *pw, uint8_t len) {
    bool res = true;
    uint8_t eedata = DATAEE_ReadByte(password_position);
    if (eedata != len) {
        res = false;
    } else {
        for (int x = 0; x < len; x++) {
            eedata = DATAEE_ReadByte(x + password_position + 1);
            uint8_t indata = *(pw + x);
            if (eedata != indata) {
                res = false;
                break;
            }
        }
    }
    return res;
}

void save_pw(uint8_t *pw, uint8_t len) {
    if (DATAEE_ReadByte(password_position) != len) DATAEE_WriteByte(password_position, len);
    for (int x = 0; x < len; x++) if (DATAEE_ReadByte(x + password_position + 1) != *(pw + x)) DATAEE_WriteByte(x + password_position + 1, *(pw + x));
}

void read_hall_codes(void) {
    hall_codes[0] = (uint8_t) DATAEE_ReadByte(0);
    hall_codes[1] = (uint8_t) DATAEE_ReadByte(1);
}

void save_hall_codes(uint8_t open, uint8_t close) {
    if (DATAEE_ReadByte(0) != open) DATAEE_WriteByte(0, open);
    if (DATAEE_ReadByte(1) != close) DATAEE_WriteByte(1, close);
}

int seek_confirmation(int wait_num, int continue_num, int retry_num, int cancel_num) {
    if((status_storage & (1<<7)) > 0) {
        switch(status_storage & (~(1<<7))) {
            case 0b00000010:
                return continue_num;
                break;
            case 0b00000100:
                return cancel_num;
                break;
            case 0b00000101:
                return retry_num;
                break;
        }
        status_storage = status_storage & (~(1<<7));
    }
    return wait_num;
}

bool wait_for_requested(uint8_t req) {
    if(((data_storage[0] & (1<<7)) > 0) && (req == (data_storage[0] & (~(1<<7))))) {
        return true;
    }
    return false;
}

void stop_cmd(void) {
    cmd_time = 0;
    cmd_timer_len = 0;
    cmd_exe_counter = 0;
    cmd_exe = WAITTOSTART;
}

bool door_status_changed(bool open_or_close, int16_t val) {                  //true: open, false: close
    if(open_or_close) {
        if((hall_codes[0] > hall_codes[1] && val >= hall_codes[1] + ((hall_codes[0] - hall_codes[1]) * 9 / 10)) || (hall_codes[0] < hall_codes[1] && val <= hall_codes[1] - ((hall_codes[1] - hall_codes[0]) * 9 / 10))) return true;
    }else {
        if((hall_codes[1] > hall_codes[0] && val >= hall_codes[0] + ((hall_codes[1] - hall_codes[0]) * 9 / 10)) || (hall_codes[1] < hall_codes[0] && val <= hall_codes[0] - ((hall_codes[0] - hall_codes[1]) * 9 / 10))) return true;
    }
    return false;
}

/**
 End of File
 */