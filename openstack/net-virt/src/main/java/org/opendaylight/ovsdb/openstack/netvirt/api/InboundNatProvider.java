/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.api;

import java.net.InetAddress;

/**
 *  This interface allows NAT flows to be written to devices
 */
public interface InboundNatProvider {
    Status programIpRewriteRule(Long dpid, Long inPort, String destSegId, InetAddress matchAddress,
                                InetAddress rewriteAddress, Action action);

    Status programIpRewriteExclusion(Long dpid, String segmentationId,
                                     String excludedCidr, Action action);

}
