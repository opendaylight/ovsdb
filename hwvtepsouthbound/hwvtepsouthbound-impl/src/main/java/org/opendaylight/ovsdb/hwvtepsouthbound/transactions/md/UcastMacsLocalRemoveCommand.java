/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import java.util.Collection;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.UcastMacsLocal;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalUcastMacsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UcastMacsLocalRemoveCommand extends AbstractTransactionCommand {

    private static final Logger LOG = LoggerFactory.getLogger(UcastMacsLocalRemoveCommand.class);

    public UcastMacsLocalRemoveCommand(HwvtepConnectionInstance key, TableUpdates updates,
            DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        Collection<UcastMacsLocal> deletedLUMRows = TyperUtils.extractRowsRemoved(UcastMacsLocal.class, getUpdates(),getDbSchema()).values();
        if(deletedLUMRows!=null && !deletedLUMRows.isEmpty()){
               for (UcastMacsLocal lum : deletedLUMRows){
                InstanceIdentifier<LocalUcastMacs> lumId = getOvsdbConnectionInstance().getInstanceIdentifier()
                           .augmentation(HwvtepGlobalAugmentation.class)
                           .child(LocalUcastMacs.class, new LocalUcastMacsKey(new MacAddress(lum.getMac())));
                transaction.delete(LogicalDatastoreType.OPERATIONAL, lumId);
               }
            }
    }

}
