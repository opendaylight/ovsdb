/*
 * Copyright (c) 2020 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.utils.mdsal.utils;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class TransactionProxy implements ReadWriteTransaction {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionProxy.class);

    private ReadWriteTransaction readWriteDelegate;
    private BindingTransactionChain bindingTransactionChain;
    private TransactionProxyTracker transactionProxyTracker;
    private Object cmd;
    private List<TransactionHistory> txHistory = new ArrayList<>();
    private List<ActionableResource> transactions = new ArrayList<>();
    private Map<InstanceIdentifier, DataObject> txInFlight = new HashMap<>();

    public TransactionProxy(BindingTransactionChain chain, Object cmd,
                            TransactionProxyTracker transactionProxyTracker) {
        this.bindingTransactionChain = chain;
        this.cmd = cmd;
        this.transactionProxyTracker = transactionProxyTracker;
    }

    public List<TransactionHistory> getTxHistory() {
        return txHistory;
    }

    private ReadWriteTransaction getReadWriteDelegate() {
        if (readWriteDelegate == null) {
            readWriteDelegate = bindingTransactionChain.newReadWriteTransaction();
            transactionProxyTracker.putProxyFor(readWriteDelegate, this);
        }
        return readWriteDelegate;
    }

    enum TransactionType {
        ADD, UPDATE, DELETE;
    }

    public static class TransactionHistory<T> {
        InstanceIdentifier iid;
        TransactionType transactionType;

        public TransactionHistory(InstanceIdentifier iid, TransactionType transactionType) {
            this.iid = iid;
            this.transactionType = transactionType;
        }

        @Override
        public String toString() {
            return transactionType + ":" + iid;
        }
    }

    public ReadWriteTransaction getBackingDelegate() {
        return readWriteDelegate;
    }

    @Override
    public <T extends DataObject> CheckedFuture<Optional<T>, ReadFailedException> read(
            LogicalDatastoreType logicalDatastoreType, InstanceIdentifier<T> instanceIdentifier) {
        if (LogicalDatastoreType.CONFIGURATION.equals(logicalDatastoreType)) {
            return getReadWriteDelegate().read(logicalDatastoreType, instanceIdentifier);
        } else {
            if (txInFlight.containsKey(instanceIdentifier)) {
                return Futures.immediateCheckedFuture(Optional.of((T) txInFlight.get(instanceIdentifier)));
            }
            return OvsdbBatchingManager.getInstance().read(
                    OvsdbBatchingManager.ShardResource.OPERATIONAL_TOPOLOGY.name(), instanceIdentifier);
        }
    }

    public void put(InstanceIdentifier<?> identifier, DataObject updatedData) {
        transactions.add(new ActionableResourceImpl(
                identifier, ActionableResource.CREATE, updatedData, null/*oldData*/));
        txInFlight.put(identifier, updatedData);
    }

    @Override
    public <T extends DataObject> void put(LogicalDatastoreType logicalDatastoreType,
                                           InstanceIdentifier<T> instanceIdentifier, T upadatedData) {
        txHistory.add(new TransactionHistory(instanceIdentifier, TransactionType.ADD));
        if (LogicalDatastoreType.CONFIGURATION.equals(logicalDatastoreType)) {
            getReadWriteDelegate().put(logicalDatastoreType, instanceIdentifier, upadatedData);
        } else {
            put(instanceIdentifier, upadatedData);
        }
    }

    @Override
    public <T extends DataObject> void put(LogicalDatastoreType logicalDatastoreType,
                                           InstanceIdentifier<T> instanceIdentifier, T updatedData,
                                           boolean createMissingParents) {
        txHistory.add(new TransactionHistory(instanceIdentifier, TransactionType.ADD));
        if (LogicalDatastoreType.CONFIGURATION.equals(logicalDatastoreType)) {
            getReadWriteDelegate().put(logicalDatastoreType, instanceIdentifier, updatedData, createMissingParents);
        } else {
            put(instanceIdentifier, updatedData);
        }
    }

    public void merge(InstanceIdentifier<?> identifier, DataObject updatedData) {
        transactions.add(new ActionableResourceImpl(
                identifier, ActionableResource.UPDATE, updatedData, null/*oldData*/));
        txInFlight.put(identifier, updatedData);
    }

    @Override
    public <T extends DataObject> void merge(LogicalDatastoreType logicalDatastoreType,
                                             InstanceIdentifier<T> instanceIdentifier, T updatedData) {
        txHistory.add(new TransactionHistory(instanceIdentifier, TransactionType.UPDATE));
        if (LogicalDatastoreType.CONFIGURATION.equals(logicalDatastoreType)) {
            getReadWriteDelegate().merge(logicalDatastoreType, instanceIdentifier, updatedData);
        } else {
            merge(instanceIdentifier, updatedData);
        }
    }

    @Override
    public <T extends DataObject> void merge(LogicalDatastoreType logicalDatastoreType,
                                             InstanceIdentifier<T> instanceIdentifier,
                                             T updatedData, boolean createMissingParents) {
        txHistory.add(new TransactionHistory(instanceIdentifier, TransactionType.UPDATE));
        if (LogicalDatastoreType.CONFIGURATION.equals(logicalDatastoreType)) {
            getReadWriteDelegate().merge(logicalDatastoreType, instanceIdentifier, updatedData, createMissingParents);
        } else {
            merge(instanceIdentifier, updatedData);
        }
    }

    @Override
    public boolean cancel() {
        if (readWriteDelegate != null) {
            return readWriteDelegate.cancel();
        }
        return false;
    }

    public void delete(InstanceIdentifier<?> identifier) {
        transactions.add(new ActionableResourceImpl(
                identifier, ActionableResource.DELETE, null, null/*oldData*/));
    }

    @Override
    public void delete(LogicalDatastoreType logicalDatastoreType, InstanceIdentifier<?> instanceIdentifier) {
        txHistory.add(new TransactionHistory(instanceIdentifier, TransactionType.DELETE));
        delete(instanceIdentifier);
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> submit() {
        if (readWriteDelegate != null) {
            return readWriteDelegate.submit();
        }
        if (!transactions.isEmpty()) {
            ActionableResourceImpl actionableResource = new ActionableResourceImpl("OVSDB");
            actionableResource.setModifications(transactions);
            OvsdbBatchingManager.getInstance().addMdsalTask(
                    OvsdbBatchingManager.ShardResource.OPERATIONAL_TOPOLOGY, actionableResource);

        }
        //LOG.debug("No mdsal update performed for the event that could be suppressed {}", cmd);
        return Futures.immediateCheckedFuture(null);
    }

    @Override
    public FluentFuture<? extends CommitInfo> commit() {
        if (readWriteDelegate != null) {
            return readWriteDelegate.commit();
        }
        return FluentFuture.from(Futures.immediateFuture(CommitInfo.empty()));
    }

    @Override
    public Object getIdentifier() {
        if (readWriteDelegate != null) {
            return readWriteDelegate.getIdentifier();
        }
        return null;
    }
}