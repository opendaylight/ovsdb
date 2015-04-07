/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class DataChangesTerminationPointEvent implements AsyncDataChangeEvent<InstanceIdentifier<?>,
        OvsdbTerminationPointAugmentation> {
    private static final Logger LOG = LoggerFactory.getLogger(DataChangesTerminationPointEvent.class);

    private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event;
    private InstanceIdentifier<?> iid;
    private DataBroker db;

    // local caches of computed data
    private Map<InstanceIdentifier<?>, OvsdbTerminationPointAugmentation> createdData = null;
    private Map<InstanceIdentifier<?>, OvsdbTerminationPointAugmentation> updatedData = null;
    private Set<InstanceIdentifier<?>> removedPaths = null;
    private Map<InstanceIdentifier<?>, OvsdbTerminationPointAugmentation> originalData;

    public DataChangesTerminationPointEvent(DataBroker db,
                                            InstanceIdentifier<?> iid,
                                            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event) {
        this.db = db;
        this.iid = iid;
        this.event = event;
    }

    private Map<InstanceIdentifier<?>, OvsdbTerminationPointAugmentation> filter(Map<InstanceIdentifier<?>,
            DataObject> data) {
        Map<InstanceIdentifier<?>, OvsdbTerminationPointAugmentation> result
            = new HashMap<InstanceIdentifier<?>, OvsdbTerminationPointAugmentation>();
        ReadOnlyTransaction transaction = db.newReadOnlyTransaction();
        for (Entry<InstanceIdentifier<?>, DataObject> created: data.entrySet()) {
            if (created.getValue() != null
                    && created.getValue() instanceof OvsdbTerminationPointAugmentation) {
                OvsdbBridgeAugmentation tpBridgeAugmentation
                    = getBridgeOfTerminationPoint(transaction,created.getKey()).get();
                if (tpBridgeAugmentation.getManagedBy() != null
                        && tpBridgeAugmentation.getManagedBy().getValue() != null
                        && tpBridgeAugmentation.getManagedBy().getValue().equals(iid)) {
                    result.put(created.getKey(),((OvsdbTerminationPointAugmentation)created.getValue()));
                }
            }
        }
        transaction.close();
        return result;
    }

    @Override
    public Map<InstanceIdentifier<?>, OvsdbTerminationPointAugmentation> getCreatedData() {
        if (this.createdData == null) {
            this.createdData = filter(event.getCreatedData());
        }
        return this.createdData;
    }

    @Override
    public Map<InstanceIdentifier<?>, OvsdbTerminationPointAugmentation> getUpdatedData() {
        if (this.updatedData == null) {
            this.updatedData = filter(event.getUpdatedData());
        }
        return this.updatedData;
    }

    @Override
    public Set<InstanceIdentifier<?>> getRemovedPaths() {
        if (this.removedPaths != null) {
            this.removedPaths = new HashSet<InstanceIdentifier<?>>();
            for (InstanceIdentifier<?> path: event.getRemovedPaths()) {
                DataObject original = this.event.getOriginalData().get(path);
                if (original != null
                        && original instanceof OvsdbTerminationPointAugmentation) {
                    this.removedPaths.add(path);
                }
            }
        }
        return this.removedPaths;
    }

    @Override
    public Map<InstanceIdentifier<?>, OvsdbTerminationPointAugmentation> getOriginalData() {
        if (this.originalData == null) {
            this.originalData = filter(event.getOriginalData());
        }
        return this.originalData;
    }

    @Override
    public OvsdbTerminationPointAugmentation getOriginalSubtree() {
        if (this.event.getOriginalSubtree() instanceof OvsdbTerminationPointAugmentation) {
            return (OvsdbTerminationPointAugmentation)this.event.getOriginalSubtree();
        } else {
            return null;
        }
    }

    @Override
    public OvsdbTerminationPointAugmentation getUpdatedSubtree() {
        if (this.event.getUpdatedSubtree() instanceof OvsdbTerminationPointAugmentation) {
            return (OvsdbTerminationPointAugmentation)this.event.getUpdatedSubtree();
        } else {
            return null;
        }
    }

    private Optional<OvsdbBridgeAugmentation> getBridgeOfTerminationPoint(
            ReadOnlyTransaction tx,InstanceIdentifier<?> terminationPointPath) {

        Optional<OvsdbBridgeAugmentation> result = Optional.absent();

        InstanceIdentifier<Node> nodePath = terminationPointPath.firstIdentifierOf(Node.class);
        CheckedFuture<Optional<Node>, ReadFailedException> future
            = tx.read(LogicalDatastoreType.OPERATIONAL, nodePath);
        Optional<Node> optional;
        try {
            optional = future.get();
            if (optional.isPresent()) {
                OvsdbBridgeAugmentation bridge = optional.get().getAugmentation(OvsdbBridgeAugmentation.class);
                if (bridge != null && bridge.getBridgeUuid() != null) {
                    return Optional.of(bridge);
                }
            }
        } catch (InterruptedException e) {
            LOG.warn("Unable to retrieve bridge of termination poing from operational store",e);
        } catch (ExecutionException e) {
            LOG.warn("Unable to retrieve bridge of termination poing from operational store",e);
        }
        return result;
    }

}
