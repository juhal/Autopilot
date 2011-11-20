#include <stdint.h>

void xbee_init(void);
void xbee_putc(char c);
void xbee_write(char *buf, uint8_t n);
void xbee_printf(const char *fmt, ...);
void xbee_on(void);
void xbee_off(void);
