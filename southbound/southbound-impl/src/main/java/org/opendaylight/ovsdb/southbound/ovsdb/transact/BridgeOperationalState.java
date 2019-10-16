/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import com.google.common.base.Optional;
import java.util.Collection;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
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
    private DataBroker db;

    public BridgeOperationalState(DataBroker db, DataChangeEvent changes) {
        this.db = db;
    }

    public BridgeOperationalState(DataBroker db, Collection<DataTreeModification<Node>> changes) {
        this.db = db;
    }

    @SuppressWarnings("IllegalCatch")
    public Optional<Node> getBridgeNode(InstanceIdentifier<?> iid) {
        InstanceIdentifier<Node> nodeIid = iid.firstIdentifierOf(Node.class);
        Optional<Node> bridgeNode = Optional.absent();
        try (ReadOnlyTransaction transaction = db.newReadOnlyTransaction()) {
            bridgeNode = SouthboundUtil.readNode(transaction, nodeIid);
            transaction.close();
        } catch (Exception exp) {
            LOG.error("Error in getting the brideNode for {}", iid, exp);
        }
        return bridgeNode;
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
