#include <stdio.h>
#include <stdarg.h>
#include <avr/io.h>
#include "util.h"
#include "xbee.h"

void xbee_init()
{
    UCSR0B = (1<<TXEN0);
    #define BAUD 9600
    #include <util/setbaud.h>
    UBRR0H = UBRRH_VALUE;
    UBRR0L = UBRRL_VALUE;
    #if USE_2X
    UCSR0A |= (1<<U2X0);
    #else
    UCSR0A &= ~(1<<U2X0);
    #endif
    DDRD |= (1<<DDD2); // Sleep_RQ
    xbee_off();
}

void xbee_putc(char c)
{
    while (!(UCSR0A & (1<<UDRE0)));
    UDR0 = c;
}

void xbee_write(char *buf, uint8_t n)
{
    UCSR0A &= ~(1<<TXC0);
    for (uint8_t i = 0; i < n; ++i)
        xbee_putc(buf[i]);
    while (!(UCSR0A & (1<<TXC0)));
}

void xbee_printf(const char *fmt, ...)
{
    static char buf[32];
    va_list ap;
    uint8_t n;

    va_start(ap, fmt);
    n = vsnprintf(buf, sizeof(buf), fmt, ap);
    va_end(ap);
    
    xbee_write(buf, MIN(n,sizeof(buf)));
}

void xbee_on(void)
{
    PORTD &= ~(1<<2);
}

void xbee_off(void)
{
    PORTD |= (1<<2);
}
