/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepDeviceInfo;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.ConnectionInfo;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTransactionCommand<T extends DataObject> implements TransactionCommand {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractTransactionCommand.class);
    private final TableUpdates updates;
    private final DatabaseSchema dbSchema;
    protected final HwvtepConnectionInstance key;
    protected Set<Pair<Class<? extends Identifiable>, InstanceIdentifier>> addedKeys = new HashSet<>();
    protected Set<Pair<Class<? extends Identifiable>, InstanceIdentifier>> deletedKeys = new HashSet<>();
    protected HwvtepDeviceInfo deviceInfo;


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

    public AbstractTransactionCommand(HwvtepConnectionInstance key,TableUpdates updates, DatabaseSchema dbSchema) {
        this.updates = updates;
        this.dbSchema = dbSchema;
        this.key = key;
        if (key != null) {
            this.deviceInfo = key.getDeviceInfo();
        }
    }

    public HwvtepDeviceInfo getDeviceInfo() {
        return key.getDeviceInfo();
    }

    void addToDeviceUpdate(TransactionType transactionType, Object element) {
        deviceInfo.addToDeviceUpdate(transactionType, element);
    }

    public void clearDeviceOpUUID(Class<? extends Identifiable> cls, InstanceIdentifier iid, UUID uuid) {
        deviceInfo.clearDeviceOperUUID(cls, iid, uuid);
    }

    public void addToDeleteTx(ReadWriteTransaction tx, Class<? extends Identifiable> cls, InstanceIdentifier iid,
                              UUID uuid) {
        if (deviceInfo.isAvailableInOperDs(cls, iid)) {
            tx.delete(LogicalDatastoreType.OPERATIONAL, iid);
        }
        deletedKeys.add(Pair.of(cls, iid));
        clearDeviceOpUUID(cls, iid, uuid);
    }

    public void addToUpdateTx(Class<? extends Identifiable> cls, InstanceIdentifier iid, UUID uuid,
                              Object southboundData) {
        addedKeys.add(Pair.of(cls, iid));
        deviceInfo.updateDeviceOperData(cls, iid, uuid, southboundData);
    }

    public void onSuccess() {
        addedKeys.stream().forEach(pair -> {
            deviceInfo.markAvailableInOperDs(pair.getLeft(), pair.getRight());
        });
        deletedKeys.stream().forEach(pair -> {
            deviceInfo.clearOperDsAvailability(pair.getLeft(), pair.getRight());
            deviceInfo.clearDeviceOperData(pair.getLeft(), pair.getRight());
        });
    }

    public void onFailure() {
        addedKeys.stream().forEach(pair -> {
            LOG.error("Failed to add {}", pair.getLeft().getSimpleName());
        });
        deletedKeys.stream().forEach(pair -> {
            LOG.error("Failed to delete {}", pair.getLeft().getSimpleName());
        });
    }
}
