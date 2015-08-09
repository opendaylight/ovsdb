/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.ovssfc;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class AbstractDataListener implements DataChangeListener {
    private DataBroker dataBroker;
    private InstanceIdentifier<?> iID;
    private ListenerRegistration<DataChangeListener> dataChangeListenerRegistration;

    public void setDataBroker (DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public void setIID (InstanceIdentifier<?> IID) {
        this.iID = IID;
    }

    public void registerAsDataChangeListener () {
        dataChangeListenerRegistration = dataBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                iID, this, DataBroker.DataChangeScope.SUBTREE);
    }

    public void registerAsDataChangeListener (AsyncDataBroker.DataChangeScope dataChangeScope) {
        dataChangeListenerRegistration = dataBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                iID, this, dataChangeScope);
    }

    public void closeDataChangeListener () {
        dataChangeListenerRegistration.close();
    }
}
