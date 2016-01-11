/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Class is a wrapper for MdsalUtils.java class. It wrap all the methods
 * from MdsalUtils and call it only when *this* instance is net-virt master
 * instances.
 *
 * Created by vishnoianil on 1/11/16.
 */

public class ClusterAwareMdsalUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterAwareMdsalUtils.class);
    private final MdsalUtils mdsalUtils;

    /**
     * Class constructor setting the MdsalUtils instance.
     *
     * @param dataBroker the {@link org.opendaylight.controller.md.sal.binding.api.DataBroker}
     */
    public ClusterAwareMdsalUtils(DataBroker dataBroker) {
        mdsalUtils = new MdsalUtils(dataBroker);
    }

    /**
     * Wrapper method to executes delete as a blocking transaction.
     *
     * @param store {@link LogicalDatastoreType} which should be modified
     * @param path {@link InstanceIdentifier} to read from
     * @param <D> the data object type
     * @return the result of the request
     */
    public <D extends org.opendaylight.yangtools.yang.binding.DataObject> boolean delete(
            final LogicalDatastoreType store, final InstanceIdentifier<D> path)  {
        if (NetvirtProvider.isMasterProviderInstance()) {
            return mdsalUtils.delete(store,path);
        }
        return true;
    }

    /**
     * Wrapper method to executes merge as a blocking transaction.
     *
     * @param logicalDatastoreType {@link LogicalDatastoreType} which should be modified
     * @param path {@link InstanceIdentifier} for path to read
     * @param data object of type D
     * @return the result of the request
     */
    public <D extends org.opendaylight.yangtools.yang.binding.DataObject> boolean merge(
            final LogicalDatastoreType logicalDatastoreType, final InstanceIdentifier<D> path, D data) {
        if (NetvirtProvider.isMasterProviderInstance()) {
            return mdsalUtils.merge(logicalDatastoreType,path, data);
        }
        return true;
    }

    /**
     * Wrapper method to executes put as a blocking transaction.
     *
     * @param logicalDatastoreType {@link LogicalDatastoreType} which should be modified
     * @param path {@link InstanceIdentifier} for path to read
     * @param data object of type D
     * @return the result of the request
     */
    public <D extends org.opendaylight.yangtools.yang.binding.DataObject> boolean put(
            final LogicalDatastoreType logicalDatastoreType, final InstanceIdentifier<D> path, D data) {
        if (NetvirtProvider.isMasterProviderInstance()) {
            return mdsalUtils.put(logicalDatastoreType,path, data);
        }
        return true;
    }

    /**
     * Wrapper method to executes read as a blocking transaction.
     * Read is open for all instances to execute their normal
     * control flow. Because with current implementation all the
     * net-virt instances execute in similar way, because they want
     * to build their local caches so that all the instances has same
     * state of internal cache.
     *
     * @param store {@link LogicalDatastoreType} to read
     * @param path {@link InstanceIdentifier} for path to read
     * @param <D> the data object type
     * @return the result as the data object requested
     */
    public <D extends org.opendaylight.yangtools.yang.binding.DataObject> D read(
            final LogicalDatastoreType store, final InstanceIdentifier<D> path)  {
        return mdsalUtils.read(store,path);
    }
}
