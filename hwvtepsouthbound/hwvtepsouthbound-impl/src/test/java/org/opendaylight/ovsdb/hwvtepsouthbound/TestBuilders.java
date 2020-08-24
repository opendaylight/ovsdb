/*
 * Copyright © 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepLogicalSwitchRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
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

public final class TestBuilders {

    public static final String VXLAN_OVER_IPV4 = "vxlan_over_ipv4";

    private TestBuilders() {
    }

    public static List<LogicalSwitches> addLogicalSwitches(HwvtepGlobalAugmentationBuilder augmentationBuilder,
            String[]... data) {
        List<LogicalSwitches> logicalSwitcheses = new ArrayList<>();
        for (String[] row : data) {
            logicalSwitcheses.add(TestBuilders.buildLogicalSwitch(row));
        }
        augmentationBuilder.setLogicalSwitches(logicalSwitcheses);
        return logicalSwitcheses;
    }

    public static List<RemoteMcastMacs> addRemoteMcastMacs(InstanceIdentifier<Node> iid,
                                          HwvtepGlobalAugmentationBuilder augmentationBuilder, String[]... data) {
        List<RemoteMcastMacs> remoteMcastMacses = new ArrayList<>();
        for (String[] row : data) {
            String[] teps = Arrays.copyOfRange(row, 2, row.length);
            remoteMcastMacses.add(TestBuilders.buildRemoteMcastMacs(iid, row[0], row[1], teps));
        }
        augmentationBuilder.setRemoteMcastMacs(remoteMcastMacses);
        return remoteMcastMacses;
    }

    public static List<RemoteUcastMacs> addRemoteUcastMacs(InstanceIdentifier<Node> iid,
                                                           HwvtepGlobalAugmentationBuilder augmentationBuilder,
                                                           String[]... data) {
        List<RemoteUcastMacs> remoteUcastMacses = new ArrayList<>();
        for (String[] row : data) {
            remoteUcastMacses.add(TestBuilders.buildRemoteUcastMacs(iid, row[0], row[1], row[2], row[3]));
        }
        augmentationBuilder.setRemoteUcastMacs(remoteUcastMacses);
        return remoteUcastMacses;
    }

    public static void addGlobalTerminationPoints(NodeBuilder nodeBuilder, InstanceIdentifier<Node> nodeIid,
                                                  String[]... data) {
        List<TerminationPoint> terminationPoints = new ArrayList<>();
        for (String[] row : data) {
            terminationPoints.add(TestBuilders.buildTerminationPoint(nodeIid, row[0]));
        }
        nodeBuilder.setTerminationPoint(terminationPoints);
    }

    public static HwvtepLogicalSwitchRef buildLogicalSwitchesRef(InstanceIdentifier<Node> nodeIid,
                                                                 String logicalSwitchName) {
        InstanceIdentifier<LogicalSwitches> switchIid = nodeIid.augmentation(HwvtepGlobalAugmentation.class)
                .child(LogicalSwitches.class, new LogicalSwitchesKey(new HwvtepNodeName(logicalSwitchName)));
        return new HwvtepLogicalSwitchRef(switchIid);
    }

    public static RemoteUcastMacs buildRemoteUcastMacs(InstanceIdentifier<Node> nodeIid, String vmMac,
                                                       String vmip, String tepIp, String logicalSwitchName) {
        RemoteUcastMacsBuilder ucmlBuilder = new RemoteUcastMacsBuilder();
        ucmlBuilder.setIpaddr(IpAddressBuilder.getDefaultInstance(vmip));
        ucmlBuilder.setMacEntryKey(new MacAddress(vmMac));
        ucmlBuilder.setMacEntryUuid(getUUid(vmMac));
        ucmlBuilder.setLocatorRef(buildLocatorRef(nodeIid, tepIp));
        ucmlBuilder.setLogicalSwitchRef(buildLogicalSwitchesRef(nodeIid, logicalSwitchName));
        return ucmlBuilder.build();
    }

    public static TerminationPoint buildTerminationPoint(InstanceIdentifier<Node> nodeIid, String ip) {
        TerminationPointKey tpKey = new TerminationPointKey(new TpId("vxlan_over_ipv4:" + ip));
        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        if (nodeIid != null) {
            tpBuilder.withKey(tpKey);
            tpBuilder.setTpId(tpKey.getTpId());
            tpBuilder.addAugmentation(new HwvtepPhysicalLocatorAugmentationBuilder()
                .setPhysicalLocatorUuid(getUUid(ip))
                .setEncapsulationType(HwvtepSouthboundMapper.createEncapsulationType(VXLAN_OVER_IPV4))
                .setDstIp(IpAddressBuilder.getDefaultInstance(ip))
                .build());
        }
        return tpBuilder.build();
    }

    public static LogicalSwitches buildLogicalSwitch(String... keys) {
        String logicalSwitch = keys[0];
        String tunnelKey = keys[1];
        LogicalSwitchesBuilder logicalSwitchesBuilder = new LogicalSwitchesBuilder();
        logicalSwitchesBuilder.withKey(new LogicalSwitchesKey(new HwvtepNodeName(logicalSwitch)));
        logicalSwitchesBuilder.setHwvtepNodeName(new HwvtepNodeName(logicalSwitch));
        logicalSwitchesBuilder.setTunnelKey(tunnelKey);
        Uuid uuid = getUUid(logicalSwitch);
        logicalSwitchesBuilder.setLogicalSwitchUuid(uuid);
        return logicalSwitchesBuilder.build();
    }

    public static RemoteMcastMacs buildRemoteMcastMacs(InstanceIdentifier<Node> iid, String mac,
                                                       String logicalSwitchName, String[] tepIps) {

        RemoteMcastMacsBuilder macLocalBuilder = new RemoteMcastMacsBuilder();
        if (mac.equals(HwvtepSouthboundConstants.UNKNOWN_DST_STRING)) {
            macLocalBuilder.setMacEntryKey(HwvtepSouthboundConstants.UNKNOWN_DST_MAC);
        } else {
            macLocalBuilder.setMacEntryKey(new MacAddress(mac));
        }
        macLocalBuilder.setMacEntryUuid(getUUid(mac));
        macLocalBuilder.setLogicalSwitchRef(buildLogicalSwitchesRef(iid, logicalSwitchName));
        List<LocatorSet> locatorSets = new ArrayList<>();
        for (String tepIp : tepIps) {
            locatorSets.add(new LocatorSetBuilder().setLocatorRef(
                    buildLocatorRef(iid, tepIp)).build());
        }
        macLocalBuilder.setLocatorSet(locatorSets);
        return macLocalBuilder.build();
    }

    public static HwvtepPhysicalLocatorRef buildLocatorRef(InstanceIdentifier<Node> nodeIid,String tepIp) {
        InstanceIdentifier<TerminationPoint> tepId = buildTpId(nodeIid, tepIp);
        return new HwvtepPhysicalLocatorRef(tepId);
    }

    public static Uuid getUUid(String key) {
        return new Uuid(UUID.nameUUIDFromBytes(key.getBytes()).toString());
    }

    public static InstanceIdentifier<TerminationPoint> buildTpId(InstanceIdentifier<Node> nodeIid,String tepIp) {
        String tpKeyStr = VXLAN_OVER_IPV4 + ':' + tepIp;
        TerminationPointKey tpKey = new TerminationPointKey(new TpId(tpKeyStr));
        return nodeIid.child(TerminationPoint.class, tpKey);
    }
}
