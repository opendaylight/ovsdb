/*
 * Copyright (c) 2015 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import java.util.Collection;

import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

public abstract class AbstractTransactCommand implements TransactCommand {

    private HwvtepOperationalState operationalState;
    private Collection<DataTreeModification<Node>> changes;

    protected AbstractTransactCommand() {
        // NO OP
    }

    public AbstractTransactCommand(HwvtepOperationalState state, Collection<DataTreeModification<Node>> changes) {
        this.operationalState = state;
        this.changes = changes;
    }

    public HwvtepOperationalState getOperationalState() {
        return operationalState;
    }

    public Collection<DataTreeModification<Node>> getChanges() {
        return changes;
    }
}
