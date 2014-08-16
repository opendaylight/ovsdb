#!/bin/sh



cd loadbalancer
mvn clean install
cd ..

cp -p loadbalancer/target/ovsdb.loadbalancer-0.0.1-SNAPSHOT.jar  distribution/opendaylight/target/distribution.ovsdb-1.0.1-SNAPSHOT-osgipackage/opendaylight/plugins/org.opendaylight.ovsdb.ovsdb.loadbalancer-0.0.1-SNAPSHOT.jar


# Northbound

#cd northbound/loadbalancer
#mvn clean install
#cd ../..

#cp -p northbound/loadbalancer/target/ovsdb.loadbalancer.northbound-0.0.1-SNAPSHOT.jar  distribution/opendaylight/target/distribution.ovsdb-1.0.1-SNAPSHOT-osgipackage/opendaylight/plugins/org.opendaylight.ovsdb.ovsdb.loadbalancer.northbound-0.0.1-SNAPSHOT.jar



