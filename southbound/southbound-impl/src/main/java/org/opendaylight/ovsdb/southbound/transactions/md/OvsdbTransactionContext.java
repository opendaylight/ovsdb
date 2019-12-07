/*
 * Copyright © 2019 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.typed.TypedDatabaseSchema;
import org.opendaylight.ovsdb.lib.storage.mdsal.TransactionContext;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

// FIXME: revise visibility
public class OvsdbTransactionContext extends TransactionContext {
    private final @NonNull ReadWriteTransaction transaction;
    private final OvsdbConnectionInstance connection;

    public OvsdbTransactionContext(final TypedDatabaseSchema dbSchema, final TableUpdates updates,
            final OvsdbConnectionInstance connection, final ReadWriteTransaction transaction) {
        super(dbSchema, updates);
        this.connection = requireNonNull(connection);
        this.transaction = requireNonNull(transaction);
    }

    // FIXME: this should be KeyedInstanceIdentifier<Node
    public final @NonNull InstanceIdentifier<Node> getNode() {
        return connection.getInstanceIdentifier();
    }

    public final ConnectionInfo getConnectionInfo() {
        return connection.getMDConnectionInfo();
    }

    public final @NonNull ReadWriteTransaction getTransaction() {
        return transaction;
    }


}
