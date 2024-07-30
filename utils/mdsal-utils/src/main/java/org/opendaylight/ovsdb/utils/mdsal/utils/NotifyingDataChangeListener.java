/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.utils.mdsal.utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides methods for checking or waiting for various md-sal operations to complete.
 * Once an instance is created one must invoke the registerDataChangeListener method
 * with a DataBroker.
 */
public final class NotifyingDataChangeListener implements AutoCloseable, DataTreeChangeListener<DataObject> {
    private static final Logger LOG = LoggerFactory.getLogger(NotifyingDataChangeListener.class);
    private static final int RETRY_WAIT = 100;
    private static final int MDSAL_TIMEOUT_OPERATIONAL = 10000;
    private static final int MDSAL_TIMEOUT_CONFIG = 1000;

    public static final int BIT_CREATE = 1;
    public static final int BIT_UPDATE = 2;
    public static final int BIT_DELETE = 4;
    public static final int BIT_ALL = 7;

    private final Set<InstanceIdentifier<?>> createdIids = ConcurrentHashMap.newKeySet();
    private final Set<InstanceIdentifier<?>> removedIids = ConcurrentHashMap.newKeySet();
    private final Set<InstanceIdentifier<?>> updatedIids = ConcurrentHashMap.newKeySet();
    private final List<NotifyingDataChangeListener> waitList;
    private Registration listenerRegistration;
    private int mdsalTimeout = MDSAL_TIMEOUT_OPERATIONAL;
    private volatile InstanceIdentifier<?> iid;
    private volatile LogicalDatastoreType type;
    private volatile boolean listen;
    private volatile int mask;

    public NotifyingDataChangeListener(LogicalDatastoreType type, int mask,
                                       InstanceIdentifier<?> iid, List<NotifyingDataChangeListener> waitList) {
        this(type, iid, waitList);
        this.mask = mask;
    }

    /**
     * Create a new NotifyingDataChangeListener.
     *
     * @param type DataStore type
     * @param iid of the md-sal object we're waiting for
     * @param waitList for tracking outstanding changes
     */
    public NotifyingDataChangeListener(LogicalDatastoreType type,
                                        InstanceIdentifier<?> iid, List<NotifyingDataChangeListener> waitList) {
        this.type = type;
        this.iid = iid;
        this.waitList = waitList;
        if (this.waitList != null) {
            this.waitList.add(this);
        }

        mdsalTimeout = MDSAL_TIMEOUT_OPERATIONAL;
        if (type == LogicalDatastoreType.CONFIGURATION) {
            mdsalTimeout = MDSAL_TIMEOUT_CONFIG;
        }
        listen = true;
        mask = BIT_ALL;
    }

    /**
     * Completely reset the state of this NotifyingDataChangeListener.
     *
     * @param newType DataStore type
     * @param newIid of the md-sal object we're waiting for
     */
    public void modify(LogicalDatastoreType newType, InstanceIdentifier<?> newIid) {
        this.close();
        this.clear();
        this.type = newType;
        this.iid = newIid;
    }

    public void setlisten(boolean value) {
        this.listen = value;
    }

    public void setMask(int mask) {
        this.mask = mask;
    }

    @SuppressFBWarnings("NN_NAKED_NOTIFY")
    @Override
    public void onDataTreeChanged(List<DataTreeModification<DataObject>> changes) {
        if (!listen) {
            return;
        }

        for (DataTreeModification<DataObject> change: changes) {
            DataObjectModification<DataObject> rootNode = change.getRootNode();
            final InstanceIdentifier<DataObject> identifier = change.getRootPath().path();
            switch (rootNode.modificationType()) {
                case SUBTREE_MODIFIED:
                case WRITE:
                    if (rootNode.dataBefore() == null) {
                        if ((mask & BIT_CREATE) == BIT_CREATE) {
                            LOG.info("{} DataTreeChanged: created {}", type, identifier);
                            createdIids.add(identifier);
                        }
                    } else if ((mask & BIT_UPDATE) == BIT_UPDATE) {
                        LOG.info("{} DataTreeChanged: updated {}", type, identifier);
                        updatedIids.add(identifier);
                    }
                    break;
                case DELETE:
                    if ((mask & BIT_DELETE) == BIT_DELETE) {
                        LOG.info("{} DataTreeChanged: removed {}", type, identifier);
                        removedIids.add(identifier);
                    }
                    break;
                default:
                    break;
            }
        }

        synchronized (this) {
            notifyAll();
        }
    }

    public boolean isCreated(InstanceIdentifier<?> path) {
        return createdIids.remove(path);
    }

    public boolean isUpdated(InstanceIdentifier<?> path) {
        return updatedIids.remove(path);
    }

    public boolean isRemoved(InstanceIdentifier<?> path) {
        return removedIids.remove(path);
    }

    public void clear() {
        createdIids.clear();
        updatedIids.clear();
        removedIids.clear();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void registerDataChangeListener(DataBroker dataBroker) {
        listenerRegistration = dataBroker.registerDataTreeChangeListener(DataTreeIdentifier.of(type,
                (InstanceIdentifier)iid), this);
    }

    public void waitForCreation() throws InterruptedException {
        waitForCreation(mdsalTimeout);
    }

    public void waitForCreation(long timeout) throws InterruptedException {
        synchronized (this) {
            long start = System.currentTimeMillis();
            LOG.info("Waiting for {} DataChanged creation on {}", type, iid);
            while (!isCreated(iid) && System.currentTimeMillis() - start < timeout) {
                wait(RETRY_WAIT);
            }
            LOG.info("Woke up, waited {}ms for creation of {}", System.currentTimeMillis() - start, iid);
        }
    }

    public void waitForUpdate() throws InterruptedException {
        waitForUpdate(mdsalTimeout);
    }

    public void waitForUpdate(long timeout) throws InterruptedException {
        synchronized (this) {
            long start = System.currentTimeMillis();
            LOG.info("Waiting for {} DataChanged update on {}", type, iid);
            while (!isUpdated(iid) && System.currentTimeMillis() - start < timeout) {
                wait(RETRY_WAIT);
            }
            LOG.info("Woke up, waited {}ms for update of {}", System.currentTimeMillis() - start, iid);
        }
    }

    public void waitForDeletion() throws InterruptedException {
        waitForDeletion(mdsalTimeout);
    }

    public void waitForDeletion(long timeout) throws InterruptedException {
        synchronized (this) {
            long start = System.currentTimeMillis();
            LOG.info("Waiting for {} DataChanged deletion on {}", type, iid);
            while (!isRemoved(iid) && System.currentTimeMillis() - start < timeout) {
                wait(RETRY_WAIT);
            }
            LOG.info("Woke up, waited {}ms for deletion of {}", System.currentTimeMillis() - start, iid);
        }
    }

    @Override
    public void close() {
        if (listenerRegistration != null) {
            listenerRegistration.close();
        }

        if (waitList != null) {
            waitList.remove(this);
        }

        listenerRegistration = null;
    }
}
