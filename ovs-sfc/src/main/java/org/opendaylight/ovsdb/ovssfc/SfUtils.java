/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.ovssfc;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public class SfUtils {
    private static final Logger logger = LoggerFactory.getLogger(SfUtils.class);

    public ServiceFunction readServiceFunction (String name) {
        InstanceIdentifier<ServiceFunction> iID =
                InstanceIdentifierUtils.createServiceFunctionPath(name);

        ReadOnlyTransaction readTx = OvsSfcProvider.getOvsSfcProvider().getDataBroker().newReadOnlyTransaction();
        Optional<ServiceFunction> dataObject = null;
        try {
            dataObject = readTx.read(LogicalDatastoreType.CONFIGURATION, iID).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        if ((dataObject != null) && (dataObject.get() != null)) {
            logger.trace("\nOVSSFC Exit: {}\n   sf: {}",
                    Thread.currentThread().getStackTrace()[1], dataObject.get().toString());
            return dataObject.get();
        } else {
            logger.trace("\nOVSSFC Exit: {}, sf: null", Thread.currentThread().getStackTrace()[1]);
            return null;
        }
    }
}
