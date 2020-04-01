/*
 * Copyright (c) 2015 - 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.utils.mdsal.utils;

import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface ResourceHandler {
    void create(WriteTransaction tx, LogicalDatastoreType datastoreType, InstanceIdentifier identifer, Object vrfEntry,
                List<SubTransaction> transactionObjects);

    void delete(WriteTransaction tx, LogicalDatastoreType datastoreType, InstanceIdentifier identifer, Object vrfEntry,
                List<SubTransaction> transactionObjects);

    void update(WriteTransaction tx, LogicalDatastoreType datastoreType, InstanceIdentifier identifier, Object original,
                Object update, List<SubTransaction> transactionObjects);

    LogicalDatastoreType getDatastoreType();

    int getBatchSize();

    int getBatchInterval();

    DataBroker getResourceBroker();
}
