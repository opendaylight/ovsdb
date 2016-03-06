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
# Script for booting VMs onto networks
#

#defaults
vm=`hostname`
net=private
image=cirros-0.3.1-x86_64-uec

nova hypervisor-list
echo -n "Enter the hypervisor name to use for new instance [$vm]: "
read -a entry
if [ "$entry" != "" ]; then
    vm=$entry
fi

neutron net-list
echo -n "Enter the name of the network to use for the VM [$net]: "
read -a entry
if [ "$entry" != "" ]; then
    net=$entry
fi

nova image-list
echo -n "Enter the name of the image to use for the VM [$image]: "
read -a entry
if [ "$entry" != "" ]; then
    image=$entry
fi

nova boot --flavor m1.tiny --image $(nova image-list | grep "$image\s" | awk '{print $2}') --nic net-id=$(neutron net-list | grep $net | awk '{print $2}') admin-private --availability_zone=nova:$vm
