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
import org.opendaylight.ovsdb.schema.hardwarevtep.UcastMacsLocal;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
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
import org.opendaylight.yangtools.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HwvtepUcastMacsLocalUpdateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(HwvtepUcastMacsLocalUpdateCommand.class);

    private final Map<UUID, UcastMacsLocal> updatedUMacsLocalRows;
    private final Map<UUID, PhysicalLocator> updatedPLocRows;

    public HwvtepUcastMacsLocalUpdateCommand(HwvtepConnectionInstance key, TableUpdates updates,
            DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedUMacsLocalRows = TyperUtils.extractRowsUpdated(UcastMacsLocal.class, getUpdates(), getDbSchema());
        updatedPLocRows = TyperUtils.extractRowsUpdated(PhysicalLocator.class, getUpdates(), getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        if (updatedUMacsLocalRows != null && !updatedUMacsLocalRows.isEmpty()) {
            updateData(transaction, updatedUMacsLocalRows.values());
        }
    }

    private void updateData(ReadWriteTransaction transaction, Collection<UcastMacsLocal> ucml) {
        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        LOG.info("DEVICE - {} LocalUcastMacs for Node {} - {}", TransactionType.ADD,
            connectionIId.firstKeyOf(Node.class).getNodeId().getValue(), ucml);
        Node connectionNode = buildConnectionNode(ucml);
        transaction.merge(LogicalDatastoreType.OPERATIONAL, connectionIId, connectionNode);
    }

    private Node buildConnectionNode(final Collection<UcastMacsLocal> ucml) {
        //Update node with UcastMacsLocal reference
        NodeBuilder connectionNode = new NodeBuilder();
        connectionNode.setNodeId(getOvsdbConnectionInstance().getNodeId());
        var umclList = ucml.stream().map(this::buildLocalUcastMac).collect(BindingMap.toOrderedMap());
        connectionNode.addAugmentation(new HwvtepGlobalAugmentationBuilder().setLocalUcastMacs(umclList).build());
        return connectionNode.build();
    }

    private LocalUcastMacs buildLocalUcastMac(final UcastMacsLocal ucml) {
        LocalUcastMacsBuilder ucmlBuilder = new LocalUcastMacsBuilder();
        if (ucml.getIpAddr() != null && !ucml.getIpAddr().isEmpty()) {
            ucmlBuilder.setIpaddr(TransactUtils.parseIpAddress(ucml.getIpAddr()));
        }
        ucmlBuilder.setMacEntryKey(new MacAddress(ucml.getMac()));
        ucmlBuilder.setMacEntryUuid(new Uuid(ucml.getUuid().toString()));
        if (ucml.getLocatorColumn() != null && ucml.getLocatorColumn().getData() != null) {
            UUID plocUUID = ucml.getLocatorColumn().getData();
            PhysicalLocator physicalLocator = updatedPLocRows.get(plocUUID);
            if (physicalLocator == null) {
                physicalLocator = getOvsdbConnectionInstance()
                        .getDeviceInfo().getPhysicalLocator(plocUUID);
            }
            if (physicalLocator != null) {
                InstanceIdentifier<Node> nodeIid = getOvsdbConnectionInstance().getInstanceIdentifier();
                InstanceIdentifier<TerminationPoint> plIid =
                        HwvtepSouthboundMapper.createInstanceIdentifier(nodeIid, physicalLocator);
                ucmlBuilder.setLocatorRef(new HwvtepPhysicalLocatorRef(plIid.toIdentifier()));
            }
        }
        if (ucml.getLogicalSwitchColumn() != null && ucml.getLogicalSwitchColumn().getData() != null) {
            UUID lsUUID = ucml.getLogicalSwitchColumn().getData();
            LogicalSwitch logicalSwitch = getOvsdbConnectionInstance().getDeviceInfo().getLogicalSwitch(lsUUID);
            if (logicalSwitch != null) {
                InstanceIdentifier<LogicalSwitches> switchIid =
                        HwvtepSouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(), logicalSwitch);
                ucmlBuilder.setLogicalSwitchRef(new HwvtepLogicalSwitchRef(switchIid.toIdentifier()));
            }
        }
        LocalUcastMacs ucastMacsLocal = ucmlBuilder.build();
        InstanceIdentifier iid = key.getInstanceIdentifier().augmentation(HwvtepGlobalAugmentation.class)
                .child(LocalUcastMacs.class, ucastMacsLocal.key());
        addToUpdateTx(LocalUcastMacs.class, iid, ucml.getUuid(), ucastMacsLocal);
        return ucastMacsLocal;
    }
}
