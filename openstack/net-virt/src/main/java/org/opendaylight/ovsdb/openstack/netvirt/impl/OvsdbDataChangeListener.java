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
import org.opendaylight.ovsdb.openstack.netvirt.api.OvsdbInventoryListener;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
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

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        LOG.debug(">>>>> onDataChanged: {}", changes);
        //TODO SB_MIGRATION: off load this process to execution service, blocking md-sal notification thread
        // has performance impact on overall controller performance. With new notification broker
        //it might create weird issues.
        processOvsdbConnections(changes);
        processOvsdbConnectionAttributeUpdates(changes);
        processBridgeCreation(changes);
        processBridgeUpdate(changes);
        processPortCreation(changes);
        processPortUpdate(changes);
        processPortDeletion(changes);
        processBridgeDeletion(changes);
        processOvsdbDisconnect(changes);
    }

    private void processOvsdbConnections(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        for (Map.Entry<InstanceIdentifier<?>, DataObject> created : changes.getCreatedData().entrySet()) {
            if (created.getValue() instanceof OvsdbNodeAugmentation) {
                Node ovsdbNode = getNode(changes.getCreatedData(), created);
                LOG.info("Processing ovsdb connections : {}, ovsdbNode: {}", created, ovsdbNode);
                ovsdbUpdate(ovsdbNode, created.getValue(), OvsdbInventoryListener.OvsdbType.NODE, Action.ADD);
            }
        }
    }

    private void processOvsdbDisconnect(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {

        for(InstanceIdentifier<?> removedOvsdbNode : changes.getRemovedPaths()) {
            if(removedOvsdbNode.getTargetType().equals(OvsdbNodeAugmentation.class)){
                //Get top node to get details of all the bridge/termination point augmentation
                // in case we want to do any cleanup task while processing node disconnection
                Node parentNode = getNode(changes.getOriginalData(), removedOvsdbNode);
                if(parentNode == null){
                    //Throwing this warning in case behavior of southbound plugin changes.
                    LOG.warn("OvsdbNode's {} parent node details are not present in original data,"
                            + " it should not happen", parentNode);
                    continue;
                }
                //Fetch data of removed connection info from original data
                @SuppressWarnings("unchecked")
                OvsdbNodeAugmentation removedOvsdbNodeAugmentationData = getDataChanges(changes.getOriginalData(),
                        (InstanceIdentifier<OvsdbNodeAugmentation>)removedOvsdbNode);

                LOG.debug("Process ovsdb node delete : {} ", removedOvsdbNode);
                ////Assuming Openvswitch type represent the ovsdb node connection and not OvsdbType.NODE

                ovsdbUpdate(parentNode, removedOvsdbNodeAugmentationData,
                        OvsdbInventoryListener.OvsdbType.NODE, Action.DELETE);
            }
        }
    }

    private void processOvsdbConnectionAttributeUpdates(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {

        for(Map.Entry<InstanceIdentifier<?>, DataObject> updatedOvsdbNode : changes.getUpdatedData().entrySet()){
            if(updatedOvsdbNode.getKey().getTargetType().equals(OvsdbNodeAugmentation.class)){
                LOG.info("Processing Ovsdb Node attributes update : {}",updatedOvsdbNode);
                /* XXX (NOTE): Till now we don't really need the old ovsdb connection attributes data before update.
                 * I am passing the updated data of both Node and resource augmentation data (connection attributes).
                 * If in future we need old OvsdbNodeAugmentation attributes data, we will extract it from
                 * original data and pass it as a resourceAugmentationData.
                 */
                Node parentNode  = getNode(changes.getUpdatedData(),updatedOvsdbNode);
                if(parentNode == null){
                    // Logging this warning, to catch any change in southbound plugin's behavior.
                    LOG.warn("Parent Node for OvsdbNodeAugmentation is not found. On OvsdbNodeAugmentation update "
                            + "data store must provide the parent node update. This condition should not occur "
                            + "with the existing models defined in southbound plugin." );
                    continue;
                }
                LOG.debug("Process ovsdb connection  {} related update on Node : {}",
                        updatedOvsdbNode.getValue(), parentNode);

                ovsdbUpdate(parentNode, updatedOvsdbNode.getValue(),
                        OvsdbInventoryListener.OvsdbType.NODE, Action.UPDATE);
            }
        }
    }

    private void processPortCreation(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        for(Map.Entry<InstanceIdentifier<?>, DataObject> newPort : changes.getCreatedData().entrySet()){
            if(newPort.getKey().getTargetType().equals(OvsdbTerminationPointAugmentation.class)){
                LOG.info("Processing creation of new port : {}",newPort);
                //If user created termination point only, Node will get updated
                Node tpParentNode  = getNode(changes.getUpdatedData(), newPort);
                if(tpParentNode == null){
                    //If user created port with the bridge itself, Node will be in created data
                    tpParentNode = getNode(changes.getCreatedData(),newPort);
                }
                if(tpParentNode == null){
                    // Logging this warning, to make sure we didn't change anything
                    // in southbound plugin that changes this behavior.
                    LOG.warn("Parent Node for port is not found. Port creation must create or "
                            + "update the Node. This condition should not occure" );
                    continue;
                }

                LOG.debug("Process new port {} creation on Node : {}", newPort.getValue(),tpParentNode);
                ovsdbUpdate(tpParentNode, newPort.getValue(),OvsdbInventoryListener.OvsdbType.PORT, Action.ADD);
            }
        }
    }

    private void processPortDeletion(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {

        for(InstanceIdentifier<?> removedPort : changes.getRemovedPaths()) {
            if(removedPort.getTargetType().equals(OvsdbTerminationPointAugmentation.class)){
                Node tpParentNode = getNode(changes.getOriginalData(), removedPort);
                if(tpParentNode == null){
                    //Throwing this warning in case behavior of southbound plugin changes.
                    LOG.warn("Port's {} parent node details are not present in original data, "
                            + "it should not happen", removedPort);
                    continue;
                }
                //Fetch data of removed port from original data
                @SuppressWarnings("unchecked")
                OvsdbTerminationPointAugmentation removedTPAugmentationData = getDataChanges(changes.getOriginalData(),
                        (InstanceIdentifier<OvsdbTerminationPointAugmentation>)removedPort);

                LOG.debug("Process port {} deletion on Node : {}", removedPort,tpParentNode);
                ovsdbUpdate(tpParentNode, removedTPAugmentationData,
                        OvsdbInventoryListener.OvsdbType.PORT, Action.DELETE);

            }
        }
    }

    private void processPortUpdate(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {

        for(Map.Entry<InstanceIdentifier<?>, DataObject> updatedPort : changes.getUpdatedData().entrySet()){
            if(updatedPort.getKey().getTargetType().equals(OvsdbTerminationPointAugmentation.class)){
                LOG.info("Processing port update : {}",updatedPort);
                /* XXX (NOTE): Till now we don't really need the old termination point data before update.
                 * I am passing the updated data of both Node and resource augmentation data (termination-point).
                 * If in future we need old TerminationPointAugmentation data, we will extract it from
                 * original data and pass it as a resourceAugmentationData.
                 */
                Node tpParentNode  = getNode(changes.getUpdatedData(),updatedPort);
                if(tpParentNode == null){
                    // Logging this warning, to catch any change in southbound plugin's behavior.
                    LOG.warn("Parent Node for port is not found. On Port/Interface update data store"
                            + " must provide the parent node update. This condition should not occure "
                            + "with the existing models define in southbound plugin." );
                    continue;
                }

                LOG.debug("Process port's {} update on Node : {}", updatedPort.getValue(),tpParentNode);
                ovsdbUpdate(tpParentNode, updatedPort.getValue(),
                        OvsdbInventoryListener.OvsdbType.PORT, Action.UPDATE);
            }
        }
    }

    private void processBridgeCreation(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {

        for(Map.Entry<InstanceIdentifier<?>, DataObject> newBridge : changes.getCreatedData().entrySet()){
            if(newBridge.getKey().getTargetType().equals(OvsdbBridgeAugmentation.class)){
                LOG.info("Processing creation of new bridge : {}",newBridge);
                //Bridge augmentation happens directly on the Node so Node details should also exist in created data.
                Node bridgeParentNode  = getNode(changes.getCreatedData(),newBridge);
                if(bridgeParentNode == null){
                    // Logging this warning, to catch any change in southbound plugin behavior
                    LOG.warn("Parent Node for bridge is not found. Bridge creation must provide the Node "
                            + "details in create Data Changes. This condition should not occure" );
                    continue;
                }
                LOG.debug("Process new bridge {} creation on Node : {}", newBridge.getValue(),bridgeParentNode);
                ovsdbUpdate(bridgeParentNode, newBridge.getValue(),
                        OvsdbInventoryListener.OvsdbType.BRIDGE, Action.ADD);
            }
        }
    }

    private void processBridgeUpdate(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {

        for (Map.Entry<InstanceIdentifier<?>, DataObject> updatedBridge : changes.getUpdatedData().entrySet()) {
            if(updatedBridge.getKey().getTargetType().equals(OvsdbBridgeAugmentation.class)){
                LOG.info("Processing update on a bridge : {}",updatedBridge);
                /* XXX (NOTE): Till now we don't really need the old bridge data before update.
                 * I am passing the updated data of both Node and resource augmentation data.
                 * If in future we need old bridgeAugmentationData, we will extract it from
                 * original data and pass it as a resourceAugmentationData.
                 */

                Node bridgeParentNode = getNode(changes.getUpdatedData(), updatedBridge);
                if(bridgeParentNode == null){
                    // Logging this warning, to catch any change in southbound plugin behavior
                    LOG.warn("Parent Node for bridge is not found. Bridge update must provide the Node "
                            + "details in updated Data Changes. This condition should not occure" );
                    continue;
                }
                LOG.debug("Process bridge {} update on Node : {}", updatedBridge.getValue(),bridgeParentNode);
                ovsdbUpdate(bridgeParentNode, updatedBridge.getValue(),
                        OvsdbInventoryListener.OvsdbType.BRIDGE, Action.UPDATE);
            }
        }
    }

    private void processBridgeDeletion(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {

        for(InstanceIdentifier<?> removedBridge : changes.getRemovedPaths()) {
            if(removedBridge.getTargetType().equals(OvsdbBridgeAugmentation.class)){
                Node bridgeParentNode = getNode(changes.getOriginalData(), removedBridge);
                if(bridgeParentNode == null){
                    //Throwing this warning to catch the behavior change of southbound plugin.
                    LOG.warn("Bridge's {} parent node details are not present in original data"
                            + ", it should not happen", removedBridge);
                    continue;
                }
                //Fetch data of removed bridge from original data
                @SuppressWarnings("unchecked")
                OvsdbBridgeAugmentation removedBridgeAugmentationData = getDataChanges(changes.getOriginalData(),
                        (InstanceIdentifier<OvsdbBridgeAugmentation>) removedBridge);

                LOG.debug("Process bridge {} deletion on Node : {}", removedBridge,bridgeParentNode);
                ovsdbUpdate(bridgeParentNode, removedBridgeAugmentationData,
                        OvsdbInventoryListener.OvsdbType.BRIDGE, Action.DELETE);
            }
        }
    }

    private Node getNode(Map<InstanceIdentifier<?>, DataObject> changes,
                         Map.Entry<InstanceIdentifier<?>, DataObject> change) {
        InstanceIdentifier<Node> nodeInstanceIdentifier = change.getKey().firstIdentifierOf(Node.class);
        return (Node)changes.get(nodeInstanceIdentifier);
    }

    private Node getNode(Map<InstanceIdentifier<?>, DataObject> changes,InstanceIdentifier<?> path) {
        InstanceIdentifier<Node> nodeInstanceIdentifier = path.firstIdentifierOf(Node.class);
        return (Node)changes.get(nodeInstanceIdentifier);
    }

    private <T extends DataObject> T getDataChanges(
            Map<InstanceIdentifier<?>, DataObject> changes,InstanceIdentifier<T> path){

        for(Map.Entry<InstanceIdentifier<?>,DataObject> change : changes.entrySet()){
            if(change.getKey().getTargetType().equals(path.getTargetType())){
                @SuppressWarnings("unchecked")
                T dataObject = (T) change.getValue();
                return dataObject;
            }
        }
        return null;
    }

    private void ovsdbUpdate(Node node, DataObject resourceAugmentationDataChanges,
            OvsdbInventoryListener.OvsdbType ovsdbType, Action action) {

        Set<OvsdbInventoryListener> mdsalConsumerListeners = OvsdbInventoryServiceImpl.getMdsalConsumerListeners();
        for (OvsdbInventoryListener mdsalConsumerListener : mdsalConsumerListeners) {
            mdsalConsumerListener.ovsdbUpdate(node, resourceAugmentationDataChanges, ovsdbType, action);
        }
    }
}
