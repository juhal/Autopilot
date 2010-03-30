#ifndef USART_H
#define USART_H

#include <stdint.h>

void usart_init(void);
char *usart_readline(uint8_t blocking);
void usart_putc(char c);
void usart_write(char *buf, uint8_t n);
void usart_printf(const char *fmt, ...);
void usart_dump(const char *prefix, const uint8_t *buf, uint16_t n);

#endif
