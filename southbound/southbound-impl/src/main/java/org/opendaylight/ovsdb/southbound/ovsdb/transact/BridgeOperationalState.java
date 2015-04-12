package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
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
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class BridgeOperationalState {
    private static final Logger LOG = LoggerFactory.getLogger(BridgeOperationalState.class);
    private DataBroker db;
    private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes;
    private Map<InstanceIdentifier<Node>, Node> operationalNodes = new HashMap<InstanceIdentifier<Node>, Node>();

    public BridgeOperationalState(DataBroker db, AsyncDataChangeEvent<InstanceIdentifier<?>,
            DataObject> changes) {
        this.db = db;
        this.changes = changes;
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
                } catch (InterruptedException e) {
                    LOG.warn("Error reading from datastore",e);
                } catch (ExecutionException e) {
                    LOG.warn("Error reading from datastore",e);
                }
            }
        }
        transaction.close();
    }

    public Optional<Node> getBridgeNode(InstanceIdentifier<?> iid) {
        InstanceIdentifier<Node> nodeIid = iid.firstIdentifierOf(Node.class);
        if (operationalNodes.containsKey(nodeIid)) {
            return Optional.of(operationalNodes.get(nodeIid));
        }
        return Optional.absent();
    }

    public Optional<OvsdbBridgeAugmentation> getOvsdbBridgeAugmentation(InstanceIdentifier<?> iid) {
        Optional<Node> nodeOptional = getBridgeNode(iid);
        if (nodeOptional.isPresent() && nodeOptional.get().getAugmentation(OvsdbBridgeAugmentation.class) != null) {
            return Optional.of(nodeOptional.get().getAugmentation(OvsdbBridgeAugmentation.class));
        }
        return Optional.absent();
    }

    public Optional<TerminationPoint> getBrideTermiationPoint(InstanceIdentifier<?> iid) {
        Optional<Node> nodeOptional = getBridgeNode(iid);
        if (nodeOptional.isPresent()
                && nodeOptional.get().getTerminationPoint() != null
                && iid != null) {
            TerminationPointKey key = iid.firstKeyOf(TerminationPoint.class, TerminationPointKey.class);
            if (key != null) {
                for (TerminationPoint tp:nodeOptional.get().getTerminationPoint()) {
                    if (tp.getKey().equals(key)) {
                        return Optional.of(tp);
                    }
                }
            }
        }
        return Optional.absent();
    }

    public Optional<OvsdbTerminationPointAugmentation> getOvsdbTerminationPointAugmentation(InstanceIdentifier<?> iid) {
        Optional<TerminationPoint> tpOptional = getBrideTermiationPoint(iid);
        if (tpOptional.isPresent()
                && tpOptional.get().getAugmentation(OvsdbTerminationPointAugmentation.class) != null) {
            return Optional.of(tpOptional.get().getAugmentation(OvsdbTerminationPointAugmentation.class));
        }
        return Optional.absent();
    }

    public Optional<ControllerEntry> getControllerEntry(InstanceIdentifier<?> iid) {
        Optional<OvsdbBridgeAugmentation> ovsdbBridgeOptional = getOvsdbBridgeAugmentation(iid);
        if (ovsdbBridgeOptional.isPresent()
                && ovsdbBridgeOptional.get().getControllerEntry() != null
                && iid != null) {
            ControllerEntryKey key = iid.firstKeyOf(ControllerEntry.class, ControllerEntryKey.class);
            if (key != null) {
                for (ControllerEntry entry: ovsdbBridgeOptional.get().getControllerEntry()) {
                    if (entry.getKey().equals(key)) {
                        return Optional.of(entry);
                    }
                }
            }
        }
        return Optional.absent();
    }

    public Optional<ProtocolEntry> getProtocolEntry(InstanceIdentifier<ProtocolEntry> iid) {
        Optional<OvsdbBridgeAugmentation> ovsdbBridgeOptional = getOvsdbBridgeAugmentation(iid);
        if (ovsdbBridgeOptional.isPresent()
                && ovsdbBridgeOptional.get().getProtocolEntry() != null
                && iid != null) {
            ProtocolEntryKey key = iid.firstKeyOf(ProtocolEntry.class, ProtocolEntryKey.class);
            if (key != null) {
                for (ProtocolEntry entry: ovsdbBridgeOptional.get().getProtocolEntry()) {
                    if (entry.getKey().equals(key)) {
                        return Optional.of(entry);
                    }
                }
            }
        }
        return Optional.absent();
    }

}
