#include <stdio.h>
#include <stdint.h>
#include <avr/io.h>
#include <avr/interrupt.h>
#include "util.h"
#include "usart.h"

static void motor_set(uint8_t power, bool_t dir)
{
    OCR2A = power;
    if (dir) {
        PORTB |= (1<<4);
    } else {
        PORTB &= ~(1<<4);
    }
    if (power) LED_ON;
    else LED_OFF;
}

ISR(TIMER1_COMPA_vect) {
    motor_set(0, 0);
}

static void motor_init(void)
{
    DDRB  |= (1<<DDB4)   // PB4 is output (DIR)
        |    (1<<DDB3);  // PB3(OC2A) is output (PWM)
    TCCR2A = (1<<COM2A1) // Non-inverting PWM on OC2A
        |    (1<<WGM20); // PWM, Phase Correct, TOP=0xFF
    TCCR2B = (1<<CS22);  // clk_T2S/64 => f_PWM = 1/(1/(F_CPU/64.)*510) = 490Hz
}

int main(void)
{
    int joy;
    char *line;

    DDRB |= (1<<DDB5);  // LED
    LED_ON;
    _delay_ms(100);
    LED_OFF;

    motor_init();
    usart_init();

    // Setup Timer/Counter1 to interrupt once/sec
    OCR1A = F_CPU/1024;
    TCCR1B = (1<<WGM12)   // Mode 4 CTC
        |    (1<<CS12)    // clk_IO/1024
        |    (1<<CS10);   // "
    TIMSK1 = (1<<OCIE1A); // Enable interrupt
    
    sei();

    while (1) {
        line = usart_readline(TRUE);
        if (sscanf(line, "$J %d", &joy) == 1) {
            TCNT1 = 0; // reset timer
            motor_set(ABS(joy), joy > 0 ? 1 : 0);
        }
    }
    
    return 0;
}
