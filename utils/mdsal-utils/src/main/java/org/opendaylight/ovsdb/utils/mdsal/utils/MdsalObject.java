/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.utils.mdsal.utils;

import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

public final class MdsalObject<T extends DataObject> {
    private final DataObjectIdentifier<T> iid;
    private final T dataObject;

    public MdsalObject(final DataObjectIdentifier<T> iid, final T dataObject) {
        this.dataObject = dataObject;
        this.iid = iid;
    }

    @Override
    public String toString() {
        return "MdsalObject{" + "dataObject=" + dataObject + ", iid=" + iid + '}';
    }
}
