/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound;

import com.google.common.collect.Lists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepLogicalSwitchRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitchesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitchesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.locator.set.attributes.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.locator.set.attributes.LocatorSetBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class TestBuilders {

    public static final String VXLAN_OVER_IPV4 = "vxlan_over_ipv4";

    public static void addLogicalSwitches(HwvtepGlobalAugmentationBuilder augmentationBuilder, String[]... data) {
        List<LogicalSwitches> logicalSwitcheses = Lists.newArrayList();
        for (String row[] : data) {
            logicalSwitcheses.add(TestBuilders.buildLogicalSwitch(row));
        }
        augmentationBuilder.setLogicalSwitches(logicalSwitcheses);
    }

    public static void addRemoteMcastMacs(InstanceIdentifier<Node> iid,
                                          HwvtepGlobalAugmentationBuilder augmentationBuilder, String[]... data) {
        List<RemoteMcastMacs> remoteMcastMacses = Lists.newArrayList();
        for (String row[] : data) {
            String teps[] = Arrays.copyOfRange(row, 2, row.length);
            remoteMcastMacses.add(TestBuilders.buildRemoteMcastMacs(iid, row[0], row[1], teps));
        }
        augmentationBuilder.setRemoteMcastMacs(remoteMcastMacses);
    }

    public static List<RemoteUcastMacs> addRemoteUcastMacs(InstanceIdentifier<Node> iid,
                                                           HwvtepGlobalAugmentationBuilder augmentationBuilder,
                                                           String[]... data) {
        List<RemoteUcastMacs> remoteUcastMacses = Lists.newArrayList();
        for (String row[] : data) {
            remoteUcastMacses.add(TestBuilders.buildRemoteUcastMacs(iid, row[0], row[1], row[2], row[3]));
        }
        augmentationBuilder.setRemoteUcastMacs(remoteUcastMacses);
        return remoteUcastMacses;
    }

    public static void addGlobalTerminationPoints(NodeBuilder nodeBuilder, InstanceIdentifier<Node> nodeIid,
                                                  String[]... data) {
        List<TerminationPoint> terminationPoints = Lists.newArrayList();
        for (String row[] : data) {
            terminationPoints.add(TestBuilders.buildTerminationPoint(nodeIid, row[0]));
        }
        nodeBuilder.setTerminationPoint(terminationPoints);
    }

    public static HwvtepLogicalSwitchRef buildLogicalSwitchesRef(InstanceIdentifier<Node> nodeIid,
                                                                 String logicalSwitchName ) {
        InstanceIdentifier<LogicalSwitches> lSwitchIid = nodeIid.augmentation(HwvtepGlobalAugmentation.class)
                .child(LogicalSwitches.class, new LogicalSwitchesKey(new HwvtepNodeName(logicalSwitchName)));
        return new HwvtepLogicalSwitchRef(lSwitchIid);
    }

    public static RemoteUcastMacs buildRemoteUcastMacs(InstanceIdentifier<Node> nodeIid, String vmMac,
                                                       String vmip, String tepIp, String logicalSwitchName) {
        RemoteUcastMacsBuilder ucmlBuilder = new RemoteUcastMacsBuilder();
        ucmlBuilder.setIpaddr(new IpAddress(vmip.toCharArray()));
        ucmlBuilder.setMacEntryKey(new MacAddress(vmMac));
        ucmlBuilder.setMacEntryUuid(getUUid(vmMac));
        ucmlBuilder.setLocatorRef(buildLocatorRef(nodeIid, tepIp));
        ucmlBuilder.setLogicalSwitchRef(buildLogicalSwitchesRef(nodeIid, logicalSwitchName));
        return ucmlBuilder.build();
    }

    public static TerminationPoint buildTerminationPoint(InstanceIdentifier<Node> nodeIid, String ip) {
        TerminationPointKey tpKey = new TerminationPointKey(new TpId("vxlan_over_ipv4:"+ip));
        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        if (nodeIid != null) {
            tpBuilder.setKey(tpKey);
            tpBuilder.setTpId(tpKey.getTpId());
            HwvtepPhysicalLocatorAugmentationBuilder tpAugmentationBuilder =
                    new HwvtepPhysicalLocatorAugmentationBuilder();
            tpAugmentationBuilder.setPhysicalLocatorUuid(getUUid(ip));
            tpAugmentationBuilder.setEncapsulationType(HwvtepSouthboundMapper.createEncapsulationType(VXLAN_OVER_IPV4));
            tpAugmentationBuilder.setDstIp(new IpAddress(ip.toCharArray()));
            tpBuilder.addAugmentation(HwvtepPhysicalLocatorAugmentation.class, tpAugmentationBuilder.build());
        }
        return tpBuilder.build();
    }

    public static LogicalSwitches buildLogicalSwitch(String... keys) {
        String logicalSwitch = keys[0];
        String tunnelKey = keys[1];
        LogicalSwitchesBuilder logicalSwitchesBuilder = new LogicalSwitchesBuilder();
        logicalSwitchesBuilder.setKey(new LogicalSwitchesKey(new HwvtepNodeName(logicalSwitch)));
        logicalSwitchesBuilder.setHwvtepNodeName(new HwvtepNodeName(logicalSwitch));
        logicalSwitchesBuilder.setTunnelKey(tunnelKey);
        Uuid uuid = getUUid(logicalSwitch);
        logicalSwitchesBuilder.setLogicalSwitchUuid(uuid);
        return logicalSwitchesBuilder.build();
    }

    public static RemoteMcastMacs buildRemoteMcastMacs(InstanceIdentifier<Node> iid, String mac,
                                                       String logicalSwitchName,String tepIps[]) {

        RemoteMcastMacsBuilder mMacLocalBuilder = new RemoteMcastMacsBuilder();
        if (mac.equals(HwvtepSouthboundConstants.UNKNOWN_DST_STRING)) {
            mMacLocalBuilder.setMacEntryKey(HwvtepSouthboundConstants.UNKNOWN_DST_MAC);
        } else {
            mMacLocalBuilder.setMacEntryKey(new MacAddress(mac));
        }
        mMacLocalBuilder.setMacEntryUuid(getUUid(mac));
        mMacLocalBuilder.setLogicalSwitchRef(buildLogicalSwitchesRef(iid, logicalSwitchName));
        List<LocatorSet> locatorSets = Lists.newArrayList();
        for (String tepIp : tepIps) {
            locatorSets.add(new LocatorSetBuilder().setLocatorRef(
                    buildLocatorRef(iid, tepIp)).build());
        }
        mMacLocalBuilder.setLocatorSet(locatorSets);
        return mMacLocalBuilder.build();
    }

    public static HwvtepPhysicalLocatorRef buildLocatorRef(InstanceIdentifier<Node> nodeIid,String tepIp) {
        InstanceIdentifier<TerminationPoint> tepId = buildTpId(nodeIid, tepIp);
        return new HwvtepPhysicalLocatorRef(tepId);
    }

    public static Uuid getUUid(String key) {
        return new Uuid(UUID.nameUUIDFromBytes(key.getBytes()).toString());
    }

    public static InstanceIdentifier<TerminationPoint> buildTpId(InstanceIdentifier<Node> nodeIid,String tepIp) {
        String tpKeyStr = VXLAN_OVER_IPV4 +':'+tepIp;
        TerminationPointKey tpKey = new TerminationPointKey(new TpId(tpKeyStr));
        return nodeIid.child(TerminationPoint.class, tpKey);
    }
}
