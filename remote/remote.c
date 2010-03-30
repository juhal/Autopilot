#include <stdint.h>
#include <avr/io.h>
#include <avr/sleep.h>
#include <avr/interrupt.h>
#include "xbee.h"
#include "util.h"
#include "joy.h"

#define WAIT_TIME 5
static volatile uint8_t wait_time;

ISR(TIMER2_OVF_vect) 
{
    if (wait_time)
        --wait_time;
}

int main(void)
{
    int joy_new, joy = 0;

    DDRB  |= (1<<DDB5);  // LED
    LED_ON;
    _delay_ms(100);
    LED_OFF;

    TIMSK2 = (1<<TOIE2); // Overflow interrupt enabled
    TCCR2B = (1<<CS22)   // clk_IO/1024 => overflow freq 30.63Hz
        |    (1<<CS21)
        |    (1<<CS20);

    sei();
    xbee_init();
    joy_init();

    while (1) {
        if (!wait_time) {
            wait_time = WAIT_TIME;
            joy_new = joy_get();
            if (joy_new || joy) {
                joy = joy_new;
                xbee_printf("$J %d\r", joy);
            }
        }
        set_sleep_mode(SLEEP_MODE_IDLE);
        sleep_mode();
    }
    
    return 0;
}
