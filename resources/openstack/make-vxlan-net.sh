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
# Create a neutron network and subnet for VXLAN (hard-coded)
#

neutron net-create vxlan-net --tenant_id  $(keystone tenant-list | grep '\sadmin' | awk '{print $2}') --provider:network_type vxlan --provider:segmentation_id 1300
neutron  subnet-create vxlan-net 10.100.1.0/24 --name vxlan-net
