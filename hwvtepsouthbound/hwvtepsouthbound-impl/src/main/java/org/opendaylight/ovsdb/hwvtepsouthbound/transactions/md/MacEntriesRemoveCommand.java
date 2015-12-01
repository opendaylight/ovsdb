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
import org.opendaylight.ovsdb.schema.hardwarevtep.McastMacsLocal;
import org.opendaylight.ovsdb.schema.hardwarevtep.McastMacsRemote;
import org.opendaylight.ovsdb.schema.hardwarevtep.UcastMacsLocal;
import org.opendaylight.ovsdb.schema.hardwarevtep.UcastMacsRemote;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalMcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalUcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MacEntriesRemoveCommand extends AbstractTransactionCommand {

    private static final Logger LOG = LoggerFactory.getLogger(MacEntriesRemoveCommand.class);

    public MacEntriesRemoveCommand(HwvtepConnectionInstance key, TableUpdates updates,
            DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        removeUcastMacsLocal(transaction);
        removeUcastMacsRemote(transaction);
        removeMcastMacsLocal(transaction);
        removeMcastMacsRemote(transaction);
    }

    private void removeUcastMacsLocal(ReadWriteTransaction transaction) {
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

    private void removeUcastMacsRemote(ReadWriteTransaction transaction) {
        Collection<UcastMacsRemote> deletedUMRRows = TyperUtils.extractRowsRemoved(UcastMacsRemote.class, getUpdates(),getDbSchema()).values();
        if(deletedUMRRows!=null && !deletedUMRRows.isEmpty()){
               for (UcastMacsRemote lum : deletedUMRRows){
                InstanceIdentifier<RemoteUcastMacs> lumId = getOvsdbConnectionInstance().getInstanceIdentifier()
                           .augmentation(HwvtepGlobalAugmentation.class)
                           .child(RemoteUcastMacs.class, new RemoteUcastMacsKey(new MacAddress(lum.getMac())));
                transaction.delete(LogicalDatastoreType.OPERATIONAL, lumId);
               }
            }
    }

    private void removeMcastMacsLocal(ReadWriteTransaction transaction) {
        Collection<McastMacsLocal> deletedLMMRows = TyperUtils.extractRowsRemoved(McastMacsLocal.class, getUpdates(),getDbSchema()).values();
        if(deletedLMMRows!=null && !deletedLMMRows.isEmpty()){
           for (McastMacsLocal lmm : deletedLMMRows){
            InstanceIdentifier<LocalMcastMacs> lumId = getOvsdbConnectionInstance().getInstanceIdentifier()
                       .augmentation(HwvtepGlobalAugmentation.class)
                       .child(LocalMcastMacs.class, new LocalMcastMacsKey(new MacAddress(lmm.getMac())));
            transaction.delete(LogicalDatastoreType.OPERATIONAL, lumId);
           }
        }
    }

    private void removeMcastMacsRemote(ReadWriteTransaction transaction) {
        Collection<McastMacsRemote> deletedMMRRows = TyperUtils.extractRowsRemoved(McastMacsRemote.class, getUpdates(),getDbSchema()).values();
        if(deletedMMRRows!=null && !deletedMMRRows.isEmpty()){
               for (McastMacsRemote lum : deletedMMRRows){
                InstanceIdentifier<RemoteMcastMacs> lumId = getOvsdbConnectionInstance().getInstanceIdentifier()
                           .augmentation(HwvtepGlobalAugmentation.class)
                           .child(RemoteMcastMacs.class, new RemoteMcastMacsKey(new MacAddress(lum.getMac())));
                transaction.delete(LogicalDatastoreType.OPERATIONAL, lumId);
               }
            }
    }

}
