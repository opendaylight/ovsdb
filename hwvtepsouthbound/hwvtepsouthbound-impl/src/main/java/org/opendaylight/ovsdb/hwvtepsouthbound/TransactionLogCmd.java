/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionElement;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionLog;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Command(scope = "hwvtep", name = "txlog", description = "prints hwvtep tx log")
public class TransactionLogCmd extends OsgiCommandSupport {

    @Option(name = "-nodeid", description = "Node Id",
            required = false, multiValued = false)
    String nodeid;

    private HwvtepConnectionManager hwvtepConnectionManager;
    private DataBroker dataBroker;

    public void setDataBroker(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public void setHwvtepConnectionManager(HwvtepConnectionManager hwvtepConnectionManager) {
        this.hwvtepConnectionManager = hwvtepConnectionManager;
    }

    @Override
    protected Object doExecute() throws Exception {
        Map<InstanceIdentifier<Node>, TransactionLog> controllerTxLogs = hwvtepConnectionManager.getControllerTxLog();
        Map<InstanceIdentifier<Node>, TransactionLog> deviceUpdateLogs = hwvtepConnectionManager.getDeviceUpdateLog();
        if (nodeid != null) {
            InstanceIdentifier<Node> iid = HwvtepSouthboundMapper.createInstanceIdentifier(new NodeId(nodeid));
            printLogs(controllerTxLogs, deviceUpdateLogs, iid);
        } else {
            Map<InstanceIdentifier<Node>, TransactionLog> txlogs
                    = controllerTxLogs.isEmpty() ? deviceUpdateLogs : controllerTxLogs;
            txlogs.keySet().forEach( (iid) -> {
                printLogs(controllerTxLogs, deviceUpdateLogs, iid);
            });
            session.getConsole().println("Device tx logs size "+deviceUpdateLogs.keySet().size());
        }
        return null;
    }

    private void printLogs(Map<InstanceIdentifier<Node>, TransactionLog> controllerTxLogs,
                           Map<InstanceIdentifier<Node>, TransactionLog> deviceUpdateLogs,
                           InstanceIdentifier<Node> iid) {
        session.getConsole().println("Printing for iid "+ iid);
        List<HwvtepTransactionLogElement> controllerTxLog = controllerTxLogs.get(iid).getElements()
                .stream().map(ele -> new HwvtepTransactionLogElement(ele, false)).collect(Collectors.toList());
        List<HwvtepTransactionLogElement> deviceUpdateLog = deviceUpdateLogs.get(iid).getElements()
                .stream().map(ele -> new HwvtepTransactionLogElement(ele, false)).collect(Collectors.toList());
        //deviceUpdateLog.forEach( (log) -> log.setDeviceLog(true));
        List<HwvtepTransactionLogElement> allLogs = mergeLogsByDate(controllerTxLog, deviceUpdateLog);
        session.getConsole().println("======================================");
        session.getConsole().println("======================================");
        session.getConsole().print("printing logs for node ");
        session.getConsole().println(iid);
        printLogs(allLogs);
    }

    private void sortLogsByDate(ArrayList<TransactionElement> logs) {
        Collections.sort(logs, new Comparator<TransactionElement>() {
            @Override
            public int compare(TransactionElement o1, TransactionElement o2) {
                return (int) (o1.getDate() - o2.getDate());
            }
        });
    }

    private List<HwvtepTransactionLogElement> mergeLogsByDate(
            List<HwvtepTransactionLogElement> logs1,
            List<HwvtepTransactionLogElement> logs2) {

        ArrayList<HwvtepTransactionLogElement> result = new ArrayList();
        int firstIdx = 0;
        int secondIdx = 0;
        int firstSize = logs1.size();
        int secondSize = logs2.size();
        while ( firstIdx < firstSize && secondIdx < secondSize) {
            if (logs1.get(firstIdx).getDate() < logs2.get(secondIdx).getDate()) {
                result.add(logs1.get(firstIdx));
                firstIdx++;
            } else {
                result.add(logs2.get(secondIdx));
                secondIdx++;
            }
        }
        while (firstIdx < firstSize) {
            result.add(logs1.get(firstIdx));
            firstIdx++;
        }
        while (secondIdx < secondSize) {
            result.add(logs2.get(secondIdx));
            secondIdx++;
        }
        return result;
    }

    private void printLogs(List<HwvtepTransactionLogElement> logs) {
        logs.forEach( (log) -> {
            session.getConsole().print(new Date(log.getDate()));
            session.getConsole().print(" ");
            session.getConsole().print(log.getTransactionType());
            session.getConsole().print(" ");
            session.getConsole().println(log.getData());
        });
    }
}
