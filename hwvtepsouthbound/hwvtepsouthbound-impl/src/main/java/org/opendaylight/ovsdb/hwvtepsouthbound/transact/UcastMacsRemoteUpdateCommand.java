/*
 * Copyright (c) 2015, 2017 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepDeviceInfo;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.UcastMacsRemote;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UcastMacsRemoteUpdateCommand extends AbstractTransactCommand<RemoteUcastMacs, HwvtepGlobalAugmentation> {
    private static final Logger LOG = LoggerFactory.getLogger(UcastMacsRemoteUpdateCommand.class);
    private static final UcastMacUnMetDependencyGetter UCAST_MAC_DATA_VALIDATOR = new UcastMacUnMetDependencyGetter();

    public UcastMacsRemoteUpdateCommand(HwvtepOperationalState state,
            Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, List<RemoteUcastMacs>> updateds =
                extractUpdated(getChanges(),RemoteUcastMacs.class);
        if (!updateds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, List<RemoteUcastMacs>> updated:
                updateds.entrySet()) {
                updateUcastMacsRemote(transaction,  updated.getKey(), updated.getValue());
            }
        }
    }

    private void updateUcastMacsRemote(TransactionBuilder transaction,
                                       InstanceIdentifier<Node> instanceIdentifier,
                                       List<RemoteUcastMacs> remoteUcastMacs) {
        if (remoteUcastMacs == null) {
            return;
        }
        for (RemoteUcastMacs remoteUcastMac : remoteUcastMacs) {
            onConfigUpdate(transaction, instanceIdentifier, remoteUcastMac, null);
        }
    }

    @Override
    public void onConfigUpdate(TransactionBuilder transaction,
                                  InstanceIdentifier<Node> nodeIid,
                                  RemoteUcastMacs remoteUcastMacs,
                                  InstanceIdentifier macKey,
                                  Object... extraData) {
        InstanceIdentifier<RemoteUcastMacs> macIid = nodeIid.augmentation(HwvtepGlobalAugmentation.class).
                child(RemoteUcastMacs.class, remoteUcastMacs.getKey());
        processDependencies(UCAST_MAC_DATA_VALIDATOR, transaction, nodeIid, macIid, remoteUcastMacs);
    }

    @Override
    public void doDeviceTransaction(TransactionBuilder transaction,
                                   InstanceIdentifier<Node> instanceIdentifier,
                                   RemoteUcastMacs remoteUcastMac,
                                   InstanceIdentifier macKey,
                                   Object... extraData) {
            LOG.debug("Creating remoteUcastMacs, mac address: {}", remoteUcastMac.getMacEntryKey().getValue());
            HwvtepDeviceInfo.DeviceData deviceData =
                    getOperationalState().getDeviceInfo().getDeviceOperData(RemoteUcastMacs.class, macKey);

            UcastMacsRemote ucastMacsRemote = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), UcastMacsRemote.class);
            setIpAddress(ucastMacsRemote, remoteUcastMac);
            setLocator(transaction, ucastMacsRemote, remoteUcastMac);
            setLogicalSwitch(ucastMacsRemote, remoteUcastMac);
            if (deviceData == null) {
                setMac(ucastMacsRemote, remoteUcastMac);
                LOG.trace("doDeviceTransaction: creating RemotUcastMac entry: {}", ucastMacsRemote);
                transaction.add(op.insert(ucastMacsRemote));
                transaction.add(op.comment("UcastMacRemote: Creating " + remoteUcastMac.getMacEntryKey().getValue()));
            } else if (deviceData.getUuid() != null) {
                UUID macEntryUUID = deviceData.getUuid();
                UcastMacsRemote extraMac = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(),
                                UcastMacsRemote.class, null);
                extraMac.getUuidColumn().setData(macEntryUUID);
                LOG.trace("doDeviceTransaction: updating RemotUcastMac entry: {}", ucastMacsRemote);
                transaction.add(op.update(ucastMacsRemote)
                        .where(extraMac.getUuidColumn().getSchema().opEqual(macEntryUUID))
                        .build());
                transaction.add(op.comment("UcastMacRemote: Updating " + remoteUcastMac.getMacEntryKey().getValue()));
            } else {
                LOG.warn("Unable to update remoteMcastMacs {} because uuid not found in the operational store",
                                remoteUcastMac.getMacEntryKey().getValue());
            }
    }

    private void setLogicalSwitch(UcastMacsRemote ucastMacsRemote, RemoteUcastMacs inputMac) {
        if (inputMac.getLogicalSwitchRef() != null) {
            @SuppressWarnings("unchecked")
            InstanceIdentifier<LogicalSwitches> lswitchIid =
                    (InstanceIdentifier<LogicalSwitches>) inputMac.getLogicalSwitchRef().getValue();
            HwvtepDeviceInfo.DeviceData deviceData = getOperationalState().getDeviceInfo().getDeviceOperData(
                    LogicalSwitches.class, lswitchIid);
            if (deviceData != null && deviceData.getUuid() != null) {
                ucastMacsRemote.setLogicalSwitch(deviceData.getUuid());
            } else {
                ucastMacsRemote.setLogicalSwitch(TransactUtils.getLogicalSwitchUUID(lswitchIid));
            }
        }
    }

    private void setLocator(TransactionBuilder transaction, UcastMacsRemote ucastMacsRemote, RemoteUcastMacs inputMac) {
        //get UUID by locatorRef
        if (inputMac.getLocatorRef() != null) {
            UUID locatorUuid = null;
            @SuppressWarnings("unchecked")
            InstanceIdentifier<TerminationPoint> iid = (InstanceIdentifier<TerminationPoint>) inputMac.getLocatorRef().getValue();
            //try to find locator in operational DS
            HwvtepDeviceInfo.DeviceData deviceData = getOperationalState().getDeviceInfo().getDeviceOperData(TerminationPoint.class, iid);
            if (deviceData != null) {
                //if exist, get uuid
                locatorUuid = deviceData.getUuid();
            } else {
                locatorUuid = getOperationalState().getUUIDFromCurrentTx(TerminationPoint.class, iid);
                if (locatorUuid == null) {
                    locatorUuid = TransactUtils.createPhysicalLocator(transaction, getOperationalState(),
                            (InstanceIdentifier<TerminationPoint>) inputMac.getLocatorRef().getValue());
                    updateCurrentTxData(TerminationPoint.class, iid, locatorUuid, null);
                }
            }
            if (locatorUuid != null) {
                ucastMacsRemote.setLocator(locatorUuid);
            }
        }
    }

    private void setIpAddress(UcastMacsRemote ucastMacsRemote, RemoteUcastMacs inputMac) {
        if (inputMac.getIpaddr() != null) {
            ucastMacsRemote.setIpAddress(inputMac.getIpaddr().getIpv4Address().getValue());
        }
    }

    private void setMac(UcastMacsRemote ucastMacsRemote, RemoteUcastMacs inputMac) {
        if (inputMac.getMacEntryKey() != null) {
            ucastMacsRemote.setMac(inputMac.getMacEntryKey().getValue());
        }
    }

    protected List<RemoteUcastMacs> getData(HwvtepGlobalAugmentation augmentation) {
        return augmentation.getRemoteUcastMacs();
    }

    static class UcastMacUnMetDependencyGetter extends UnMetDependencyGetter<RemoteUcastMacs> {

        public List<InstanceIdentifier<?>> getLogicalSwitchDependencies(RemoteUcastMacs data) {
            if (data == null) {
                return Collections.EMPTY_LIST;
            }
            return Lists.newArrayList(data.getLogicalSwitchRef().getValue());
        }

        public List<InstanceIdentifier<?>> getTerminationPointDependencies(RemoteUcastMacs data) {
            if (data == null) {
                return Collections.EMPTY_LIST;
            }
            return Lists.newArrayList(data.getLocatorRef().getValue());
        }
    }
}
