/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.it;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Assert;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.mdsal.it.base.AbstractMdsalTestBase;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocator;
import org.opendaylight.ovsdb.utils.hwvtepsouthbound.utils.HwvtepSouthboundUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelIpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelIpsKey;
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
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertTrue;

/**
 * Created by eriytal on 10/20/2016.
 */
public class HwvtepBuilderUtils extends HwvtepSouthboundIT{

    private static final Logger LOG = LoggerFactory.getLogger(HwvtepSouthboundIT.class);

    @Override
    public MavenUrlReference getFeatureRepo() {
        return null;
    }

    @Override
    public String getFeatureName() {
        return null;
    }

    public List<String> getData(String data){
        String[] dataArray=data.split(",");
        List<String> splitData= new ArrayList<>();
        for(int i=0;i<dataArray.length;i++){
            splitData.add(dataArray[i]);
        }
        return splitData;
    }

    public static InstanceIdentifier<Node> getInstanceIdentifier(String haUUidVal) {
        String nodeString = HwvtepSouthboundConstants.HWVTEP_URI_PREFIX + "://" +
                HwvtepSouthboundConstants.UUID + "/" + UUID.nameUUIDFromBytes(haUUidVal.getBytes()).toString();
        NodeId nodeId = new NodeId(nodeString);
        NodeKey nodeKey = new NodeKey(nodeId);
        TopologyKey topoKey = new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID);
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, topoKey)
                .child(Node.class, nodeKey)
                .build();
    }

    public PhysicalSwitchAugmentation getPhysicalSwitch(ConnectionInfo connectionInfo) {
        return getPhysicalSwitch(connectionInfo, super.PS_NAME);
    }

    private PhysicalSwitchAugmentation getPhysicalSwitch(ConnectionInfo connectionInfo, String psName) {
        return getPhysicalSwitch(connectionInfo, psName, LogicalDatastoreType.OPERATIONAL);
    }

    private PhysicalSwitchAugmentation getPhysicalSwitch(ConnectionInfo connectionInfo, String psName,
                                                         LogicalDatastoreType dataStore) {
        Node psNode = getPhysicalSwitchNode(connectionInfo, psName, dataStore);
        Assert.assertNotNull("Physical switch node from OP data store", psNode);
        PhysicalSwitchAugmentation psAugmentation = psNode.getAugmentation(PhysicalSwitchAugmentation.class);
        Assert.assertNotNull("Physical switch Augmentation", psAugmentation);
        return psAugmentation;
    }

    private Node getPhysicalSwitchNode(ConnectionInfo connectionInfo, String psName, LogicalDatastoreType dataStore) {
        InstanceIdentifier<Node> psIid =
                HwvtepSouthboundUtils.createInstanceIdentifier(connectionInfo, new HwvtepNodeName(psName));
        return super.mdsalUtils.read(dataStore, psIid);
    }



    public static LogicalSwitches createLogicalSwitch(String name, String desc, String tunnelKey) {
        HwvtepNodeName hwvtepName = new HwvtepNodeName(name);
        LogicalSwitchesBuilder lsBuilder = new LogicalSwitchesBuilder().setHwvtepNodeDescription(desc)
                .setHwvtepNodeName(hwvtepName).setKey(new LogicalSwitchesKey(hwvtepName)).setTunnelKey(tunnelKey);
        return lsBuilder.build();
    }

    public ListenableFuture<Void> addLogicalSwitch(DataBroker broker, LogicalDatastoreType logicalDatastoreType,
                                                   InstanceIdentifier iid,
                                                   LogicalSwitches logicalSwitch) {
        WriteTransaction transaction = broker.newWriteOnlyTransaction();
        putLogicalSwitch(transaction,logicalDatastoreType, iid, logicalSwitch);
        return transaction.submit();
    }

    public void putLogicalSwitch(final WriteTransaction transaction, LogicalDatastoreType logicalDatastoreType,
                                 final InstanceIdentifier iid, final LogicalSwitches logicalSwitch) {
       /* InstanceIdentifier<LogicalSwitches> iid = createLogicalSwitchesInstanceIdentifier(nodeId,
                logicalSwitch.getHwvtepNodeName());*/
        LOG.error("Checking iid for config{}"+iid);
        transaction.put(logicalDatastoreType, iid, logicalSwitch, true);
    }

    public static InstanceIdentifier<LogicalSwitches> createLogicalSwitchesInstanceIdentifier(NodeId nodeId,
                                                                                              HwvtepNodeName hwvtepNodeName) {
        return createInstanceIdentifier(nodeId).augmentation(HwvtepGlobalAugmentation.class)
                .child(LogicalSwitches.class, new LogicalSwitchesKey(hwvtepNodeName));
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(NodeId nodeId) {
        return InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(nodeId));
    }

    public static InstanceIdentifier<LogicalSwitches> createInstanceIdentifier(String logicalSwitch) {
        NodeId id = dId.firstKeyOf(Node.class).getNodeId();
        NodeKey nodeKey = new NodeKey(id);
        InstanceIdentifier<LogicalSwitches> iid = null;
        iid = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                .child(Node.class, nodeKey).augmentation(HwvtepGlobalAugmentation.class)
                .child(LogicalSwitches.class, new LogicalSwitchesKey(new HwvtepNodeName(logicalSwitch)))
                .build();
        return iid;
    }


    public static Uuid getUUid(String key) {
        return new Uuid(UUID.nameUUIDFromBytes(key.getBytes()).toString());
    }

    public static HwvtepPhysicalLocatorRef buildLocatorRef(InstanceIdentifier<Node> nodeIid, String tepIp) {
        InstanceIdentifier tepId = buildTpId(nodeIid, tepIp);
        return new HwvtepPhysicalLocatorRef(tepId);
    }

    public static InstanceIdentifier<TerminationPoint> buildTpId(InstanceIdentifier<Node> nodeIid, String tepIp) {
        String tpKeyStr = "vxlan_over_ipv4:" + tepIp;
        TerminationPointKey tpKey = new TerminationPointKey(new TpId(tpKeyStr));
        KeyedInstanceIdentifier plIid = nodeIid.child(TerminationPoint.class, tpKey);
        return plIid;
    }

    public static HwvtepLogicalSwitchRef buildLogicalSwitchesRef(InstanceIdentifier<Node> nodeIid, String logicalSwitchName ) {
        InstanceIdentifier<LogicalSwitches> lSwitchIid = nodeIid.augmentation(HwvtepGlobalAugmentation.class)
                .child(LogicalSwitches.class, new LogicalSwitchesKey(new HwvtepNodeName(logicalSwitchName)));
        return new HwvtepLogicalSwitchRef(lSwitchIid);
    }

    NodeBuilder prepareOperationalNode(InstanceIdentifier<Node> iid) {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(iid.firstKeyOf(Node.class).getNodeId());
        return nodeBuilder;
    }


    public static List<TerminationPoint> addPhysicalSwitchTerminationPoints(InstanceIdentifier<Node> switchIid,WriteTransaction transaction,List<String> portNames) {
        List<TerminationPoint> tps = Lists.newArrayList();
        for(String pName : portNames){
            tps.add(buildTerminationPointForPhysicalSwitch(switchIid,pName,transaction,getVlanBindingData(1)));
        }
        return tps;
    }

    public static TerminationPoint buildTerminationPointForPhysicalSwitch(InstanceIdentifier<Node> switchIid,String portName,WriteTransaction transaction,Map<Long,String> vlanBindingData) {
        TerminationPointKey tpKey = new TerminationPointKey(new TpId(portName));
        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(tpKey);
        tpBuilder.setTpId(tpKey.getTpId());
        //InstanceIdentifier<TerminationPoint> tpPath = switchIid.child(TerminationPoint.class, new TerminationPointKey(new TpId(portName)));
        HwvtepPhysicalPortAugmentationBuilder tpAugmentationBuilder = new HwvtepPhysicalPortAugmentationBuilder();
        buildTerminationPoint(tpAugmentationBuilder, portName, vlanBindingData);
        tpBuilder.addAugmentation(HwvtepPhysicalPortAugmentation.class, tpAugmentationBuilder.build());
        return tpBuilder.build();
    }

    public static void buildTerminationPoint(HwvtepPhysicalPortAugmentationBuilder tpAugmentationBuilder,
                                             String portName,Map<Long,String> vlanBindingData) {
        updatePhysicalPortId(portName, tpAugmentationBuilder);
        updatePort(portName, tpAugmentationBuilder,vlanBindingData);
    }

    public static void updatePhysicalPortId(String portName,
                                            HwvtepPhysicalPortAugmentationBuilder tpAugmentationBuilder) {
        tpAugmentationBuilder.setHwvtepNodeName(new HwvtepNodeName(portName));
        tpAugmentationBuilder.setHwvtepNodeDescription("");
    }
    public static void updatePort(String portName, HwvtepPhysicalPortAugmentationBuilder tpAugmentationBuilder,
                                  Map<Long,String> vlanBindings) {
        updateVlanBindings(vlanBindings, tpAugmentationBuilder);
        tpAugmentationBuilder.setPhysicalPortUuid(new Uuid(UUID.randomUUID().toString()));
    }

    public static void updateVlanBindings(Map<Long,String> vlanBindings,
                                          HwvtepPhysicalPortAugmentationBuilder tpAugmentationBuilder) {
        List<VlanBindings> vlanBindingsList = new ArrayList<>();
        for (Map.Entry<Long,String> vlanBindingEntry : vlanBindings.entrySet()) {
            Long vlanBindingKey = vlanBindingEntry.getKey();
            String logicalSwitch  = vlanBindingEntry.getValue();
            if (logicalSwitch != null && vlanBindingKey != null) {
                vlanBindingsList.add(createVlanBinding(vlanBindingKey,logicalSwitch));
            }
        }
        tpAugmentationBuilder.setVlanBindings(vlanBindingsList);
    }

    public static VlanBindings createVlanBinding(Long key,String logicalSwitch) {
        VlanBindingsBuilder vbBuilder = new VlanBindingsBuilder();
        VlanBindingsKey vbKey = new VlanBindingsKey(new VlanId(key.intValue()));
        vbBuilder.setKey(vbKey);
        vbBuilder.setVlanIdKey(vbKey.getVlanIdKey());
        HwvtepLogicalSwitchRef lSwitchRef = new HwvtepLogicalSwitchRef(createInstanceIdentifier(logicalSwitch));
        vbBuilder.setLogicalSwitchRef(lSwitchRef);
        return vbBuilder.build();
    }


    public static Map<Long,String> getVlanBindingData(int mapSize){
        Map<Long,String> vlanBindings = new HashMap<>();
        for(Integer i=0;i<mapSize;i++){
            i=i*100;
            vlanBindings.put(Long.valueOf(i),"ls0");
        }
        return vlanBindings;
    }

    public static HwvtepGlobalAugmentation addRemoteUcastMacs(InstanceIdentifier<Node> iid, HwvtepGlobalAugmentation augmentation, List<String> remoteUcastMacdata) {
        HwvtepGlobalAugmentationBuilder augmentationBuilder = new HwvtepGlobalAugmentationBuilder(augmentation);
        List<RemoteUcastMacs> x = Lists.newArrayList();
        for(int i=0;i<remoteUcastMacdata.size();i+=4){
            x.add(buildRemoteUcastMacs(iid, remoteUcastMacdata.get(i),remoteUcastMacdata.get(i+1),remoteUcastMacdata.get(i+2),remoteUcastMacdata.get(i+3)));
        }
        augmentationBuilder.setRemoteUcastMacs(x);
        return augmentationBuilder.build();
    }

    public static HwvtepGlobalAugmentation addLocalUcastMacs(InstanceIdentifier<Node> iid, HwvtepGlobalAugmentation augmentation, List<String> localUcastMacdata) {
        HwvtepGlobalAugmentationBuilder augmentationBuilder = new HwvtepGlobalAugmentationBuilder(augmentation);
        List<LocalUcastMacs> x = Lists.newArrayList();
        for(int i=0;i<localUcastMacdata.size();i+=4){
            x.add(buildLocalUcastMacs(iid, localUcastMacdata.get(i),localUcastMacdata.get(i+1),localUcastMacdata.get(i+2),localUcastMacdata.get(i+3)));
        }
        augmentationBuilder.setLocalUcastMacs(x);
        return augmentationBuilder.build();
    }

    public static RemoteUcastMacs buildRemoteUcastMacs(InstanceIdentifier<Node> nodeIid, String vmMac,
                                                       String vmip, String tepIp, String logicalSwitchName) {
        RemoteUcastMacsBuilder remoteUcastMacsBuilder = new RemoteUcastMacsBuilder();
        remoteUcastMacsBuilder.setIpaddr(new IpAddress(vmip.toCharArray()));
        remoteUcastMacsBuilder.setMacEntryKey(new MacAddress(vmMac));
        remoteUcastMacsBuilder.setMacEntryUuid(getUUid(vmMac));
        remoteUcastMacsBuilder.setLocatorRef(buildLocatorRef(nodeIid, tepIp));
        remoteUcastMacsBuilder.setLogicalSwitchRef(buildLogicalSwitchesRef(nodeIid, logicalSwitchName));
        return remoteUcastMacsBuilder.build();
    }

    public static LocalUcastMacs buildLocalUcastMacs(InstanceIdentifier<Node> nodeIid, String vmMac,
                                                     String vmip, String tepIp, String logicalSwitchName) {
        LocalUcastMacsBuilder localUcastMacsBuilder = new LocalUcastMacsBuilder();
        localUcastMacsBuilder.setIpaddr(new IpAddress(vmip.toCharArray()));
        localUcastMacsBuilder.setMacEntryKey(new MacAddress(vmMac));
        localUcastMacsBuilder.setMacEntryUuid(getUUid(vmMac));
        localUcastMacsBuilder.setLocatorRef(buildLocatorRef(nodeIid, tepIp));
        localUcastMacsBuilder.setLogicalSwitchRef(buildLogicalSwitchesRef(nodeIid, logicalSwitchName));
        return localUcastMacsBuilder.build();
    }

    private void setEncapsType(HwvtepPhysicalLocatorAugmentationBuilder tpAugmentationBuilder, PhysicalLocator pLoc) {
        String encapsType = pLoc.getEncapsulationTypeColumn().getData();
        if (HwvtepSouthboundMapper.createEncapsulationType(encapsType) != null) {
            tpAugmentationBuilder.setEncapsulationType(HwvtepSouthboundMapper.createEncapsulationType(encapsType));
        }
    }

    private void setDstIp(HwvtepPhysicalLocatorAugmentationBuilder tpAugmentationBuilder, PhysicalLocator pLoc) {
        IpAddress ip = new IpAddress(pLoc.getDstIpColumn().getData().toCharArray());
        tpAugmentationBuilder.setDstIp(ip);
    }

    static String TEP_PREFIX = "vxlan_over_ipv4:";

    static InstanceIdentifier<TerminationPoint> createRemotePhysicalLocatorEntry(WriteTransaction transaction,
                                                                                 InstanceIdentifier<Node> nodeIid, IpAddress destIPAddress) throws ExecutionException, InterruptedException {
        String remoteIp = destIPAddress.getIpv4Address().getValue();
        LOG.error("creating remote physical locator entry {}", remoteIp);
        TerminationPointKey tpKey = getTerminationPointKey(remoteIp);
        InstanceIdentifier<TerminationPoint> tpPath =
                createInstanceIdentifier(nodeIid, tpKey);
        createPhysicalLocatorEntry(transaction, tpPath, tpKey, destIPAddress);
        return tpPath;
    }

    public static InstanceIdentifier<TerminationPoint> createInstanceIdentifier(InstanceIdentifier<Node> nodeIid,
                                                                                TerminationPointKey tpKey) {
        return nodeIid.child(TerminationPoint.class, tpKey);
    }

    private static void createPhysicalLocatorEntry(WriteTransaction transaction,
                                                   InstanceIdentifier<TerminationPoint> tpPath, TerminationPointKey terminationPointKey,
                                                   IpAddress destIPAddress) throws ExecutionException, InterruptedException {
        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        HwvtepPhysicalLocatorAugmentationBuilder tpAugmentationBuilder =
                new HwvtepPhysicalLocatorAugmentationBuilder();
        tpBuilder.setKey(terminationPointKey);
        tpBuilder.setTpId(terminationPointKey.getTpId());
        tpAugmentationBuilder.setEncapsulationType(EncapsulationTypeVxlanOverIpv4.class);
        setDstIp(tpAugmentationBuilder, destIPAddress);
        tpBuilder.addAugmentation(HwvtepPhysicalLocatorAugmentation.class, tpAugmentationBuilder.build());
        LOG.error("creating physical locator entry for {}", terminationPointKey);
        transaction.put(LogicalDatastoreType.CONFIGURATION,
                tpPath, tpBuilder.build(), true);
        transaction.submit();
        Thread.sleep(10000);
        boolean created = dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION, tpPath).get().isPresent();
        LOG.error("created: {}", created);
        assertTrue("Check for Physical locator creation", created);

    }

    public static void setDstIp(HwvtepPhysicalLocatorAugmentationBuilder tpAugmentationBuilder, IpAddress ipAddress) {
        IpAddress ip = new IpAddress(ipAddress);
        tpAugmentationBuilder.setDstIp(ip);
    }

    public static String getTerminationPointKeyString(String ipAddress) {
        String tpKeyStr = null;
        if(ipAddress != null) {
            tpKeyStr = new StringBuilder(TEP_PREFIX).
                    append(ipAddress).toString();
        }
        return tpKeyStr;
    }

    public static TerminationPointKey getTerminationPointKey(String ipAddress) {
        TerminationPointKey tpKey = null;
        String tpKeyStr = getTerminationPointKeyString(ipAddress);
        if(tpKeyStr != null) {
            tpKey = new TerminationPointKey(new TpId(tpKeyStr));
        }
        return tpKey;
    }

    public static List<String> getPortNameListD1(){
        List<String> portNames = new ArrayList<>();
        portNames.add("s3-eth1");
/*        portNames.add("s3-eth2");
        portNames.add("s3-eth3");
        portNames.add("s3-eth4");
        portNames.add("s3-eth5");
        portNames.add("s3-eth6");
        portNames.add("s3-eth7");*/
        return portNames;
    }


    void addPsNode(InstanceIdentifier<Node> path, InstanceIdentifier<Node> parentPath,List<String> portNameList) throws Exception {
        NodeBuilder nodeBuilder = prepareOperationalNode(path);
        WriteTransaction transaction = super.dataBroker.newWriteOnlyTransaction();

        PhysicalSwitchAugmentationBuilder physicalSwitchAugmentationBuilder = new PhysicalSwitchAugmentationBuilder();
        physicalSwitchAugmentationBuilder.setManagedBy(new HwvtepGlobalRef(parentPath));
        physicalSwitchAugmentationBuilder.setPhysicalSwitchUuid(getUUid("d1s3"));
        physicalSwitchAugmentationBuilder.setHwvtepNodeName(new HwvtepNodeName("s3"));
        physicalSwitchAugmentationBuilder.setHwvtepNodeDescription("description");

        List<TunnelIps> tunnelIps = new ArrayList<>();
        IpAddress ip = new IpAddress("192.168.122.30".toCharArray());
        tunnelIps.add(new TunnelIpsBuilder().setKey(new TunnelIpsKey(ip)).setTunnelIpsKey(ip).build());
        physicalSwitchAugmentationBuilder.setTunnelIps(tunnelIps);

        nodeBuilder.addAugmentation(PhysicalSwitchAugmentation.class, physicalSwitchAugmentationBuilder.build());
        dId = parentPath;
        nodeBuilder.setTerminationPoint(addPhysicalSwitchTerminationPoints(path,transaction,portNameList));

        mdsalUtils.put(LogicalDatastoreType.CONFIGURATION, path, nodeBuilder.build());
    }
}
