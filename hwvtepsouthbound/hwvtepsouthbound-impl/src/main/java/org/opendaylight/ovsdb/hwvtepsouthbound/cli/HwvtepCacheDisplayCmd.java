/*
 * Copyright (c) 2019 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.cli;

import java.io.PrintStream;
import java.util.Map;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepDeviceInfo;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundProviderInfo;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalPort;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepLogicalSwitchRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.EntryObject;

@Service
@Command(scope = "hwvtep", name = "cache", description = "Disply hwvtep cache")
public class HwvtepCacheDisplayCmd implements Action {

    @Argument(name = "nodeid", description = "Node Id",
            required = false, multiValued = false)
    private String nodeid;

    @Reference
    private HwvtepSouthboundProviderInfo hwvtepSouthboundProvider;

    private static final TopologyId HWVTEP_TOPOLOGY_ID = new TopologyId(new Uri("hwvtep:1"));
    private static final String SEPERATOR = "#######################################################";
    private static final String SECTION_SEPERATOR = "==================================================="
            + "=============================";


    @Override
    @SuppressWarnings("checkstyle:RegexpSinglelineJava")
    public Object execute() throws Exception {
        var allConnectedInstances = hwvtepSouthboundProvider.getAllConnectedInstances();
        if (nodeid == null) {
            allConnectedInstances.entrySet().forEach(entry -> {
                System.out.println(SEPERATOR + " START " + SEPERATOR);
                print(entry.getKey(), entry.getValue());
                System.out.println(SEPERATOR + " END " + SEPERATOR);
                System.out.println();
                System.out.println();
            });
        } else {
            System.out.println(SEPERATOR + " START " + SEPERATOR);
            print(getIid(), allConnectedInstances.get(getIid()));
            System.out.println(SEPERATOR + " END " + SEPERATOR);
        }
        return null;
    }

    private DataObjectIdentifier<Node> getIid() {
        return DataObjectIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(HWVTEP_TOPOLOGY_ID))
            .child(Node.class, new NodeKey(new NodeId(new Uri(nodeid))))
            .build();
    }

    @SuppressWarnings("checkstyle:RegexpSinglelineJava")
    private static void print(DataObjectIdentifier<Node> iid, HwvtepDeviceInfo deviceInfo) {
        PrintStream printStream = System.out;
        printStream.print("Printing for Node :  ");
        printStream.println(iid.getFirstKeyOf(Node.class).getNodeId().getValue());

        printStream.println(SECTION_SEPERATOR);
        printStream.println("Config data");
        printStream.println(SECTION_SEPERATOR);
        deviceInfo.getConfigData().entrySet().forEach(entry -> {
            printEntry(printStream, entry);
        });

        printStream.println(SECTION_SEPERATOR);
        printStream.println("Oper data");
        printStream.println(SECTION_SEPERATOR);
        deviceInfo.getOperData().entrySet().forEach(entry -> {
            printEntry(printStream, entry);
        });

        printStream.println(SECTION_SEPERATOR);
        printStream.println("Uuid data");
        printStream.println(SECTION_SEPERATOR);
        deviceInfo.getUuidData().entrySet().forEach(entry -> {
            printEntryUUID(printStream, entry);
        });
        printStream.println(SECTION_SEPERATOR);
        printStream.println(SECTION_SEPERATOR);
    }

    private static void printEntry(PrintStream console, Map.Entry<Class<? extends EntryObject<?, ?, ?>>,
            Map<DataObjectIdentifier, HwvtepDeviceInfo.DeviceData>> entry) {
        Class<? extends EntryObject<?, ?, ?>> cls = entry.getKey();
        var map = entry.getValue();
        String clsName = cls.getSimpleName();
        console.println(clsName + " - ");
        map.values().forEach(deviceData -> {
            printTable(console, clsName, deviceData);
        });
    }

    private static void printTable(PrintStream console, String clsName, HwvtepDeviceInfo.DeviceData deviceData) {
        console.print("    ");
        if (clsName.equals("LogicalSwitches")) {
            printLogicalSwitches(console, deviceData);
        } else if (clsName.equals("RemoteMcastMacs")) {
            printRemoteMcasts(console, deviceData);
        } else if (clsName.equals("RemoteUcastMacs")) {
            printRemoteUcasts(console, deviceData);
        } else if (clsName.equals("TerminationPoint") || clsName.equals("VlanBindings")) {
            printTerminationPoint(console, deviceData);
        } else if (clsName.equals("Node")) {
            printNode(console, deviceData);
        } else {
            printCommon(console, deviceData);
        }

        if (deviceData.getData() == null && deviceData.getStatus() != HwvtepDeviceInfo.DeviceDataStatus.IN_TRANSIT) {
            console.print("data null unexpected ");
        }
        console.print(" ");
        console.print(deviceData.getStatus());
        console.print(" ");
        console.println(deviceData.getUuid());
    }

    private static void printLogicalSwitches(PrintStream console, HwvtepDeviceInfo.DeviceData deviceData) {
        var ls = deviceData.getKey();
        console.print(ls.getFirstKeyOf(LogicalSwitches.class).getHwvtepNodeName().getValue());
    }

    private static void printRemoteMcasts(PrintStream console, HwvtepDeviceInfo.DeviceData deviceData) {
        var remoteMcastMacsIid = deviceData.getKey();
        String macAddress = remoteMcastMacsIid.getFirstKeyOf(RemoteMcastMacs.class).getMacEntryKey().getValue();
        String logicalSwitchRef =
            getLogicalSwitchRef(remoteMcastMacsIid.getFirstKeyOf(RemoteMcastMacs.class).getLogicalSwitchRef());
        StringBuilder macEntryDetails = new StringBuilder(macAddress).append("   LogicalSwitchRef  ")
                .append(logicalSwitchRef);
        console.print(macEntryDetails);
    }

    private static void printRemoteUcasts(PrintStream console, HwvtepDeviceInfo.DeviceData deviceData) {
        var remoteUcastMacsIid = deviceData.getKey();
        String macAddress = remoteUcastMacsIid.getFirstKeyOf(RemoteUcastMacs.class).getMacEntryKey().getValue();
        String logicalSwitchRef =
            getLogicalSwitchRef(remoteUcastMacsIid.getFirstKeyOf(RemoteUcastMacs.class).getLogicalSwitchRef());
        StringBuilder macEntryDetails = new StringBuilder(macAddress).append("   LogicalSwitchRef  ")
                .append(logicalSwitchRef);
        console.print(macEntryDetails);
    }

    private static String getLogicalSwitchRef(HwvtepLogicalSwitchRef hwvtepLogicalSwitchRef) {
        return ((DataObjectIdentifier<?>) hwvtepLogicalSwitchRef.getValue())
            .getFirstKeyOf(LogicalSwitches.class).getHwvtepNodeName().getValue();
    }

    private static void printTerminationPoint(PrintStream console, HwvtepDeviceInfo.DeviceData deviceData) {
        var terminationPointIid = deviceData.getKey();
        console.print(terminationPointIid.getFirstKeyOf(TerminationPoint.class).getTpId().getValue());
        try {
            PhysicalPort physicalPort = (PhysicalPort) deviceData.getData();
            console.print("    " + physicalPort.getVlanBindingsColumn().getData().keySet());
        } catch (ClassCastException exp) {
            //This is the Karaf cli display command , ignore the exception.
        }
    }

    private static void printNode(PrintStream console, HwvtepDeviceInfo.DeviceData deviceData) {
        var ls = deviceData.getKey();
        console.print(ls.getFirstKeyOf(Node.class).getNodeId().getValue());
    }

    private static void printCommon(PrintStream console, HwvtepDeviceInfo.DeviceData deviceData) {
        console.print(deviceData.getKey());
        console.print(" ");
        if (deviceData.getData() == null && deviceData.getStatus() != HwvtepDeviceInfo.DeviceDataStatus.IN_TRANSIT) {
            console.print("data null unexpected ");
        }
        console.print(deviceData.getStatus());
        console.print(" ");
        console.println(deviceData.getUuid());
    }

    private static void printEntryUUID(PrintStream console, Map.Entry<Class<? extends EntryObject<?, ?, ?>>, Map<UUID,
            HwvtepDeviceInfo.DeviceData>> entry) {
        Class<? extends EntryObject<?, ?, ?>> cls = entry.getKey();
        Map<UUID, HwvtepDeviceInfo.DeviceData> map = entry.getValue();
        String clsName = cls.getSimpleName();
        console.println(clsName + " - ");
        map.values().forEach(deviceData -> {
            printTable(console, clsName, deviceData);
        });
    }
}
