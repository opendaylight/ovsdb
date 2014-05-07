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
# Creates a directory for each source project, 
# pulls the source
username=$1

if [ "$username" == "" ]; then
    echo "Please provide a username for the OpenDaylight git server"
    exit 1
fi

project=$1
commit_hash=$2

#
# if directory exists, don't fail out
#
mkdir -p $project

cd $project
if [ $? -ne 0 ]; then
    echo "failed to cd to $project"
    exit 1
fi

#
# see if this is already a git repo for the project
#
status=`git status &> /dev/null`
if [ $? -ne 0 ]; then
    #
    # not a repo, so go ahead and clone
    #
    git clone ssh://$username@git.opendaylight.org:29418/$project.git .
else
    git pull origin
fi
if [ $? -ne 0 ]; then
    echo "failed to clone or update $project"
    exit 1
fi
git checkout $commit_hash
if [ $? -ne 0 ]; then
    echo "failed to checkout $commit_hash for $project"
    exit 1
fi
