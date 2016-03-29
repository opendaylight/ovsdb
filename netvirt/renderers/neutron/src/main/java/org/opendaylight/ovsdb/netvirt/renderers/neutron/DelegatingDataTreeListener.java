/*
 * Copyright (c) 2013, 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.netvirt.renderers.neutron;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data-tree listener which delegates data processing to a {@link INeutronDataProcessor}.
 */
public class DelegatingDataTreeListener<T extends DataObject> implements AutoCloseable, ClusteredDataTreeChangeListener<T> {
    private static final Logger LOG = LoggerFactory.getLogger(DelegatingDataTreeListener.class);
    protected NeutronProvider provider;
    protected DataBroker db;
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);
    private final INeutronDataProcessor<T> dataProcessor;
    private ListenerRegistration<DelegatingDataTreeListener<T>> listenerRegistration;

    public DelegatingDataTreeListener(NeutronProvider provider, INeutronDataProcessor<T> dataProcessor,
                                      DataBroker db, DataTreeIdentifier<T> treeId) {
        this.provider = Preconditions.checkNotNull(provider, "provider can not be null!");
        this.dataProcessor = Preconditions.checkNotNull(dataProcessor, "Data processor can not be null!");
        registerListener(Preconditions.checkNotNull(db, "Data broker can not be null!"),
                Preconditions.checkNotNull(treeId, "Tree identifier can not be null!"));
    }

    private void registerListener(final DataBroker db, DataTreeIdentifier<T> treeId) {
        try {
            LOG.info("Registering Data Change Listener for {}", getClass().getSimpleName());
            this.db = db;
            listenerRegistration = db.registerDataTreeChangeListener(treeId, this);
        } catch (final Exception e) {
            LOG.warn("{} DataChange listener registration fail!", getClass().getSimpleName(), e);
            throw new IllegalStateException("DataTreeListener startup fail! System needs restart.", e);
        }
    }

    private void processChanges(Collection<DataTreeModification<T>> changes) {
        LOG.info("onDataTreeChanged: Received Data Tree Changed ...", changes);
        for (DataTreeModification<T> change : changes) {
            final InstanceIdentifier<T> key = change.getRootPath().getRootIdentifier();
            final DataObjectModification<T> mod = change.getRootNode();
            LOG.info("onDataTreeChanged: Received Data Tree Changed Update of Type={} for Key={}",
                    mod.getModificationType(), key);
            switch (mod.getModificationType()) {
                case DELETE:
                    dataProcessor.remove(key, mod.getDataBefore());
                    break;
                case SUBTREE_MODIFIED:
                    dataProcessor.update(key, mod.getDataBefore(), mod.getDataAfter());
                    break;
                case WRITE:
                    if (mod.getDataBefore() == null) {
                        dataProcessor.add(key, mod.getDataAfter());
                    } else {
                        dataProcessor.update(key, mod.getDataBefore(), mod.getDataAfter());
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled modification type " + mod.getModificationType());
            }
        }
    }

    @Override
    public void onDataTreeChanged(@Nonnull final Collection<DataTreeModification<T>> changes) {
        Preconditions.checkNotNull(changes, "Changes may not be null!");
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                processChanges(changes);
            }
        });
    }

    @Override
    public void close() {
        if (listenerRegistration != null) {
            listenerRegistration.close();
            listenerRegistration = null;
        }
    }
}
