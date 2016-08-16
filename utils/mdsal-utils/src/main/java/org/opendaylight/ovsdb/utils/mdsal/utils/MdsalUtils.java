/*
 * Copyright (c) 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.utils.mdsal.utils;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface MdsalUtils {

    /**
     * Executes delete as a blocking transaction.
     *
     * @param store {@link org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType} which should be modified
     * @param path {@link org.opendaylight.yangtools.yang.binding.InstanceIdentifier} to read from
     * @param <D> the data object type
     * @return the result of the request
     */
    public <D extends org.opendaylight.yangtools.yang.binding.DataObject> boolean delete(
            final LogicalDatastoreType store, final InstanceIdentifier<D> path);

    /**
     * Executes merge as a blocking transaction.
     *
     * @param logicalDatastoreType {@link org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType} which should be modified
     * @param path {@link org.opendaylight.yangtools.yang.binding.InstanceIdentifier} for path to read
     * @param <D> the data object type
     * @return the result of the request
     */
    public <D extends org.opendaylight.yangtools.yang.binding.DataObject> boolean merge(
            final LogicalDatastoreType logicalDatastoreType, final InstanceIdentifier<D> path, D data);
    /**
     * Executes put as a blocking transaction.
     *
     * @param logicalDatastoreType {@link org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType} which should be modified
     * @param path {@link org.opendaylight.yangtools.yang.binding.InstanceIdentifier} for path to read
     * @param <D> the data object type
     * @return the result of the request
     */
    public <D extends org.opendaylight.yangtools.yang.binding.DataObject> boolean put(
            final LogicalDatastoreType logicalDatastoreType, final InstanceIdentifier<D> path, D data);
    /**
     * Executes read as a blocking transaction.
     *
     * @param store {@link org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType} to read
     * @param path {@link org.opendaylight.yangtools.yang.binding.InstanceIdentifier} for path to read
     * @param <D> the data object type
     * @return the result as the data object requested
     */
    public <D extends org.opendaylight.yangtools.yang.binding.DataObject> D read(
            final LogicalDatastoreType store, final InstanceIdentifier<D> path);
}
