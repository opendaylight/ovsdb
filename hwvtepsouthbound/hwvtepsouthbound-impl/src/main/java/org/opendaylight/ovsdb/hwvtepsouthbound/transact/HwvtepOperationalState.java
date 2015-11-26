/*
 * Copyright (c) 2015 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalPortAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitchesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.locator.set.attributes.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class HwvtepOperationalState {
    private static final Logger LOG = LoggerFactory.getLogger(HwvtepOperationalState.class);
    private Map<InstanceIdentifier<Node>, Node> operationalNodes = new HashMap<>();

    public HwvtepOperationalState(DataBroker db, Collection<DataTreeModification<Node>> changes) {
        ReadOnlyTransaction transaction = db.newReadOnlyTransaction();
        Map<InstanceIdentifier<Node>, Node> nodeCreateOrUpdate =
            TransactUtils.extractCreatedOrUpdatedOrRemoved(changes, Node.class);
        if (nodeCreateOrUpdate != null) {
            for (Entry<InstanceIdentifier<Node>, Node> entry: nodeCreateOrUpdate.entrySet()) {
                CheckedFuture<Optional<Node>, ReadFailedException> nodeFuture =
                        transaction.read(LogicalDatastoreType.OPERATIONAL, entry.getKey());
                try {
                    Optional<Node> nodeOptional = nodeFuture.get();
                    if (nodeOptional.isPresent()) {
                        operationalNodes.put(entry.getKey(), nodeOptional.get());
                    }
                } catch (InterruptedException | ExecutionException e) {
                    LOG.warn("Error reading from datastore",e);
                }
            }
        }
        transaction.close();
    }

    public Optional<Node> getGlobalNode(InstanceIdentifier<?> iid) {
        InstanceIdentifier<Node> nodeIid = iid.firstIdentifierOf(Node.class);
        return Optional.fromNullable(operationalNodes.get(nodeIid));
    }

    public Optional<LogicalSwitches> getLogicalSwitches(InstanceIdentifier<?> iid, LogicalSwitchesKey lswitchKey) {
        Optional<Node> nodeOptional = getGlobalNode(iid);
        if (nodeOptional.isPresent()) {
            List<LogicalSwitches> lswitchList = nodeOptional.get().getAugmentation(HwvtepGlobalAugmentation.class).getLogicalSwitches();
            if (lswitchList != null) {
                for (LogicalSwitches lswitch: lswitchList) {
                    if (lswitch.getKey().equals(lswitchKey)) {
                        return Optional.fromNullable(lswitch);
                    }
                }
            }
        }
        return Optional.absent();
    }

    public Optional<PhysicalSwitchAugmentation> getPhysicalSwitchAugmentation(InstanceIdentifier<?> iid) {
        Optional<Node> nodeOptional = getGlobalNode(iid);
        if (nodeOptional.isPresent()) {
            return Optional.fromNullable(nodeOptional.get().getAugmentation(PhysicalSwitchAugmentation.class));
        }
        return Optional.absent();
    }

    public Optional<LocatorSet> getPhysicalLocatorSet(InstanceIdentifier<?> iid) {
        Optional<Node> nodeOptional = getGlobalNode(iid);
        //TODO: physical locator set are under different logical switches
        /*if (nodeOptional.isPresent()) {
            return Optional.fromNullable(nodeOptional.get().getAugmentation(HwvtepGlobalAugmentation.class).getLogicalSwitches());
        }*/
        return Optional.absent();
    }

    public Optional<TerminationPoint> getHwvtepTerminationPoint(InstanceIdentifier<?> iid) {
        if (iid != null) {
            Optional<Node> nodeOptional = getGlobalNode(iid);
            if (nodeOptional.isPresent() && nodeOptional.get().getTerminationPoint() != null) {
                TerminationPointKey key = iid.firstKeyOf(TerminationPoint.class, TerminationPointKey.class);
                if (key != null) {
                    for (TerminationPoint tp:nodeOptional.get().getTerminationPoint()) {
                        if (tp.getKey().equals(key)) {
                            return Optional.of(tp);
                        }
                    }
                }
            }
        }
        return Optional.absent();
    }

    public Optional<HwvtepPhysicalLocatorAugmentation> getPhysicalLocatorAugmentation(InstanceIdentifier<?> iid) {
        Optional<TerminationPoint> nodeOptional = getHwvtepTerminationPoint(iid);
        if (nodeOptional.isPresent()) {
            return Optional.fromNullable(nodeOptional.get().getAugmentation(HwvtepPhysicalLocatorAugmentation.class));
        }
        return Optional.absent();
    }

    public Optional<HwvtepPhysicalPortAugmentation> getPhysicalPortAugmentation(InstanceIdentifier<?> iid) {
        Optional<TerminationPoint> tpOptional = getHwvtepTerminationPoint(iid);
        if (tpOptional.isPresent()) {
            return Optional.fromNullable(tpOptional.get().getAugmentation(HwvtepPhysicalPortAugmentation.class));
        }
        return Optional.absent();
    }
}
