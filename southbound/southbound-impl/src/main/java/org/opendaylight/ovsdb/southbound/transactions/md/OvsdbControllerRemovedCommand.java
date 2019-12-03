/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Controller;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class OvsdbControllerRemovedCommand extends AbstractTransactionCommand {
    private final InstanceIdentifierCodec instanceIdentifierCodec;
    private final Map<UUID, Bridge> oldBridgeRows;
    private final Map<UUID, Controller> removedControllerRows;
    private final Map<UUID, Bridge> updatedBridgeRows;

    public OvsdbControllerRemovedCommand(final InstanceIdentifierCodec instanceIdentifierCodec,
            final OvsdbConnectionInstance key, final TableUpdates updates, final DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        this.instanceIdentifierCodec = instanceIdentifierCodec;
        updatedBridgeRows = extractRowsUpdated(Bridge.class);
        oldBridgeRows = extractRowsOld(Bridge.class);
        removedControllerRows = extractRowsRemoved(Controller.class);
    }

    @Override
    public void execute(final ReadWriteTransaction transaction) {
        for (Bridge bridge : updatedBridgeRows.values()) {
            InstanceIdentifier<Node> bridgeIid =
                    SouthboundMapper.createInstanceIdentifier(instanceIdentifierCodec, getOvsdbConnectionInstance(),
                            bridge);
            deleteControllers(transaction, controllerEntriesToRemove(bridgeIid,bridge));
        }
    }

    private static void deleteControllers(final ReadWriteTransaction transaction,
            final List<InstanceIdentifier<ControllerEntry>> controllerEntryIids) {
        for (InstanceIdentifier<ControllerEntry> controllerEntryIid: controllerEntryIids) {
            transaction.delete(LogicalDatastoreType.OPERATIONAL, controllerEntryIid);
        }
    }

    private List<InstanceIdentifier<ControllerEntry>> controllerEntriesToRemove(
            final InstanceIdentifier<Node> bridgeIid, final Bridge bridge) {
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
