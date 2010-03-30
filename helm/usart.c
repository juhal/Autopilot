#include <stdio.h>
#include <stdarg.h>
#include <avr/io.h>
#include <avr/interrupt.h>

#include "usart.h"
#include "util.h"

#define TX_BUF_SZ 128
static char tx_buf[TX_BUF_SZ];
static volatile uint8_t tx_wi, tx_ri, tx_len;

#define RX_BUF_SZ 128
#define RX_INC(X) X = (X == RX_BUF_SZ-1 ? 0 : X+1)
static char rx_buf[RX_BUF_SZ];
static volatile uint8_t rx_ri, rx_wi;

#define MAX_LINE 64
static char line[MAX_LINE];

// Change these if using a different USART.
// Or better yet figure out a preprocessor trick to do it.
#define UCSRnA UCSR0A
#define UCSRnB UCSR0B
#define UCSRnC UCSR0C
#define UBRRnH UBRR0H
#define UBRRnL UBRR0L
#define USARTn_RX_vect USART_RX_vect
#define USARTn_TX_vect USART_TX_vect
#define UDREn UDRE0
#define UDRn UDR0

void usart_init()
{
    cli();
    UCSRnB = 0xD8;
    UCSRnC = 0x06;
    #define BAUD 9600
    #include <util/setbaud.h>
    UBRRnH = UBRRH_VALUE;
    UBRRnL = UBRRL_VALUE;
    #if USE_2X
    UCSRnA |= (1<<U2X0);
    #else
    UCSRnA &= ~(1<<U2X0);
    #endif
    sei();
}

ISR(USARTn_RX_vect)
{
    uint8_t i;
    char c;

    c = UDRn;
    i = rx_wi;
    RX_INC(i);
    if (i != rx_ri) {
        rx_buf[i] = c;
        rx_wi = i;
    }
}

char *usart_readline(uint8_t blocking)
{
    uint8_t i = rx_ri, j = 0;
    char c;
    
    while (1) {
        if (i == rx_wi) {
            if (!blocking) return NULL;
            while (i == rx_wi);
        }
        RX_INC(i);
        c = rx_buf[i];
        if (c == '\r') break;
        line[j++] = c;
        if (j == RX_BUF_SZ - 1) break;
    }
    line[j] = '\0';
    rx_ri = i;
    return line;
}

ISR(USARTn_TX_vect)
{
    if (tx_len) {
        --tx_len;
        UDRn = tx_buf[tx_ri];
        if (++tx_ri == TX_BUF_SZ)
            tx_ri = 0;
    }
}

void usart_putc(char c)
{
    uint8_t sreg;
    while (tx_len == TX_BUF_SZ);
    sreg = SREG;
    cli();
    if (tx_len || ((UCSRnA & (1<<UDREn)) == 0)) {
        tx_buf[tx_wi] = c;
        if (++tx_wi == TX_BUF_SZ) 
            tx_wi = 0;
        ++tx_len;
    }
    else UDRn = c;
    SREG = sreg;
}

void usart_write(char *buf, uint8_t n)
{
    size_t i;
    for (i = 0; i < n; ++i)
        usart_putc(buf[i]);
}

void usart_printf(const char *fmt, ...)
{
    va_list ap;
    uint8_t n;

    va_start(ap, fmt);
    n = vsnprintf(line, MAX_LINE, fmt, ap);
    va_end(ap);
    
    usart_write(line, MIN(n,MAX_LINE));
}

void usart_dump(const char *prefix, const uint8_t *buf, uint16_t n)
{
    uint8_t b;
    char *hex = "0123456789ABCDEF";

    usart_printf(prefix);
    while(n--) {
        b = *buf++;
        usart_putc(hex[b >> 4]);
        usart_putc(hex[b & 0xf]);
        usart_putc(' ');
    }
    usart_printf("\r\n");
}
