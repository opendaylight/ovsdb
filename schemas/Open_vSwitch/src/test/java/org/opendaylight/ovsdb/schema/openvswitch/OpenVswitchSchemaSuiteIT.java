/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Dave Tucker
 */
package org.opendaylight.ovsdb.schema.openvswitch;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;

import java.util.HashMap;
import java.util.Map;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        MonitorTestCases.class,
        BridgeTestCases.class,
        ControllerTestCases.class,
        PortAndInterfaceTestCases.class,
        NetflowTestCases.class,
        SflowTestCases.class,
        IpfixTestCases.class,
        FlowSampleCollectorSetTestCases.class,
        FlowTableTestCases.class,
        SslTestCases.class,
        QosTestCases.class,
        QueueTestCases.class,
        ManagerTestCases.class,
        MirrorTestCases.class,
        TearDown.class
})
public class OpenVswitchSchemaSuiteIT {

    // Keep this data between test runs
    static OvsdbClient ovsdbClient;
    static UUID testBridgeUuid;
    static UUID testSslUuid;
    static UUID testQosUuid;
    static UUID testQosPortUuid;
    static UUID testQueueUuid;
    static UUID testManagerUuid;
    static UUID testMirrorUuid;

    static Map<String, Map<UUID, Row>> tableCache = new HashMap<String, Map<UUID, Row>>();

    public static OvsdbClient getOvsdbClient() {
        return ovsdbClient;
    }

    public static void setOvsdbClient(OvsdbClient ovsdbClient) {
        OpenVswitchSchemaSuiteIT.ovsdbClient = ovsdbClient;
    }

    public static UUID getTestBridgeUuid() {
        return testBridgeUuid;
    }

    public static void setTestBridgeUuid(UUID testBridgeUuid) {
        OpenVswitchSchemaSuiteIT.testBridgeUuid = testBridgeUuid;
    }

    public static UUID getTestSslUuid() {
        return testSslUuid;
    }

    public static void setTestSslUuid(UUID testSslUuid) {
        OpenVswitchSchemaSuiteIT.testSslUuid = testSslUuid;
    }

    public static UUID getTestQosUuid() {
        return testQosUuid;
    }

    public static void setTestQosUuid(UUID testQosUuid) {
        OpenVswitchSchemaSuiteIT.testQosUuid = testQosUuid;
    }

    public static UUID getTestQosPortUuid() {
        return testQosPortUuid;
    }

    public static void setTestQosPortUuid(UUID testQosPortUuid) {
        OpenVswitchSchemaSuiteIT.testQosPortUuid = testQosPortUuid;
    }

    public static UUID getTestQueueUuid() {
        return testQueueUuid;
    }

    public static void setTestQueueUuid(UUID testQueueUuid) {
        OpenVswitchSchemaSuiteIT.testQueueUuid = testQueueUuid;
    }

    public static UUID getTestManagerUuid() {
        return testManagerUuid;
    }

    public static void setTestManagerUuid(UUID testManagerUuid) {
        OpenVswitchSchemaSuiteIT.testManagerUuid = testManagerUuid;
    }

    public static UUID getTestMirrorUuid() {
        return testMirrorUuid;
    }

    public static void setTestMirrorUuid(UUID testMirrorUuid) {
        OpenVswitchSchemaSuiteIT.testMirrorUuid = testMirrorUuid;
    }

    public static Map<String, Map<UUID, Row>> getTableCache() {
        return tableCache;
    }


}
