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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.McastMacsLocal;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocator;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocatorSet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitchesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.locator.set.attributes.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class McastMacsLocalUpdateCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(PhysicalPortRemoveCommand.class);

    public McastMacsLocalUpdateCommand(HwvtepOperationalState state,
            Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, List<LocalMcastMacs>> createds =
                extractCreated(getChanges(),LocalMcastMacs.class);
        if (!createds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, List<LocalMcastMacs>> created:
                createds.entrySet()) {
                updateMcastMacsLocal(transaction,  created.getKey(), created.getValue());
            }
        }
        Map<InstanceIdentifier<Node>, List<LocalMcastMacs>> updateds =
                extractUpdated(getChanges(),LocalMcastMacs.class);
        if (!updateds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, List<LocalMcastMacs>> updated:
                updateds.entrySet()) {
                updateMcastMacsLocal(transaction,  updated.getKey(), updated.getValue());
            }
        }
    }

    private void updateMcastMacsLocal(TransactionBuilder transaction,
            InstanceIdentifier<Node> instanceIdentifier, List<LocalMcastMacs> localMcastMacs) {
        for (LocalMcastMacs localMcastMac: localMcastMacs) {
            LOG.debug("Creating localMcastMac, mac address: {}", localMcastMac.getMacEntryKey().getValue());
            Optional<LocalMcastMacs> operationalMacOptional =
                    getOperationalState().getLocalMcastMacs(instanceIdentifier, localMcastMac.getKey());
            McastMacsLocal mcastMacsLocal = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), McastMacsLocal.class);
            setIpAddress(mcastMacsLocal, localMcastMac);
            setLocatorSet(transaction, mcastMacsLocal, localMcastMac);
            setLogicalSwitch(instanceIdentifier, mcastMacsLocal, localMcastMac);
            if (!operationalMacOptional.isPresent()) {
                setMac(mcastMacsLocal, localMcastMac, operationalMacOptional);
                transaction.add(op.insert(mcastMacsLocal));
            } else {
                LocalMcastMacs updatedMac = operationalMacOptional.get();
                String existingMac = updatedMac.getMacEntryKey().getValue();
                McastMacsLocal extraMac = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), McastMacsLocal.class);
                extraMac.setMac("");;
                transaction.add(op.update(mcastMacsLocal)
                        .where(extraMac.getMacColumn().getSchema().opEqual(existingMac))
                        .build());
            }
        }
    }

    private void setLogicalSwitch(InstanceIdentifier<Node> iid, McastMacsLocal mcastMacsLocal, LocalMcastMacs inputMac) {
        if (inputMac.getLogicalSwitchRef() != null) {
            HwvtepNodeName lswitchName = new HwvtepNodeName(inputMac.getLogicalSwitchRef().getValue());
            Optional<LogicalSwitches> operationalSwitchOptional =
                    getOperationalState().getLogicalSwitches(iid, new LogicalSwitchesKey(lswitchName));
            if (operationalSwitchOptional.isPresent()) {
                Uuid logicalSwitchUuid = operationalSwitchOptional.get().getLogicalSwitchUuid();
                UUID logicalSwitchUUID = new UUID(logicalSwitchUuid.getValue());
                mcastMacsLocal.setLogicalSwitch(logicalSwitchUUID);
            } else {
                LOG.warn("Create or update localMcastMac: No logical switch named {} found in operational datastore!",
                        lswitchName);
            }
        }
    }

    private void setLocatorSet(TransactionBuilder transaction, McastMacsLocal mcastMacsLocal, LocalMcastMacs inputMac) {
        if (inputMac.getLocatorSet() != null && !inputMac.getLocatorSet().isEmpty()) {
            UUID locatorSetUuid = TransactUtils.createPhysicalLocatorSet(getOperationalState(), transaction, inputMac.getLocatorSet());
            mcastMacsLocal.setLocatorSet(locatorSetUuid);
        }
    }

    private void setIpAddress(McastMacsLocal mcastMacsLocal, LocalMcastMacs inputMac) {
        if (inputMac.getIpaddr() != null) {
            mcastMacsLocal.setIpAddress(inputMac.getIpaddr().getIpv4Address().getValue());
        }
    }

    private void setMac(McastMacsLocal mcastMacsLocal, LocalMcastMacs inputMac,
            Optional<LocalMcastMacs> inputSwitchOptional) {
        if (inputMac.getMacEntryKey() != null) {
            mcastMacsLocal.setMac(inputMac.getMacEntryKey().getValue());
        } else if (inputSwitchOptional.isPresent() && inputSwitchOptional.get().getMacEntryKey() != null) {
            mcastMacsLocal.setMac(inputSwitchOptional.get().getMacEntryKey().getValue());
        }
    }

    private Map<InstanceIdentifier<Node>, List<LocalMcastMacs>> extractCreated(
            Collection<DataTreeModification<Node>> changes, Class<LocalMcastMacs> class1) {
        Map<InstanceIdentifier<Node>, List<LocalMcastMacs>> result
            = new HashMap<InstanceIdentifier<Node>, List<LocalMcastMacs>>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node created = TransactUtils.getCreated(mod);
                if (created != null) {
                    List<LocalMcastMacs> macListUpdated = null;
                    HwvtepGlobalAugmentation hgAugmentation = created.getAugmentation(HwvtepGlobalAugmentation.class);
                    if (hgAugmentation != null) {
                        macListUpdated = hgAugmentation.getLocalMcastMacs();
                    }
                    if (macListUpdated != null) {
                        result.put(key, macListUpdated);
                    }
                }
            }
        }
        return result;
    }

    private Map<InstanceIdentifier<Node>, List<LocalMcastMacs>> extractUpdated(
            Collection<DataTreeModification<Node>> changes, Class<LocalMcastMacs> class1) {
        Map<InstanceIdentifier<Node>, List<LocalMcastMacs>> result
            = new HashMap<InstanceIdentifier<Node>, List<LocalMcastMacs>>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node updated = TransactUtils.getUpdated(mod);
                Node before = mod.getDataBefore();
                if (updated != null && before != null) {
                    List<LocalMcastMacs> macListUpdated = null;
                    List<LocalMcastMacs> macListBefore = null;
                    HwvtepGlobalAugmentation hgUpdated = updated.getAugmentation(HwvtepGlobalAugmentation.class);
                    if (hgUpdated != null) {
                        macListUpdated = hgUpdated.getLocalMcastMacs();
                    }
                    HwvtepGlobalAugmentation hgBefore = before.getAugmentation(HwvtepGlobalAugmentation.class);
                    if (hgBefore != null) {
                        macListBefore = hgBefore.getLocalMcastMacs();
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