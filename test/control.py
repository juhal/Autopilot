#!/usr/bin/env python

import sys
import bluetooth as bt

def set_power(s, p):
    print "Setting power: %d" % p
    s.send("$J %d\r" % p)

def main():
    p = 0
    s = None
    s = bt.BluetoothSocket(bt.RFCOMM)
    try:
        s.connect(('00:11:09:06:06:66',1))
        print "Connected. Enter power -255..255 or 'q' to quit."
        while True:
            print ">> ",
            cmd = raw_input() or p
            if cmd == 'q':
                break
            try:
                p = int(cmd)
            except:
                print "Invalid command."
            else:
                set_power(s, p)
        set_power(s, 0)
    finally:
        s.close()
    print "Bye."

if __name__ == '__main__':
    main()
