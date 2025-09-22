/*
 * Copyright © 2017 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalRouter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalRouters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalRoutersKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HwvtepLogicalRouterRemoveCommand extends AbstractTransactionCommand<LogicalRouters> {
    private static final Logger LOG = LoggerFactory.getLogger(HwvtepLogicalRouterRemoveCommand.class);

    public HwvtepLogicalRouterRemoveCommand(final HwvtepConnectionInstance key, final TableUpdates updates,
            final DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
    }

    @Override
    public void execute(final ReadWriteTransaction transaction) {
        final var deletedLRRows =
                TyperUtils.extractRowsRemoved(LogicalRouter.class, getUpdates(), getDbSchema()).values();
        if (deletedLRRows != null) {
            for (LogicalRouter router : deletedLRRows) {
                HwvtepNodeName routerNode = new HwvtepNodeName(router.getName());
                LOG.debug("Clearing device operational data for logical router {}", routerNode);
                final var routerIid = getOvsdbConnectionInstance().getInstanceIdentifier().toBuilder()
                        .augmentation(HwvtepGlobalAugmentation.class)
                        .child(LogicalRouters.class, new LogicalRoutersKey(routerNode))
                        .build();
                transaction.delete(LogicalDatastoreType.OPERATIONAL, routerIid);
                getOvsdbConnectionInstance().getDeviceInfo().clearDeviceOperData(LogicalRouters.class, routerIid);
            }
        }
    }
}
