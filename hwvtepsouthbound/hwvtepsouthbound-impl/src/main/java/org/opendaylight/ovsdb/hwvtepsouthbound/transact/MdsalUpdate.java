/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import org.opendaylight.yangtools.binding.EntryObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MdsalUpdate<T extends EntryObject<?, ?>> {

    private InstanceIdentifier key;
    private T newData;
    private T oldData;

    public MdsalUpdate(InstanceIdentifier key, T newData, T oldData) {
        this.key = key;
        this.newData = newData;
        this.oldData = oldData;
    }

    public InstanceIdentifier getKey() {
        return key;
    }

    public T getNewData() {
        return newData;
    }

    public T getOldData() {
        return oldData;
    }
}