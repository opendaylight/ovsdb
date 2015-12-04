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
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class TransactUtils {
    private static final Logger LOG = LoggerFactory.getLogger(TransactUtils.class);

    public static Node getCreated(DataObjectModification<Node> mod) {
        if((mod.getModificationType() == ModificationType.WRITE)
                        && (mod.getDataBefore() == null)){
            return mod.getDataAfter();
        }
        return null;
    }

    public static Node getRemoved(DataObjectModification<Node> mod) {
        if(mod.getModificationType() == ModificationType.DELETE){
            return mod.getDataBefore();
        }
        return null;
    }

    public static Node getUpdated(DataObjectModification<Node> mod) {
        Node node = null;
        switch(mod.getModificationType()) {
            case SUBTREE_MODIFIED:
                node = mod.getDataAfter();
                break;
            case WRITE:
                if(mod.getDataBefore() !=  null) {
                    node = mod.getDataAfter();
                }
                break;
            default:
                break;
        }
        return node;
    }

    public static Node getOriginal(DataObjectModification<Node> mod) {
        Node node = null;
        switch(mod.getModificationType()) {
            case SUBTREE_MODIFIED:
                node = mod.getDataBefore();
                break;
            case WRITE:
                if(mod.getDataBefore() !=  null) {
                    node = mod.getDataBefore();
                }
                break;
            case DELETE:
                node = mod.getDataBefore();
                break;
            default:
                break;
        }
        return node;
    }

    //TODO: change this function to be generic
    public static Map<InstanceIdentifier<Node>, Node> extractCreatedOrUpdatedOrRemoved(
            Collection<DataTreeModification<Node>> changes, Class<Node> class1) {
        Map<InstanceIdentifier<Node>, Node> result = new HashMap<InstanceIdentifier<Node>, Node>();
        for(DataTreeModification<Node> change : changes) {
            final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
            final DataObjectModification<Node> mod = change.getRootNode();
            Node created = getCreated(mod);
            if (created != null) {
                result.put(key, created);
            }
            Node updated = getUpdated(mod);
            if (updated != null) {
                result.put(key, updated);
            }
            Node deleted = getRemoved(mod);
            if (deleted != null) {
                result.put(key, deleted);
            }
        }
        return result;
    }

    /*
    public static <T extends Augmentation<Node>> Map<InstanceIdentifier<? extends DataObject>, T> extractCreated(
            Collection<DataTreeModification<Node>> changes, Class<T> class1) {
        // TODO Auto-generated method stub
        Map<InstanceIdentifier<?>, T> result =
            new HashMap<InstanceIdentifier<?>, T>();
        if(changes != null && !changes.isEmpty()) {
            for(DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node created = getCreated(mod);
                if(created != null) {
                    T logicalSwitch = created.getAugmentation(class1);
                    created.getKey().getNodeId().get
                    logicalSwitch.
                    InstanceIdentifier<?> iid = change.getRootPath().getRootIdentifier()..augmentation(class1);
                    if(logicalSwitch != null) {
                        result.put(iid, logicalSwitch);
                    }
                }
            }
        }
        return result;
    }
    */

    public static <D extends org.opendaylight.yangtools.yang.binding.DataObject> Optional<D> readNodeFromConfig(
            ReadWriteTransaction transaction, final InstanceIdentifier<D> connectionIid) {
        Optional<D> node = Optional.absent();
        try {
            node = transaction.read(LogicalDatastoreType.CONFIGURATION, connectionIid).checkedGet();
        } catch (final ReadFailedException e) {
            LOG.warn("Read Configration/DS for Node failed! {}", connectionIid, e);
        }
        return node;
    }

    public static UUID createPhysicalLocator(TransactionBuilder transaction, HwvtepPhysicalLocatorAugmentation inputLocator) {
        LOG.debug("Creating a physical locator: {}", inputLocator.getDstIp());
        PhysicalLocator physicalLocator = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), PhysicalLocator.class);
        setEncapsulationType(physicalLocator, inputLocator);
        setDstIp(physicalLocator, inputLocator);
        String locatorUuid = "PhysicalLocator_" + HwvtepSouthboundMapper.getRandomUUID();
        transaction.add(op.insert(physicalLocator).withId(locatorUuid));
        return new UUID(locatorUuid);
    }

    private static final void setEncapsulationType(PhysicalLocator physicalLocator, HwvtepPhysicalLocatorAugmentation inputLocator) {
        if (inputLocator.getEncapsulationType() != null) {
            String encapType = HwvtepSouthboundConstants.ENCAPS_TYPE_MAP.get(HwvtepSouthboundMapper.createEncapsulationType(""));
            physicalLocator.setEncapsulationType(encapType);
        }
    }

    private static final void setDstIp(PhysicalLocator physicalLocator, HwvtepPhysicalLocatorAugmentation inputLocator) {
        if (inputLocator.getDstIp() != null) {
            physicalLocator.setDstIp(inputLocator.getDstIp().getIpv4Address().getValue());
        }
    }
}
