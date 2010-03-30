#ifndef JOY_H
#define JOY_H

#include <stdint.h>

void joy_init(void);
uint16_t joy_read(void);
int joy_get(void);

#endif
