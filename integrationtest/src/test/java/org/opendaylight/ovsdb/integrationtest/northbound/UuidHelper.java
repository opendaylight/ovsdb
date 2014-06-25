/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Dave Tucker
 */

package org.opendaylight.ovsdb.integrationtest.northbound;

/**
 * This class persists UUID's generated during the IT run. This is required
 * as the parameterized runner creates an instance of the NorthboundIT class for every test case
 */
public class UuidHelper {

    private static String uuid;
    private static String bridgeUuid;
    private static String portUuid;
    private static String qosUuid;
    private static String ovsUuid;

    public static String getUuid() {
        return uuid;
    }

    public static void setUuid(String uuid) {
        UuidHelper.uuid = uuid;
    }

    public static String getBridgeUuid() {
        return bridgeUuid;
    }

    public static void setBridgeUuid(String bridgeUuid) {
        UuidHelper.bridgeUuid = bridgeUuid;
    }

    public static String getPortUuid() {
        return portUuid;
    }

    public static void setPortUuid(String portUuid) {
        UuidHelper.portUuid = portUuid;
    }

    public static String getQosUuid() {
        return qosUuid;
    }

    public static void setQosUuid(String qosUuid) {
        UuidHelper.qosUuid = qosUuid;
    }

    public static String getOvsUuid() {
        return ovsUuid;
    }

    public static void setOvsUuid(String ovsUuid) {
        UuidHelper.ovsUuid = ovsUuid;
    }
}
