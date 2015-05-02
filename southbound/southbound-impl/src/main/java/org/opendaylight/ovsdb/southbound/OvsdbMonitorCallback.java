/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import org.opendaylight.ovsdb.lib.MonitorCallBack;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.southbound.transactions.md.OvsdbOperationalCommandAggregator;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionInvoker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbMonitorCallback implements MonitorCallBack {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbMonitorCallback.class);
    private TransactionInvoker txInvoker;
    private ConnectionInfo key;
    InstanceIdentifier<Node> connectionIid;

    OvsdbMonitorCallback(ConnectionInfo key,TransactionInvoker txInvoker,InstanceIdentifier<Node> connectionIid) {
        this.txInvoker = txInvoker;
        this.key = key;
        this.connectionIid = connectionIid;
    }

    @Override
    public void update(TableUpdates result, DatabaseSchema dbSchema) {
        LOG.debug("result: {} dbSchema: {}",result,dbSchema);
        txInvoker.invoke(new OvsdbOperationalCommandAggregator(key, result, dbSchema, connectionIid));
    }

    @Override
    public void exception(Throwable exception) {
        LOG.warn("exception {}", exception);
    }

}
