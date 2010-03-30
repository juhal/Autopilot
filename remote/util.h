#ifndef UTIL_H
#define UTIL_H

#include <stdint.h>
#include <avr/io.h>
#define F_CPU 8000000UL
#include <util/delay.h>

typedef unsigned char bool_t;
#define TRUE 1
#define FALSE 0

#define LED_ON PORTB |= (1<<5)
#define LED_OFF PORTB &= ~(1<<5)
#define LED_TOGGLE PORTB ^= (1<<5)

#define MAX(X,Y) ((X)<(Y)?(Y):(X))
#define MIN(X,Y) ((X)>(Y)?(Y):(X))
#define ABS(X) ((X)<0?-(X):(X))
#define HI(X) ((X)>>8)
#define LO(X) ((X)&0xff)
#define LOHI(X) (((X)<<8)|((X)>>8))

#endif
