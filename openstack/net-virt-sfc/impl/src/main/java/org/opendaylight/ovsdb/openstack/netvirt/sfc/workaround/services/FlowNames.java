/*
 * Copyright © 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc.workaround.services;

public class FlowNames {

    public static String getSfcIngressClass(String ruleName, long nsp, short nsi) {
        return "sfcIngressClass_" + nsp + "_" + nsi + "_" + ruleName;
    }

    public static String getSfcTable(long vxGpeOfPort) {
        return "sfcTable_" + vxGpeOfPort;
    }

    public static String getSfcEgressClass1(long vxGpeOfPort) {
        return "sfcEgressClass1_" + vxGpeOfPort;
    }

    public static String getSfcEgressClass(long vxGpeOfPort, long nsp, short nsi) {
        return "sfcEgressClass_" + nsp + "_" + nsi + "_" + vxGpeOfPort;
    }

    public static String getSfcEgressClassBypass(long nsp, short nsi, long sfOfPort) {
        return "sfcEgressClassBypass_" + nsp + "_" + nsi + "_"  + sfOfPort;
    }

    public static String getSfEgress(int dstPort) {
        return "sfEgress_" + dstPort;
    }

    public static String getSfIngress(int dstPort, String ipAddress) {
        return "sfIngress_" + dstPort + "_" + ipAddress;
    }

    public static String getArpResponder(String ipAddress) {
        return "ArpResponder_" + ipAddress;
    }
}
