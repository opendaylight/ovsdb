/*
 * Copyright (c) 2015, 2016 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.UcastMacsRemote;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class UcastMacsRemoteUpdateCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(UcastMacsRemoteUpdateCommand.class);

    public UcastMacsRemoteUpdateCommand(HwvtepOperationalState state,
            Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, List<RemoteUcastMacs>> createds =
                extractCreated(getChanges(),RemoteUcastMacs.class);
        if (!createds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, List<RemoteUcastMacs>> created:
                createds.entrySet()) {
                updateUcastMacsRemote(transaction,  created.getKey(), created.getValue());
            }
        }
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
            InstanceIdentifier<Node> instanceIdentifier, List<RemoteUcastMacs> remoteUcastMacs) {
        for (RemoteUcastMacs remoteUcastMac: remoteUcastMacs) {
            LOG.debug("Creating remoteUcastMacs, mac address: {}", remoteUcastMac.getMacEntryKey().getValue());
            Optional<RemoteUcastMacs> operationalMacOptional =
                    getOperationalState().getRemoteUcastMacs(instanceIdentifier, remoteUcastMac.getKey());
            UcastMacsRemote ucastMacsRemote = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), UcastMacsRemote.class);
            setIpAddress(ucastMacsRemote, remoteUcastMac);
            setLocator(transaction, ucastMacsRemote, remoteUcastMac);
            setLogicalSwitch(ucastMacsRemote, remoteUcastMac);
            if (!operationalMacOptional.isPresent()) {
                setMac(ucastMacsRemote, remoteUcastMac, operationalMacOptional);
                LOG.trace("execute: creating RemotUcastMac entry: {}", ucastMacsRemote);
                transaction.add(op.insert(ucastMacsRemote));
                transaction.add(op.comment("UcastMacRemote: Creating " + remoteUcastMac.getMacEntryKey().getValue()));
            } else if (operationalMacOptional.get().getMacEntryUuid() != null) {
                UUID macEntryUUID = new UUID(operationalMacOptional.get().getMacEntryUuid().getValue());
                UcastMacsRemote extraMac = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(),
                                UcastMacsRemote.class, null);
                extraMac.getUuidColumn().setData(macEntryUUID);
                LOG.trace("execute: updating RemotUcastMac entry: {}", ucastMacsRemote);
                transaction.add(op.update(ucastMacsRemote)
                        .where(extraMac.getUuidColumn().getSchema().opEqual(macEntryUUID))
                        .build());
                transaction.add(op.comment("UcastMacRemote: Updating " + remoteUcastMac.getMacEntryKey().getValue()));
            } else {
                LOG.warn("Unable to update remoteMcastMacs {} because uuid not found in the operational store",
                                remoteUcastMac.getMacEntryKey().getValue());
            }
        }
    }

    private void setLogicalSwitch(UcastMacsRemote ucastMacsRemote, RemoteUcastMacs inputMac) {
        if (inputMac.getLogicalSwitchRef() != null) {
            @SuppressWarnings("unchecked")
            InstanceIdentifier<LogicalSwitches> lswitchIid = (InstanceIdentifier<LogicalSwitches>) inputMac.getLogicalSwitchRef().getValue();
            Optional<LogicalSwitches> operationalSwitchOptional =
                    getOperationalState().getLogicalSwitches(lswitchIid);
            if (operationalSwitchOptional.isPresent()) {
                Uuid logicalSwitchUuid = operationalSwitchOptional.get().getLogicalSwitchUuid();
                UUID logicalSwitchUUID = new UUID(logicalSwitchUuid.getValue());
                ucastMacsRemote.setLogicalSwitch(logicalSwitchUUID);
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
            Optional<HwvtepPhysicalLocatorAugmentation> operationalLocatorOptional =
                    getOperationalState().getPhysicalLocatorAugmentation(iid);
            if (operationalLocatorOptional.isPresent()) {
                //if exist, get uuid
                HwvtepPhysicalLocatorAugmentation locatorAugmentation = operationalLocatorOptional.get();
                locatorUuid = new UUID(locatorAugmentation.getPhysicalLocatorUuid().getValue());
            } else {
                //TODO: need to optimize by eliminating reading Configuration datastore
                //if no, get it from config DS and create id
                locatorUuid = getOperationalState().getPhysicalLocatorInFlight(iid);
                if (locatorUuid == null) {
                    Optional<TerminationPoint> configLocatorOptional =
                            TransactUtils.readNodeFromConfig(getOperationalState().getReadWriteTransaction(), iid);
                    if (configLocatorOptional.isPresent()) {
                        HwvtepPhysicalLocatorAugmentation locatorAugmentation =
                                configLocatorOptional.get().getAugmentation(HwvtepPhysicalLocatorAugmentation.class);
                        locatorUuid = TransactUtils.createPhysicalLocator(transaction, locatorAugmentation);
                        getOperationalState().setPhysicalLocatorInFlight(iid, locatorUuid);
                    } else {
                        LOG.warn("Create or update remoteUcastMac: No physical locator found in operational datastore!"
                                + "Its indentifier is {}", inputMac.getLocatorRef().getValue());
                    }
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

    private void setMac(UcastMacsRemote ucastMacsRemote, RemoteUcastMacs inputMac,
            Optional<RemoteUcastMacs> inputSwitchOptional) {
        if (inputMac.getMacEntryKey() != null) {
            ucastMacsRemote.setMac(inputMac.getMacEntryKey().getValue());
        } else if (inputSwitchOptional.isPresent() && inputSwitchOptional.get().getMacEntryKey() != null) {
            ucastMacsRemote.setMac(inputSwitchOptional.get().getMacEntryKey().getValue());
        }
    }

    private Map<InstanceIdentifier<Node>, List<RemoteUcastMacs>> extractCreated(
            Collection<DataTreeModification<Node>> changes, Class<RemoteUcastMacs> class1) {
        Map<InstanceIdentifier<Node>, List<RemoteUcastMacs>> result
            = new HashMap<InstanceIdentifier<Node>, List<RemoteUcastMacs>>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node created = TransactUtils.getCreated(mod);
                if (created != null) {
                    List<RemoteUcastMacs> macListUpdated = null;
                    HwvtepGlobalAugmentation hgAugmentation = created.getAugmentation(HwvtepGlobalAugmentation.class);
                    if (hgAugmentation != null) {
                        macListUpdated = hgAugmentation.getRemoteUcastMacs();
                    }
                    if (macListUpdated != null) {
                        result.put(key, macListUpdated);
                    }
                }
            }
        }
        return result;
    }

    private Map<InstanceIdentifier<Node>, List<RemoteUcastMacs>> extractUpdated(
            Collection<DataTreeModification<Node>> changes, Class<RemoteUcastMacs> class1) {
        Map<InstanceIdentifier<Node>, List<RemoteUcastMacs>> result
            = new HashMap<InstanceIdentifier<Node>, List<RemoteUcastMacs>>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node updated = TransactUtils.getUpdated(mod);
                Node before = mod.getDataBefore();
                if (updated != null && before != null) {
                    List<RemoteUcastMacs> macListUpdated = null;
                    List<RemoteUcastMacs> macListBefore = null;
                    HwvtepGlobalAugmentation hgUpdated = updated.getAugmentation(HwvtepGlobalAugmentation.class);
                    if (hgUpdated != null) {
                        macListUpdated = hgUpdated.getRemoteUcastMacs();
                    }
                    HwvtepGlobalAugmentation hgBefore = before.getAugmentation(HwvtepGlobalAugmentation.class);
                    if (hgBefore != null) {
                        macListBefore = hgBefore.getRemoteUcastMacs();
                    }
                    if (macListUpdated != null) {
                        if (macListBefore != null) {
                            macListUpdated.removeAll(macListBefore);
                        }
                        result.put(key, macListUpdated);
                    }
                }
            }
        }
        return result;
    }
}