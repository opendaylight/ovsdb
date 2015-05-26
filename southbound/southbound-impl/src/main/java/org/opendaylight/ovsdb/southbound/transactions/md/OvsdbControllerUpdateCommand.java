/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Controller;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class OvsdbControllerUpdateCommand extends AbstractTransactionCommand {



    private Map<UUID, Controller> updatedControllerRows;
    private Map<UUID, Bridge> updatedBridgeRows;

    public OvsdbControllerUpdateCommand(ConnectionInfo key,
            TableUpdates updates, DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedBridgeRows = TyperUtils.extractRowsUpdated(Bridge.class, getUpdates(), getDbSchema());
        updatedControllerRows = TyperUtils.extractRowsUpdated(Controller.class,
                getUpdates(), getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        for (Bridge bridge: updatedBridgeRows.values()) {
            setController(transaction, bridge);
        }

    }

    private void setController(ReadWriteTransaction transaction, Bridge bridge) {
        for (ControllerEntry controllerEntry: SouthboundMapper.createControllerEntries(bridge, updatedControllerRows)) {
            InstanceIdentifier<ControllerEntry> iid =
                    SouthboundMapper.createInstanceIdentifier(getConnectionInfo(), bridge)
                    .augmentation(OvsdbBridgeAugmentation.class)
                    .child(ControllerEntry.class,controllerEntry.getKey());
            transaction.put(LogicalDatastoreType.OPERATIONAL, iid, controllerEntry);
        }
    }

}
