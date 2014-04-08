/*
 *
 *  * Copyright (C) 2014 EBay Software Foundation
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *  *
 *  * Authors : Ashwin Raveendran
 *
 */

package org.opendaylight.ovsdb.lib;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.ovsdb.lib.message.MonitorRequest;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;

import java.util.List;

/**
 * The main interface to interact with a device speaking ovsdb protocol in an asynchronous fashion and hence most
 * operations return a Future object representing the eventual response data from the remote.
 */
public interface OvsDBClient {

    /**
     * Represents the Open Vswitch Schema
     */
    String OPEN_VSWITCH_SCHEMA = "Open_vSwitch";

    /**
     * Gets the list of database names exposed by this ovsdb capable device
     * @return list of database names
     */
    ListenableFuture<List<String>> getDatabases();

    /**
     * Asynchronously returns the schema object for a specific database
     * @param database name of the database schema
     * @param cacheResult if the results be cached by this instance
     * @return DatabaseSchema future
     */
    ListenableFuture<DatabaseSchema> getSchema(String database, boolean cacheResult);

    /**
     * Allows for a mini DSL way of collecting the transactions to be executed against the ovsdb instance.
     * @return TransactionBuilder
     */
    TransactionBuilder transactBuilder();

    /**
     * Execute the list of operations in a single Transactions. Similar to the transactBuilder() method
     * @param operations List of operations that needs to be part of a transact call
     * @return Future object representing the result of the transaction. Calling
     * cancel on the Future would cause OVSDB cancel operation to be fired against
     * the device.
     */
    ListenableFuture<List<OperationResult>> transact(List<Operation> operations);


    /**
     * ovsdb <a href="http://tools.ietf.org/html/draft-pfaff-ovsdb-proto-04#section-4.1.5">monitor</a> operation.
     * @param monitorRequest represents what needs to be monitored including a client specified monitor handle. This
     *                       handle is used to later cancel ({@link #cancelMonitor(MonitorHandle)}) the monitor.
     * @param callback receives the monitor response
     */
    public void monitor(MonitorRequest monitorRequest, MonitorCallBack callback);

    /**
     * Cancels an existing monitor method.
     * @param handler Handle identifying a specific monitor request that is being cancelled.
     * @throws java.lang.IllegalStateException if there is no outstanding monitor request for this handle
     */
    public void cancelMonitor(MonitorHandle handler);

    /**
     * ovsdb <a href="http://tools.ietf.org/html/draft-pfaff-ovsdb-proto-04#section-4.1.8">lock</a> operation.
     * @param lockId a client specified id for the lock; this can be used for unlocking ({@link #unLock(String)})
     * @param lockedCallBack Callback to nofify when the lock is acquired
     * @param stolenCallback Callback to notify when an acquired lock is stolen by another client
     */
    public void lock(String lockId, LockAquisitionCallback lockedCallBack, LockStolenCallback stolenCallback);

    /**
     * ovsdb steal operation, see {@link #lock(String, LockAquisitionCallback, LockStolenCallback)}
     * @param lockId
     * @return
     */
    public ListenableFuture<Boolean> steal(String lockId);

    /**
     * ovsdb unlock operaiton, see {@link #unLock(String)}
     * @param lockId
     * @return
     */
    public ListenableFuture<Boolean> unLock(String lockId);

    /**
     * Starts the echo service. The {@code callbackFilters} can be used to get notified on the absence of echo
     * notifications from the remote device and control the frequency of such notifications.
     * @param callbackFilters callbacks for notifying the client of missing echo calls from remote.
     */
    public void startEchoService(EchoServiceCallbackFilters callbackFilters);

    /**
     * Stops the echo service, i.e echo requests from the remote would not be acknowledged after this call.
     */
    public void stopEchoService();

}
