#!/bin/bash

set -e

mkdir ~/odl
cd /vagrant/distribution
mvn clean install
cp opendaylight/target/distribution.ovsdb-1.0.1-SNAPSHOT-osgipackage/opendaylight ~/odl
