/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionHistory;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@Service
@Command(scope = "hwvtep", name = "txlog", description = "prints hwvtep tx log")
public class TransactionHistoryCmd implements Action {
    private static final String SEPERATOR = "#######################################################";

    @Reference
    private HwvtepSouthboundProviderInfo hwvtepProvider;

    @Option(name = "-nodeid", description = "Node Id", required = false, multiValued = false)
    private String nodeid;

    @SuppressWarnings("checkstyle:RegexpSinglelineJava")
    @Override
    public Object execute() {
        final PrintStream out = System.out;

        Map<InstanceIdentifier<Node>, TransactionHistory> controllerTxLogs = hwvtepProvider.getControllerTxHistory();
        Map<InstanceIdentifier<Node>, TransactionHistory> deviceUpdateLogs = hwvtepProvider.getDeviceUpdateHistory();
        if (nodeid != null) {
            printLogs(out, controllerTxLogs, deviceUpdateLogs,
                HwvtepSouthboundMapper.createInstanceIdentifier(new NodeId(nodeid)));
        } else {
            Map<InstanceIdentifier<Node>, TransactionHistory> txlogs
                    = controllerTxLogs.isEmpty() ? deviceUpdateLogs : controllerTxLogs;
            txlogs.keySet().forEach(iid -> printLogs(out, controllerTxLogs, deviceUpdateLogs, iid));
            out.println("Device tx logs size " + deviceUpdateLogs.size());
        }
        return null;
    }

    private static void printLogs(final PrintStream out,
                                  final Map<InstanceIdentifier<Node>, TransactionHistory> controllerTxLogs,
                                  final Map<InstanceIdentifier<Node>, TransactionHistory> deviceUpdateLogs,
                                  final InstanceIdentifier<Node> iid) {
        out.println(SEPERATOR + " START " + SEPERATOR);
        List<HwvtepTransactionLogElement> controllerTxLog = controllerTxLogs.get(iid).getElements()
                .stream().map(ele -> new HwvtepTransactionLogElement(ele, false)).collect(Collectors.toList());
        List<HwvtepTransactionLogElement> deviceUpdateLog = deviceUpdateLogs.get(iid).getElements()
                .stream().map(ele -> new HwvtepTransactionLogElement(ele, true)).collect(Collectors.toList());
        List<Pair<HwvtepTransactionLogElement, Boolean>> allLogs = mergeLogsByDate(controllerTxLog, deviceUpdateLog);
        out.print("Printing for Node :  ");
        out.println(iid.firstKeyOf(Node.class).getNodeId().getValue());
        printLogs(out, allLogs);
        out.println(SEPERATOR + " END " + SEPERATOR);
        out.println();
    }

    private static void printLogs(final PrintStream out, final List<Pair<HwvtepTransactionLogElement, Boolean>> logs) {
        logs.forEach(pair -> {
            HwvtepTransactionLogElement log = pair.getLeft();
            out.print(new Date(log.getDate()));
            out.print(" ");
            out.print(pair.getRight() ? "CONTROLLER" : "DEVICE");
            out.print(" ");
            out.print(log.getTransactionType());
            out.print(" ");
            out.println(log.getData());
        });
    }

    private static List<Pair<HwvtepTransactionLogElement, Boolean>> mergeLogsByDate(
            final List<HwvtepTransactionLogElement> logs1,
            final List<HwvtepTransactionLogElement> logs2) {

        ArrayList<Pair<HwvtepTransactionLogElement, Boolean>> result = new ArrayList();
        int firstIdx = 0;
        int secondIdx = 0;
        int firstSize = logs1.size();
        int secondSize = logs2.size();
        while (firstIdx < firstSize && secondIdx < secondSize) {
            if (logs1.get(firstIdx).getDate() < logs2.get(secondIdx).getDate()) {
                result.add(ImmutablePair.of(logs1.get(firstIdx++), Boolean.TRUE));
            } else {
                result.add(ImmutablePair.of(logs2.get(secondIdx++), Boolean.FALSE));
            }
        }
        while (firstIdx < firstSize) {
            result.add(ImmutablePair.of(logs1.get(firstIdx++), Boolean.TRUE));
        }
        while (secondIdx < secondSize) {
            result.add(ImmutablePair.of(logs2.get(secondIdx++), Boolean.FALSE));
        }
        return result;
    }
}
