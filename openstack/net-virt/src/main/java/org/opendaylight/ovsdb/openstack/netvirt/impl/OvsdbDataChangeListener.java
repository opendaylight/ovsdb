/*
 * Copyright (c) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.impl;

import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.OvsdbInventoryListener;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TransactUtils;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MDSAL dataChangeListener for the OVSDB Southbound
 *
 * @author Sam Hague (shague@redhat.com)
 */
public class OvsdbDataChangeListener implements DataChangeListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbDataChangeListener.class);
    private DataBroker dataBroker = null;
    private ListenerRegistration<DataChangeListener> registration;
    private NodeCacheManager nodeCacheManager = null;

    public OvsdbDataChangeListener (DataBroker dataBroker) {
        LOG.info(">>>>> Registering OvsdbNodeDataChangeListener: dataBroker= {}", dataBroker);
        this.dataBroker = dataBroker;
        InstanceIdentifier<Node> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class);
        registration = dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, path, this,
                DataChangeScope.SUBTREE);
        LOG.info("netvirt OvsdbDataChangeListener: registration= {}", registration);
    }

    @Override
    public void close () throws Exception {
        registration.close();
    }

    /* TODO
     * Recognize when netvirt added a bridge to config and then the operational update comes in
     * can it be ignored or just viewed as a new switch? ports and interfaces can likely be mapped
     * to the old path where there were updates for them for update and insert row.
     */
    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        LOG.info(">>>>> onDataChanged: {}", changes);

        updateConnections(changes);
        updateOpenflowConnections(changes);

    }

    private void updateConnections(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        for (Map.Entry<InstanceIdentifier<?>, DataObject> created : changes.getCreatedData().entrySet()) {
            LOG.info("updateConnections created: {}", created);
            if (created.getValue() instanceof OvsdbNodeAugmentation) {
                Node ovsdbNode = getNode(changes, created);
                LOG.info("ovsdbNode: {}", ovsdbNode);
                ovsdbUpdate(ovsdbNode, OvsdbInventoryListener.OvsdbType.NODE, Action.ADD);
            }
        }
    }

    private void updateOpenflowConnections(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        for (Map.Entry<InstanceIdentifier<?>, DataObject> change : changes.getCreatedData().entrySet()) {
            LOG.info("updateOpenflowConnections created: {}", change);
            if (change.getValue() instanceof OvsdbBridgeAugmentation) {
                OvsdbBridgeAugmentation ovsdbBridgeAugmentation = (OvsdbBridgeAugmentation)change.getValue();
                String datapathId = MdsalUtils.getDatapathId(ovsdbBridgeAugmentation);
                // Having a datapathId means the bridge has connected so it exists
                if (datapathId == null) {
                    LOG.info("dataPathId not found");
                    continue;
                }
                Node node = getNode(changes.getCreatedData(), change);
                if (node == null) {
                    LOG.warn("node not found");
                    continue;
                }
                // This value is not being set right now - OvsdbBridgeUpdateCommand
                //if (ovsdbBridgeAugmentation.getBridgeOpenflowNodeRef() != null) {
                    nodeCacheManager =
                            (NodeCacheManager) ServiceHelper.getGlobalInstance(NodeCacheManager.class, this);
                    nodeCacheManager.nodeAdded(node);
                //}
            }
        }

        for (Map.Entry<InstanceIdentifier<?>, DataObject> change : changes.getUpdatedData().entrySet()) {
            LOG.info("updateOpenflowConnections updated: {}", change);
            if (change.getValue() instanceof OvsdbBridgeAugmentation) {
                OvsdbBridgeAugmentation ovsdbBridgeAugmentation = (OvsdbBridgeAugmentation)change.getValue();
                String datapathId = MdsalUtils.getDatapathId(ovsdbBridgeAugmentation);
                // Having a datapathId means the bridge has connected so it exists
                if (datapathId == null) {
                    LOG.info("dataPathId not found");
                    continue;
                }
                Node node = getNode(changes.getUpdatedData(), change);
                if (node == null) {
                    LOG.warn("node not found");
                    continue;
                }
                // This value is not being set right now - OvsdbBridgeUpdateCommand
                // if (ovsdbBridgeAugmentation.getBridgeOpenflowNodeRef() != null) {
                    nodeCacheManager =
                            (NodeCacheManager) ServiceHelper.getGlobalInstance(NodeCacheManager.class, this);
                    nodeCacheManager.nodeAdded(node);
                //}
            }
        }
    }

    private Node getNode(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
                         Map.Entry<InstanceIdentifier<?>, DataObject> change) {
        InstanceIdentifier<Node> nodeInstanceIdentifier = change.getKey().firstIdentifierOf(Node.class);
        return (Node)changes.getCreatedData().get(nodeInstanceIdentifier);
    }

    private Node getNode(Map<InstanceIdentifier<?>, DataObject> changes,
                         Map.Entry<InstanceIdentifier<?>, DataObject> change) {
        InstanceIdentifier<Node> nodeInstanceIdentifier = change.getKey().firstIdentifierOf(Node.class);
        return (Node)changes.get(nodeInstanceIdentifier);
    }

    private void ovsdbUpdate(Node node, OvsdbInventoryListener.OvsdbType ovsdbType, Action action) {
        Set<OvsdbInventoryListener> mdsalConsumerListeners = OvsdbInventoryServiceImpl.getMdsalConsumerListeners();
        for (OvsdbInventoryListener mdsalConsumerListener : mdsalConsumerListeners) {
            mdsalConsumerListener.ovsdbUpdate(node, ovsdbType, action);
        }
    }
}
