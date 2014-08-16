#!/bin/sh

cd distribution/opendaylight/target/distribution.ovsdb-1.0.1-SNAPSHOT-osgipackage/opendaylight
./run.sh -debug -Xmx2048m -XX:MaxPermSize=1024m -virt ovsdb
