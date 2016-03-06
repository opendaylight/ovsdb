#!/usr/bin/env bash

set -e

for i in `seq 1 $NUM_NODES`; do
  hostname="netvirtsfc"$i
  switchname="sw"$i
  echo $hostname
  vagrant ssh $hostname -c "sudo ovs-vsctl del-br $switchname; sudo ovs-vsctl del-manager; sudo /vagrant/vmclean.sh"

done
 
./rest-clean.py

if [ -f "demo.lock" ] ; then
  rm demo.lock
fi
