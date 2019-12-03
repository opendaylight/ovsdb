/*
 * Copyright (c) 2015 - 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import com.google.common.base.Optional;
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
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class HwvtepPhysicalLocatorUpdateCommand extends AbstractTransactionCommand {

    private final Map<UUID, PhysicalLocator> updatedPLocRows;
    private final Map<UUID, PhysicalLocator> oldPLocRows;

    public HwvtepPhysicalLocatorUpdateCommand(final HwvtepConnectionInstance key, final TableUpdates updates,
            final DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedPLocRows = extractRowsUpdated(PhysicalLocator.class);
        oldPLocRows = extractRowsOld(PhysicalLocator.class);
    }

    @Override
    public void execute(final ReadWriteTransaction transaction) {
        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        if (updatedPLocRows.isEmpty()) {
            return;
        }
        Optional<Node> node = HwvtepSouthboundUtil.readNode(transaction, connectionIId);
        if (node.isPresent()) {
            updateTerminationPoints(transaction, node.get());
        }
    }

    private void updateTerminationPoints(final ReadWriteTransaction transaction, final Node node) {
        for (Entry<UUID, PhysicalLocator> locUpdate : updatedPLocRows.entrySet()) {
            PhysicalLocator locator = locUpdate.getValue();
            InstanceIdentifier<Node> nodeIid = HwvtepSouthboundMapper.createInstanceIdentifier(node.getNodeId());
            TerminationPointKey tpKey = HwvtepSouthboundMapper.getTerminationPointKey(locator);
            if (nodeIid != null && tpKey != null) {
                TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
                tpBuilder.withKey(tpKey);
                tpBuilder.setTpId(tpKey.getTpId());
                InstanceIdentifier<TerminationPoint> tpPath =
                        HwvtepSouthboundMapper.createInstanceIdentifier(nodeIid, locator);
                HwvtepPhysicalLocatorAugmentationBuilder tpAugmentationBuilder =
                        new HwvtepPhysicalLocatorAugmentationBuilder();
                tpAugmentationBuilder.setPhysicalLocatorUuid(new Uuid(locator.getUuid().toString()));
                setEncapsType(tpAugmentationBuilder, locator);
                setDstIp(tpAugmentationBuilder, locator);
                tpBuilder.addAugmentation(HwvtepPhysicalLocatorAugmentation.class, tpAugmentationBuilder.build());
                if (oldPLocRows.containsKey(locUpdate.getKey())) {
                    transaction.merge(LogicalDatastoreType.OPERATIONAL,
                            tpPath, tpBuilder.build());
                } else {
                    transaction.put(LogicalDatastoreType.OPERATIONAL,
                            tpPath, tpBuilder.build());
                }
                getOvsdbConnectionInstance().getDeviceInfo().updateDeviceOperData(
                        TerminationPoint.class, tpPath, locator.getUuid(), locator);
            }
        }
    }

    private static void setEncapsType(final HwvtepPhysicalLocatorAugmentationBuilder tpAugmentationBuilder,
            final PhysicalLocator locator) {
        String encapsType = locator.getEncapsulationTypeColumn().getData();
        if (HwvtepSouthboundMapper.createEncapsulationType(encapsType) != null) {
            tpAugmentationBuilder.setEncapsulationType(HwvtepSouthboundMapper.createEncapsulationType(encapsType));
        }
    }

    private static void setDstIp(final HwvtepPhysicalLocatorAugmentationBuilder tpAugmentationBuilder,
            final PhysicalLocator locator) {
        tpAugmentationBuilder.setDstIp(IpAddressBuilder.getDefaultInstance(locator.getDstIpColumn().getData()));
    }

}
