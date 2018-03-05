/*
 * Copyright (c) 2015, 2017 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactCommandAggregator implements TransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(TransactCommandAggregator.class);

    private final List<TransactCommand> commands = new ArrayList<>();
    private final HwvtepOperationalState operationalState;
    /* stores the modified and deleted data for each child type of each node id
       Map<nodeid , Pair < updated, deleted >
       each updated/ deleted contains Map < child type, List<ChildData>>
       child type is the child of hwvtep Global augmentation
     */
    private final Map<InstanceIdentifier<Node>,
            Pair<Map<Class<? extends Identifiable>, List<Identifiable>>,
                Map<Class<? extends Identifiable>, List<Identifiable>>>> modifiedData = new HashMap<>();


    public TransactCommandAggregator(HwvtepOperationalState state, Collection<DataTreeModification<Node>> changes) {
        this.operationalState = state;
        onDataTreeChanged(changes);
        commands.add(new PhysicalSwitchUpdateCommand(state,changes));
        commands.add(new PhysicalSwitchRemoveCommand(state,changes));
        commands.add(new LogicalSwitchUpdateCommand(state,changes));
        commands.add(new LogicalSwitchRemoveCommand(state,changes));
        commands.add(new PhysicalPortUpdateCommand(state,changes));
        commands.add(new PhysicalPortRemoveCommand(state,changes));
        commands.add(new McastMacsRemoteUpdateCommand(state,changes));
        commands.add(new McastMacsRemoteRemoveCommand(state,changes));
        commands.add(new McastMacsLocalUpdateCommand(state,changes));
        commands.add(new McastMacsLocalRemoveCommand(state,changes));
        commands.add(new UcastMacsRemoteUpdateCommand(state,changes));
        commands.add(new UcastMacsRemoteRemoveCommand(state,changes));
        commands.add(new UcastMacsLocalUpdateCommand(state,changes));
        commands.add(new UcastMacsLocalRemoveCommand(state,changes));
        commands.add(new TunnelUpdateCommand(state,changes));
        commands.add(new TunnelRemoveCommand(state,changes));
        commands.add(new LogicalRouterUpdateCommand(state,changes));
        commands.add(new LogicalRouterRemoveCommand(state,changes));
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        for (TransactCommand command:commands) {
            try {
                command.execute(transaction);
            } catch (NullPointerException e) {
                LOG.error("Execution of command {} failed with the following exception."
                        + " Continuing the execution of remaining commands", command, e);
            }
        }
    }

    @Override
    public void onConfigUpdate(TransactionBuilder transaction, InstanceIdentifier nodeIid, Identifiable data,
                               InstanceIdentifier key,
                               Object... extraData) {
    }

    @Override
    public void doDeviceTransaction(TransactionBuilder transaction, InstanceIdentifier nodeIid, Identifiable data,
                                    InstanceIdentifier key,
                                    Object... extraData) {
    }

    private void onDataTreeChanged(final Collection<DataTreeModification<Node>> changes) {
        boolean readOperationalNodes = false;
        for (DataTreeModification<Node> change : changes) {
            final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
            final DataObjectModification<Node> mod = change.getRootNode();
            final Map<Class<? extends Identifiable>, List<Identifiable>> updatedData = new HashMap<>();
            final Map<Class<? extends Identifiable>, List<Identifiable>> deletedData = new HashMap<>();
            extractDataChanged(key, mod, updatedData, deletedData);
            modifiedData.put(key, Pair.of(updatedData, deletedData));
            operationalState.setModifiedData(modifiedData);
            if (!isMacOnlyUpdate(updatedData, deletedData)) {
                readOperationalNodes = true;
            }
        }
        if (readOperationalNodes) {
            operationalState.readOperationalNodes();
        }
    }

    private boolean isMacOnlyUpdate(final Map<Class<? extends Identifiable>, List<Identifiable>> updatedData,
                                    final Map<Class<? extends Identifiable>, List<Identifiable>> deletedData) {
        return updatedData.containsKey(RemoteUcastMacs.class) && updatedData.size() == 1
                || deletedData.containsKey(RemoteUcastMacs.class) && deletedData.size() == 1;
    }

    private void extractDataChanged(final InstanceIdentifier<Node> key,
                                    final DataObjectModification<Node> mod,
                                    final Map<Class<? extends Identifiable>, List<Identifiable>> updatedData,
                                    final Map<Class<? extends Identifiable>, List<Identifiable>> deletedData) {

        extractDataChanged(mod.getModifiedChildren(), updatedData, deletedData);
        DataObjectModification<HwvtepGlobalAugmentation> aug = mod.getModifiedAugmentation(
                HwvtepGlobalAugmentation.class);
        if (aug != null && getModificationType(aug) != null) {
            extractDataChanged(aug.getModifiedChildren(), updatedData, deletedData);
        }
        DataObjectModification<PhysicalSwitchAugmentation> psAug = mod.getModifiedAugmentation(
                PhysicalSwitchAugmentation.class);
        if (psAug != null && getModificationType(psAug) != null) {
            extractDataChanged(psAug.getModifiedChildren(), updatedData, deletedData);
        }
    }

    private void extractDataChanged(final Collection<DataObjectModification<? extends DataObject>> children,
                                    final Map<Class<? extends Identifiable>, List<Identifiable>> updatedData,
                                    final Map<Class<? extends Identifiable>, List<Identifiable>> deletedData) {
        if (children == null) {
            return;
        }
        for (DataObjectModification<? extends DataObject> child : children) {
            DataObjectModification.ModificationType type = getModificationType(child);
            if (type == null) {
                continue;
            }
            InstanceIdentifier instanceIdentifier = null;
            Class<? extends Identifiable> childClass = (Class<? extends Identifiable>) child.getDataType();
            InstanceIdentifier.PathArgument pathArgument = child.getIdentifier();
            switch (type) {
                case WRITE:
                case SUBTREE_MODIFIED:
                    DataObject dataAfter = child.getDataAfter();
                    if (!(dataAfter instanceof Identifiable)) {
                        continue;
                    }
                    DataObject before = child.getDataBefore();
                    if (Objects.equals(dataAfter, before)) {
                        /*
                        in cluster reboot scenarios,
                        application rewrites the data tx.put( logicalswitchiid, logicalswitch )
                        that time it fires the update again ignoring such updates here
                         */
                        continue;
                    }
                    Identifiable identifiable = (Identifiable) dataAfter;
                    addToUpdatedData(updatedData, childClass, identifiable);
                    break;
                case DELETE:
                    DataObject dataBefore = child.getDataBefore();
                    if (!(dataBefore instanceof Identifiable)) {
                        continue;
                    }
                    addToUpdatedData(deletedData, childClass, (Identifiable)dataBefore);
                    break;
                default:
                    break;
            }
        }
    }

    private void addToUpdatedData(Map<Class<? extends Identifiable>, List<Identifiable>> updatedData,
                                  Class<? extends Identifiable> childClass, Identifiable identifiable) {
        updatedData.computeIfAbsent(childClass, (cls) -> new ArrayList<>());
        updatedData.get(childClass).add(identifiable);
    }

    private DataObjectModification.ModificationType getModificationType(
            DataObjectModification<? extends DataObject> mod) {
        try {
            return mod.getModificationType();
        } catch (IllegalStateException e) {
            //not sure why this getter throws this exception, could be some mdsal bug
            LOG.warn("Failed to get the modification type for mod {}", mod);
        }
        return null;
    }

    @Override
    public void onFailure(TransactionBuilder deviceTransaction) {
        commands.forEach(cmd -> cmd.onFailure(deviceTransaction));
        operationalState.clearIntransitKeys();
    }

    @Override
    public void onSuccess(TransactionBuilder deviceTransaction) {
        commands.forEach(cmd -> cmd.onSuccess(deviceTransaction));
    }
}
