#!/bin/sh
#
# Copyright (C) 2013 Cisco Systems, Inc.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# Authors : Thomas Bachman


#
# Ping an instance created by DevStack
#

# get the prefix
ip=$1
uuid=`neutron net-list | grep $(echo $ip | awk 'BEGIN {FS="."} {print $1 "." $2 "." $3}') | awk '{print $2}'`
dhcp_server="qdhcp-$uuid"
foo="ip netns exec $dhcp_server ping $ip"
sudo $foo
