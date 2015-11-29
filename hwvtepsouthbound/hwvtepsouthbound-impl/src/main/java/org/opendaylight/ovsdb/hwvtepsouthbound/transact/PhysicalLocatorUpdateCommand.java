/*
 * Copyright (c) 2015 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class PhysicalLocatorUpdateCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(PhysicalLocatorUpdateCommand.class);

    public PhysicalLocatorUpdateCommand(HwvtepOperationalState state,
            Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, HwvtepPhysicalLocatorAugmentation> createds =
                extractCreated(getChanges(),HwvtepPhysicalLocatorAugmentation.class);
        if (!createds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, HwvtepPhysicalLocatorAugmentation> created:
                createds.entrySet()) {
                updatePhysicalLocator(transaction,  created.getKey(), created.getValue());
            }
        }
        Map<InstanceIdentifier<Node>, HwvtepPhysicalLocatorAugmentation> updateds =
                extractUpdated(getChanges(),HwvtepPhysicalLocatorAugmentation.class);
        if (!updateds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, HwvtepPhysicalLocatorAugmentation> updated:
                updateds.entrySet()) {
                updatePhysicalLocator(transaction,  updated.getKey(), updated.getValue());
            }
        }
    }

    private void updatePhysicalLocator(TransactionBuilder transaction,
            InstanceIdentifier<Node> iid, HwvtepPhysicalLocatorAugmentation physicalLocatorAugmentation) {
        LOG.debug("Creating a physical locator: {}", physicalLocatorAugmentation.getDstIp());
        Optional<HwvtepPhysicalLocatorAugmentation> operationalPhysicalLocatorOptional =
                getOperationalState().getPhysicalLocatorAugmentation(iid);
        PhysicalLocator physicalLocator = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), PhysicalLocator.class);
        setEncapsulationType(physicalLocator, physicalLocatorAugmentation);
        setDstIp(physicalLocator, physicalLocatorAugmentation);
        if (!operationalPhysicalLocatorOptional.isPresent()) {
            transaction.add(op.insert(physicalLocator));
        } else {
            //Since our key for physical locator is encaps type and IP,
            //changing IP is basically creating a new PhysicalLocator.
            //Unless it has any other fields, it will not require updates.
        }
    }

    private void setEncapsulationType(PhysicalLocator physicalLocator, HwvtepPhysicalLocatorAugmentation inputLocator) {
        if (inputLocator.getEncapsulationType() != null) {
            String encapType = HwvtepSouthboundConstants.ENCAPS_TYPE_MAP.get(HwvtepSouthboundMapper.createEncapsulationType(""));
            physicalLocator.setEncapsulationType(encapType);
        }
    }

    private void setDstIp(PhysicalLocator physicalLocator, HwvtepPhysicalLocatorAugmentation inputLocator) {
        if (inputLocator.getDstIp() != null) {
            physicalLocator.setDstIp(inputLocator.getDstIp().getIpv4Address().getValue());
        }
    }

    private Map<InstanceIdentifier<Node>, HwvtepPhysicalLocatorAugmentation> extractCreated(
            Collection<DataTreeModification<Node>> changes, Class<HwvtepPhysicalLocatorAugmentation> class1) {
        Map<InstanceIdentifier<Node>, HwvtepPhysicalLocatorAugmentation> result
            = new HashMap<InstanceIdentifier<Node>, HwvtepPhysicalLocatorAugmentation>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node created = TransactUtils.getCreated(mod);
                if (created != null) {
                    List<TerminationPoint> physicalLocators = created.getTerminationPoint();
                    if (physicalLocators != null) {
                        for (TerminationPoint physicalLocator : physicalLocators) {
                            HwvtepPhysicalLocatorAugmentation physicalLocatorAugmentation =
                                    physicalLocator.getAugmentation(HwvtepPhysicalLocatorAugmentation.class);
                            if (physicalLocatorAugmentation != null) {
                                result.put(key, physicalLocatorAugmentation);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    private Map<InstanceIdentifier<Node>, HwvtepPhysicalLocatorAugmentation> extractUpdated(
            Collection<DataTreeModification<Node>> changes, Class<HwvtepPhysicalLocatorAugmentation> class1) {
        Map<InstanceIdentifier<Node>, HwvtepPhysicalLocatorAugmentation> result
            = new HashMap<InstanceIdentifier<Node>, HwvtepPhysicalLocatorAugmentation>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node updated = TransactUtils.getUpdated(mod);
                if (updated != null) {
                    List<TerminationPoint> physicalLocators = updated.getTerminationPoint();
                    if (physicalLocators != null) {
                        for (TerminationPoint physicalLocator : physicalLocators) {
                            HwvtepPhysicalLocatorAugmentation physicalLocatorAugmentation =
                                    physicalLocator.getAugmentation(HwvtepPhysicalLocatorAugmentation.class);
                            if (physicalLocatorAugmentation != null) {
                                result.put(key, physicalLocatorAugmentation);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }
}
