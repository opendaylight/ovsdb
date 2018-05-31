/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntryKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BridgeOperationalState {
    private static final Logger LOG = LoggerFactory.getLogger(BridgeOperationalState.class);
    private final Map<InstanceIdentifier<Node>, Node> operationalNodes = new HashMap<>();

    public BridgeOperationalState(DataBroker db, DataChangeEvent changes) {
        try (ReadOnlyTransaction transaction = db.newReadOnlyTransaction()) {
            Map<InstanceIdentifier<Node>, Node> nodeCreateOrUpdate =
                    TransactUtils.extractCreatedOrUpdatedOrRemoved(changes, Node.class);
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
    }

    public BridgeOperationalState(DataBroker db, Collection<DataTreeModification<Node>> changes) {
        try (ReadOnlyTransaction transaction = db.newReadOnlyTransaction()) {
            Map<InstanceIdentifier<Node>, Node> nodeCreateOrUpdateOrRemove =
                    TransactUtils.extractCreatedOrUpdatedOrRemoved(changes, Node.class);
            for (Entry<InstanceIdentifier<Node>, Node> entry : nodeCreateOrUpdateOrRemove.entrySet()) {
                try {
                    Optional<Node> nodeOptional =
                            transaction.read(LogicalDatastoreType.OPERATIONAL, entry.getKey()).checkedGet();
                    if (nodeOptional.isPresent()) {
                        operationalNodes.put(entry.getKey(), nodeOptional.get());
                    }
                } catch (ReadFailedException e) {
                    LOG.warn("Error reading from datastore", e);
                }
            }
        }
    }

    public Optional<Node> getBridgeNode(InstanceIdentifier<?> iid) {
        InstanceIdentifier<Node> nodeIid = iid.firstIdentifierOf(Node.class);
        return Optional.fromNullable(operationalNodes.get(nodeIid));
    }

    public Optional<OvsdbBridgeAugmentation> getOvsdbBridgeAugmentation(InstanceIdentifier<?> iid) {
        Optional<Node> nodeOptional = getBridgeNode(iid);
        if (nodeOptional.isPresent()) {
            return Optional.fromNullable(nodeOptional.get().augmentation(OvsdbBridgeAugmentation.class));
        }
        return Optional.absent();
    }

    public Optional<TerminationPoint> getBridgeTerminationPoint(InstanceIdentifier<?> iid) {
        if (iid != null) {
            Optional<Node> nodeOptional = getBridgeNode(iid);
            if (nodeOptional.isPresent() && nodeOptional.get().getTerminationPoint() != null) {
                TerminationPointKey key = iid.firstKeyOf(TerminationPoint.class);
                if (key != null) {
                    for (TerminationPoint tp:nodeOptional.get().getTerminationPoint()) {
                        if (tp.key().equals(key)) {
                            return Optional.of(tp);
                        }
                    }
                }
            } else {
                LOG.debug("TerminationPoints or Operational BridgeNode missing for {}", iid);
            }
        }
        return Optional.absent();
    }

    public Optional<OvsdbTerminationPointAugmentation> getOvsdbTerminationPointAugmentation(InstanceIdentifier<?> iid) {
        Optional<TerminationPoint> tpOptional = getBridgeTerminationPoint(iid);
        if (tpOptional.isPresent()) {
            return Optional.fromNullable(tpOptional.get().augmentation(OvsdbTerminationPointAugmentation.class));
        }
        return Optional.absent();
    }

    public Optional<ControllerEntry> getControllerEntry(InstanceIdentifier<?> iid) {
        if (iid != null) {
            Optional<OvsdbBridgeAugmentation> ovsdbBridgeOptional = getOvsdbBridgeAugmentation(iid);
            if (ovsdbBridgeOptional.isPresent() && ovsdbBridgeOptional.get().getControllerEntry() != null) {
                ControllerEntryKey key = iid.firstKeyOf(ControllerEntry.class);
                if (key != null) {
                    for (ControllerEntry entry: ovsdbBridgeOptional.get().getControllerEntry()) {
                        if (entry.key().equals(key)) {
                            return Optional.of(entry);
                        }
                    }
                }
            }
        }
        return Optional.absent();
    }

    public Optional<ProtocolEntry> getProtocolEntry(InstanceIdentifier<ProtocolEntry> iid) {
        if (iid != null) {
            Optional<OvsdbBridgeAugmentation> ovsdbBridgeOptional = getOvsdbBridgeAugmentation(iid);
            if (ovsdbBridgeOptional.isPresent() && ovsdbBridgeOptional.get().getProtocolEntry() != null) {
                ProtocolEntryKey key = iid.firstKeyOf(ProtocolEntry.class);
                if (key != null) {
                    for (ProtocolEntry entry: ovsdbBridgeOptional.get().getProtocolEntry()) {
                        if (entry.key().equals(key)) {
                            return Optional.of(entry);
                        }
                    }
                }
            }
        }
        return Optional.absent();
    }

}
