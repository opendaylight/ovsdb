/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
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
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocator;
import org.opendaylight.ovsdb.schema.hardwarevtep.UcastMacsRemote;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepLogicalSwitchRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class UcastMacsRemoteUpdateCommand extends AbstractTransactionCommand {

    private static final Logger LOG = LoggerFactory.getLogger(UcastMacsRemoteUpdateCommand.class);
    private Map<UUID, UcastMacsRemote> updatedUMacsRemoteRows;
    private Map<UUID, UcastMacsRemote> oldUMacsRemoteRows;
    private Map<UUID, PhysicalLocator> updatedPLocRows;
    private Map<UUID, LogicalSwitch> updatedLSRows;

    public UcastMacsRemoteUpdateCommand(HwvtepConnectionInstance key, TableUpdates updates,
            DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedUMacsRemoteRows = TyperUtils.extractRowsUpdated(UcastMacsRemote.class, getUpdates(),getDbSchema());
        oldUMacsRemoteRows = TyperUtils.extractRowsOld(UcastMacsRemote.class, getUpdates(),getDbSchema());
        updatedPLocRows = TyperUtils.extractRowsUpdated(PhysicalLocator.class, getUpdates(),getDbSchema());
        updatedLSRows = TyperUtils.extractRowsUpdated(LogicalSwitch.class, getUpdates(),getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        if (updatedUMacsRemoteRows != null || !updatedUMacsRemoteRows.isEmpty()) {
            for (Entry<UUID, UcastMacsRemote> umrUpdate : updatedUMacsRemoteRows.entrySet()) {
                updateUcastMacsRemote(transaction, umrUpdate.getValue());
            }
        }
    }

    private void updateUcastMacsRemote(ReadWriteTransaction transaction, UcastMacsRemote ucastMacsRemote) {
        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        Optional<Node> connection = HwvtepSouthboundUtil.readNode(transaction, connectionIId);
        if(connection.isPresent()) {
            Node connectionNode = buildConnectionNode(ucastMacsRemote);
            transaction.merge(LogicalDatastoreType.OPERATIONAL, connectionIId, connectionNode);
            //TODO: Handle any deletes
        }
    }

    private Node buildConnectionNode(UcastMacsRemote uMacRemote) {
        NodeBuilder connectionNode = new NodeBuilder();
        connectionNode.setNodeId(getOvsdbConnectionInstance().getNodeId());
        InstanceIdentifier<Node> nodeIid = getOvsdbConnectionInstance().getInstanceIdentifier();
        HwvtepGlobalAugmentationBuilder hgAugmentationBuilder = new HwvtepGlobalAugmentationBuilder();
        List<RemoteUcastMacs> remoteUMacs = new ArrayList<>();
        RemoteUcastMacsBuilder rumBuilder = new RemoteUcastMacsBuilder();
        rumBuilder.setMacEntryKey(new MacAddress(uMacRemote.getMac()));
        if(uMacRemote.getIpAddr() != null && !uMacRemote.getIpAddr().isEmpty()) {
            rumBuilder.setIpaddr(new IpAddress(uMacRemote.getIpAddr().toCharArray()));
        }
        if(uMacRemote.getLocatorColumn() != null
                        && uMacRemote.getLocatorColumn().getData() != null) {
            UUID pLocUUID = uMacRemote.getLocatorColumn().getData();
            if(updatedPLocRows.get(pLocUUID) != null) {
                InstanceIdentifier<TerminationPoint> plIid = HwvtepSouthboundMapper.createInstanceIdentifier(nodeIid, updatedPLocRows.get(pLocUUID));
                rumBuilder.setLocatorRef(new HwvtepPhysicalLocatorRef(plIid));
            }
        }
        if(uMacRemote.getLogicalSwitchColumn() != null
                        && uMacRemote.getLogicalSwitchColumn().getData() != null) {
            UUID lsUUID = uMacRemote.getLogicalSwitchColumn().getData();
            if(updatedLSRows.get(lsUUID) != null) {
                rumBuilder.setLogicalSwitchRef(new HwvtepLogicalSwitchRef(updatedLSRows.get(lsUUID).getName()));
            }
        }
        remoteUMacs.add(rumBuilder.build());
        hgAugmentationBuilder.setRemoteUcastMacs(remoteUMacs );
        connectionNode.addAugmentation(HwvtepGlobalAugmentation.class, hgAugmentationBuilder.build());
        return connectionNode.build();
    }

}
