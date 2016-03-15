#!/usr/bin/python

import subprocess
import sys
import re
from collections import defaultdict

help ='''## readable_flows.py:

This script pretty prints ovs flows created by ovsdb. Features include:

1. Where possible, MACs are followed by their corresponding IP (in parenthases) and vice-versa
2. Tunnel ids are followed by the decimal representation used by Open Daylight
3. Counters and stats are removed so that meaningful diffs may be generated
4. Table numbers are given together with descriptive names
5. Flows are grouped together by priority in decending order

### Usage:
This script must be run on the OpenStack controller since it uses the
neutron command to map MACs and IPs.
> sudo ovs-ofctl -OOpenFlow13 dump-flows br-int | python readable_flows.py
'''

if len(sys.argv) > 1 and sys.argv[1] in ['-h', '-?', '--h', '--help']:
    print help
    sys.exit(0)

DEFAULT_PRIO=32768

TABLE_NAME = { \
0: 'CLASSIFIER',\
20: 'GATEWAY_RESOLVER',\
10: 'DIRECTOR',\
10: 'SFC_CLASSIFIER',\
20: 'ARP_RESPONDER',\
30: 'INBOUND_NAT',\
40: 'EGRESS_ACL',\
50: 'LOAD_BALANCER',\
60: 'ROUTING',\
70: 'ICMP_ECHO',\
70: 'L3_FORWARDING',\
80: 'L2_REWRITE',\
90: 'INGRESS_ACL',\
100: 'OUTBOUND_NAT',\
110: 'L2_FORWARDING'}

PORT_LIST_CMD = 'neutron port-list -c mac_address -c fixed_ips'.split(' ')

LINE_PATTERN = '.*(?P<table>table=\d*), (?P<counters>n_packets=\d*, n_bytes=\d*), (?P<prio>priority=\d*)?,?(?P<rule>.*)'
MAX_LINE = 30

def print_rules(table, rules_by_prio):
    print ''
    cut = table.find('=')
    print table.upper() + ' (' + TABLE_NAME[int(table[cut+1:])] + ')'

    prios = rules_by_prio.keys()
    prios.sort()
    prios.reverse()
    for prio in prios:
        print '    priority=%i' % prio,
        if DEFAULT_PRIO == prio: print '(DEFAULT_PRIO)'
        else: print ''
        for rule in rules_by_prio[prio]:
            print_flow('        ', re.sub('actions=', 'ACTIONS=', rule))

def tun_match_to_decimal(m):
    s = m.group('num')
    return 'tun_id=0x%s(%i)' % (s, int(s, 16))
def tun_set_to_decimal(m):
    s = m.group('num')
    return '0x%s(%i)->tun_id' % (s, int(s, 16))

def print_flow(indent, f):
    print indent + f
# WIP
#    flow = indent + f
#    if len(flow) <= MAX_LINE: 
#        print flow
#        return
#    
#    cut = flow.find('ACTIONS')
#    match = flow[0:cut - 1]
#    action = indent + indent + flow[cut:]
#    print match
#    while action:
#        if len(action) <= MAX_LINE:
#            print action
#            break
#        cut = action.rfind(',', 0, MAX_LINE)
#        if cut < 2: 
#            print action
#            break
#        print action[0:cut + 1]
#        action = indent + indent + (' ' * len('ACTIONS=')) + action[cut +1:]

port_list_out = subprocess.check_output(PORT_LIST_CMD)

addresses = []
for line in port_list_out.split('\n')[2:]:
    if re.match('^$', line): continue
    if '----' in line: continue
    line = re.sub('^\| ', '', line)
    line = re.sub(' *\|$', '', line)
    (mac, fixed_ip) = line.split(' | ')
    ip = eval(fixed_ip)['ip_address']
    addresses.append((mac, ip))

table = ''
rules_by_prio = defaultdict(list)
for line in sys.stdin:
    for (mac, ip) in addresses: 
        line = re.sub(mac, '%s(%s)' % (mac, ip), line)
        line = re.sub('=%s(\D)' % ip, '=%s(%s)\\1' % (ip, mac), line)
    line = re.sub('tun_id=0x(?P<num>[0-9a-fA-F]*)', tun_match_to_decimal, line)
    line = re.sub('0x(?P<num>[0-9a-fA-F]*)->tun_id', tun_set_to_decimal, line)
    match = re.match(LINE_PATTERN, line)
    if not match: 
        print '[Not a flow line?]:  ' + line,
        continue

    if  match.group('table') != table:
        if table:
            print_rules(table, rules_by_prio)
            rules_by_prio = defaultdict(list)
        table = match.group('table')

    prio = DEFAULT_PRIO
    prio_str = match.group('prio')
    if None != prio_str: prio = int(prio_str[9:])
    rules_by_prio[prio].append(match.group('rule'))

print_rules(table, rules_by_prio)

