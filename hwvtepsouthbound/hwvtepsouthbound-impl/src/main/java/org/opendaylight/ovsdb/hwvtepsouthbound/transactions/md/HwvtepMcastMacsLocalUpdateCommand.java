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
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;
import org.opendaylight.ovsdb.schema.hardwarevtep.McastMacsLocal;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocator;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocatorSet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepLogicalSwitchRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalMcastMacsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.locator.set.attributes.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.locator.set.attributes.LocatorSetBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class HwvtepMcastMacsLocalUpdateCommand extends AbstractTransactionCommand {

    private final Map<UUID, McastMacsLocal> updatedMMacsLocalRows;
    private final Map<UUID, PhysicalLocatorSet> updatedPLocSetRows;
    private final Map<UUID, PhysicalLocator> updatedPLocRows;

    public HwvtepMcastMacsLocalUpdateCommand(HwvtepConnectionInstance key, TableUpdates updates,
            DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedMMacsLocalRows = TyperUtils.extractRowsUpdated(McastMacsLocal.class, getUpdates(), getDbSchema());
        updatedPLocSetRows = TyperUtils.extractRowsUpdated(PhysicalLocatorSet.class, getUpdates(), getDbSchema());
        updatedPLocRows = TyperUtils.extractRowsUpdated(PhysicalLocator.class, getUpdates(), getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        for (McastMacsLocal mcastMacsLocal : updatedMMacsLocalRows.values()) {
            updateData(transaction, mcastMacsLocal);
        }
    }

    private void updateData(ReadWriteTransaction transaction, McastMacsLocal macLocal) {
        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        Node connectionNode = buildConnectionNode(macLocal);
        transaction.merge(LogicalDatastoreType.OPERATIONAL, connectionIId, connectionNode);
    }

    private Node buildConnectionNode(McastMacsLocal macLocal) {
        NodeBuilder connectionNode = new NodeBuilder();
        connectionNode.setNodeId(getOvsdbConnectionInstance().getNodeId());
        LocalMcastMacsBuilder macLocalBuilder = new LocalMcastMacsBuilder();
        if (macLocal.getMac().equals(HwvtepSouthboundConstants.UNKNOWN_DST_STRING)) {
            macLocalBuilder.setMacEntryKey(HwvtepSouthboundConstants.UNKNOWN_DST_MAC);
        } else {
            macLocalBuilder.setMacEntryKey(new MacAddress(macLocal.getMac()));
        }
        macLocalBuilder.setMacEntryUuid(new Uuid(macLocal.getUuid().toString()));
        setIpAddress(macLocalBuilder, macLocal);
        setLocatorSet(macLocalBuilder, macLocal);
        setLogicalSwitch(macLocalBuilder, macLocal);

        List<LocalMcastMacs> macLocalList = new ArrayList<>();
        macLocalList.add(macLocalBuilder.build());

        HwvtepGlobalAugmentationBuilder hgAugmentationBuilder = new HwvtepGlobalAugmentationBuilder();
        hgAugmentationBuilder.setLocalMcastMacs(macLocalList);
        connectionNode.addAugmentation(HwvtepGlobalAugmentation.class, hgAugmentationBuilder.build());
        return connectionNode.build();
    }

    private void setLogicalSwitch(LocalMcastMacsBuilder macLocalBuilder, McastMacsLocal macLocal) {
        if (macLocal.getLogicalSwitchColumn() != null && macLocal.getLogicalSwitchColumn().getData() != null) {
            UUID lsUUID = macLocal.getLogicalSwitchColumn().getData();
            LogicalSwitch logicalSwitch = getOvsdbConnectionInstance().getDeviceInfo().getLogicalSwitch(lsUUID);
            if (logicalSwitch != null) {
                InstanceIdentifier<LogicalSwitches> switchIid =
                        HwvtepSouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(), logicalSwitch);
                macLocalBuilder.setLogicalSwitchRef(new HwvtepLogicalSwitchRef(switchIid));
            }
        }
    }

    private void setIpAddress(LocalMcastMacsBuilder macLocalBuilder, McastMacsLocal macLocal) {
        if (macLocal.getIpAddr() != null && !macLocal.getIpAddr().isEmpty()) {
            macLocalBuilder.setIpaddr(IpAddressBuilder.getDefaultInstance(macLocal.getIpAddr()));
        }
    }

    private void setLocatorSet(LocalMcastMacsBuilder macLocalBuilder, McastMacsLocal macLocal) {
        if (macLocal.getLocatorSetColumn() != null && macLocal.getLocatorSetColumn().getData() != null) {
            UUID locSetUUID = macLocal.getLocatorSetColumn().getData();
            PhysicalLocatorSet plSet = updatedPLocSetRows.get(locSetUUID);
            if (plSet != null) {
                if (plSet.getLocatorsColumn() != null && plSet.getLocatorsColumn().getData() != null
                        && !plSet.getLocatorsColumn().getData().isEmpty()) {
                    List<LocatorSet> plsList = new ArrayList<>();
                    for (UUID locUUID : plSet.getLocatorsColumn().getData()) {
                        PhysicalLocator locator = updatedPLocRows.get(locUUID);
                        if (locator == null) {
                            locator = (PhysicalLocator) getOvsdbConnectionInstance()
                                    .getDeviceInfo().getPhysicalLocator(locUUID);
                        }
                        InstanceIdentifier<TerminationPoint> tpIid = HwvtepSouthboundMapper.createInstanceIdentifier(
                                getOvsdbConnectionInstance().getInstanceIdentifier(), locator);
                        plsList.add(new LocatorSetBuilder()
                                .setLocatorRef(new HwvtepPhysicalLocatorRef(tpIid)).build());
                    }
                    macLocalBuilder.setLocatorSet(plsList);
                }
            }
        }
    }
}
