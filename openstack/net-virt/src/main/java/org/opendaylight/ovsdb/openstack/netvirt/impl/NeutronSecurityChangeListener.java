/*
 * Copyright (c) 2014, 2015 HP, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.neutron.spi.INeutronNetworkCRUD;
import org.opendaylight.neutron.spi.INeutronSecurityGroupCRUD;
import org.opendaylight.neutron.spi.NeutronNetwork;
import org.opendaylight.neutron.spi.NeutronSecurityGroup;
import org.opendaylight.neutron.spi.Neutron_IPs;
import org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.EgressAclProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.IngressAclProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.SecurityServicesManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev141002.ports.attributes.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev141002.ports.attributes.ports.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150325.Neutron;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev141002.security.groups.attributes.SecurityGroups;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev141002.security.rules.attributes.SecurityRules;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * MDSAL dataChangeListener for the Port, SecurityGroup and SecurityRule changes.
 *
 * @author Aswin Suryanarayanan (aswin.suryanarayanan@hpe.com)
 */

public class NeutronSecurityChangeListener implements DataChangeListener,
        AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbDataChangeListener.class);
    private DataBroker dataBroker = null;
    private ListenerRegistration<DataChangeListener> portRegistration;
    private ListenerRegistration<DataChangeListener> securityGroupRegistration;
    private ListenerRegistration<DataChangeListener> securityRuleRegistration;
    private ExecutorService executorService;
    private volatile Southbound southbound;
    private volatile INeutronNetworkCRUD neutronNetworkCache;
    private volatile ConfigurationService configurationService;
    private volatile IngressAclProvider ingressAclProvider;
    private volatile EgressAclProvider egressAclProvider;
    private volatile SecurityServicesManager securityServicesManager;
    private volatile INeutronSecurityGroupCRUD securityGroupCache = null;

    /**
     * Creates an instance of the NeutronSecurityChangeListener, it will listen for the datastore
     * passed along with the data broker.
     * @param db the data broker
     * @param executorService the executer service
     */
    public NeutronSecurityChangeListener(DataBroker db, ExecutorService executorService) {
        this.dataBroker = db;
        this.executorService = executorService;
        InstanceIdentifier<Ports> path = InstanceIdentifier
                .create(Neutron.class).child(Ports.class);
        LOG.trace("NeutronSecurityChangeListener: Register listener for Neutron Port model and aecurity "
                + "Group/rule model data changes");
        portRegistration =
                this.dataBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                                                           path, this, DataChangeScope.SUBTREE);
        InstanceIdentifier<SecurityGroups> securityGroupPath = InstanceIdentifier
                .create(Neutron.class).child(SecurityGroups.class);
        securityGroupRegistration = dataBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                                                                          securityGroupPath, this,
                DataChangeScope.SUBTREE);
        InstanceIdentifier<SecurityRules> securityRulePath = InstanceIdentifier
                .create(Neutron.class).child(SecurityRules.class);
        securityRuleRegistration = dataBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                                                                         securityRulePath, this,
                                                                         DataChangeScope.SUBTREE);
        southbound = (Southbound) ServiceHelper.getGlobalInstance(Southbound.class, this);
        neutronNetworkCache =
                (INeutronNetworkCRUD) ServiceHelper.getGlobalInstance(INeutronNetworkCRUD.class, this);
        configurationService =
                (ConfigurationService) ServiceHelper.getGlobalInstance(ConfigurationService.class, this);
        ingressAclProvider =
                (IngressAclProvider) ServiceHelper.getGlobalInstance(IngressAclProvider.class, this);
        egressAclProvider =
                (EgressAclProvider) ServiceHelper.getGlobalInstance(EgressAclProvider.class, this);
        securityServicesManager =
                (SecurityServicesManager) ServiceHelper.getGlobalInstance(SecurityServicesManager.class, this);
        securityGroupCache =
                (INeutronSecurityGroupCRUD) ServiceHelper.getGlobalInstance(INeutronSecurityGroupCRUD.class, this);
    }

    @Override
    public void close() throws Exception {
        portRegistration.close();
        securityGroupRegistration.close();
        securityRuleRegistration.close();
        executorService.shutdown();
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        executorService.submit(new Runnable(){
            @Override
            public void run() {
                LOG.trace(">>>>> onDataChanged: {}", change);
                processNeutronPort(change);
            }
        });
    }

    private void processNeutronPort(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        LOG.trace("processNeutronPort:" + changes);
        /**
         * Get updated data and original data for the the changed. Identify the security groups that got
         * added and removed and call the appropriate providers for updating the flows.
         */
        try {
            Map<String,Port> updatedPortMap = getChangedPorts(changes.getUpdatedData());
            Map<String,Port> originalPortMap = getChangedPorts(changes.getOriginalData());
            for (String portId : updatedPortMap.keySet()) {
                Port updatedPort = updatedPortMap.get(portId);
                Port originalPort = originalPortMap.get(portId);

                List<Uuid> addedGroup = getsecurityGroupChanged(updatedPort, originalPort);
                List<Uuid> deletedGroup = getsecurityGroupChanged(originalPort, updatedPort);

                if (null != addedGroup && !addedGroup.isEmpty()) {
                    syncSecurityGroup(updatedPort,addedGroup,true);
                }
                if (null != deletedGroup && !deletedGroup.isEmpty()) {
                    syncSecurityGroup(updatedPort,deletedGroup,false);
                }
            }
        } catch (Exception e) {
            LOG.error("Exception in processNeutronPort", e);
        }
    }

    private List<Uuid> getsecurityGroupChanged(Port port1, Port port2) {
        LOG.trace("getsecurityGroupChanged:" + "Port1:" + port1 + "Port2" + port2);
        ArrayList<Uuid> list1 = new ArrayList<Uuid>(port1.getSecurityGroups());
        ArrayList<Uuid> list2 = new ArrayList<Uuid>(port2.getSecurityGroups());
        list1.removeAll(list2);
        return list1;
    }

    private  Map<String,Port> getChangedPorts(Map<InstanceIdentifier<?>, DataObject> changedData) {
        LOG.trace("getChangedPorts:" + changedData);
        Map<String,Port> portMap = new HashMap<String,Port>();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> updated : changedData.entrySet()) {
            if (updated.getValue() instanceof Port) {
                Port port = getPort(changedData, updated);
                portMap.put(port.getUuid().getValue(), port);
            }
        }
        return portMap;
    }

    private void syncSecurityGroup(Port port, List<Uuid> securityGroupUuidList, boolean write) {
        LOG.trace("syncSecurityGroup:" + securityGroupUuidList + " Write:" + Boolean.valueOf(write));
        List<NeutronSecurityGroup> securityGroupList = getSecurityGroupList(securityGroupUuidList);
        if (null != port && null != port.getSecurityGroups()) {
            Node node = getNode(port);
            NeutronNetwork neutronNetwork = neutronNetworkCache.getNetwork(port.getNetworkId().getValue());
            String segmentationId = neutronNetwork.getProviderSegmentationID();
            OvsdbTerminationPointAugmentation intf = getInterface(node, port);
            long localPort = southbound.getOFPort(intf);
            String attachedMac = southbound.getInterfaceExternalIdsValue(intf, Constants.EXTERNAL_ID_VM_MAC);
            if (attachedMac == null) {
                LOG.debug("programVlanRules: No AttachedMac seen in {}", intf);
                return;
            }
            long dpid = getIntegrationBridgeOfDpid(node);
            List<Neutron_IPs> srcAddressList = securityServicesManager.getIpAddressList(node, intf);
            for (NeutronSecurityGroup securityGroupInPort:securityGroupList) {
                ingressAclProvider.programPortSecurityAcl(dpid, segmentationId, attachedMac, localPort,
                                                          securityGroupInPort,srcAddressList, write);
                egressAclProvider.programPortSecurityAcl(dpid, segmentationId, attachedMac, localPort,
                                                         securityGroupInPort,srcAddressList, write);
            }
        }
    }

    private  List<NeutronSecurityGroup> getSecurityGroupList( List<Uuid> securityGroupUuidList) {
        LOG.trace("getSecurityGroupList:UUID List" + securityGroupUuidList);
        List<NeutronSecurityGroup> securityGroupList = new ArrayList<NeutronSecurityGroup>();
        for (Uuid uuid :securityGroupUuidList) {
            NeutronSecurityGroup securityGroup = securityGroupCache
                    .getNeutronSecurityGroup(uuid.getValue());
            if (null != securityGroup) {
                securityGroupList.add(securityGroup);
            }
        }
        return securityGroupList;
    }

    private long getDpid(Node node) {
        LOG.trace("getDpid" + node);
        long dpid = southbound.getDataPathId(node);
        if (dpid == 0) {
            LOG.warn("getDpid: dpid not found: {}", node);
        }
        return dpid;
    }

    private long getIntegrationBridgeOfDpid(Node node) {
        LOG.trace("getIntegrationBridgeOfDpid:" + node);
        long dpid = 0L;
        if (southbound.getBridgeName(node).equals(configurationService.getIntegrationBridgeName())) {
            dpid = getDpid(node);
        }
        return dpid;
    }

    private Port getPort(Map<InstanceIdentifier<?>, DataObject> changes,
                         Map.Entry<InstanceIdentifier<?>, DataObject> change) {
        InstanceIdentifier<Port> nodeInstanceIdentifier = change.getKey().firstIdentifierOf(Port.class);
        return (Port)changes.get(nodeInstanceIdentifier);
    }

    private Node getNode(Port port) {
        LOG.trace("getNode" + port);
        List<Node> toplogyNodes = southbound.readOvsdbTopologyNodes();

        for (Node topologyNode : toplogyNodes) {
            try {
                Node node = southbound.getBridgeNode(topologyNode,Constants.INTEGRATION_BRIDGE);
                List<OvsdbTerminationPointAugmentation> ovsdbPorts = southbound.getTerminationPointsOfBridge(node);
                for (OvsdbTerminationPointAugmentation ovsdbPort : ovsdbPorts) {
                    String uuid = southbound.getInterfaceExternalIdsValue(ovsdbPort,
                                                            Constants.EXTERNAL_ID_INTERFACE_ID);
                    if (null != uuid && uuid.equals(port.getUuid().getValue())) {
                        return node;
                    }
                }
            } catch (Exception e) {
                LOG.error("Exception during handlingNeutron network delete", e);
            }
        }
        return null;
    }

    private OvsdbTerminationPointAugmentation getInterface(Node node, Port port) {
        LOG.trace("getInterface:Node:" + node + " Port:" + port);
        try {
            List<OvsdbTerminationPointAugmentation> ovsdbPorts = southbound.getTerminationPointsOfBridge(node);
            for (OvsdbTerminationPointAugmentation ovsdbPort : ovsdbPorts) {
                String uuid = southbound.getInterfaceExternalIdsValue(ovsdbPort,
                                                                      Constants.EXTERNAL_ID_INTERFACE_ID);
                if (null != uuid && uuid.equals(port.getUuid().getValue())) {
                    return ovsdbPort;
                }
            }
        } catch (Exception e) {
            LOG.error("Exception during handlingNeutron network delete", e);
        }
        return null;
    }
}
