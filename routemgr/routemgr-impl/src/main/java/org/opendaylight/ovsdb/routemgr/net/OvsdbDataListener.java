/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.routemgr.net;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;

import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OvsdbDataListener implements DataChangeListener {
    public static final TopologyId OVSDB_TOPOLOGY_ID = new TopologyId(new Uri("ovsdb:1"));
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbDataListener.class);
    private final DataBroker dataService;

    //private static final String CURRTOPO = "router:1";
    private ListenerRegistration<DataChangeListener> registration;

    // Interface Manager
    private IfMgr ifMgr;

    public OvsdbDataListener(DataBroker dataBroker) {
        this.dataService = dataBroker;
    }

    public void registerDataChangeListener() {
        LOG.debug("registering OVSDB listener");
        InstanceIdentifier<Node> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(OVSDB_TOPOLOGY_ID))
                .child(Node.class);
        registration = dataService.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, path, this,
                DataChangeScope.SUBTREE);
        LOG.info("RouteMgr OvsdbDataListener: dataBroker= {}, registration= {}",
                dataService, registration);
        triggerUpdates();
        LOG.info("registered");
        return;
    }

    public void closeListeners() throws Exception  {
        registration.close();
        return;
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent) {
        if (dataChangeEvent == null) {
            return;
        }
        processPortCreation(dataChangeEvent);
        processPortUpdate(dataChangeEvent);
        processPortDeletion(dataChangeEvent);
    }

    public void triggerUpdates() {
        // TODO:: Need to perform initial data store read
    }

    private void processPortCreation(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        OvsdbTerminationPointAugmentation intf;
        OvsdbBridgeAugmentation           ovsdbBridgeAugmentation;
        String                            dpId;
        Uuid                              interfaceUuid;
        Long                              ofPort = 0L;

        for (Map.Entry<InstanceIdentifier<?>, DataObject> newPort : changes.getCreatedData().entrySet()) {
            if (newPort.getKey().getTargetType().equals(OvsdbTerminationPointAugmentation.class)) {
                //LOG.trace("processPortCreation: {}", newPort);
                //If user created termination point only, Node will get updated
                Node tpParentNode  = getNode(changes.getUpdatedData(), newPort);
                if (tpParentNode == null) {
                    //If user created port with the bridge itself, Node will be in created data
                    tpParentNode = getNode(changes.getCreatedData(),newPort);
                }
                if (tpParentNode == null) {
                    // Logging this warning, to make sure we didn't change anything
                    // in southbound plugin that changes this behavior.
                    LOG.warn("Parent Node for port is not found. Port creation must create or "
                            + "update the Node. This condition should not occur." );
                    continue;
                }

                LOG.trace("processPortCreation <{}> creation on Node <{}>", newPort.getValue(), tpParentNode);

                //ovsdbUpdate(tpParentNode, newPort.getValue(),OvsdbInventoryListener.OvsdbType.PORT, Action.ADD);

                intf = (OvsdbTerminationPointAugmentation) newPort.getValue();
                interfaceUuid = intf.getInterfaceUuid();
                ovsdbBridgeAugmentation = tpParentNode.getAugmentation(OvsdbBridgeAugmentation.class);
                if (ovsdbBridgeAugmentation != null && ovsdbBridgeAugmentation.getDatapathId() != null) {
                    dpId = tpParentNode.getAugmentation(OvsdbBridgeAugmentation.class).getDatapathId().getValue();
                    if (intf.getOfport() != null) {
                        ofPort = intf.getOfport();
                        ifMgr.updateInterface(interfaceUuid, dpId, ofPort);
                    }
                }
            }
        }
    }

    private void processPortDeletion(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        OvsdbBridgeAugmentation           ovsdbBridgeAugmentation;
        String                            dpId;
        Uuid                              interfaceUuid;

        for (InstanceIdentifier<?> removedPort : changes.getRemovedPaths()) {
            if (removedPort.getTargetType().equals(OvsdbTerminationPointAugmentation.class)) {
                Node tpParentNode = getNode(changes.getOriginalData(), removedPort);
                if (tpParentNode == null) {
                    //Throwing this warning in case behavior of southbound plugin changes.
                    LOG.warn("Port's {} parent node details are not present in original data, "
                            + "it should not happen", removedPort);
                    continue;
                }
                //Fetch data of removed port from original data
                @SuppressWarnings("unchecked")
                OvsdbTerminationPointAugmentation removedTPAugmentationData = getDataChanges(changes.getOriginalData(),
                        (InstanceIdentifier<OvsdbTerminationPointAugmentation>)removedPort);

                LOG.trace("processPortDeletion <{}> deletion on Node <{}>", removedPort, tpParentNode);
                //ovsdbUpdate(tpParentNode, removedTPAugmentationData,
                //        OvsdbInventoryListener.OvsdbType.PORT, Action.DELETE);

                interfaceUuid = removedTPAugmentationData.getInterfaceUuid();
                ovsdbBridgeAugmentation = tpParentNode.getAugmentation(OvsdbBridgeAugmentation.class);
                if (ovsdbBridgeAugmentation != null && ovsdbBridgeAugmentation.getDatapathId() != null) {
                    dpId = tpParentNode.getAugmentation(OvsdbBridgeAugmentation.class).getDatapathId().getValue();
                    ifMgr.deleteInterface(interfaceUuid, dpId);
                }
            }
        }
    }

    private void processPortUpdate(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        OvsdbTerminationPointAugmentation intf;
        OvsdbBridgeAugmentation           ovsdbBridgeAugmentation;
        String                            dpId;
        Uuid                              interfaceUuid;
        Long                              ofPort = 0L;

        for (Map.Entry<InstanceIdentifier<?>, DataObject> updatedPort : changes.getUpdatedData().entrySet()) {
            if (updatedPort.getKey().getTargetType().equals(OvsdbTerminationPointAugmentation.class)) {
                //LOG.trace("processPortUpdate: <{}>", updatedPort);
                /* XXX (NOTE): Till now we don't really need the old termination point data before update.
                 * I am passing the updated data of both Node and resource augmentation data (termination-point).
                 * If in future we need old TerminationPointAugmentation data, we will extract it from
                 * original data and pass it as a resourceAugmentationData.
                 */
                Node tpParentNode  = getNode(changes.getUpdatedData(), updatedPort);
                if (tpParentNode == null) {
                    // Logging this warning, to catch any change in southbound plugin's behavior.
                    LOG.warn("Parent Node for port is not found. On Port/Interface update data store"
                            + " must provide the parent node update. This condition should not occure "
                            + "with the existing models define in southbound plugin." );
                    continue;
                }

                LOG.trace("processPortUpdate <{}> update on Node <{}>", updatedPort.getValue(), tpParentNode);
                //ovsdbUpdate(tpParentNode, updatedPort.getValue(),
                //       OvsdbInventoryListener.OvsdbType.PORT, Action.UPDATE);

                intf = (OvsdbTerminationPointAugmentation) updatedPort.getValue();
                interfaceUuid = intf.getInterfaceUuid();
                ovsdbBridgeAugmentation = tpParentNode.getAugmentation(OvsdbBridgeAugmentation.class);
                if (ovsdbBridgeAugmentation != null && ovsdbBridgeAugmentation.getDatapathId() != null) {
                    dpId = tpParentNode.getAugmentation(OvsdbBridgeAugmentation.class).getDatapathId().getValue();
                    if (intf.getOfport() != null) {
                        ofPort = intf.getOfport();
                        ifMgr.updateInterface(interfaceUuid, dpId, ofPort);
                    }
                }
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
            Map<InstanceIdentifier<?>, DataObject> changes,InstanceIdentifier<T> path) {

        for (Map.Entry<InstanceIdentifier<?>,DataObject> change : changes.entrySet()) {
            if (change.getKey().getTargetType().equals(path.getTargetType())) {
                @SuppressWarnings("unchecked")
                T dataObject = (T) change.getValue();
                return dataObject;
            }
        }
        return null;
    }
}
