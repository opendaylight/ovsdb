#!/bin/sh
# Copyright (C) 2013 Cisco Systems, Inc.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# Authors : Thomas Bachman

#
# Builds the source in the projects listed in the project
# array, using the "no snapshot updates" option
#


# just in case they didn't set up their environment...
export MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=512m"

# remove the script name from the list of projects
shift
projects="$*"

for project in projects
do
    cd $project
    if [ $? -ne 0 ]; then
        echo "Failed to cd to $project (does it exist?)."
        exit 1
    fi
    mvn clean install -nsu
    if [ $? -ne 0 ]; then
        echo "failed to build $project"
        exit 1
    fi
    cd ..
done
