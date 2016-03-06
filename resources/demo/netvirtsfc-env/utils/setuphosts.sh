#!/usr/bin/env bash

set -e

for i in `seq 1 $NUM_NODES`; do
  hostname="netvirtsfc"$i
  echo $hostname
  vagrant ssh $hostname -c "sudo cp /vagrant/utils/hosts /etc/hosts"
done
 

