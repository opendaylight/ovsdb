/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.ha.listeners;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.HAUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.Managers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.managers.ManagerOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class HAConfigClusteredListener extends ListenerBase implements ClusteredDataTreeChangeListener<Node> {
    private static final Logger LOG = LoggerFactory.getLogger(HAConfigClusteredListener.class);

    private static DataBroker dataBroker;
    private ListenerRegistration<HAConfigClusteredListener> registration;

    public HAConfigClusteredListener(DataBroker db) {
        super(LogicalDatastoreType.CONFIGURATION, db);
        LOG.info("Registering HAConfigClusteredListener");
    }

    @Override
    void handleDeleted(InstanceIdentifier<Node> key, Node deleted, ReadWriteTransaction tx) throws Exception {
        HACache.cleanupParent(key);
    }

    @Override
    void handleConnected(InstanceIdentifier<Node> key, Node haCreated, ReadWriteTransaction tx) throws Exception {
        List<NodeId> childNodeIds = HAUtil.getChildNodeIds(Optional.of(haCreated));
        if (childNodeIds != null) {
            for (NodeId nodeId : childNodeIds) {
                InstanceIdentifier<Node> childId = HAUtil.createInstanceIdentifier(nodeId.getValue());
                HACache.addChild(key, childId);
            }
        }
    }

    @Override
    void handleUpdated(InstanceIdentifier<Node> key, Node updated, Node before, ReadWriteTransaction tx) throws Exception {
        updateHACache(key, updated, before, dataBroker, tx);
    }

    static void updateHACache(InstanceIdentifier<Node> key, Node updated, Node before,
                              DataBroker dataBroker,
                              ReadWriteTransaction tx) throws Exception {
        HwvtepGlobalAugmentation updatedAugmentaion = updated.getAugmentation(HwvtepGlobalAugmentation.class);
        HwvtepGlobalAugmentation beforeAugmentaion = before.getAugmentation(HwvtepGlobalAugmentation.class);
        List<Managers> up = null;
        List<Managers> be = null;
        if (updatedAugmentaion != null) {
            up  = updatedAugmentaion.getManagers();
        }
        if (beforeAugmentaion != null) {
            be  = beforeAugmentaion.getManagers();
        }
        if (up != null && be != null) {
            if (up.size() > 0) {
                if (be.size() > 0) {
                    Managers m1 = up.get(0);
                    Managers m2 = be.get(0);
                    if (!m1.equals(m2)) {
                        LOG.error("manager entry updated for node {} ", updated.getNodeId().getValue());
                        if (!Strings.isNullOrEmpty(getHAChildren(dataBroker, updated))) {
                            List<NodeId> childNodeIds = HAUtil.getChildNodeIds(Optional.of(updated));
                            if (childNodeIds != null) {
                                for (NodeId nodeId : childNodeIds) {
                                    InstanceIdentifier<Node> childId = HAUtil.createInstanceIdentifier(nodeId.getValue());
                                    HACache.addChild(key, childId);
                                }
                            }
                        } else {
                            String haParentId = getHAParentId(dataBroker, updated);
                            if (haParentId != null) {
                                InstanceIdentifier<Node> parentId = HAUtil.getInstanceIdentifier(haParentId);
                                if (haParentId != null) {
                                    HACache.addChild(parentId, key/*child*/);
                                }
                            }
                        }
                    }
                }
            }
        }
        //TODO handle unhaed case
    }

    public static String getHAChildren(DataBroker broker, Node node) {
        NodeId hwvtepNodeId = null;
        if (node.getAugmentation(HwvtepGlobalAugmentation.class) == null) {
            return null;
        }
        hwvtepNodeId = node.getNodeId();
        boolean haEnabled = false;
        HwvtepGlobalAugmentation globalAugmentation = node.getAugmentation(HwvtepGlobalAugmentation.class);
        if (globalAugmentation != null) {
            List<Managers> managers = globalAugmentation.getManagers();
            if (managers != null && managers.size() > 0 && managers.get(0).getManagerOtherConfigs() != null) {
                for (ManagerOtherConfigs configs : managers.get(0).getManagerOtherConfigs()) {
                    if (configs.getOtherConfigKey().equals("ha_children")) {
                        return configs.getOtherConfigValue();
                    }
                }
            }
        }
        return null;
    }

    public static String getHAParentId(DataBroker broker, Node node) {
        NodeId hwvtepNodeId = null;
        if (node.getAugmentation(HwvtepGlobalAugmentation.class) == null) {
            return null;
        }
        hwvtepNodeId = node.getNodeId();
        boolean haEnabled = false;
        HwvtepGlobalAugmentation globalAugmentation = node.getAugmentation(HwvtepGlobalAugmentation.class);
        if (globalAugmentation != null) {
            List<Managers> managers = globalAugmentation.getManagers();
            if (managers != null && managers.size() > 0 && managers.get(0).getManagerOtherConfigs() != null) {
                for (ManagerOtherConfigs configs : managers.get(0).getManagerOtherConfigs()) {
                    if (configs.getOtherConfigKey().equals("ha_id")) {
                        return configs.getOtherConfigValue();
                    }
                }
            }
        }
        return null;
    }

}
