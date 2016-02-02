/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.utils.hwvtepsouthbound.utils;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
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
     * @param physicalSwitchNodeId
     *            the physical switch node id
     * @param phyPortName
     *            the phy port name
     * @return the instance identifier
     */
    public static InstanceIdentifier<HwvtepPhysicalPortAugmentation> createPhysicalPortInstanceIdentifier(
            NodeId physicalSwitchNodeId, String phyPortName) {
        return createInstanceIdentifier(physicalSwitchNodeId)
                .child(TerminationPoint.class, new TerminationPointKey(new TpId(phyPortName)))
                .augmentation(HwvtepPhysicalPortAugmentation.class);
    }

    /**
     * Gets the termination point key.
     *
     * @param phyLocator
     *            the phy locator
     * @return the termination point key
     */
    public static TerminationPointKey getTerminationPointKey(HwvtepPhysicalLocatorAugmentation phyLocator) {
        TerminationPointKey tpKey = null;
        if (phyLocator.getEncapsulationType() != null && phyLocator.getDstIp() != null) {
            String encapType = HwvtepSouthboundConstants.ENCAPS_TYPE_MAP.get(phyLocator.getEncapsulationType());
            String tpKeyStr = encapType + ":" + String.valueOf(phyLocator.getDstIp().getValue());
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
        String phySwitchNodeId = nodeId.getValue() + "/" + HwvtepSouthboundConstants.PSWITCH_URI_PREFIX + "/"
                + physicalSwitchName;
        return new NodeId(phySwitchNodeId);
    }

    /**
     * Create logical switch.
     *
     * @param name
     *            the name
     * @param desc
     *            the desc
     * @param tunnelKey
     *            the tunnel key
     * @return the logical switches
     */
    public static LogicalSwitches createLogicalSwitch(String name, String desc, String tunnelKey) {
        HwvtepNodeName hwvtepName = new HwvtepNodeName(name);
        LogicalSwitchesBuilder lsBuilder = new LogicalSwitchesBuilder().setHwvtepNodeDescription(desc)
                .setHwvtepNodeName(hwvtepName).setKey(new LogicalSwitchesKey(hwvtepName)).setTunnelKey(tunnelKey);
        return lsBuilder.build();
    }

    /**
     * Create hwvtep physical locator augmentation.
     *
     * @param ipAddress
     *            the ip address
     * @return the hwvtep physical locator augmentation
     */
    public static HwvtepPhysicalLocatorAugmentation createHwvtepPhysicalLocatorAugmentation(String ipAddress) {
        // FIXME: Get encapsulation type dynamically
        Class<? extends EncapsulationTypeBase> encapTypeClass = HwvtepSouthboundMapper
                .createEncapsulationType(StringUtils.EMPTY);
        HwvtepPhysicalLocatorAugmentationBuilder phyLocBuilder = new HwvtepPhysicalLocatorAugmentationBuilder()
                .setEncapsulationType(encapTypeClass).setDstIp(new IpAddress(ipAddress.toCharArray()));
        return phyLocBuilder.build();
    }

    /**
     * Create remote ucast mac.
     *
     * @param nodeId
     *            the node id
     * @param mac
     *            the mac
     * @param ipAddress
     *            the ip address
     * @param logicalSwitchName
     *            the logical switch name
     * @param physicalLocatorAug
     *            the physical locator aug
     * @return the remote ucast macs
     */
    public static RemoteUcastMacs createRemoteUcastMac(NodeId nodeId, String mac, IpAddress ipAddress,
            String logicalSwitchName, HwvtepPhysicalLocatorAugmentation physicalLocatorAug) {
        HwvtepLogicalSwitchRef lsRef = new HwvtepLogicalSwitchRef(
                createLogicalSwitchesInstanceIdentifier(nodeId, new HwvtepNodeName(logicalSwitchName)));
        HwvtepPhysicalLocatorRef phyLocRef = new HwvtepPhysicalLocatorRef(
                createPhysicalLocatorInstanceIdentifier(nodeId, physicalLocatorAug));

        RemoteUcastMacs remoteUcastMacs = new RemoteUcastMacsBuilder().setMacEntryKey(new MacAddress(mac))
                .setIpaddr(ipAddress).setLogicalSwitchRef(lsRef).setLocatorRef(phyLocRef).build();
        return remoteUcastMacs;
    }

    /**
     * Gets the hwvtep node.
     *
     * @param nodeId
     *            the node id
     * @return the hwvtep node
     */
    public Node getHwvtepNode(NodeId nodeId) {
        final InstanceIdentifier<Node> iid = createInstanceIdentifier(nodeId);
        return this.mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, iid);
    }

    /**
     * Put logical switch in the transaction.
     *
     * @param transaction
     *            the transaction
     * @param nodeId
     *            the node id
     * @param logicalSwitch
     *            the logical switch
     */
    public static void putLogicalSwitch(final WriteTransaction transaction, final NodeId nodeId,
            final LogicalSwitches logicalSwitch) {
        putLogicalSwitches(transaction, nodeId, Lists.newArrayList(logicalSwitch));
    }

    /**
     * Put the logical switches in the transaction.
     *
     * @param transaction
     *            the transaction
     * @param nodeId
     *            the node id
     * @param lstSwitches
     *            the lst switches
     */
    public static void putLogicalSwitches(final WriteTransaction transaction, final NodeId nodeId,
            final List<LogicalSwitches> lstSwitches) {
        if (lstSwitches != null) {
            InstanceIdentifier<LogicalSwitches> iid;
            for (LogicalSwitches logicalSwitch : lstSwitches) {
                iid = createLogicalSwitchesInstanceIdentifier(nodeId, logicalSwitch.getHwvtepNodeName());
                transaction.put(LogicalDatastoreType.CONFIGURATION, iid, logicalSwitch, true);
            }
        }
    }

    /**
     * Put physical locator.
     *
     * @param transaction
     *            the transaction
     * @param nodeId
     *            the node id
     * @param phyLocator
     *            the phy locator
     */
    public static void putPhysicalLocator(final WriteTransaction transaction, final NodeId nodeId,
            final HwvtepPhysicalLocatorAugmentation phyLocator) {
        putPhysicalLocators(transaction, nodeId, Lists.newArrayList(phyLocator));
    }

    /**
     * Put physical locators.
     *
     * @param transaction
     *            the transaction
     * @param nodeId
     *            the node id
     * @param lstPhysicalLocator
     *            the lst physical locator
     */
    private static void putPhysicalLocators(WriteTransaction transaction, NodeId nodeId,
            List<HwvtepPhysicalLocatorAugmentation> lstPhysicalLocator) {
        if (lstPhysicalLocator != null) {
            InstanceIdentifier<TerminationPoint> iid;
            for (HwvtepPhysicalLocatorAugmentation phyLocator : lstPhysicalLocator) {
                iid = createPhysicalLocatorInstanceIdentifier(nodeId, phyLocator);
                TerminationPoint terminationPoint = new TerminationPointBuilder()
                        .setKey(getTerminationPointKey(phyLocator))
                        .addAugmentation(HwvtepPhysicalLocatorAugmentation.class, phyLocator).build();

                transaction.put(LogicalDatastoreType.CONFIGURATION, iid, terminationPoint, true);
            }
        }
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
     * Delete logical switch from the transaction.
     *
     * @param transaction
     *            the transaction
     * @param nodeId
     *            the node id
     * @param logicalSwitchName
     *            the logical switch name
     */
    public static void deleteLogicalSwitch(final WriteTransaction transaction, final NodeId nodeId,
            final String logicalSwitchName) {
        transaction.delete(LogicalDatastoreType.CONFIGURATION,
                createLogicalSwitchesInstanceIdentifier(nodeId, new HwvtepNodeName(logicalSwitchName)));
    }

    /**
     * Put remote ucast macs in the transaction.
     *
     * @param transaction
     *            the transaction
     * @param nodeId
     *            the node id
     * @param lstRemoteUcastMacs
     *            the lst remote ucast macs
     */
    public static void putRemoteUcastMacs(final WriteTransaction transaction, final NodeId nodeId,
            final List<RemoteUcastMacs> lstRemoteUcastMacs) {
        if (lstRemoteUcastMacs != null && !lstRemoteUcastMacs.isEmpty()) {
            for (RemoteUcastMacs remoteUcastMac : lstRemoteUcastMacs) {
                InstanceIdentifier<RemoteUcastMacs> iid = createRemoteUcastMacsInstanceIdentifier(nodeId,
                        remoteUcastMac.getMacEntryKey());
                transaction.put(LogicalDatastoreType.CONFIGURATION, iid, remoteUcastMac, true);
            }
        }
    }

    /**
     * Delete remote ucast mac from the transaction.
     *
     * @param transaction
     *            the transaction
     * @param nodeId
     *            the node id
     * @param mac
     *            the mac
     */
    public static void deleteRemoteUcastMac(final WriteTransaction transaction, final NodeId nodeId, final String mac) {
        deleteRemoteUcastMacs(transaction, nodeId, Lists.newArrayList(mac));
    }

    /**
     * Delete remote ucast macs from the transaction.
     *
     * @param transaction
     *            the transaction
     * @param nodeId
     *            the node id
     * @param lstMac
     *            the lst mac
     */
    public static void deleteRemoteUcastMacs(final WriteTransaction transaction, final NodeId nodeId,
            final List<String> lstMac) {
        if (lstMac != null && !lstMac.isEmpty()) {
            for (String mac : lstMac) {
                transaction.delete(LogicalDatastoreType.CONFIGURATION,
                        createRemoteUcastMacsInstanceIdentifier(nodeId, new MacAddress(mac)));
            }
        }
    }

    /**
     * Merge vlan bindings in the transaction.
     *
     * @param transaction
     *            the transaction
     * @param nodeId
     *            the node id
     * @param phySwitchName
     *            the phy switch name
     * @param phyPortName
     *            the phy port name
     * @param vlanBindings
     *            the vlan bindings
     */
    public static void mergeVlanBindings(final WriteTransaction transaction, final NodeId nodeId,
            final String phySwitchName, final String phyPortName, final List<VlanBindings> vlanBindings) {
        NodeId physicalSwitchNodeId = createManagedNodeId(nodeId, phySwitchName);
        mergeVlanBindings(transaction, physicalSwitchNodeId, phyPortName, vlanBindings);
    }

    /**
     * Merge vlan bindings in the transaction.
     *
     * @param transaction
     *            the transaction
     * @param physicalSwitchNodeId
     *            the physical switch node id
     * @param phyPortName
     *            the phy port name
     * @param vlanBindings
     *            the vlan bindings
     */
    private static void mergeVlanBindings(final WriteTransaction transaction, final NodeId physicalSwitchNodeId,
            final String phyPortName, final List<VlanBindings> vlanBindings) {
        HwvtepPhysicalPortAugmentation phyPortAug = new HwvtepPhysicalPortAugmentationBuilder()
                .setHwvtepNodeName(new HwvtepNodeName(phyPortName)).setVlanBindings(vlanBindings).build();

        final InstanceIdentifier<HwvtepPhysicalPortAugmentation> iid = createPhysicalPortInstanceIdentifier(
                physicalSwitchNodeId, phyPortName);
        transaction.merge(LogicalDatastoreType.CONFIGURATION, iid, phyPortAug, true);
    }

    /**
     * Create vlan binding.
     *
     * @param nodeId
     *            the node id
     * @param vlanId
     *            the vlan id
     * @param logicalSwitchName
     *            the logical switch name
     * @return the vlan bindings
     */
    public static VlanBindings createVlanBinding(NodeId nodeId, int vlanId, String logicalSwitchName) {
        VlanBindingsBuilder vbBuilder = new VlanBindingsBuilder();
        VlanBindingsKey vbKey = new VlanBindingsKey(new VlanId(vlanId));
        vbBuilder.setKey(vbKey);
        vbBuilder.setVlanIdKey(vbKey.getVlanIdKey());

        final InstanceIdentifier<LogicalSwitches> lSwitchIid = createLogicalSwitchesInstanceIdentifier(nodeId,
                new HwvtepNodeName(logicalSwitchName));
        HwvtepLogicalSwitchRef lsRef = new HwvtepLogicalSwitchRef(lSwitchIid);
        vbBuilder.setLogicalSwitchRef(lsRef);
        return vbBuilder.build();
    }

    /**
     * Gets the physical port.
     *
     * @param nodeId
     *            the node id
     * @param phySwitchName
     *            the phy switch name
     * @param phyPortName
     *            the phy port name
     * @return the physical port
     */
    public HwvtepPhysicalPortAugmentation getPhysicalPort(NodeId nodeId, String phySwitchName, String phyPortName) {
        NodeId physicalSwitchNodeId = createManagedNodeId(nodeId, phySwitchName);
        final InstanceIdentifier<HwvtepPhysicalPortAugmentation> iid = createPhysicalPortInstanceIdentifier(
                physicalSwitchNodeId, phyPortName);
        return this.mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, iid);
    }

    /**
     * Gets the all hwvtep nodes.
     *
     * @return the all hwvtep nodes
     */
    public List<Node> getAllHwvtepNodes() {
        Topology topology = this.mdsalUtils.read(LogicalDatastoreType.OPERATIONAL,
                createHwvtepTopologyInstanceIdentifier());
        return topology.getNode();
    }

}
