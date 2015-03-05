/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Dave Tucker, Flavio Fernandes
 */

package org.opendaylight.ovsdb.openstack.netvirt.api;

import org.opendaylight.ovsdb.plugin.api.Status;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;

import java.net.InetAddress;

/**
 * This interface allows ARP flows to be written to devices
 */
public interface ArpProvider {

    Status programStaticArpEntry(Node node, Long dpid, String segmentationId,
                                 String macAddress, InetAddress ipAddress, Action action);

}
