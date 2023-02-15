/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionHistory;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@Command(scope = "hwvtep", name = "txlog", description = "prints hwvtep tx log")
public class TransactionHistoryCmd extends OsgiCommandSupport {

    @Option(name = "-nodeid", description = "Node Id",
            required = false, multiValued = false)
    String nodeid;
    private static final String SEPERATOR = "#######################################################";

    private final HwvtepSouthboundProviderInfo hwvtepProvider;

    public TransactionHistoryCmd(final HwvtepSouthboundProviderInfo hwvtepProvider) {
        this.hwvtepProvider = requireNonNull(hwvtepProvider);
    }

    @Override
    protected Object doExecute() throws Exception {
        Map<InstanceIdentifier<Node>, TransactionHistory> controllerTxLogs = hwvtepProvider.getControllerTxHistory();
        Map<InstanceIdentifier<Node>, TransactionHistory> deviceUpdateLogs = hwvtepProvider.getDeviceUpdateHistory();
        if (nodeid != null) {
            printLogs(controllerTxLogs, deviceUpdateLogs,
                HwvtepSouthboundMapper.createInstanceIdentifier(new NodeId(nodeid)));
        } else {
            Map<InstanceIdentifier<Node>, TransactionHistory> txlogs
                    = controllerTxLogs.isEmpty() ? deviceUpdateLogs : controllerTxLogs;
            txlogs.keySet().forEach(iid -> {
                printLogs(controllerTxLogs, deviceUpdateLogs, iid);
            });
            session.getConsole().println("Device tx logs size " + deviceUpdateLogs.size());
        }
        return null;
    }

    private void printLogs(final Map<InstanceIdentifier<Node>, TransactionHistory> controllerTxLogs,
                           final Map<InstanceIdentifier<Node>, TransactionHistory> deviceUpdateLogs,
                           final InstanceIdentifier<Node> iid) {
        session.getConsole().println(SEPERATOR + " START " + SEPERATOR);
        List<HwvtepTransactionLogElement> controllerTxLog = controllerTxLogs.get(iid).getElements()
                .stream().map(ele -> new HwvtepTransactionLogElement(ele, false)).collect(Collectors.toList());
        List<HwvtepTransactionLogElement> deviceUpdateLog = deviceUpdateLogs.get(iid).getElements()
                .stream().map(ele -> new HwvtepTransactionLogElement(ele, true)).collect(Collectors.toList());
        List<Pair<HwvtepTransactionLogElement, Boolean>> allLogs = mergeLogsByDate(controllerTxLog, deviceUpdateLog);
        session.getConsole().print("Printing for Node :  ");
        session.getConsole().println(iid.firstKeyOf(Node.class).getNodeId().getValue());
        printLogs(allLogs);
        session.getConsole().println(SEPERATOR + " END " + SEPERATOR);
        session.getConsole().println();
    }

    private void printLogs(final List<Pair<HwvtepTransactionLogElement, Boolean>> logs) {
        logs.forEach(pair -> {
            HwvtepTransactionLogElement log = pair.getLeft();
            session.getConsole().print(new Date(log.getDate()));
            session.getConsole().print(" ");
            session.getConsole().print(pair.getRight() ? "CONTROLLER" : "DEVICE");
            session.getConsole().print(" ");
            session.getConsole().print(log.getTransactionType());
            session.getConsole().print(" ");
            session.getConsole().println(log.getData());
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
