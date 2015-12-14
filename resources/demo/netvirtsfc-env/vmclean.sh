#!/usr/bin/env bash

docker stop -t=1 $(docker ps -a -q) > /dev/null 2>&1
docker rm $(docker ps -a -q) > /dev/null 2>&1

/etc/init.d/openvswitch-switch stop > /dev/null
rm /etc/openvswitch/conf.db > /dev/null
/etc/init.d/openvswitch-switch start > /dev/null


ovs-vsctl show

