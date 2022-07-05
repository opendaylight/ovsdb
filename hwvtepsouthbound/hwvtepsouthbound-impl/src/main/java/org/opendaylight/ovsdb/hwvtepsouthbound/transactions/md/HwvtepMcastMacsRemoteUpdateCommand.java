/*
 * Copyright (c) 2015, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.TransactUtils;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;
import org.opendaylight.ovsdb.schema.hardwarevtep.McastMacsRemote;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocator;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocatorSet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepLogicalSwitchRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.locator.set.attributes.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.locator.set.attributes.LocatorSetBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class HwvtepMcastMacsRemoteUpdateCommand extends AbstractTransactionCommand {
    private final Map<UUID, McastMacsRemote> updatedMMacsRemoteRows;
    private final Map<UUID, PhysicalLocatorSet> updatedPLocSetRows;
    private final Map<UUID, PhysicalLocator> updatedPLocRows;

    public HwvtepMcastMacsRemoteUpdateCommand(HwvtepConnectionInstance key, TableUpdates updates,
            DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedMMacsRemoteRows = TyperUtils.extractRowsUpdated(McastMacsRemote.class, getUpdates(), getDbSchema());
        updatedPLocSetRows = TyperUtils.extractRowsUpdated(PhysicalLocatorSet.class, getUpdates(), getDbSchema());
        updatedPLocRows = TyperUtils.extractRowsUpdated(PhysicalLocator.class, getUpdates(), getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        for (Entry<UUID, McastMacsRemote> entry : updatedMMacsRemoteRows.entrySet()) {
            updateData(transaction, entry.getValue());
        }
    }

    private void updateData(ReadWriteTransaction transaction, McastMacsRemote macRemote) {
        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();

        // Ensure the node exists
        transaction.merge(LogicalDatastoreType.OPERATIONAL, connectionIId,
            new NodeBuilder().setNodeId(getOvsdbConnectionInstance().getNodeId()).build());

        // Prepare the update in internal structures
        final RemoteMcastMacs mac = buildRemoteMcastMacs(macRemote);
        final InstanceIdentifier<RemoteMcastMacs> macIid = connectionIId.augmentation(HwvtepGlobalAugmentation.class)
                .child(RemoteMcastMacs.class, mac.key());
        addToUpdateTx(RemoteMcastMacs.class, macIid, macRemote.getUuid(), macRemote);

        // Merge update, relying on automatic lifecycle...
        transaction.merge(LogicalDatastoreType.OPERATIONAL, macIid, mac);
        if (mac.getLocatorSet() == null) {
            // ... but delete locator set if it is empty
            // FIXME: can we use .put() of instead of merge/delete?
            transaction.delete(LogicalDatastoreType.OPERATIONAL, macIid.child(LocatorSet.class));
        }
    }

    private RemoteMcastMacs buildRemoteMcastMacs(McastMacsRemote macRemote) {
        RemoteMcastMacsBuilder macRemoteBuilder = new RemoteMcastMacsBuilder();
        if (macRemote.getMac().equals(HwvtepSouthboundConstants.UNKNOWN_DST_STRING)) {
            macRemoteBuilder.setMacEntryKey(HwvtepSouthboundConstants.UNKNOWN_DST_MAC);
        } else {
            macRemoteBuilder.setMacEntryKey(new MacAddress(macRemote.getMac()));
        }
        macRemoteBuilder.setMacEntryUuid(new Uuid(macRemote.getUuid().toString()));
        setIpAddress(macRemoteBuilder, macRemote);
        setLocatorSet(macRemoteBuilder, macRemote);
        setLogicalSwitch(macRemoteBuilder, macRemote);

        return macRemoteBuilder.build();
    }

    private void setLogicalSwitch(RemoteMcastMacsBuilder macRemoteBuilder, McastMacsRemote macRemote) {
        if (macRemote.getLogicalSwitchColumn() != null && macRemote.getLogicalSwitchColumn().getData() != null) {
            UUID lsUUID = macRemote.getLogicalSwitchColumn().getData();
            LogicalSwitch logicalSwitch = getOvsdbConnectionInstance().getDeviceInfo().getLogicalSwitch(lsUUID);
            if (logicalSwitch != null) {
                InstanceIdentifier<LogicalSwitches> switchIid =
                        HwvtepSouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(), logicalSwitch);
                macRemoteBuilder.setLogicalSwitchRef(new HwvtepLogicalSwitchRef(switchIid));
            }
        }
    }

    private static void setIpAddress(RemoteMcastMacsBuilder macRemoteBuilder, McastMacsRemote macRemote) {
        if (macRemote.getIpAddr() != null && !macRemote.getIpAddr().isEmpty()) {
            macRemoteBuilder.setIpaddr(TransactUtils.parseIpAddress(macRemote.getIpAddr()));
        }
    }

    private void setLocatorSet(RemoteMcastMacsBuilder macRemoteBuilder, McastMacsRemote macRemote) {
        if (macRemote.getLocatorSetColumn() != null && macRemote.getLocatorSetColumn().getData() != null) {
            UUID locSetUUID = macRemote.getLocatorSetColumn().getData();
            PhysicalLocatorSet plSet = updatedPLocSetRows.get(locSetUUID);
            if (plSet != null) {
                if (plSet.getLocatorsColumn() != null && plSet.getLocatorsColumn().getData() != null
                        && !plSet.getLocatorsColumn().getData().isEmpty()) {
                    List<LocatorSet> plsList = new ArrayList<>();
                    for (UUID locUUID : plSet.getLocatorsColumn().getData()) {
                        PhysicalLocator locator = updatedPLocRows.get(locUUID);
                        if (locator == null) {
                            locator = getOvsdbConnectionInstance().getDeviceInfo().getPhysicalLocator(locUUID);
                        }
                        InstanceIdentifier<TerminationPoint> tpIid = HwvtepSouthboundMapper.createInstanceIdentifier(
                                getOvsdbConnectionInstance().getInstanceIdentifier(), locator);
                        plsList.add(new LocatorSetBuilder()
                                .setLocatorRef(new HwvtepPhysicalLocatorRef(tpIid)).build());
                    }
                    macRemoteBuilder.setLocatorSet(plsList);
                }
            }
        }
    }
}
