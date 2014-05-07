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
# This script is used with a DevStack Icehouse (or later) distribution,
# to deploy the OpenDaylight projects from the current working directory
# into DevStack so that they can be used for source level debugging
#

files=""
distdir=""
distname=""
controller_dir=`dirname $(find controller/ -name run.sh -print | grep osgipackage)`
odl_deploy=`pwd`/$controller_dir
#`dirname $(find controller/ -name run.sh -print | grep osgipackage)`
ds_deploy=/opt/stack/opendaylight/opendaylight


# Get rid of script name from list of projects

#
# list of projects to copy bundles from (excludes controller,
# as that's the target). Change projects here as needed, and
# make sure there's a corresponding if in the loop below
#
projects="$*"

get_dist_dir()
{
    for file in $files
    do
        filename=`basename $file`
        distdir=`dirname $file`
        distname=`echo "${filename%?}"`
    done
}


#
#  If this is the first deploy, save off the original
#  OpenDaylight virtualization distro directory, and
#  create a symbolic link to the current working dir
#  source
if [ ! -h $ds_deploy ]; then
    echo "Saving off $ds_deploy to $ds_deploy.orig..."
    mv $ds_deploy $ds_deploy.orig
    if [ $? -ne 0 ]; then
        echo "failed to back up $ds_deploy"
        exit 1
    fi
    echo "Linking in new source..."
    ln -s $odl_deploy $ds_deploy
    if [ $? -ne 0 ]; then
        echo "failed to create symbolic link to $odl_deploy"
        exit 1
    fi
fi

#
#  copy the run scripts from the virtualizaiton
#  directory. We do this every time, as rebuilds
#  of the controller wipe them out.
#
cp $ds_deploy.orig/run.* $ds_deploy

#
#  Copy the project artifacts in to the controller
#  directory for running
#
for project in $projects
do
    #
    # for projects that have paths that include version numbers,
    # get the highest numbered version
    #
    if [ "$project" == "openflowplugin" ]; then
        files=`ls openflowplugin/distribution/base/target/distribution* | awk '/:$/ {print $1}'`
        get_dist_dir
        cp $distdir/$distname/opendaylight/plugins/*openflow* $odl_deploy/plugins

    elif [ "$project" == "ovsdb" ]; then
        files=`ls ovsdb/distribution/opendaylight/target/distribution* | awk '/:$/ {print $1}'`
        get_dist_dir
        cp $distdir/$distname/opendaylight/plugins/*ovsdb* $odl_deploy/plugins

    elif [ "$project" == "openflowjava" ]; then
        cp openflowjava/openflow-protocol-api/target/*jar $odl_deploy/plugins
        cp openflowjava/openflow-protocol-impl/target/*jar $odl_deploy/plugins
        cp openflowjava/openflow-protocol-it/target/*jar $odl_deploy/plugins
        cp openflowjava/openflow-protocol-it/target/*jar $odl_deploy/plugins
    fi
done



