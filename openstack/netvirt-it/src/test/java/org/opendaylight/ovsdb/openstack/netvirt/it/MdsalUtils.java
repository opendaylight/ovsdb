/*
 * Copyright (c) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.it;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to wrap mdsal transactions.
 *
 * @author Sam Hague (shague@redhat.com)
 */
public class MdsalUtils {
    private static final Logger LOG = LoggerFactory.getLogger(MdsalUtils.class);
    private static DataBroker databroker = null;

    /**
     * Class constructor setting the data broker.
     *
     * @param dataBroker the {@link DataBroker}
     */
    public MdsalUtils(DataBroker dataBroker) {
        this.databroker = dataBroker;
    }

    /**
     * Executes read transaction as a test2.
     *
     * @param logicalDatastoreType {@link LogicalDatastoreType} from which read should occur
     * @param path {@link InstanceIdentifier} for path to read
     * @param <D> the data object type
     * @return the data object requested
     */
    public static <D extends org.opendaylight.yangtools.yang.binding.DataObject> D readTransaction(
            final LogicalDatastoreType logicalDatastoreType, final InstanceIdentifier<D> path)  {
        D ret = null;
        final ReadOnlyTransaction readTx = databroker.newReadOnlyTransaction();
        Optional<D> optionalDataObject = Optional.absent();
        CheckedFuture<Optional<D>, ReadFailedException> submitFuture = readTx.read(logicalDatastoreType, path);
        try {
            optionalDataObject = submitFuture.checkedGet();
            if (optionalDataObject.isPresent()) {
                ret = optionalDataObject.get();
            } else {
                LOG.debug("{}: Failed to read {}",
                        Thread.currentThread().getStackTrace()[1], path);
            }
        } catch (ReadFailedException e) {
            LOG.warn("Failed to read {} ", path, e);
        }
        readTx.close();
        return ret;
    }

}
