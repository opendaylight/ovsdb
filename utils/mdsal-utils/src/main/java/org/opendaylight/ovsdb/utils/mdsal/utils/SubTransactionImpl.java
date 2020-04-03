/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.utils.mdsal.utils;

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SubTransactionImpl implements SubTransaction {
    private Object instance;
    private InstanceIdentifier identifier;
    private short action;

    public SubTransactionImpl() {
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }

    public Object getInstance() {
        return this.instance;
    }

    public void setInstanceIdentifier(InstanceIdentifier instanceIdentifier) {
        this.identifier = instanceIdentifier;
    }

    public InstanceIdentifier getInstanceIdentifier() {
        return this.identifier;
    }

    public void setAction(short action) {
        this.action = action;
    }

    public short getAction() {
        return action;
    }
}
