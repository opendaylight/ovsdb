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
 * Created by dave on 25/06/2014.
 */
public class UuidHelper {

    private static String uuid;
    private static String bridgeUuid;
    private static String portUuid;

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
}
