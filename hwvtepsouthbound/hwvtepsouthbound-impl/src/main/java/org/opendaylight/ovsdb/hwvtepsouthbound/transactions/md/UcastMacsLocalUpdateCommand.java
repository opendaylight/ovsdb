/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import org.opendaylight.ovsdb.schema.hardwarevtep.UcastMacsLocal;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepLogicalSwitchRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalUcastMacsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class UcastMacsLocalUpdateCommand extends AbstractTransactionCommand {

    private Map<UUID, UcastMacsLocal> updatedUMacsLocalRows;
    private Map<UUID, PhysicalLocator> updatedPLocRows;
    private Map<UUID, LogicalSwitch> updatedLSRows;

    public UcastMacsLocalUpdateCommand(HwvtepConnectionInstance key, TableUpdates updates, DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedUMacsLocalRows = TyperUtils.extractRowsUpdated(UcastMacsLocal.class, getUpdates(), getDbSchema());
        updatedPLocRows = TyperUtils.extractRowsUpdated(PhysicalLocator.class, getUpdates(), getDbSchema());
        updatedLSRows = TyperUtils.extractRowsUpdated(LogicalSwitch.class, getUpdates(), getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        for (UcastMacsLocal ucastMacsLocal : updatedUMacsLocalRows.values()) {
            updateData(transaction, ucastMacsLocal);
        }
    }

    private void updateData(ReadWriteTransaction transaction, UcastMacsLocal ucml) {
        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        Optional<Node> connection = HwvtepSouthboundUtil.readNode(transaction, connectionIId);
        if (connection.isPresent()) {
            // Update the connection node to let it know it manages this UCastMacsLocal
            Node connectionNode = buildConnectionNode(ucml);
            transaction.merge(LogicalDatastoreType.OPERATIONAL, connectionIId, connectionNode);
            // TODO: Delete entries that are no longer needed
        }
    }

    private Node buildConnectionNode(UcastMacsLocal ucml) {
        //Update node with UcastMacsLocal reference
        NodeBuilder connectionNode = new NodeBuilder();
        connectionNode.setNodeId(getOvsdbConnectionInstance().getNodeId());
        InstanceIdentifier<Node> nodeIid = getOvsdbConnectionInstance().getInstanceIdentifier();
        HwvtepGlobalAugmentationBuilder hgAugmentationBuilder = new HwvtepGlobalAugmentationBuilder();
        LocalUcastMacsBuilder ucmlBuilder = new LocalUcastMacsBuilder();
        if (ucml.getIpAddr() != null && !ucml.getIpAddr().isEmpty()) {
            ucmlBuilder.setIpaddr(new IpAddress(ucml.getIpAddr().toCharArray()));
        }
        ucmlBuilder.setMacEntryKey(new MacAddress(ucml.getMac()));
        if (ucml.getLocatorColumn() != null && ucml.getLocatorColumn().getData() != null) {
            UUID plocUUID = ucml.getLocatorColumn().getData();
            PhysicalLocator physicalLocator = updatedPLocRows.get(plocUUID);
            if (physicalLocator != null) {
                InstanceIdentifier<TerminationPoint> plIid =
                        HwvtepSouthboundMapper.createInstanceIdentifier(nodeIid, physicalLocator);
                ucmlBuilder.setLocatorRef(new HwvtepPhysicalLocatorRef(plIid));
            }
        }
        if (ucml.getLogicalSwitchColumn() != null && ucml.getLogicalSwitchColumn().getData() != null) {
            UUID lsUUID = ucml.getLogicalSwitchColumn().getData();
            LogicalSwitch logicalSwitch = updatedLSRows.get(lsUUID);
            if (logicalSwitch != null) {
                InstanceIdentifier<LogicalSwitches> lSwitchIid =
                        HwvtepSouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(), logicalSwitch);
                ucmlBuilder.setLogicalSwitchRef(new HwvtepLogicalSwitchRef(lSwitchIid));
            }
        }
        List<LocalUcastMacs> umclList = new ArrayList<>();
        umclList.add(ucmlBuilder.build());
        hgAugmentationBuilder.setLocalUcastMacs(umclList);
        connectionNode.addAugmentation(HwvtepGlobalAugmentation.class, hgAugmentationBuilder.build());
        return connectionNode.build();
    }

}
