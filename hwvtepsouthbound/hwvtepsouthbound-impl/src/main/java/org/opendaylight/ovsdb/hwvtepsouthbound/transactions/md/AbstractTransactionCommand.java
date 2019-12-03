/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import java.util.Map;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepDeviceInfo;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.ConnectionInfo;
import org.opendaylight.yangtools.yang.binding.DataObject;

public abstract class AbstractTransactionCommand<T extends DataObject> implements TransactionCommand {
    private final TableUpdates updates;
    private final DatabaseSchema dbSchema;
    private final HwvtepConnectionInstance key;

    public TableUpdates getUpdates() {
        return updates;
    }

    public DatabaseSchema getDbSchema() {
        return dbSchema;
    }

    public ConnectionInfo getConnectionInfo() {
        return key.getMDConnectionInfo();
    }

    public HwvtepConnectionInstance getOvsdbConnectionInstance() {
        return key;
    }

    public AbstractTransactionCommand(final HwvtepConnectionInstance key,final TableUpdates updates,
            final DatabaseSchema dbSchema) {
        this.updates = updates;
        this.dbSchema = dbSchema;
        this.key = key;
    }

    public HwvtepDeviceInfo getDeviceInfo() {
        return key.getDeviceInfo();
    }

    void addToDeviceUpdate(final TransactionType transactionType, final Object element) {
        key.getDeviceInfo().addToDeviceUpdate(transactionType, element);
    }

    final <R> Map<UUID, R> extractRowsOld(final Class<R> klazz) {
        return dbSchema.extractRowsOld(klazz, updates);
    }

    final <R> Map<UUID, R> extractRowsRemoved(final Class<R> klazz) {
        return dbSchema.extractRowsRemoved(klazz, updates);
    }

    final <R> Map<UUID, R> extractRowsUpdated(final Class<R> klazz) {
        return dbSchema.extractRowsUpdated(klazz, updates);
    }
}
