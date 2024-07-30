/*
 * Copyright (c) 2015, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import java.util.Collection;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;
import org.opendaylight.ovsdb.schema.hardwarevtep.McastMacsLocal;
import org.opendaylight.ovsdb.schema.hardwarevtep.McastMacsRemote;
import org.opendaylight.ovsdb.schema.hardwarevtep.UcastMacsLocal;
import org.opendaylight.ovsdb.schema.hardwarevtep.UcastMacsRemote;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepLogicalSwitchRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalMcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalUcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HwvtepMacEntriesRemoveCommand extends AbstractTransactionCommand {

    private static final Logger LOG = LoggerFactory.getLogger(HwvtepMacEntriesRemoveCommand.class);

    public HwvtepMacEntriesRemoveCommand(HwvtepConnectionInstance key, TableUpdates updates, DatabaseSchema dbSchema) {
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
        Collection<UcastMacsLocal> deletedLUMRows =
                TyperUtils.extractRowsRemoved(UcastMacsLocal.class, getUpdates(), getDbSchema()).values();
        for (UcastMacsLocal lum : deletedLUMRows) {
            if (lum.getMac() != null && lum.getLogicalSwitchColumn() != null
                    && lum.getLogicalSwitchColumn().getData() != null) {
                LOG.info("DEVICE - {} LocalUcastMacs for Node {} - {}", TransactionType.DELETE,
                    getOvsdbConnectionInstance().getInstanceIdentifier().firstKeyOf(Node.class)
                        .getNodeId().getValue(), lum.getMac());
                InstanceIdentifier<LocalUcastMacs> lumId = getOvsdbConnectionInstance().getInstanceIdentifier()
                    .augmentation(HwvtepGlobalAugmentation.class).child(LocalUcastMacs.class,
                                    new LocalUcastMacsKey(getLogicalSwitchRef(lum.getLogicalSwitchColumn().getData()),
                                                    getMacAddress(lum.getMac())));
                addToDeleteTx(transaction, LocalUcastMacs.class, lumId, lum.getUuid());
            } else {
                LOG.debug("Failed to delete UcastMacLocal entry {}", lum.getUuid());
            }
        }
    }

    private void removeUcastMacsRemote(ReadWriteTransaction transaction) {
        Collection<UcastMacsRemote> deletedUMRRows =
                TyperUtils.extractRowsRemoved(UcastMacsRemote.class, getUpdates(), getDbSchema()).values();
        for (UcastMacsRemote rum : deletedUMRRows) {
            if (rum.getMac() != null && rum.getLogicalSwitchColumn() != null
                    && rum.getLogicalSwitchColumn().getData() != null) {
                InstanceIdentifier<RemoteUcastMacs> rumId = getOvsdbConnectionInstance().getInstanceIdentifier()
                    .augmentation(HwvtepGlobalAugmentation.class).child(RemoteUcastMacs.class,
                                    new RemoteUcastMacsKey(getLogicalSwitchRef(rum.getLogicalSwitchColumn().getData()),
                                                    getMacAddress(rum.getMac())));
                addToDeleteTx(transaction, RemoteUcastMacs.class, rumId, rum.getUuid());
            } else {
                LOG.debug("Failed to delete UcastMacRemote entry {}", rum.getUuid());
            }
        }
    }

    private void removeMcastMacsLocal(ReadWriteTransaction transaction) {
        Collection<McastMacsLocal> deletedLMMRows =
                TyperUtils.extractRowsRemoved(McastMacsLocal.class, getUpdates(), getDbSchema()).values();
        for (McastMacsLocal lmm : deletedLMMRows) {
            if (lmm.getMac() != null && lmm.getLogicalSwitchColumn() != null
                    && lmm.getLogicalSwitchColumn().getData() != null) {
                InstanceIdentifier<LocalMcastMacs> lumId = getOvsdbConnectionInstance().getInstanceIdentifier()
                    .augmentation(HwvtepGlobalAugmentation.class)
                    .child(LocalMcastMacs.class,
                                    new LocalMcastMacsKey(getLogicalSwitchRef(lmm.getLogicalSwitchColumn().getData()),
                                                    getMacAddress(lmm.getMac())));
                addToDeleteTx(transaction, LocalMcastMacs.class, lumId, lmm.getUuid());
            } else {
                LOG.debug("Failed to delete McastMacLocal entry {}", lmm.getUuid());
            }
        }
    }

    private void removeMcastMacsRemote(ReadWriteTransaction transaction) {
        Collection<McastMacsRemote> deletedMMRRows =
                TyperUtils.extractRowsRemoved(McastMacsRemote.class, getUpdates(), getDbSchema()).values();
        for (McastMacsRemote rmm : deletedMMRRows) {
            if (rmm.getMac() != null && rmm.getLogicalSwitchColumn() != null
                    && rmm.getLogicalSwitchColumn().getData() != null) {
                InstanceIdentifier<RemoteMcastMacs> lumId = getOvsdbConnectionInstance().getInstanceIdentifier()
                    .augmentation(HwvtepGlobalAugmentation.class)
                    .child(RemoteMcastMacs.class,
                                    new RemoteMcastMacsKey(getLogicalSwitchRef(rmm.getLogicalSwitchColumn().getData()),
                                                    getMacAddress(rmm.getMac())));
                addToDeleteTx(transaction, RemoteMcastMacs.class, lumId, rmm.getUuid());
                getOvsdbConnectionInstance().getDeviceInfo().clearDeviceOperData(RemoteMcastMacs.class, lumId);
            } else {
                LOG.debug("Failed to delete McastMacRemote entry {}", rmm.getUuid());
            }
        }
    }

    private HwvtepLogicalSwitchRef getLogicalSwitchRef(UUID switchUUID) {
        LogicalSwitch logicalSwitch = getOvsdbConnectionInstance().getDeviceInfo().getLogicalSwitch(switchUUID);
        if (logicalSwitch != null) {
            InstanceIdentifier<LogicalSwitches> switchIid =
                    HwvtepSouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(), logicalSwitch);
            return new HwvtepLogicalSwitchRef(switchIid.toIdentifier());
        }
        LOG.debug("Failed to get LogicalSwitch {}", switchUUID);
        LOG.trace("Available LogicalSwitches: {}",
                        getOvsdbConnectionInstance().getDeviceInfo().getLogicalSwitches().values());
        return null;
    }

    private static MacAddress getMacAddress(String mac) {
        if (mac.equals(HwvtepSouthboundConstants.UNKNOWN_DST_STRING)) {
            return HwvtepSouthboundConstants.UNKNOWN_DST_MAC;
        } else {
            return new MacAddress(mac);
        }
    }
}
