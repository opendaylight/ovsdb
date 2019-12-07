/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntryKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class OvsdbBridgeRemovedCommand extends AbstractTransactionComponent {
    public OvsdbBridgeRemovedCommand() {
        super(Bridge.class);
    }

    @Override
    public void execute(final OvsdbTransactionContext context) {
        for (Bridge bridge : context.getRemovedRows(Bridge.class).values()) {
            InstanceIdentifier<Node> bridgeIid =
                    SouthboundMapper.createInstanceIdentifier(instanceIdentifierCodec, getOvsdbConnectionInstance(),
                            bridge);
            InstanceIdentifier<ManagedNodeEntry> mnIid = context.getNode()
                    .augmentation(OvsdbNodeAugmentation.class)
                    .child(ManagedNodeEntry.class, new ManagedNodeEntryKey(new OvsdbBridgeRef(bridgeIid)));
            // TODO handle removal of reference to managed node from model
            transaction.delete(LogicalDatastoreType.OPERATIONAL, bridgeIid);
            transaction.delete(LogicalDatastoreType.OPERATIONAL, mnIid);
        }
    }

}
