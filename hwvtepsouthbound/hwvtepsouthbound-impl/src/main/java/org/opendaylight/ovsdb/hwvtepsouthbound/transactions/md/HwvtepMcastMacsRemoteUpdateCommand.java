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
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;
import org.opendaylight.ovsdb.schema.hardwarevtep.McastMacsRemote;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocator;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocatorSet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepLogicalSwitchRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.locator.set.attributes.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.locator.set.attributes.LocatorSetBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class HwvtepMcastMacsRemoteUpdateCommand extends AbstractTransactionCommand {
    private final Map<UUID, McastMacsRemote> updatedMMacsRemoteRows;
    private final Map<UUID, PhysicalLocatorSet> updatedPLocSetRows;
    private final Map<UUID, PhysicalLocator> updatedPLocRows;

    public HwvtepMcastMacsRemoteUpdateCommand(final HwvtepConnectionInstance key, final TableUpdates updates,
            final DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedMMacsRemoteRows = extractRowsUpdated(McastMacsRemote.class);
        updatedPLocSetRows = extractRowsUpdated(PhysicalLocatorSet.class);
        updatedPLocRows = extractRowsUpdated(PhysicalLocator.class);
    }

    @Override
    public void execute(final ReadWriteTransaction transaction) {
        for (Entry<UUID, McastMacsRemote> entry : updatedMMacsRemoteRows.entrySet()) {
            updateData(transaction, entry.getValue());
        }
    }

    private void updateData(final ReadWriteTransaction transaction, final McastMacsRemote macRemote) {
        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        Node connectionNode = buildConnectionNode(macRemote);
        transaction.merge(LogicalDatastoreType.OPERATIONAL, connectionIId, connectionNode);
        InstanceIdentifier<RemoteMcastMacs> macIid = getMacIid(connectionIId, connectionNode);
        getOvsdbConnectionInstance().getDeviceInfo().updateDeviceOperData(RemoteMcastMacs.class,
                macIid, macRemote.getUuid(), macRemote);
    }

    private static InstanceIdentifier<RemoteMcastMacs> getMacIid(final InstanceIdentifier<Node> connectionIId,
            final Node connectionNode) {
        RemoteMcastMacsKey macsKey =
                connectionNode.augmentation(HwvtepGlobalAugmentation.class).getRemoteMcastMacs().get(0).key();
        InstanceIdentifier<RemoteMcastMacs> key = connectionIId.augmentation(HwvtepGlobalAugmentation.class)
                .child(RemoteMcastMacs.class, macsKey);
        return key;
    }

    private Node buildConnectionNode(final McastMacsRemote macRemote) {
        NodeBuilder connectionNode = new NodeBuilder();
        connectionNode.setNodeId(getOvsdbConnectionInstance().getNodeId());
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

        List<RemoteMcastMacs> macRemoteList = new ArrayList<>();
        macRemoteList.add(macRemoteBuilder.build());

        HwvtepGlobalAugmentationBuilder hgAugmentationBuilder = new HwvtepGlobalAugmentationBuilder();
        hgAugmentationBuilder.setRemoteMcastMacs(macRemoteList);
        connectionNode.addAugmentation(HwvtepGlobalAugmentation.class, hgAugmentationBuilder.build());
        return connectionNode.build();
    }

    private void setLogicalSwitch(final RemoteMcastMacsBuilder macRemoteBuilder, final McastMacsRemote macRemote) {
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

    private static void setIpAddress(final RemoteMcastMacsBuilder macRemoteBuilder, final McastMacsRemote macRemote) {
        if (macRemote.getIpAddr() != null && !macRemote.getIpAddr().isEmpty()) {
            macRemoteBuilder.setIpaddr(IpAddressBuilder.getDefaultInstance(macRemote.getIpAddr()));
        }
    }

    private void setLocatorSet(final RemoteMcastMacsBuilder macRemoteBuilder, final McastMacsRemote macRemote) {
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
                            locator = (PhysicalLocator) getOvsdbConnectionInstance()
                                    .getDeviceInfo().getDeviceOperData(TerminationPoint.class, locUUID);
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
