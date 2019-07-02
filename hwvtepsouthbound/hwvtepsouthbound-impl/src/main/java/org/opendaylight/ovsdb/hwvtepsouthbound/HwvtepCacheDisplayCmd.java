/*
 * Copyright (c) 2019 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.TransactUtils;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalPortAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.VlanBindings;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

@Command(scope = "hwvtep", name = "cache", description = "Disply hwvtep cache")
public class HwvtepCacheDisplayCmd extends OsgiCommandSupport {

    private final HwvtepSouthboundProvider hwvtepProvider;

    @Option(name = "-nodeid", description = "Node Id",
            required = false, multiValued = false)
    String nodeid;

    private static final TopologyId HWVTEP_TOPOLOGY_ID = new TopologyId(new Uri("hwvtep:1"));
    private static final String SEPERATOR = "#######################################################";
    private static final String SECTION_SEPERATOR = "===================================================" +
            "=============================";


    public HwvtepCacheDisplayCmd(HwvtepSouthboundProvider hwvtepProvider) {
        this.hwvtepProvider = hwvtepProvider;
    }

    @Override
    protected Object doExecute() throws Exception {
        Map<InstanceIdentifier<Node>, HwvtepConnectionInstance> allConnectedInstances =
                hwvtepProvider.getHwvtepConnectionManager().getAllConnectedInstances();
        if (nodeid == null) {
            allConnectedInstances.entrySet().forEach( (entry) -> {
                session.getConsole().println(SEPERATOR + " START " + SEPERATOR);
                print(entry.getKey(), entry.getValue());
                session.getConsole().println(SEPERATOR + " END " + SEPERATOR);
                session.getConsole().println();
                session.getConsole().println();
            });
        } else {
            session.getConsole().println(SEPERATOR + " START " + SEPERATOR);
            print(getIid(), allConnectedInstances.get(getIid()));
            session.getConsole().println(SEPERATOR + " END " + SEPERATOR);
        }
        return null;
    }

    private InstanceIdentifier<Node> getIid() {
        NodeId nodeId = new NodeId(new Uri(nodeid));
        NodeKey nodeKey = new NodeKey(nodeId);
        TopologyKey topoKey = new TopologyKey(HWVTEP_TOPOLOGY_ID);
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, topoKey)
                .child(Node.class, nodeKey)
                .build();
    }

    protected void print(InstanceIdentifier<Node> iid,
                                           HwvtepConnectionInstance connectionInstance) {
        HwvtepDeviceInfo deviceInfo = connectionInstance.getDeviceInfo();
        PrintStream printStream = session.getConsole();
        printStream.print("Printing for Node :  ");
        printStream.println(iid.firstKeyOf(Node.class).getNodeId().getValue());

        printStream.println(SECTION_SEPERATOR);
        printStream.println("Config data");
        printStream.println(SECTION_SEPERATOR);
        deviceInfo.getConfigData().entrySet().forEach( (entry) -> {
            printEntry(printStream, entry);
        });


        printStream.println(SECTION_SEPERATOR);
        printStream.println("Oper data");
        printStream.println(SECTION_SEPERATOR);
        deviceInfo.getOperData().entrySet().forEach( (entry) -> {
            printEntry(printStream, entry);
        });

        printStream.println(SECTION_SEPERATOR);
        printStream.println("Uuid data");
        printStream.println(SECTION_SEPERATOR);
        deviceInfo.getUuidData().entrySet().forEach( (entry) -> {
            printEntryUUID(printStream, entry);
        });
        printStream.println(SECTION_SEPERATOR);
        printStream.println(SECTION_SEPERATOR);

    }

    private void printEntry(PrintStream console, Map.Entry<Class<? extends Identifiable>,
            Map<InstanceIdentifier, HwvtepDeviceInfo.DeviceData>> entry) {
        Class<? extends Identifiable> cls = entry.getKey();
        Map<InstanceIdentifier, HwvtepDeviceInfo.DeviceData> map = entry.getValue();
        String clsName = cls.getSimpleName();
        console.println(clsName + " - ");
        map.values().forEach( (deviceData) -> {
            printTable(console, clsName, deviceData);
        });
    }

    private void printTable(PrintStream console, String clsName, HwvtepDeviceInfo.DeviceData deviceData) {
        console.print("    ");
        if(clsName.equals("LogicalSwitches"))
        {
            printLogicalSwitches(console, deviceData);
        }
        else if (clsName.equals("RemoteMcastMacs"))
        {
            printRemoteMcasts(console, deviceData);
        }
        else if(clsName.equals("RemoteUcastMacs"))
        {
            printRemoteUcasts(console, deviceData);
        }
        else if (clsName.equals("TerminationPoint") || clsName.equals("VlanBindings"))
        {
            printTerminationPoint(console, deviceData);
        }
        else if(clsName.equals("Node"))
        {
            printNode(console, deviceData);
        }
        else{
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

    private void printLogicalSwitches(PrintStream console, HwvtepDeviceInfo.DeviceData deviceData) {
        InstanceIdentifier<LogicalSwitches> ls = deviceData.getKey();
        console.print(ls.firstKeyOf(LogicalSwitches.class).getHwvtepNodeName().getValue());
    }

    private void printRemoteMcasts(PrintStream console, HwvtepDeviceInfo.DeviceData deviceData) {
        InstanceIdentifier<RemoteMcastMacs> remoteMcastMacsIid = deviceData.getKey();
        String macAddress = remoteMcastMacsIid.firstKeyOf(RemoteMcastMacs.class).getMacEntryKey().getValue();
        String logicalSwitchRef = remoteMcastMacsIid.firstKeyOf(RemoteMcastMacs.class).getLogicalSwitchRef().getValue()
                .firstKeyOf(LogicalSwitches.class).getHwvtepNodeName().getValue();
        StringBuilder macEntryDetails = new StringBuilder(macAddress).
                append("   LogicalSwitchRef  ").append(logicalSwitchRef);
        console.print(macEntryDetails);
    }

    private void printRemoteUcasts(PrintStream console, HwvtepDeviceInfo.DeviceData deviceData) {
        InstanceIdentifier<RemoteUcastMacs> remoteUcastMacsIid = deviceData.getKey();
        String macAddress = remoteUcastMacsIid.firstKeyOf(RemoteUcastMacs.class).getMacEntryKey().getValue();
        String logicalSwitchRef = remoteUcastMacsIid.firstKeyOf(RemoteUcastMacs.class).getLogicalSwitchRef().getValue()
                .firstKeyOf(LogicalSwitches.class).getHwvtepNodeName().getValue();
        StringBuilder macEntryDetails = new StringBuilder(macAddress).
                append("   LogicalSwitchRef  ").append(logicalSwitchRef);
        console.print(macEntryDetails);
    }

    private void printTerminationPoint(PrintStream console, HwvtepDeviceInfo.DeviceData deviceData) {
        InstanceIdentifier<TerminationPoint> terminationPointIid = deviceData.getKey();
        console.print(terminationPointIid.firstKeyOf(TerminationPoint.class).getTpId().getValue());
    }

    private void printNode(PrintStream console, HwvtepDeviceInfo.DeviceData deviceData) {
        InstanceIdentifier<Node> ls = deviceData.getKey();
        console.print(ls.firstKeyOf(Node.class).getNodeId().getValue());
    }

    private void printCommon(PrintStream console, HwvtepDeviceInfo.DeviceData deviceData) {
        console.print(deviceData.getKey());
        console.print(" ");
        if (deviceData.getData() == null && deviceData.getStatus() != HwvtepDeviceInfo.DeviceDataStatus.IN_TRANSIT) {
            console.print("data null unexpected ");
        }
        console.print(deviceData.getStatus());
        console.print(" ");
        console.println(deviceData.getUuid());
    }

    private void printEntryUUID(PrintStream console, Map.Entry<Class<? extends Identifiable>, Map<UUID, HwvtepDeviceInfo.DeviceData>> entry) {
        Class<? extends Identifiable> cls = entry.getKey();
        Map<UUID, HwvtepDeviceInfo.DeviceData> map = entry.getValue();
        String clsName = cls.getSimpleName();
        console.println(clsName + " - ");
        map.values().forEach((deviceData) -> {
            printTable(console, clsName, deviceData);
        });
    }


}
