#!/usr/bin/env bash

set -e


echo "Checking demo from $demo with vars:"
echo "Number of nodes: " $NUM_NODES
echo "Opendaylight Controller: " $ODL
echo "Base subnet: " $SUBNET

for i in `seq 1 $NUM_NODES`; do
  hostname="netvirtsfc"$i
  echo $hostname "flow count: "
  vagrant ssh $hostname -c "sudo ovs-ofctl dump-flows sw$i -OOpenFlow13 | wc -l "
done

