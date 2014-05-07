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
#  Top-level script to install a source tree for OpenDaylight,
#  using a set of commit hashes, which can be used for source
#  level debugging when run with DevStack
#
username=$1
projects=""

if [ "$username" == "" ]; then
    echo "Please provide a username for the OpenDaylight git server"
    exit 1
fi

# list of projects and their commit hashes
# for the Hydrogen release. Change/append
# this as needed, and update max_index
#
max_index=4
project[1]="controller"
project[2]="openflowplugin"
project[3]="ovsdb"
project[4]="openflowjava"
commit_hash[1]='48350c456393c0fe530dc29642c7d95c11a8a70a'
commit_hash[2]='77a12b7b2d1b7c4073fd7836685e3de0ee76b14c'
commit_hash[3]='66e21d5e23cdcb6df1a27237e437f6a1e00d2597'
commit_hash[4]='32cfb09797ae9706fad78d6e66a9275a2ddda417'


#
# Get the source for each project and pull Hydrogen branch
#
for index in `seq $max_index`
do
    #
    # build our projects list -- used below
    #
    projects="$projects ${project[index]}"
    ./get-odl-src.sh $username ${project[index]} ${commit_hash[index]}
    if [ $? -ne 0 ]; then
        echo "failed to get source"
        exit 1
    fi
done

#
# build all the projects
#
./build-odl-src.sh $projects
if [ $? -ne 0 ]; then
    echo "failed to build artifacts"
    exit 1
fi

#
# deploy the controller and projects to DevStack Icehouse
#
./deploy-odl-src.sh $projects
