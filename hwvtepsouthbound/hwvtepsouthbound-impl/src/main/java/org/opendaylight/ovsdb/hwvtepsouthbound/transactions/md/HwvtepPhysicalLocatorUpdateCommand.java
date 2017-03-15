/*
 * Copyright (c) 2015 - 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

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
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.Map;
import java.util.Map.Entry;

public class HwvtepPhysicalLocatorUpdateCommand extends AbstractTransactionCommand {

    private Map<UUID, PhysicalLocator> updatedPLocRows;
    private Map<UUID, PhysicalLocator> oldPLocRows;

    public HwvtepPhysicalLocatorUpdateCommand(HwvtepConnectionInstance key, TableUpdates updates, DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedPLocRows = TyperUtils.extractRowsUpdated(PhysicalLocator.class, getUpdates(), getDbSchema());
        oldPLocRows = TyperUtils.extractRowsOld(PhysicalLocator.class, getUpdates(), getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        if (updatedPLocRows.isEmpty()) {
            return;
        }
        Optional<Node> node = HwvtepSouthboundUtil.readNode(transaction, connectionIId);
        if (node.isPresent()) {
            updateTerminationPoints(transaction, node.get());
        }
    }

    private void updateTerminationPoints(ReadWriteTransaction transaction, Node node) {
        for (Entry<UUID, PhysicalLocator> pLocUpdate : updatedPLocRows.entrySet()) {
            PhysicalLocator pLoc = pLocUpdate.getValue();
            InstanceIdentifier<Node> nodeIid = HwvtepSouthboundMapper.createInstanceIdentifier(node.getNodeId());
            TerminationPointKey tpKey = HwvtepSouthboundMapper.getTerminationPointKey(pLoc);
            if (nodeIid != null && tpKey != null) {
                TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
                tpBuilder.setKey(tpKey);
                tpBuilder.setTpId(tpKey.getTpId());
                InstanceIdentifier<TerminationPoint> tpPath =
                        HwvtepSouthboundMapper.createInstanceIdentifier(nodeIid, pLoc);
                HwvtepPhysicalLocatorAugmentationBuilder tpAugmentationBuilder =
                        new HwvtepPhysicalLocatorAugmentationBuilder();
                tpAugmentationBuilder.setPhysicalLocatorUuid(new Uuid(pLoc.getUuid().toString()));
                setEncapsType(tpAugmentationBuilder, pLoc);
                setDstIp(tpAugmentationBuilder, pLoc);
                tpBuilder.addAugmentation(HwvtepPhysicalLocatorAugmentation.class, tpAugmentationBuilder.build());
                if (oldPLocRows.containsKey(pLocUpdate.getKey())) {
                    transaction.merge(LogicalDatastoreType.OPERATIONAL,
                            tpPath, tpBuilder.build());
                } else {
                    transaction.put(LogicalDatastoreType.OPERATIONAL,
                            tpPath, tpBuilder.build());
                }
                getOvsdbConnectionInstance().getDeviceInfo().updateDeviceOperData(
                        TerminationPoint.class, tpPath, pLoc.getUuid(), pLoc);
            }
        }
    }

    private void setEncapsType(HwvtepPhysicalLocatorAugmentationBuilder tpAugmentationBuilder, PhysicalLocator pLoc) {
        String encapsType = pLoc.getEncapsulationTypeColumn().getData();
        if (HwvtepSouthboundMapper.createEncapsulationType(encapsType) != null) {
            tpAugmentationBuilder.setEncapsulationType(HwvtepSouthboundMapper.createEncapsulationType(encapsType));
        }
    }

    private void setDstIp(HwvtepPhysicalLocatorAugmentationBuilder tpAugmentationBuilder, PhysicalLocator pLoc) {
        IpAddress ip = new IpAddress(pLoc.getDstIpColumn().getData().toCharArray());
        tpAugmentationBuilder.setDstIp(ip);
    }

}
