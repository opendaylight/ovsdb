/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import org.opendaylight.ovsdb.lib.MonitorCallBack;
import org.opendaylight.ovsdb.lib.message.TableUpdate;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbMonitorCallback implements MonitorCallBack {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbMonitorCallback.class);
    @Override
    public void update(TableUpdates result, DatabaseSchema dbSchema) {
        LOG.debug("result: {} dbSchema: {}",result,dbSchema);
        GenericTableSchema bridge = dbSchema.table("Bridge", GenericTableSchema.class);
        ColumnSchema<GenericTableSchema, String> name = bridge.column("name", String.class);
        TableUpdate<GenericTableSchema> update = result.getUpdate(bridge);
        if (update != null) {
            for (UUID uuid : update.getRows().keySet()) {
                Row<GenericTableSchema> schemaRow = update.getNew(uuid);
                String bridgeName = schemaRow.getColumn(name).getData();
                LOG.trace("Bridge name: {}", bridgeName);
            }
        }
    }

    @Override
    public void exception(Throwable t) {
        LOG.warn("exception {}",t);
    }

}
