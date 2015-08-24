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
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.ServiceFunctionForwarder1;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.service.function.forwarders.service.function.forwarder.ovs.ExternalIds;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import java.util.List;
import java.util.concurrent.ExecutionException;

public class SffUtils {
    private static final Logger logger = LoggerFactory.getLogger(SffUtils.class);

    public ServiceFunctionForwarder readServiceFunctionForwarder (String name) {
        InstanceIdentifier<ServiceFunctionForwarder> iID =
                InstanceIdentifierUtils.createServiceFunctionForwarderPath(name);

        ReadOnlyTransaction readTx = OvsSfcProvider.getOvsSfcProvider().getDataBroker().newReadOnlyTransaction();
        Optional<ServiceFunctionForwarder> dataObject = null;
        try {
            dataObject = readTx.read(LogicalDatastoreType.CONFIGURATION, iID).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        if ((dataObject != null) && (dataObject.get() != null)) {
            logger.trace("\nOVSSFC Exit: {}\n   sff: {}",
                    Thread.currentThread().getStackTrace()[1], dataObject.get().toString());
            return dataObject.get();
        } else {
            logger.trace("\nOVSSFC Exit: {}, sff: null", Thread.currentThread().getStackTrace()[1]);
            return null;
        }
    }

    public String getSystemId (ServiceFunctionForwarder serviceFunctionForwarder) {
        String systemId = "";

        //List<ExternalIds> externalIds = serviceFunctionForwarder.getAugmentation(ServiceFunctionForwarder1.class).getOvs().getExternalIds();
        //for (ExternalIds externalId : externalIds) {
        //    if (externalId.getName().equals("system-id")) {
        //        systemId = externalId.getValue();
        //    }
        //}

        logger.trace("\nOVSSFC {}\n system-id: {}",
                Thread.currentThread().getStackTrace()[1],
                systemId);

        return systemId;
    }
}
