/*
 * Copyright (c) 2015 China Telecom Beijing Research Institute and others.  All rights reserved.
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
import org.opendaylight.ovsdb.schema.hardwarevtep.UcastMacsLocal;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitchesKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class UcastMacsLocalUpdateCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(PhysicalPortRemoveCommand.class);

    public UcastMacsLocalUpdateCommand(HwvtepOperationalState state,
            Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, List<LocalUcastMacs>> createds =
                extractCreated(getChanges(),LocalUcastMacs.class);
        if (!createds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, List<LocalUcastMacs>> created:
                createds.entrySet()) {
                updateUcastMacsLocal(transaction,  created.getKey(), created.getValue());
            }
        }
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
            setLogicalSwitch(instanceIdentifier, ucastMacsLocal, localUcastMac);
            if (!operationalMacOptional.isPresent()) {
                setMac(ucastMacsLocal, localUcastMac, operationalMacOptional);
                transaction.add(op.insert(ucastMacsLocal));
            } else {
                LocalUcastMacs updatedMac = operationalMacOptional.get();
                String existingMac = updatedMac.getMacEntryKey().getValue();
                UcastMacsLocal extraMac = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), UcastMacsLocal.class);
                extraMac.setMac("");;
                transaction.add(op.update(ucastMacsLocal)
                        .where(extraMac.getMacColumn().getSchema().opEqual(existingMac))
                        .build());
            }
        }
    }

    private void setLogicalSwitch(InstanceIdentifier<Node> iid, UcastMacsLocal ucastMacsLocal, LocalUcastMacs inputMac) {
        if (inputMac.getLogicalSwitchRef() != null) {
            HwvtepNodeName lswitchName = new HwvtepNodeName(inputMac.getLogicalSwitchRef().getValue());
            Optional<LogicalSwitches> operationalSwitchOptional =
                    getOperationalState().getLogicalSwitches(iid, new LogicalSwitchesKey(lswitchName));
            if (operationalSwitchOptional.isPresent()) {
                Uuid logicalSwitchUuid = operationalSwitchOptional.get().getLogicalSwitchUuid();
                UUID logicalSwitchUUID = new UUID(logicalSwitchUuid.getValue());
                ucastMacsLocal.setLogicalSwitch(logicalSwitchUUID);
            } else {
                LOG.warn("Create or update localUcastMacs: No logical switch named {} found in operational datastore!",
                        lswitchName);
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

    private Map<InstanceIdentifier<Node>, List<LocalUcastMacs>> extractCreated(
            Collection<DataTreeModification<Node>> changes, Class<LocalUcastMacs> class1) {
        Map<InstanceIdentifier<Node>, List<LocalUcastMacs>> result
            = new HashMap<InstanceIdentifier<Node>, List<LocalUcastMacs>>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node created = TransactUtils.getCreated(mod);
                if (created != null) {
                    List<LocalUcastMacs> macListUpdated = null;
                    HwvtepGlobalAugmentation hgAugmentation = created.getAugmentation(HwvtepGlobalAugmentation.class);
                    if (hgAugmentation != null) {
                        macListUpdated = hgAugmentation.getLocalUcastMacs();
                    }
                    if (macListUpdated != null) {
                        result.put(key, macListUpdated);
                    }
                }
            }
        }
        return result;
    }

    private Map<InstanceIdentifier<Node>, List<LocalUcastMacs>> extractUpdated(
            Collection<DataTreeModification<Node>> changes, Class<LocalUcastMacs> class1) {
        Map<InstanceIdentifier<Node>, List<LocalUcastMacs>> result
            = new HashMap<InstanceIdentifier<Node>, List<LocalUcastMacs>>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node updated = TransactUtils.getUpdated(mod);
                Node before = mod.getDataBefore();
                if (updated != null && before != null) {
                    List<LocalUcastMacs> macListUpdated = null;
                    List<LocalUcastMacs> macListBefore = null;
                    HwvtepGlobalAugmentation hgUpdated = updated.getAugmentation(HwvtepGlobalAugmentation.class);
                    if (hgUpdated != null) {
                        macListUpdated = hgUpdated.getLocalUcastMacs();
                    }
                    HwvtepGlobalAugmentation hgBefore = before.getAugmentation(HwvtepGlobalAugmentation.class);
                    if (hgBefore != null) {
                        macListBefore = hgBefore.getLocalUcastMacs();
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
