/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.reconciliation;

import org.opendaylight.ovsdb.southbound.OvsdbConnectionManager;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Anil Vishnoi (avishnoi@Brocade.com) on 3/9/16.
 */
public abstract class ReconciliationTask {

    private static final Logger LOG = LoggerFactory.getLogger(ReconciliationTask.class);

    private final InstanceIdentifier<?> nodeIid;
    private final DataObject configData;

    protected ReconciliationTask(InstanceIdentifier<?> nodeIid, DataObject configData) {
        this.nodeIid = nodeIid;
        this.configData = configData;
    }

    public abstract boolean reconcileConfiguration(OvsdbConnectionManager connectionManager);

    public InstanceIdentifier<?> getNodeIid() {
        return nodeIid;
    }

    public DataObject getConfigData() {
        return configData;
    }
}
