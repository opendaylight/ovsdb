/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.events;

import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalPort;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;

public class ReconcilePortEvent {

    private final PhysicalPort port;
    private final NodeId nodeId;

    public ReconcilePortEvent(PhysicalPort port, NodeId nodeId) {
        this.port = port;
        this.nodeId = nodeId;
    }

    @Override
    public String toString() {
        return "ReconcilePortEvent [port=" + port + ", nodeId=" + nodeId + "]";
    }
}
