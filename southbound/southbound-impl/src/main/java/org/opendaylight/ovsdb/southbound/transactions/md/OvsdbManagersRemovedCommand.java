/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Manager;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntryKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class OvsdbManagersRemovedCommand extends AbstractTransactionCommand {
    private final Map<UUID, OpenVSwitch> oldOpenVSwitchRows;
    private final Map<UUID, Manager> removedManagerRows;
    private final Map<UUID, OpenVSwitch> updatedOpenVSwitchRows;
    private final Map<UUID, Manager> updatedManagerRows;

    public OvsdbManagersRemovedCommand(OvsdbConnectionInstance key,
            TableUpdates updates, DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedOpenVSwitchRows = TyperUtils.extractRowsUpdated(OpenVSwitch.class, getUpdates(), getDbSchema());
        oldOpenVSwitchRows = TyperUtils.extractRowsOld(OpenVSwitch.class, getUpdates(), getDbSchema());
        updatedManagerRows = TyperUtils.extractRowsUpdated(Manager.class, getUpdates(), getDbSchema());
        removedManagerRows = TyperUtils.extractRowsRemoved(Manager.class,
                getUpdates(), getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        for (OpenVSwitch openVSwitch : updatedOpenVSwitchRows.values()) {
            InstanceIdentifier<Node> ovsdbNodeIid =
                    SouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance().getNodeId());
            deleteManagers(transaction, managerEntriesToRemove(ovsdbNodeIid,openVSwitch));
        }
    }

    @VisibleForTesting
    void deleteManagers(ReadWriteTransaction transaction,
            List<InstanceIdentifier<ManagerEntry>> managerEntryIids) {
        for (InstanceIdentifier<ManagerEntry> managerEntryIid: managerEntryIids) {
            transaction.delete(LogicalDatastoreType.OPERATIONAL, managerEntryIid);
        }
    }

    @VisibleForTesting
    List<InstanceIdentifier<ManagerEntry>> managerEntriesToRemove(
            InstanceIdentifier<Node> ovsdbNodeIid, OpenVSwitch openVSwitch) {
        requireNonNull(ovsdbNodeIid);
        requireNonNull(openVSwitch);

        List<InstanceIdentifier<ManagerEntry>> result =
                new ArrayList<>();
        OpenVSwitch oldOvsdbNode = oldOpenVSwitchRows.get(openVSwitch.getUuid());

        if (oldOvsdbNode != null && oldOvsdbNode.getManagerOptionsColumn() != null) {
            for (UUID managerUuid: oldOvsdbNode.getManagerOptionsColumn().getData()) {
                if (openVSwitch.getManagerOptionsColumn() == null
                        || !openVSwitch.getManagerOptionsColumn().getData().contains(managerUuid)) {
                    Manager manager = removedManagerRows.get(managerUuid);
                    if (!checkIfManagerPresentInUpdatedManagersList(manager)) {
                        if (manager != null && manager.getTargetColumn() != null) {
                            InstanceIdentifier<ManagerEntry> iid = ovsdbNodeIid
                                    .augmentation(OvsdbNodeAugmentation.class)
                                    .child(ManagerEntry.class,
                                            new ManagerEntryKey(
                                                    new Uri(manager.getTargetColumn().getData())));
                            result.add(iid);
                        }

                    }
                }
            }
        }
        return result;
    }

    private boolean checkIfManagerPresentInUpdatedManagersList(Manager removedManager) {
        for (Map.Entry<UUID, Manager> updatedManager : updatedManagerRows.entrySet()) {
            if (updatedManager.getValue().getTargetColumn().getData()
                    .equals(removedManager.getTargetColumn().getData())) {
                return true;
            }
        }
        return false;
    }

}
