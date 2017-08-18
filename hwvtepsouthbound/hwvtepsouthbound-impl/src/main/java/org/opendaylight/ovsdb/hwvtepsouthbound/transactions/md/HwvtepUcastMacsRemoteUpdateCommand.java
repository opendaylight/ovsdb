/*
 * Copyright (c) 2015, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocator;
import org.opendaylight.ovsdb.schema.hardwarevtep.UcastMacsRemote;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepLogicalSwitchRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class HwvtepUcastMacsRemoteUpdateCommand extends AbstractTransactionCommand {

    private final Map<UUID, UcastMacsRemote> updatedUMacsRemoteRows;
    private final Map<UUID, PhysicalLocator> updatedPLocRows;

    public HwvtepUcastMacsRemoteUpdateCommand(HwvtepConnectionInstance key, TableUpdates updates, DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedUMacsRemoteRows = TyperUtils.extractRowsUpdated(UcastMacsRemote.class, getUpdates(), getDbSchema());
        updatedPLocRows = TyperUtils.extractRowsUpdated(PhysicalLocator.class, getUpdates(), getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        if (updatedUMacsRemoteRows != null && !updatedUMacsRemoteRows.isEmpty()) {
            updateUcastMacsRemote(transaction, updatedUMacsRemoteRows.values());
        }
    }

    private void updateUcastMacsRemote(ReadWriteTransaction transaction, Collection<UcastMacsRemote> ucastMacsRemote) {
        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        Node connectionNode = buildConnectionNode(ucastMacsRemote);
        transaction.merge(LogicalDatastoreType.OPERATIONAL, connectionIId, connectionNode);
    }

    private Node buildConnectionNode(final Collection<UcastMacsRemote> uMacRemotes) {
        NodeBuilder connectionNode = new NodeBuilder();
        connectionNode.setNodeId(getOvsdbConnectionInstance().getNodeId());
        InstanceIdentifier<Node> nodeIid = getOvsdbConnectionInstance().getInstanceIdentifier();
        HwvtepGlobalAugmentationBuilder hgAugmentationBuilder = new HwvtepGlobalAugmentationBuilder();
        List<RemoteUcastMacs> remoteUMacs = new ArrayList<>();
        uMacRemotes.forEach( (mac) -> remoteUMacs.add(buildRemoteUcast(mac)));
        hgAugmentationBuilder.setRemoteUcastMacs(remoteUMacs);
        connectionNode.addAugmentation(HwvtepGlobalAugmentation.class, hgAugmentationBuilder.build());
        return connectionNode.build();
    }

    private RemoteUcastMacs buildRemoteUcast(final UcastMacsRemote uMacRemote) {
        InstanceIdentifier<Node> nodeIid = getOvsdbConnectionInstance().getInstanceIdentifier();
        RemoteUcastMacsBuilder rumBuilder = new RemoteUcastMacsBuilder();
        rumBuilder.setMacEntryKey(new MacAddress(uMacRemote.getMac()));
        rumBuilder.setMacEntryUuid(new Uuid(uMacRemote.getUuid().toString()));
        if (uMacRemote.getIpAddr() != null && !uMacRemote.getIpAddr().isEmpty()) {
            rumBuilder.setIpaddr(new IpAddress(uMacRemote.getIpAddr().toCharArray()));
        }
        if (uMacRemote.getLocatorColumn() != null
                && uMacRemote.getLocatorColumn().getData() != null) {
            UUID pLocUUID = uMacRemote.getLocatorColumn().getData();
            PhysicalLocator physicalLocator = updatedPLocRows.get(pLocUUID);
            if (physicalLocator == null) {
                physicalLocator = (PhysicalLocator) getOvsdbConnectionInstance().
                        getDeviceInfo().getDeviceOperData(TerminationPoint.class, pLocUUID);
            }
            if (physicalLocator != null) {
                InstanceIdentifier<TerminationPoint> plIid = HwvtepSouthboundMapper.createInstanceIdentifier(nodeIid,
                        physicalLocator);
                rumBuilder.setLocatorRef(new HwvtepPhysicalLocatorRef(plIid));
            }
        }
        if (uMacRemote.getLogicalSwitchColumn() != null
                && uMacRemote.getLogicalSwitchColumn().getData() != null) {
            UUID lsUUID = uMacRemote.getLogicalSwitchColumn().getData();
            final LogicalSwitch logicalSwitch = getOvsdbConnectionInstance().getDeviceInfo().getLogicalSwitch(lsUUID);
            if (logicalSwitch != null) {
                InstanceIdentifier<LogicalSwitches> lSwitchIid =
                        HwvtepSouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(), logicalSwitch);
                rumBuilder.setLogicalSwitchRef(new HwvtepLogicalSwitchRef(lSwitchIid));
            }
        }
        RemoteUcastMacs remoteUcastMacs = rumBuilder.build();
        InstanceIdentifier<RemoteUcastMacs> macIid = getMacIid(remoteUcastMacs);
        getOvsdbConnectionInstance().getDeviceInfo().updateDeviceOperData(RemoteUcastMacs.class, macIid,
                uMacRemote.getUuid(), uMacRemote);
        return remoteUcastMacs;
    }

    private InstanceIdentifier<RemoteUcastMacs> getMacIid(final RemoteUcastMacs remoteUcastMacs) {
        return getOvsdbConnectionInstance().getInstanceIdentifier().
                augmentation(HwvtepGlobalAugmentation.class).child(RemoteUcastMacs.class, remoteUcastMacs.getKey());
    }
}
