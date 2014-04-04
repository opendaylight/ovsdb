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
import org.opendaylight.ovsdb.lib.notation.operations.Operation;
import org.opendaylight.ovsdb.lib.notation.operations.OperationResult;
import org.opendaylight.ovsdb.lib.notation.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;

import java.util.List;

/**
 * The main interface to interact with a device speaking OVSDB protocol in an
 * asynchronous fashion and hence most operations return a Future object representing
 * the eventual response data from the remote.
 */
public interface OvsDBClient {

    String OPEN_VSWITCH_SCHEMA = "Open_vSwitch";

    /**
     * Gets the list of database names exposed by this OVSDB capable device
     * @return list of database names
     */
    ListenableFuture<List<String>> getDatabases();

    /**
     * Asynchronosly returns the schema object for a specific database
     * @param database name of the database schema
     * @param cacheResult if the results be cached by this instance
     * @return DatabaseSchema future
     */
    ListenableFuture<DatabaseSchema> getSchema(String database, boolean cacheResult);

    /**
     * Allows for a mini DSL way of collecting the transactions to be executed against
     * the ovsdb instance.
     * @return TransactionBuilder
     */
    TransactionBuilder transactBuilder();

    /**
     * Execute the list of operations in a single Transactions. Similar to the
     * transactBuilder() method
     * @param operations List of operations that needs to be part of a transact call
     * @return Future object representing the result of the transaction.
     */
    ListenableFuture<List<OperationResult>> transact(List<Operation> operations);

}
