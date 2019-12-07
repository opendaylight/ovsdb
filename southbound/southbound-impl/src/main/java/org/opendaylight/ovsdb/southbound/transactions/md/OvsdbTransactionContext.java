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
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.typed.TypedDatabaseSchema;
import org.opendaylight.ovsdb.lib.storage.mdsal.TransactionContext;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

// FIXME: revise visibility
public class OvsdbTransactionContext extends TransactionContext {
    // FIXME: this should be KeyedInstanceIdentifier<Node
    private final @NonNull InstanceIdentifier<Node> node;

    public OvsdbTransactionContext(final TypedDatabaseSchema dbSchema, final TableUpdates updates,
            final InstanceIdentifier<Node> node) {
        super(dbSchema, updates);
        this.node = requireNonNull(node);
    }

    public final @NonNull InstanceIdentifier<Node> getNode() {
        return node;
    }
}
