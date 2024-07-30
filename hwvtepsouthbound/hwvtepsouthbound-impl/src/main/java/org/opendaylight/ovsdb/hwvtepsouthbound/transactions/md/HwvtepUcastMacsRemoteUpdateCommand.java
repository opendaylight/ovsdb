/*
 * Copyright (c) 2015, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import java.util.Collection;
import java.util.Map;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.TransactUtils;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocator;
import org.opendaylight.ovsdb.schema.hardwarevtep.UcastMacsRemote;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepLogicalSwitchRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class HwvtepUcastMacsRemoteUpdateCommand extends AbstractTransactionCommand {

    private final Map<UUID, UcastMacsRemote> updatedUMacsRemoteRows;
    private final Map<UUID, PhysicalLocator> updatedPLocRows;

    public HwvtepUcastMacsRemoteUpdateCommand(HwvtepConnectionInstance key, TableUpdates updates,
            DatabaseSchema dbSchema) {
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

    private Node buildConnectionNode(final Collection<UcastMacsRemote> macRemotes) {
        var remoteUMacs = macRemotes.stream().map(this::buildRemoteUcast).collect(BindingMap.toOrderedMap());

        return new NodeBuilder()
            .setNodeId(getOvsdbConnectionInstance().getNodeId())
            .addAugmentation(new HwvtepGlobalAugmentationBuilder().setRemoteUcastMacs(remoteUMacs).build())
            .build();
    }

    private RemoteUcastMacs buildRemoteUcast(final UcastMacsRemote macRemote) {
        RemoteUcastMacsBuilder rumBuilder = new RemoteUcastMacsBuilder();
        rumBuilder.setMacEntryKey(new MacAddress(macRemote.getMac()));
        rumBuilder.setMacEntryUuid(new Uuid(macRemote.getUuid().toString()));
        if (macRemote.getIpAddr() != null && !macRemote.getIpAddr().isEmpty()) {
            rumBuilder.setIpaddr(TransactUtils.parseIpAddress(macRemote.getIpAddr()));
        }
        if (macRemote.getLocatorColumn() != null
                && macRemote.getLocatorColumn().getData() != null) {
            UUID locUUID = macRemote.getLocatorColumn().getData();
            PhysicalLocator physicalLocator = updatedPLocRows.get(locUUID);
            if (physicalLocator == null) {
                physicalLocator = getOvsdbConnectionInstance().getDeviceInfo().getPhysicalLocator(locUUID);
            }
            if (physicalLocator != null) {
                InstanceIdentifier<Node> nodeIid = getOvsdbConnectionInstance().getInstanceIdentifier();
                InstanceIdentifier<TerminationPoint> plIid = HwvtepSouthboundMapper.createInstanceIdentifier(nodeIid,
                        physicalLocator);
                rumBuilder.setLocatorRef(new HwvtepPhysicalLocatorRef(plIid.toIdentifier()));
            }
        }
        if (macRemote.getLogicalSwitchColumn() != null
                && macRemote.getLogicalSwitchColumn().getData() != null) {
            UUID lsUUID = macRemote.getLogicalSwitchColumn().getData();
            final LogicalSwitch logicalSwitch = getOvsdbConnectionInstance().getDeviceInfo().getLogicalSwitch(lsUUID);
            if (logicalSwitch != null) {
                InstanceIdentifier<LogicalSwitches> switchIid =
                        HwvtepSouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(), logicalSwitch);
                rumBuilder.setLogicalSwitchRef(new HwvtepLogicalSwitchRef(switchIid.toIdentifier()));
            }
        }
        RemoteUcastMacs remoteUcastMacs = rumBuilder.build();
        InstanceIdentifier<RemoteUcastMacs> macIid = getMacIid(remoteUcastMacs);
        addToUpdateTx(RemoteUcastMacs.class, macIid, macRemote.getUuid(), macRemote);
        return remoteUcastMacs;
    }

    private InstanceIdentifier<RemoteUcastMacs> getMacIid(final RemoteUcastMacs remoteUcastMacs) {
        return getOvsdbConnectionInstance().getInstanceIdentifier()
                .augmentation(HwvtepGlobalAugmentation.class).child(RemoteUcastMacs.class, remoteUcastMacs.key());
    }
}
