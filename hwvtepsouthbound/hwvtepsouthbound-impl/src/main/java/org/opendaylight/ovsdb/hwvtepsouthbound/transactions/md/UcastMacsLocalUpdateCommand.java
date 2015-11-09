/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.UcastMacsLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UcastMacsLocalUpdateCommand extends AbstractTransactionCommand {

    private static final Logger LOG = LoggerFactory.getLogger(UcastMacsLocalUpdateCommand.class);
    private Map<UUID, UcastMacsLocal> updatedUMacsLocalRows;
    private Map<UUID, UcastMacsLocal> oldUMacsLocalRows;

    public UcastMacsLocalUpdateCommand(HwvtepConnectionInstance key, TableUpdates updates,
            DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedUMacsLocalRows = TyperUtils.extractRowsUpdated(UcastMacsLocal.class, getUpdates(),getDbSchema());
        oldUMacsLocalRows = TyperUtils.extractRowsOld(UcastMacsLocal.class, getUpdates(),getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        // TODO Auto-generated method stub
    }

}
