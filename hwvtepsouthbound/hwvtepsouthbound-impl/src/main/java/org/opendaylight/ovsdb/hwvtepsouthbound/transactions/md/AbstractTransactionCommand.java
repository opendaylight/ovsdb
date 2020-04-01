/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepDeviceInfo;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocator;
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
    private final HwvtepConnectionInstance key;
    private final AtomicInteger retryCount = new AtomicInteger(HwvtepSouthboundConstants.CHAIN_RETRY_COUNT);

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
    }

    public HwvtepDeviceInfo getDeviceInfo() {
        return key.getDeviceInfo();
    }

    void addToDeviceUpdate(TransactionType transactionType, Object element) {
        key.getDeviceInfo().addToDeviceUpdate(transactionType, element);
    }

    public InstanceIdentifier getDeviceOpDataKey(Class<? extends Identifiable> cls, UUID uuid) {
        return key.getDeviceInfo().getDeviceOperKey(cls, uuid);
    }

    public PhysicalLocator getLocator(Map<UUID, PhysicalLocator> updatePLRows, UUID plocuuid) {
        PhysicalLocator ploc = updatePLRows.get(plocuuid);
        if (ploc == null) {
            ploc = (PhysicalLocator) getOvsdbConnectionInstance()
                    .getDeviceInfo().getPhysicalLocator(plocuuid);
        }
        if (ploc == null) {
            LOG.error("Could not to find locator uuid {} {} from cache {}" , plocuuid);
            getOvsdbConnectionInstance().getHwvtepTableReader().refreshLocators();
            ploc = (PhysicalLocator) getOvsdbConnectionInstance()
                    .getDeviceInfo().getPhysicalLocator(plocuuid);
        }
        return ploc;
    }

    public int getTransactionChainRetryCount() {
        return retryCount.decrementAndGet();
    }

}
