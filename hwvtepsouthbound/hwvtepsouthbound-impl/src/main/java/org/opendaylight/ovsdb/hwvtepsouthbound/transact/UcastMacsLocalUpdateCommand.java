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
import java.util.Optional;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.schema.hardwarevtep.UcastMacsLocal;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalUcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UcastMacsLocalUpdateCommand
        extends AbstractTransactCommand<LocalUcastMacs, LocalUcastMacsKey, HwvtepGlobalAugmentation> {
    private static final Logger LOG = LoggerFactory.getLogger(UcastMacsLocalUpdateCommand.class);

    public UcastMacsLocalUpdateCommand(final HwvtepOperationalState state,
            final Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(final TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, List<LocalUcastMacs>> updateds =
                extractUpdated(getChanges(),LocalUcastMacs.class);
        if (!updateds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, List<LocalUcastMacs>> updated:
                updateds.entrySet()) {
                updateUcastMacsLocal(transaction,  updated.getKey(), updated.getValue());
            }
        }
    }

    private void updateUcastMacsLocal(final TransactionBuilder transaction,
            final InstanceIdentifier<Node> instanceIdentifier, final List<LocalUcastMacs> localUcastMacs) {
        for (LocalUcastMacs localUcastMac: localUcastMacs) {
            LOG.debug("Creating localUcastMacs, mac address: {}", localUcastMac.getMacEntryKey().getValue());
            final Optional<LocalUcastMacs> operationalMacOptional =
                    getOperationalState().getLocalUcastMacs(instanceIdentifier, localUcastMac.key());
            UcastMacsLocal ucastMacsLocal = transaction.getTypedRowWrapper(UcastMacsLocal.class);
            setIpAddress(ucastMacsLocal, localUcastMac);
            setLocator(transaction, ucastMacsLocal, localUcastMac);
            setLogicalSwitch(ucastMacsLocal, localUcastMac);
            if (operationalMacOptional.isEmpty()) {
                setMac(ucastMacsLocal, localUcastMac, operationalMacOptional);
                LOG.trace("execute: creating LocalUcastMac entry: {}", ucastMacsLocal);
                transaction.add(op.insert(ucastMacsLocal));
                transaction.add(op.comment("UcastMacLocal: Creating " + localUcastMac.getMacEntryKey().getValue()));
            } else if (operationalMacOptional.orElseThrow().getMacEntryUuid() != null) {
                UUID macEntryUUID = new UUID(operationalMacOptional.orElseThrow().getMacEntryUuid().getValue());
                UcastMacsLocal extraMac = transaction.getTypedRowSchema(UcastMacsLocal.class);
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

    private void setLogicalSwitch(final UcastMacsLocal ucastMacsLocal, final LocalUcastMacs inputMac) {
        if (inputMac.getLogicalSwitchRef() != null) {
            @SuppressWarnings("unchecked")
            InstanceIdentifier<LogicalSwitches> lswitchIid =
                    (InstanceIdentifier<LogicalSwitches>) inputMac.getLogicalSwitchRef().getValue();
            Optional<LogicalSwitches> operationalSwitchOptional =
                    getOperationalState().getLogicalSwitches(lswitchIid);
            if (operationalSwitchOptional.isPresent()) {
                Uuid logicalSwitchUuid = operationalSwitchOptional.orElseThrow().getLogicalSwitchUuid();
                UUID logicalSwitchUUID = new UUID(logicalSwitchUuid.getValue());
                ucastMacsLocal.setLogicalSwitch(logicalSwitchUUID);
            } else {
                LOG.warn(
                    "Create or update localUcastMacs: No logical switch with iid {} found in operational datastore!",
                    lswitchIid);
            }
        }
    }

    private void setLocator(final TransactionBuilder transaction, final UcastMacsLocal ucastMacsLocal,
            final LocalUcastMacs inputMac) {
        //get UUID by locatorRef
        if (inputMac.getLocatorRef() != null) {
            UUID locatorUuid = null;
            @SuppressWarnings("unchecked")
            InstanceIdentifier<TerminationPoint> iid =
                    (InstanceIdentifier<TerminationPoint>) inputMac.getLocatorRef().getValue();
            //try to find locator in operational DS
            Optional<HwvtepPhysicalLocatorAugmentation> operationalLocatorOptional =
                    getOperationalState().getPhysicalLocatorAugmentation(iid);
            if (operationalLocatorOptional.isPresent()) {
                //if exist, get uuid
                HwvtepPhysicalLocatorAugmentation locatorAugmentation = operationalLocatorOptional.orElseThrow();
                locatorUuid = new UUID(locatorAugmentation.getPhysicalLocatorUuid().getValue());
            } else {
                //if no, get it from config DS and create id
                Optional<TerminationPoint> configLocatorOptional = new MdsalUtils(
                        getOperationalState().getDataBroker()).readOptional(LogicalDatastoreType.CONFIGURATION, iid);
                if (configLocatorOptional.isPresent()) {
                    HwvtepPhysicalLocatorAugmentation locatorAugmentation =
                            configLocatorOptional.orElseThrow().augmentation(HwvtepPhysicalLocatorAugmentation.class);
                    locatorUuid = TransactUtils.createPhysicalLocator(transaction, locatorAugmentation,
                            getOperationalState());
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

    private static void setIpAddress(final UcastMacsLocal ucastMacsLocal, final LocalUcastMacs inputMac) {
        if (inputMac.getIpaddr() != null) {
            ucastMacsLocal.setIpAddress(inputMac.getIpaddr().getIpv4Address().getValue());
        }
    }

    private static void setMac(final UcastMacsLocal ucastMacsLocal, final LocalUcastMacs inputMac,
            final Optional<LocalUcastMacs> inputSwitchOptional) {
        if (inputMac.getMacEntryKey() != null) {
            ucastMacsLocal.setMac(inputMac.getMacEntryKey().getValue());
        } else if (inputSwitchOptional.isPresent() && inputSwitchOptional.orElseThrow().getMacEntryKey() != null) {
            ucastMacsLocal.setMac(inputSwitchOptional.orElseThrow().getMacEntryKey().getValue());
        }
    }

    @Override
    protected Map<LocalUcastMacsKey, LocalUcastMacs> getData(final HwvtepGlobalAugmentation augmentation) {
        return augmentation.getLocalUcastMacs();
    }
}
