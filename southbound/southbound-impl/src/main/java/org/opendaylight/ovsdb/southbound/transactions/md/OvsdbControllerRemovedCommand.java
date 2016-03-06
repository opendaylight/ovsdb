/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Controller;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Preconditions;

public class OvsdbControllerRemovedCommand extends AbstractTransactionCommand {



    private Map<UUID, Bridge> oldBridgeRows;
    private Map<UUID, Controller> removedControllerRows;
    private Map<UUID, Bridge> updatedBridgeRows;

    public OvsdbControllerRemovedCommand(OvsdbConnectionInstance key,
            TableUpdates updates, DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedBridgeRows = TyperUtils.extractRowsUpdated(Bridge.class, getUpdates(), getDbSchema());
        oldBridgeRows = TyperUtils.extractRowsOld(Bridge.class, getUpdates(), getDbSchema());
        removedControllerRows = TyperUtils.extractRowsRemoved(Controller.class,
                getUpdates(), getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        for (Bridge bridge : updatedBridgeRows.values()) {
            InstanceIdentifier<Node> bridgeIid =
                    SouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(), bridge);
            deleteControllers(transaction, controllerEntriesToRemove(bridgeIid,bridge));
        }
    }

    private void deleteControllers(ReadWriteTransaction transaction,
            List<InstanceIdentifier<ControllerEntry>> controllerEntryIids) {
        for (InstanceIdentifier<ControllerEntry> controllerEntryIid: controllerEntryIids) {
            transaction.delete(LogicalDatastoreType.OPERATIONAL, controllerEntryIid);
        }
    }

    private List<InstanceIdentifier<ControllerEntry>> controllerEntriesToRemove(
            InstanceIdentifier<Node> bridgeIid, Bridge bridge) {
        Preconditions.checkNotNull(bridgeIid);
        Preconditions.checkNotNull(bridge);
        List<InstanceIdentifier<ControllerEntry>> result =
                new ArrayList<>();
        Bridge oldBridgeNode = oldBridgeRows.get(bridge.getUuid());

        if (oldBridgeNode != null && oldBridgeNode.getControllerColumn() != null) {
            for (UUID controllerUuid: oldBridgeNode.getControllerColumn().getData()) {
                if (bridge.getControllerColumn() == null
                        || !bridge.getControllerColumn().getData().contains(controllerUuid)) {
                    Controller controller = removedControllerRows.get(controllerUuid);
                    if (controller != null && controller.getTargetColumn() != null) {
                        InstanceIdentifier<ControllerEntry> iid = bridgeIid
                                .augmentation(OvsdbBridgeAugmentation.class)
                                .child(ControllerEntry.class,
                                        new ControllerEntryKey(new Uri(controller.getTargetColumn().getData())));
                        result.add(iid);
                    }
                }
            }
        }
        return result;
    }

}
