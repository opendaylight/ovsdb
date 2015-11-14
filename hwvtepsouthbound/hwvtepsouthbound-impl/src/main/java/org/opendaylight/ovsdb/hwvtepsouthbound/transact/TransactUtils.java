/*
 * Copyright (c) 2015 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepLogicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static Map<InstanceIdentifier<Node>, Node> extractCreatedOrUpdatedOrRemoved(
            Collection<DataTreeModification<Node>> changes, Class<Node> class1) {
        // TODO Auto-generated method stub
        Map<InstanceIdentifier<Node>, Node> result = new HashMap<InstanceIdentifier<Node>, Node>();
        for(DataTreeModification<Node> change : changes) {
            final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
            final DataObjectModification<Node> mod = change.getRootNode();
            Node created = getCreated(mod);
            result.put(key, created);
            Node updated = getUpdated(mod);
            result.put(key, updated);
            Node deleted = getRemoved(mod);
            result.put(key, deleted);
        }
        return null;
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

}
