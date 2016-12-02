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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.UcastMacsLocal;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class UcastMacsLocalUpdateCommand extends AbstractTransactCommand<LocalUcastMacs, HwvtepGlobalAugmentation> {
    private static final Logger LOG = LoggerFactory.getLogger(UcastMacsLocalUpdateCommand.class);

    public UcastMacsLocalUpdateCommand(HwvtepOperationalState state,
            Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, List<LocalUcastMacs>> updateds =
                extractUpdated(getChanges(),LocalUcastMacs.class);
        if (!updateds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, List<LocalUcastMacs>> updated:
                updateds.entrySet()) {
                updateUcastMacsLocal(transaction,  updated.getKey(), updated.getValue());
            }
        }
    }

    private void updateUcastMacsLocal(TransactionBuilder transaction,
            InstanceIdentifier<Node> instanceIdentifier, List<LocalUcastMacs> localUcastMacs) {
        for (LocalUcastMacs localUcastMac: localUcastMacs) {
            LOG.debug("Creating localUcastMacs, mac address: {}", localUcastMac.getMacEntryKey().getValue());
            Optional<LocalUcastMacs> operationalMacOptional =
                    getOperationalState().getLocalUcastMacs(instanceIdentifier, localUcastMac.getKey());
            UcastMacsLocal ucastMacsLocal = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), UcastMacsLocal.class);
            setIpAddress(ucastMacsLocal, localUcastMac);
            setLocator(transaction, ucastMacsLocal, localUcastMac);
            setLogicalSwitch(ucastMacsLocal, localUcastMac);
            if (!operationalMacOptional.isPresent()) {
                setMac(ucastMacsLocal, localUcastMac, operationalMacOptional);
                LOG.trace("execute: creating LocalUcastMac entry: {}", ucastMacsLocal);
                transaction.add(op.insert(ucastMacsLocal));
                transaction.add(op.comment("UcastMacLocal: Creating " + localUcastMac.getMacEntryKey().getValue()));
            } else if (operationalMacOptional.get().getMacEntryUuid() != null) {
                UUID macEntryUUID = new UUID(operationalMacOptional.get().getMacEntryUuid().getValue());
                UcastMacsLocal extraMac = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(),
                                UcastMacsLocal.class, null);
                extraMac.getUuidColumn().setData(macEntryUUID);
                LOG.trace("execute: updating LocalUcastMac entry: {}", ucastMacsLocal);
                transaction.add(op.update(ucastMacsLocal)
                        .where(extraMac.getUuidColumn().getSchema().opEqual(macEntryUUID))
                        .build());
                transaction.add(op.comment("UcastMacLocal: Updating " + macEntryUUID));
            } else {
                LOG.warn("Unable to update localUcastMacs {} because uuid not found in the operational store",
                                localUcastMac.getMacEntryKey().getValue());
            }
        }
    }

    private void setLogicalSwitch(UcastMacsLocal ucastMacsLocal, LocalUcastMacs inputMac) {
        if (inputMac.getLogicalSwitchRef() != null) {
            @SuppressWarnings("unchecked")
            InstanceIdentifier<LogicalSwitches> lswitchIid = (InstanceIdentifier<LogicalSwitches>) inputMac.getLogicalSwitchRef().getValue();
            Optional<LogicalSwitches> operationalSwitchOptional =
                    getOperationalState().getLogicalSwitches(lswitchIid);
            if (operationalSwitchOptional.isPresent()) {
                Uuid logicalSwitchUuid = operationalSwitchOptional.get().getLogicalSwitchUuid();
                UUID logicalSwitchUUID = new UUID(logicalSwitchUuid.getValue());
                ucastMacsLocal.setLogicalSwitch(logicalSwitchUUID);
            } else {
                LOG.warn("Create or update localUcastMacs: No logical switch with iid {} found in operational datastore!",
                        lswitchIid);
            }
        }
    }

    private void setLocator(TransactionBuilder transaction, UcastMacsLocal ucastMacsLocal, LocalUcastMacs inputMac) {
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
                //if no, get it from config DS and create id
                Optional<TerminationPoint> configLocatorOptional =
                        TransactUtils.readNodeFromConfig(getOperationalState().getReadWriteTransaction(), iid);
                if (configLocatorOptional.isPresent()) {
                    HwvtepPhysicalLocatorAugmentation locatorAugmentation =
                            configLocatorOptional.get().getAugmentation(HwvtepPhysicalLocatorAugmentation.class);
                    locatorUuid = TransactUtils.createPhysicalLocator(transaction, locatorAugmentation);
                } else {
                    LOG.warn("Create or update localUcastMac: No physical locator found in operational datastore!"
                            + "Its indentifier is {}", inputMac.getLocatorRef().getValue());
                }
            }
            if (locatorUuid != null) {
                ucastMacsLocal.setLocator(locatorUuid);
            }
        }
    }

    private void setIpAddress(UcastMacsLocal ucastMacsLocal, LocalUcastMacs inputMac) {
        if (inputMac.getIpaddr() != null) {
            ucastMacsLocal.setIpAddress(inputMac.getIpaddr().getIpv4Address().getValue());
        }
    }

    private void setMac(UcastMacsLocal ucastMacsLocal, LocalUcastMacs inputMac,
            Optional<LocalUcastMacs> inputSwitchOptional) {
        if (inputMac.getMacEntryKey() != null) {
            ucastMacsLocal.setMac(inputMac.getMacEntryKey().getValue());
        } else if (inputSwitchOptional.isPresent() && inputSwitchOptional.get().getMacEntryKey() != null) {
            ucastMacsLocal.setMac(inputSwitchOptional.get().getMacEntryKey().getValue());
        }
    }

    protected List<LocalUcastMacs> getData(HwvtepGlobalAugmentation augmentation) {
        return augmentation.getLocalUcastMacs();
    }

}
