#!/usr/bin/python

import string
from sys import stdout,stdin,argv,exit

if len(argv) > 1 and argv[1] in ['-h', '-?', '--h', '--help']:
    print 'pplog.py pretty prints Open Daylight log lines, useful for lines containing large nested objects'
    print 'Usage: Simply pipe the lines you are interested through this script'
    exit(0)

line_num = 0
def nl():
    global line_num
    line_num += 1
    stdout.write('\n' + i)

for l in stdin:
    if '|' not in l: continue

    (t, level, component, cat, art, msg) = string.split(l, '|', maxsplit=5)

    I = '  '
    opener = '[{<'
    closer = ']}>'
    i = ''

    in_ws = 1
    is_empty_list = 0
    last_char = ''
    title = ''
    title_stack = []

    for c in msg:
        if in_ws and c in ' \t': continue
        in_ws = 0

        if c in closer:
            i = i[:-2]
            if not is_empty_list and last_char not in closer: nl()
            in_ws = 1
            is_empty_list = 0
            title = ''
        elif is_empty_list:
            is_empty_list = 0
            nl()

        if last_char in closer and c != ',': nl()

        stdout.write(c)
        if not c in opener: title += c
        last_char = c

        if c in closer:
            if len(title_stack):
                (t,ln) = title_stack.pop()
                if (line_num - ln) > 5: stdout.write(' /* ' + t.strip() + ' */')

        if c in opener:
            i += I
            in_ws = 1
            is_empty_list = 1
            if title:
                title_stack.append((title, line_num))
                title = ''

        if c == ',':
            nl()
            in_ws = 1
            title = ''
