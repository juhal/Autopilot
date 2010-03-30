#include <stdio.h>
#include <stdint.h>
#include <avr/io.h>
#include "util.h"
#include "usart.h"

static void motor_init(void)
{
    DDRB  |= (1<<DDB4)   // PB4 is output (DIR)
        |    (1<<DDB3);  // PB3(OC2A) is output (PWM)
    TCCR2A = (1<<COM2A1) // Non-inverting PWM on OC2A
        |    (1<<WGM20); // PWM, Phase Correct, TOP=0xFF
    TCCR2B = (1<<CS22);  // clk_T2S/64 => f_PWM = 1/(1/(F_CPU/64.)*510) = 490Hz
}

static void motor_set(uint8_t power, bool_t dir)
{
    OCR2A = power;
    if (dir) {
        PORTB |= (1<<4);
    } else {
        PORTB &= ~(1<<4);
    }
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

    while (1) {
        line = usart_readline(TRUE);
        if (sscanf(line, "$J %d", &joy) == 1) {
            motor_set(ABS(joy), joy > 0 ? 1 : 0);
            if (joy != 0) {
                LED_ON;
            } else {
                LED_OFF;
            }
        }
    }
    
    return 0;
}
