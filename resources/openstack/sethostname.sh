#!/bin/bash
#
# Copyright (C) 2013 Cisco Systems, Inc.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# Authors : Thomas Bachman

#
#  Set the hostname
#
currname=`cat /etc/hostname`
echo "Current hostname: $currname"

echo -n "Enter new hostname: "
read -a newname

# 
# Update /etc/hosts & /etc/hostname with new hostname
#
if [ "$newname" != "$currname" ]; then
    res=`sed -i "s/$currname/$newname/g" /etc/hosts`
    if [ $? -ne 0 ]; then
        echo "failed to update /etc/hosts"
    fi
    res=`sed -i "s/$currname/$newname/g" /etc/hostname`
    if [ $? -ne 0 ]; then
        echo "failed to update /etc/hostname"
    fi
    res=`hostname $newname`
    if [ $? -ne 0 ]; then
        echo "failed to update hostname"
    else
        echo "Your new hostname is $newname"
    fi
else
    echo "hostname already set to $newname"
fi
