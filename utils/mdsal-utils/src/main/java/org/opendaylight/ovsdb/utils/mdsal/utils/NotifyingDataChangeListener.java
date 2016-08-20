/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.utils.mdsal.utils;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class provides methods for checking or waiting for various md-sal operations to complete.
 * Once an instance is created one must invoke the registerDataChangeListener method
 * with a DataBroker.
 */
public class NotifyingDataChangeListener implements AutoCloseable, DataChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(NotifyingDataChangeListener.class);
    private LogicalDatastoreType type;
    private final Set<InstanceIdentifier<?>> createdIids = new HashSet<>();
    private final Set<InstanceIdentifier<?>> removedIids = new HashSet<>();
    private final Set<InstanceIdentifier<?>> updatedIids = new HashSet<>();
    private InstanceIdentifier<?> iid;
    private final static int RETRY_WAIT = 100;
    private final static int MDSAL_TIMEOUT_OPERATIONAL = 10000;
    private final static int MDSAL_TIMEOUT_CONFIG = 1000;
    private ListenerRegistration<?> listenerRegistration;
    private List<NotifyingDataChangeListener> waitList = null;
    private int mdsalTimeout = MDSAL_TIMEOUT_OPERATIONAL;
    private Boolean listen;
    public static final int BIT_CREATE = 1;
    public static final int BIT_UPDATE = 2;
    public static final int BIT_DELETE = 4;
    public static final int BIT_ALL = 7;
    private int mask;

    public NotifyingDataChangeListener(LogicalDatastoreType type, int mask,
                                       InstanceIdentifier<?> iid, List<NotifyingDataChangeListener> waitList) {
        this(type, iid, waitList);
        this.mask = mask;
    }

    /**
     * Create a new NotifyingDataChangeListener
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
     * @param type DataStore type
     * @param iid of the md-sal object we're waiting for
     * @throws Exception
     */
    public void modify(LogicalDatastoreType type, InstanceIdentifier<?> iid) throws Exception {
        this.close();
        this.clear();
        this.type = type;
        this.iid = iid;
    }

    public void setlisten(Boolean listen) {
        this.listen = listen;
    }

    public void setMask(int mask) {
        this.mask = mask;
    }

    @Override
    public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> asyncDataChangeEvent) {
        if (!listen) {
            return;
        }
        if ((mask & BIT_CREATE) == BIT_CREATE) {
            LOG.info("{} DataChanged: created {}", type, asyncDataChangeEvent.getCreatedData().keySet());
            createdIids.addAll(asyncDataChangeEvent.getCreatedData().keySet());
        }
        if ((mask & BIT_UPDATE) == BIT_UPDATE) {
            LOG.info("{} DataChanged: updated {}", type, asyncDataChangeEvent.getUpdatedData().keySet());
            updatedIids.addAll(asyncDataChangeEvent.getUpdatedData().keySet());
        }
        if ((mask & BIT_DELETE) == BIT_DELETE) {
            LOG.info("{} DataChanged: removed {}", type, asyncDataChangeEvent.getRemovedPaths());
            removedIids.addAll(asyncDataChangeEvent.getRemovedPaths());
        }

        synchronized (this) {
            notifyAll();
        }
    }

    public boolean isCreated(InstanceIdentifier<?> iid) {
        return createdIids.remove(iid);
    }

    public boolean isUpdated(InstanceIdentifier<?> iid) {
        return updatedIids.remove(iid);
    }

    public boolean isRemoved(InstanceIdentifier<?> iid) {
        return removedIids.remove(iid);
    }

    public void clear() {
        createdIids.clear();
        updatedIids.clear();
        removedIids.clear();
    }

    public void registerDataChangeListener(DataBroker dataBroker) {
        listenerRegistration = dataBroker.registerDataChangeListener(type, iid, this,
                AsyncDataBroker.DataChangeScope.SUBTREE);
    }

    public void waitForCreation() throws InterruptedException {
        waitForCreation(mdsalTimeout);
    }

    public void waitForCreation(long timeout) throws InterruptedException {
        synchronized (this) {
            long _start = System.currentTimeMillis();
            LOG.info("Waiting for {} DataChanged creation on {}", type, iid);
            while (!isCreated(iid) && (System.currentTimeMillis() - _start) < timeout) {
                wait(RETRY_WAIT);
            }
            LOG.info("Woke up, waited {}ms for creation of {}", (System.currentTimeMillis() - _start), iid);
        }
    }

    public void waitForUpdate() throws InterruptedException {
        waitForUpdate(mdsalTimeout);
    }

    public void waitForUpdate(long timeout) throws InterruptedException {
        synchronized (this) {
            long _start = System.currentTimeMillis();
            LOG.info("Waiting for {} DataChanged update on {}", type, iid);
            while (!isUpdated(iid) && (System.currentTimeMillis() - _start) < timeout) {
                wait(RETRY_WAIT);
            }
            LOG.info("Woke up, waited {}ms for update of {}", (System.currentTimeMillis() - _start), iid);
        }
    }

    public void waitForDeletion() throws InterruptedException {
        waitForDeletion(mdsalTimeout);
    }

    public void waitForDeletion(long timeout) throws InterruptedException {
        synchronized (this) {
            long _start = System.currentTimeMillis();
            LOG.info("Waiting for {} DataChanged deletion on {}", type, iid);
            while (!isRemoved(iid) && (System.currentTimeMillis() - _start) < timeout) {
                wait(RETRY_WAIT);
            }
            LOG.info("Woke up, waited {}ms for deletion of {}", (System.currentTimeMillis() - _start), iid);
        }
    }

    @Override
    public void close() throws Exception {
        if (listenerRegistration != null) {
            try {
                listenerRegistration.close();
            } catch (final Exception ex) {
                LOG.warn("Failed to close registration {}, iid {}", listenerRegistration, iid, ex);
            }
        }
        if (waitList != null) {
            waitList.remove(this);
        }
        listenerRegistration = null;
    }
}
