/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.schema.openvswitch.Manager;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntryKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class OvsdbManagersRemovedCommand extends AbstractTransactionComponent {
    public OvsdbManagersRemovedCommand() {
        super(OpenVSwitch.class, Manager.class);
    }

    @Override
    public void execute(final OvsdbTransactionContext context) {
        Map<UUID, OpenVSwitch> oldOpenVSwitchRows = context.getOldRows(OpenVSwitch.class);
        Map<UUID, Manager> updatedManagerRows = context.getUpdatedRows(Manager.class);
        Map<UUID, Manager> removedManagerRows = context.getRemovedRows(Manager.class);
        for (OpenVSwitch openVSwitch : context.getUpdatedRows(OpenVSwitch.class).values()) {
            InstanceIdentifier<Node> ovsdbNodeIid =
                    SouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance().getNodeId());
            deleteManagers(transaction, managerEntriesToRemove(ovsdbNodeIid,openVSwitch));
        }
    }

    @VisibleForTesting
    void deleteManagers(final ReadWriteTransaction transaction,
            final List<InstanceIdentifier<ManagerEntry>> managerEntryIids) {
        for (InstanceIdentifier<ManagerEntry> managerEntryIid: managerEntryIids) {
            transaction.delete(LogicalDatastoreType.OPERATIONAL, managerEntryIid);
        }
    }

    @VisibleForTesting
    List<InstanceIdentifier<ManagerEntry>> managerEntriesToRemove(
            final InstanceIdentifier<Node> ovsdbNodeIid, final OpenVSwitch openVSwitch) {
        Preconditions.checkNotNull(ovsdbNodeIid);
        Preconditions.checkNotNull(openVSwitch);

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

    private boolean checkIfManagerPresentInUpdatedManagersList(final Manager removedManager) {
        for (Map.Entry<UUID, Manager> updatedManager : updatedManagerRows.entrySet()) {
            if (updatedManager.getValue().getTargetColumn().getData()
                    .equals(removedManager.getTargetColumn().getData())) {
                return true;
            }
        }
        return false;
    }

}
