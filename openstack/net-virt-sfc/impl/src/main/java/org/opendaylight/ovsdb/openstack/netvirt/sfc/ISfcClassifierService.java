/*
 * Copyright © 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc;

import java.net.InetAddress;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;

public interface ISfcClassifierService {
    void programIngressClassifier(long dataPathId, String ruleName, Matches matches, long nsp, short nsi,
                                  NshUtils nshHeader, long vxGpeOfPort, String rspName, boolean write);

    void programSfcTable(long dataPathId, long vxGpeOfPort, short goToTableId, boolean write);

    void programEgressClassifier1(long dataPathId, long vxGpeOfPort, long nsp, short nsi,
                                  int tunnelOfPort, int tunnelId, short gotoTableId, boolean write);

    void programEgressClassifier(long dataPathId, long vxGpeOfPort, long nsp, short nsi,
                                 long sfOfPort, int tunnelId, String rspName, boolean write);

    void programEgressClassifierBypass(long dataPathId, long vxGpeOfPort, long nsp, short nsi,
                                       long sfOfPort, int tunnelId, String rspName, boolean write);

    void program_sfEgress(long dataPathId, int dstPort, String rspName, boolean write);

    void program_sfIngress(long dataPathId, int dstPort, long sfOfPort,
                           String ipAddress, String sfDplName, String rspName, boolean write);

    void programStaticArpEntry(long dataPathId, long ofPort, String macAddressStr,
                               String ipAddress, String rspName, boolean write);

    void clearFlows(DataBroker dataBroker, String rspName);
}
