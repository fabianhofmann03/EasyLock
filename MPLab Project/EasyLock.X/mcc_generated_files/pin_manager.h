/**
  @Generated Pin Manager Header File

  @Company:
    Microchip Technology Inc.

  @File Name:
    pin_manager.h

  @Summary:
    This is the Pin Manager file generated using PIC10 / PIC12 / PIC16 / PIC18 MCUs

  @Description
    This header file provides APIs for driver for .
    Generation Information :
        Product Revision  :  PIC10 / PIC12 / PIC16 / PIC18 MCUs - 1.81.7
        Device            :  PIC16F1827
        Driver Version    :  2.11
    The generated drivers are tested against the following:
        Compiler          :  XC8 2.31 and above
        MPLAB 	          :  MPLAB X 5.45	
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

#ifndef PIN_MANAGER_H
#define PIN_MANAGER_H

/**
  Section: Included Files
*/

#include <xc.h>

#define INPUT   1
#define OUTPUT  0

#define HIGH    1
#define LOW     0

#define ANALOG      1
#define DIGITAL     0

#define PULL_UP_ENABLED      1
#define PULL_UP_DISABLED     0

// get/set LOCK aliases
#define LOCK_TRIS                 TRISAbits.TRISA0
#define LOCK_LAT                  LATAbits.LATA0
#define LOCK_PORT                 PORTAbits.RA0
#define LOCK_ANS                  ANSELAbits.ANSA0
#define LOCK_SetHigh()            do { LATAbits.LATA0 = 1; } while(0)
#define LOCK_SetLow()             do { LATAbits.LATA0 = 0; } while(0)
#define LOCK_Toggle()             do { LATAbits.LATA0 = ~LATAbits.LATA0; } while(0)
#define LOCK_GetValue()           PORTAbits.RA0
#define LOCK_SetDigitalInput()    do { TRISAbits.TRISA0 = 1; } while(0)
#define LOCK_SetDigitalOutput()   do { TRISAbits.TRISA0 = 0; } while(0)
#define LOCK_SetAnalogMode()      do { ANSELAbits.ANSA0 = 1; } while(0)
#define LOCK_SetDigitalMode()     do { ANSELAbits.ANSA0 = 0; } while(0)

// get/set BUTTON aliases
#define BUTTON_TRIS                 TRISBbits.TRISB0
#define BUTTON_LAT                  LATBbits.LATB0
#define BUTTON_PORT                 PORTBbits.RB0
#define BUTTON_WPU                  WPUBbits.WPUB0
#define BUTTON_SetHigh()            do { LATBbits.LATB0 = 1; } while(0)
#define BUTTON_SetLow()             do { LATBbits.LATB0 = 0; } while(0)
#define BUTTON_Toggle()             do { LATBbits.LATB0 = ~LATBbits.LATB0; } while(0)
#define BUTTON_GetValue()           PORTBbits.RB0
#define BUTTON_SetDigitalInput()    do { TRISBbits.TRISB0 = 1; } while(0)
#define BUTTON_SetDigitalOutput()   do { TRISBbits.TRISB0 = 0; } while(0)
#define BUTTON_SetPullup()          do { WPUBbits.WPUB0 = 1; } while(0)
#define BUTTON_ResetPullup()        do { WPUBbits.WPUB0 = 0; } while(0)

// get/set RB1 procedures
#define RB1_SetHigh()            do { LATBbits.LATB1 = 1; } while(0)
#define RB1_SetLow()             do { LATBbits.LATB1 = 0; } while(0)
#define RB1_Toggle()             do { LATBbits.LATB1 = ~LATBbits.LATB1; } while(0)
#define RB1_GetValue()              PORTBbits.RB1
#define RB1_SetDigitalInput()    do { TRISBbits.TRISB1 = 1; } while(0)
#define RB1_SetDigitalOutput()   do { TRISBbits.TRISB1 = 0; } while(0)
#define RB1_SetPullup()             do { WPUBbits.WPUB1 = 1; } while(0)
#define RB1_ResetPullup()           do { WPUBbits.WPUB1 = 0; } while(0)
#define RB1_SetAnalogMode()         do { ANSELBbits.ANSB1 = 1; } while(0)
#define RB1_SetDigitalMode()        do { ANSELBbits.ANSB1 = 0; } while(0)

// get/set RB2 procedures
#define RB2_SetHigh()            do { LATBbits.LATB2 = 1; } while(0)
#define RB2_SetLow()             do { LATBbits.LATB2 = 0; } while(0)
#define RB2_Toggle()             do { LATBbits.LATB2 = ~LATBbits.LATB2; } while(0)
#define RB2_GetValue()              PORTBbits.RB2
#define RB2_SetDigitalInput()    do { TRISBbits.TRISB2 = 1; } while(0)
#define RB2_SetDigitalOutput()   do { TRISBbits.TRISB2 = 0; } while(0)
#define RB2_SetPullup()             do { WPUBbits.WPUB2 = 1; } while(0)
#define RB2_ResetPullup()           do { WPUBbits.WPUB2 = 0; } while(0)
#define RB2_SetAnalogMode()         do { ANSELBbits.ANSB2 = 1; } while(0)
#define RB2_SetDigitalMode()        do { ANSELBbits.ANSB2 = 0; } while(0)

// get/set HALL aliases
#define HALL_TRIS                 TRISBbits.TRISB3
#define HALL_LAT                  LATBbits.LATB3
#define HALL_PORT                 PORTBbits.RB3
#define HALL_WPU                  WPUBbits.WPUB3
#define HALL_ANS                  ANSELBbits.ANSB3
#define HALL_SetHigh()            do { LATBbits.LATB3 = 1; } while(0)
#define HALL_SetLow()             do { LATBbits.LATB3 = 0; } while(0)
#define HALL_Toggle()             do { LATBbits.LATB3 = ~LATBbits.LATB3; } while(0)
#define HALL_GetValue()           PORTBbits.RB3
#define HALL_SetDigitalInput()    do { TRISBbits.TRISB3 = 1; } while(0)
#define HALL_SetDigitalOutput()   do { TRISBbits.TRISB3 = 0; } while(0)
#define HALL_SetPullup()          do { WPUBbits.WPUB3 = 1; } while(0)
#define HALL_ResetPullup()        do { WPUBbits.WPUB3 = 0; } while(0)
#define HALL_SetAnalogMode()      do { ANSELBbits.ANSB3 = 1; } while(0)
#define HALL_SetDigitalMode()     do { ANSELBbits.ANSB3 = 0; } while(0)

/**
   @Param
    none
   @Returns
    none
   @Description
    GPIO and peripheral I/O initialization
   @Example
    PIN_MANAGER_Initialize();
 */
void PIN_MANAGER_Initialize (void);

/**
 * @Param
    none
 * @Returns
    none
 * @Description
    Interrupt on Change Handling routine
 * @Example
    PIN_MANAGER_IOC();
 */
void PIN_MANAGER_IOC(void);



#endif // PIN_MANAGER_H
/**
 End of File
*/