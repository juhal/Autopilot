#include <stdint.h>
#include <avr/io.h>
#include "util.h"

#define MAX_JOY 1021

static uint16_t base_level;

uint16_t joy_read(void)
{
    uint16_t adc;
    PORTC  |= (1<<1);    // Joystick VCC on
    ADCSRA |= (1<<ADEN)  // enabled ADC
        |     (1<<ADIF)  // clear ADC interrupt flag
        |     (1<<ADSC); // start ADC
    while (!(ADCSRA & (1<<ADIF))); // Wait for ADC to complete
    adc = ADC;
    ADCSRA &= ~(1<<ADEN);// Disable ADC
    PORTC  &= ~(1<<1);   // Joystick VCC off
    return adc;
}

void joy_init(void)
{
    DDRC  |= (1<<DDC1);  // Joystick VCC is output

    // ADC initialization
    DIDR0  = 0xff;       // Digital input buffers off on ADC0-7 (saves power)
    ADCSRA = (1<<ADEN)   // ADC Enable
        |    (1<<ADPS2)  // ADC Prescaler division factor 128 ...
        |    (1<<ADPS1)  // ...
        |    (1<<ADPS0); // ...
    ADMUX |= (1<<REFS0); // AVcc with external capacitor at AREF
                         // ADC0

    base_level = joy_read();
}

int joy_get(void)
{
    int joy = joy_read();
    if (joy > base_level) {
        joy = 255L*(joy-base_level)/(MAX_JOY-base_level);
    } else if (joy < base_level) {
        joy = -255L*(base_level-joy)/base_level;
    } else {
        joy = 0;
    }
    if (joy > 253) return 255;
    if (joy < -253) return -255;
    return joy;
}
