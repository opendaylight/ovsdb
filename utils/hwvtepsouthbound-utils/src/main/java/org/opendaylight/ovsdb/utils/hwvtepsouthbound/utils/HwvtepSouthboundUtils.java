/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.utils.hwvtepsouthbound.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.EncapsulationTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepLogicalSwitchRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalPortAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalPortAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitchesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitchesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.VlanBindings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.VlanBindingsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.VlanBindingsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class HwvtepSouthboundUtils {
    private static final Logger LOG = LoggerFactory.getLogger(HwvtepSouthboundUtils.class);
    private static final int HWVTEP_UPDATE_TIMEOUT = 1000;
    private final MdsalUtils mdsalUtils;

    public HwvtepSouthboundUtils(MdsalUtils mdsalUtils) {
        this.mdsalUtils = mdsalUtils;
    }

    public static NodeId createNodeId(IpAddress ip, PortNumber port) {
        String uriString = HwvtepSouthboundConstants.HWVTEP_URI_PREFIX + "://"
                + String.valueOf(ip.getValue()) + ":" + port.getValue();
        Uri uri = new Uri(uriString);
        return new NodeId(uri);
    }

    public static Node createNode(ConnectionInfo key) {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(createNodeId(key.getRemoteIp(), key.getRemotePort()));
        nodeBuilder.addAugmentation(HwvtepGlobalAugmentation.class, createHwvtepAugmentation(key));
        return nodeBuilder.build();
    }

    public static HwvtepGlobalAugmentation createHwvtepAugmentation(ConnectionInfo key) {
        HwvtepGlobalAugmentationBuilder hwvtepGlobalBuilder = new HwvtepGlobalAugmentationBuilder();
        hwvtepGlobalBuilder.setConnectionInfo(key);
        return hwvtepGlobalBuilder.build();
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(ConnectionInfo key) {
        return createInstanceIdentifier(key.getRemoteIp(), key.getRemotePort());
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(IpAddress ip, PortNumber port) {
        InstanceIdentifier<Node> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                .child(Node.class,createNodeKey(ip,port));
        LOG.debug("Created hwvtep path: {}",path);
        return path;
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(ConnectionInfo key, HwvtepNodeName name) {
        return HwvtepSouthboundMapper.createInstanceIdentifier(
                        createManagedNodeId(key, name));
    }

    private static NodeId createManagedNodeId(ConnectionInfo key, HwvtepNodeName nodeName) {
        return createManagedNodeId(key.getRemoteIp(), key.getRemotePort(), nodeName);
    }

    private static NodeId createManagedNodeId(IpAddress remoteIp, PortNumber remotePort, HwvtepNodeName nodeName) {
        //This assumes that HwvtepNode can only be Physical switch
        return new NodeId(createNodeId(remoteIp,remotePort).getValue()
                        + "/" + HwvtepSouthboundConstants.PSWITCH_URI_PREFIX + "/" + nodeName.getValue());
    }

    public static NodeKey createNodeKey(IpAddress ip, PortNumber port) {
        return new NodeKey(createNodeId(ip, port));
    }

    public static Object connectionInfoToString(ConnectionInfo connectionInfo) {
        return String.valueOf(
                        connectionInfo.getRemoteIp().getValue()) + ":" + connectionInfo.getRemotePort().getValue();
    }

    /**
     * Creates the hwvtep topology instance identifier.
     *
     * @return the instance identifier
     */
    public static InstanceIdentifier<Topology> createHwvtepTopologyInstanceIdentifier() {
        return InstanceIdentifier.create(NetworkTopology.class).child(Topology.class,
                new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID));
    }

    /**
     * Creates the instance identifier.
     *
     * @param nodeId
     *            the node id
     * @return the instance identifier
     */
    public static InstanceIdentifier<Node> createInstanceIdentifier(NodeId nodeId) {
        return InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(nodeId));
    }

    /**
     * Creates the logical switches instance identifier.
     *
     * @param nodeId
     *            the node id
     * @param hwvtepNodeName
     *            the hwvtep node name
     * @return the instance identifier
     */
    public static InstanceIdentifier<LogicalSwitches> createLogicalSwitchesInstanceIdentifier(NodeId nodeId,
            HwvtepNodeName hwvtepNodeName) {
        return createInstanceIdentifier(nodeId).augmentation(HwvtepGlobalAugmentation.class)
                .child(LogicalSwitches.class, new LogicalSwitchesKey(hwvtepNodeName));
    }

    /**
     * Creates the remote ucast macs instance identifier.
     *
     * @param nodeId
     *            the node id
     * @param mac
     *            the mac
     * @return the instance identifier
     */
    public static InstanceIdentifier<RemoteUcastMacs> createRemoteUcastMacsInstanceIdentifier(NodeId nodeId,
            MacAddress mac) {
        return createInstanceIdentifier(nodeId).augmentation(HwvtepGlobalAugmentation.class)
                .child(RemoteUcastMacs.class, new RemoteUcastMacsKey(mac));
    }

    /**
     * Creates the physical locator instance identifier.
     *
     * @param nodeId
     *            the node id
     * @param physicalLocatorAug
     *            the physical locator aug
     * @return the instance identifier
     */
    public static InstanceIdentifier<TerminationPoint> createPhysicalLocatorInstanceIdentifier(NodeId nodeId,
            HwvtepPhysicalLocatorAugmentation physicalLocatorAug) {
        return createInstanceIdentifier(nodeId).child(TerminationPoint.class,
                getTerminationPointKey(physicalLocatorAug));
    }

    /**
     * Creates the physical port instance identifier.
     *
     * @param nodeId
     *            the node id
     * @param phyPortName
     *            the phy port name
     * @return the instance identifier
     */
    public static InstanceIdentifier<TerminationPoint> createPhysicalPortInstanceIdentifier(NodeId nodeId,
            String phyPortName) {
        return createInstanceIdentifier(nodeId).child(TerminationPoint.class,
                new TerminationPointKey(new TpId(phyPortName)));
    }

    /**
     * Gets the termination point key.
     *
     * @param pLoc
     *            the loc
     * @return the termination point key
     */
    public static TerminationPointKey getTerminationPointKey(HwvtepPhysicalLocatorAugmentation pLoc) {
        TerminationPointKey tpKey = null;
        if (pLoc.getEncapsulationType() != null && pLoc.getDstIp() != null) {
            String encapType = HwvtepSouthboundConstants.ENCAPS_TYPE_MAP.get(pLoc.getEncapsulationType());
            String tpKeyStr = new StringBuilder(encapType).append(':')
                    .append(String.valueOf(pLoc.getDstIp().getValue())).toString();
            tpKey = new TerminationPointKey(new TpId(tpKeyStr));
        }
        return tpKey;
    }

    /**
     * Creates the managed node id.
     *
     * @param nodeId
     *            the node id
     * @param physicalSwitchName
     *            the physical switch name
     * @return the node id
     */
    public static NodeId createManagedNodeId(NodeId nodeId, String physicalSwitchName) {
        String phySwitchNodeId = new StringBuilder(nodeId.getValue()).append("/")
                .append(HwvtepSouthboundConstants.PSWITCH_URI_PREFIX).append("/").append(physicalSwitchName).toString();
        return new NodeId(phySwitchNodeId);
    }

    /**
     * Construct logical switch.
     *
     * @param name
     *            the name
     * @param desc
     *            the desc
     * @param tunnelKey
     *            the tunnel key
     * @return the logical switches
     */
    public static LogicalSwitches constructLogicalSwitch(String name, String desc, String tunnelKey) {
        HwvtepNodeName hwvtepName = new HwvtepNodeName(name);
        LogicalSwitchesBuilder lsBuilder = new LogicalSwitchesBuilder().setHwvtepNodeDescription(desc)
                .setHwvtepNodeName(hwvtepName).setKey(new LogicalSwitchesKey(hwvtepName)).setTunnelKey(tunnelKey);
        return lsBuilder.build();
    }

    /**
     * Construct hwvtep physical locator augmentation.
     *
     * @param ipAddress
     *            the ip address
     * @return the hwvtep physical locator augmentation
     */
    public static HwvtepPhysicalLocatorAugmentation constructHwvtepPhysicalLocatorAugmentation(String ipAddress) {
        // FIXME: Get encapsulation type dynamically
        Class<? extends EncapsulationTypeBase> encapTypeClass = HwvtepSouthboundMapper
                .createEncapsulationType(StringUtils.EMPTY);
        HwvtepPhysicalLocatorAugmentationBuilder phyLocBuilder = new HwvtepPhysicalLocatorAugmentationBuilder()
                .setEncapsulationType(encapTypeClass).setDstIp(new IpAddress(ipAddress.toCharArray()));
        return phyLocBuilder.build();
    }

    /**
     * Construct remote ucast mac.
     *
     * @param nodeId
     *            the node id
     * @param mac
     *            the mac
     * @param ipAddress
     *            the ip address
     * @param logicalSwitch
     *            the logical switch
     * @param physicalLocatorAug
     *            the physical locator aug
     * @return the remote ucast macs
     */
    public static RemoteUcastMacs constructRemoteUcastMac(NodeId nodeId, String mac, IpAddress ipAddress,
            LogicalSwitches logicalSwitch, HwvtepPhysicalLocatorAugmentation physicalLocatorAug) {
        HwvtepLogicalSwitchRef lsRef = new HwvtepLogicalSwitchRef(
                createLogicalSwitchesInstanceIdentifier(nodeId, logicalSwitch.getHwvtepNodeName()));
        HwvtepPhysicalLocatorRef phyLocRef = new HwvtepPhysicalLocatorRef(
                createPhysicalLocatorInstanceIdentifier(nodeId, physicalLocatorAug));

        RemoteUcastMacs remoteUcastMacs = new RemoteUcastMacsBuilder().setMacEntryKey(new MacAddress(mac))
                .setIpaddr(ipAddress).setLogicalSwitchRef(lsRef).setLocatorRef(phyLocRef).build();
        return remoteUcastMacs;
    }

    /**
     * Adds the logical switch.
     *
     * @param nodeId
     *            the node id
     * @param logicalSwitch
     *            the logical switch
     */
    public void addLogicalSwitch(NodeId nodeId, LogicalSwitches logicalSwitch) {
        final InstanceIdentifier<LogicalSwitches> iid = createLogicalSwitchesInstanceIdentifier(nodeId,
                logicalSwitch.getHwvtepNodeName());
        this.mdsalUtils.put(LogicalDatastoreType.CONFIGURATION, iid, logicalSwitch);
    }

    /**
     * Adds the logical switches.
     *
     * @param nodeId
     *            the node id
     * @param lstSwitches
     *            the lst switches
     */
    public void addLogicalSwitches(NodeId nodeId, List<LogicalSwitches> lstSwitches) {
        HwvtepGlobalAugmentationBuilder hgAugmentationBuilder = new HwvtepGlobalAugmentationBuilder()
                .setLogicalSwitches(lstSwitches);
        updateHwvtepGlobalConfig(nodeId, hgAugmentationBuilder.build());
    }

    /**
     * Gets the logical switch.
     *
     * @param nodeId
     *            the node id
     * @param logicalSwitchName
     *            the logical switch name
     * @return the logical switch
     */
    public LogicalSwitches getLogicalSwitch(NodeId nodeId, String logicalSwitchName) {
        final InstanceIdentifier<LogicalSwitches> iid = createLogicalSwitchesInstanceIdentifier(nodeId,
                new HwvtepNodeName(logicalSwitchName));
        return this.mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, iid);
    }

    /**
     * Delete logical switch.
     *
     * @param nodeId
     *            the node id
     * @param logicalSwitchName
     *            the logical switch name
     */
    public void deleteLogicalSwitch(NodeId nodeId, String logicalSwitchName) {
        this.mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION,
                createLogicalSwitchesInstanceIdentifier(nodeId, new HwvtepNodeName(logicalSwitchName)));
    }

    /**
     * Update hwvtep global config.
     *
     * @param nodeId
     *            the node id
     * @param hwvtepGlobalAugmentation
     *            the hwvtep global augmentation
     */
    public void updateHwvtepGlobalConfig(NodeId nodeId, HwvtepGlobalAugmentation hwvtepGlobalAugmentation) {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(nodeId);
        nodeBuilder.addAugmentation(HwvtepGlobalAugmentation.class, hwvtepGlobalAugmentation);

        updateHwvtepNode(nodeBuilder.build());
    }

    /**
     * Update hwvtep node.
     *
     * @param node
     *            the node
     */
    public void updateHwvtepNode(Node node) {
        final InstanceIdentifier<Node> iid = createInstanceIdentifier(node.getNodeId());
        this.mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION, iid, node);
    }

    /**
     * Adds the remote ucast macs. Assumes the referenced logical switch and
     * physical switch entry already exists. The operations fails if the references
     * are not found.
     *
     * @param nodeId
     *            the node id
     * @param lstRemoteUcastMacs
     *            the lst remote ucast macs
     */
    public void addRemoteUcastMacs(NodeId nodeId, List<RemoteUcastMacs> lstRemoteUcastMacs) {
        addRemoteUcastMacs(nodeId, lstRemoteUcastMacs, null, null);
    }

    /**
     * Adds the remote ucast macs. Adds the referenced logical switch and
     * physical switch entry if it doesn't exists.
     *
     * @param nodeId
     *            the node id
     * @param lstRemoteUcastMacs
     *            the lst remote ucast macs
     * @param logicalSwitch
     *            the logical switch
     * @param physicalLocatorAug
     *            the physical locator aug
     */
    public void addRemoteUcastMacs(NodeId nodeId, List<RemoteUcastMacs> lstRemoteUcastMacs,
            LogicalSwitches logicalSwitch, HwvtepPhysicalLocatorAugmentation physicalLocatorAug) {
        HwvtepGlobalAugmentationBuilder hgGlobalAugBuilder = new HwvtepGlobalAugmentationBuilder();
        hgGlobalAugBuilder.setRemoteUcastMacs(lstRemoteUcastMacs);
        if (logicalSwitch != null) {
            hgGlobalAugBuilder.setLogicalSwitches(Lists.newArrayList(logicalSwitch));
        }

        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(nodeId);
        nodeBuilder.addAugmentation(HwvtepGlobalAugmentation.class, hgGlobalAugBuilder.build());

        if (physicalLocatorAug != null) {
            TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
            tpBuilder.setKey(getTerminationPointKey(physicalLocatorAug));
            tpBuilder.addAugmentation(HwvtepPhysicalLocatorAugmentation.class, physicalLocatorAug);

            nodeBuilder.setTerminationPoint(Lists.newArrayList(tpBuilder.build()));
        }
        updateHwvtepNode(nodeBuilder.build());
    }

    /**
     * Delete remote ucast mac.
     *
     * @param nodeId
     *            the node id
     * @param mac
     *            the mac
     */
    public void deleteRemoteUcastMac(NodeId nodeId, String mac) {
        this.mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION,
                createRemoteUcastMacsInstanceIdentifier(nodeId, new MacAddress(mac)));
    }

    /**
     * Delete remote ucast macs.
     *
     * @param nodeId
     *            the node id
     * @param lstMac
     *            the lst mac
     */
    public void deleteRemoteUcastMacs(NodeId nodeId, List<String> lstMac) {
        List<InstanceIdentifier<RemoteUcastMacs>> lstIids = new ArrayList<>();
        if (lstMac != null && !lstMac.isEmpty()) {
            for (String mac : lstMac) {
                lstIids.add(createRemoteUcastMacsInstanceIdentifier(nodeId, new MacAddress(mac)));
            }
            this.mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, lstIids);
        }
    }

    /**
     * Update vlan bindings.
     *
     * @param physicalSwitchNodeId
     *            the physical switch node id
     * @param phyPortName
     *            the phy port name
     * @param vlanId
     *            the vlan id
     * @param logicalSwitchName
     *            the logical switch name
     */
    public void updateVlanBindings(NodeId physicalSwitchNodeId, String phyPortName, int vlanId,
            String logicalSwitchName) {
        NodeId nodeId = getManagedByNodeId(physicalSwitchNodeId);
        VlanBindings vlanBindings = constructVlanBinding(nodeId, vlanId, logicalSwitchName);
        HwvtepPhysicalPortAugmentation phyPortAug = new HwvtepPhysicalPortAugmentationBuilder()
                .setHwvtepNodeName(new HwvtepNodeName(phyPortName)).setVlanBindings(Lists.newArrayList(vlanBindings))
                .build();
        TerminationPoint terminationPoint = new TerminationPointBuilder()
                .setKey(new TerminationPointKey(new TpId(phyPortName)))
                .addAugmentation(HwvtepPhysicalPortAugmentation.class, phyPortAug).build();

        final InstanceIdentifier<TerminationPoint> iid = createPhysicalPortInstanceIdentifier(physicalSwitchNodeId,
                phyPortName);
        this.mdsalUtils.put(LogicalDatastoreType.CONFIGURATION, iid, terminationPoint);
    }

    /**
     * Gets the managed-by node id.
     *
     * @param physicalSwitchNodeId
     *            the physical switch node id
     * @return the managed by node id
     */
    public static NodeId getManagedByNodeId(NodeId physicalSwitchNodeId) {
        NodeId parentNodeId = null;
        if (physicalSwitchNodeId != null) {
            String strParentNodeId = StringUtils.substringBefore(physicalSwitchNodeId.getValue(),
                    new StringBuilder("/").append(HwvtepSouthboundConstants.PSWITCH_URI_PREFIX).toString());
            parentNodeId = new NodeId(strParentNodeId);
        }
        return parentNodeId;
    }

    /**
     * Construct vlan binding.
     *
     * @param nodeId
     *            the node id
     * @param vlanId
     *            the vlan id
     * @param logicalSwitchName
     *            the logical switch name
     * @return the vlan bindings
     */
    public static VlanBindings constructVlanBinding(NodeId nodeId, int vlanId, String logicalSwitchName) {
        VlanBindingsBuilder vbBuilder = new VlanBindingsBuilder();
        VlanBindingsKey vbKey = new VlanBindingsKey(new VlanId(vlanId));
        vbBuilder.setKey(vbKey);
        vbBuilder.setVlanIdKey(vbKey.getVlanIdKey());

        final InstanceIdentifier<LogicalSwitches> lSwitchIid = createLogicalSwitchesInstanceIdentifier(nodeId,
                new HwvtepNodeName(logicalSwitchName));
        HwvtepLogicalSwitchRef lSwitchRef = new HwvtepLogicalSwitchRef(lSwitchIid);
        vbBuilder.setLogicalSwitchRef(lSwitchRef);
        return vbBuilder.build();
    }

    /**
     * Gets the physical port.
     *
     * @param physicalSwitchNodeId
     *            the physical switch node id
     * @param phyPortName
     *            the phy port name
     * @return the physical port
     */
    public TerminationPoint getPhysicalPort(NodeId physicalSwitchNodeId, String phyPortName) {
        final InstanceIdentifier<TerminationPoint> iid = createPhysicalPortInstanceIdentifier(physicalSwitchNodeId,
                phyPortName);
        return this.mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, iid);
    }

    /**
     * Gets the all hwvtep devices.
     *
     * @return the all hwvtep devices
     */
    public List<Node> getAllHwvtepDevices() {
        Topology topology = this.mdsalUtils.read(LogicalDatastoreType.OPERATIONAL,
                createHwvtepTopologyInstanceIdentifier());
        if (topology != null) {
            return topology.getNode();
        }
        return Collections.emptyList();
    }

}
