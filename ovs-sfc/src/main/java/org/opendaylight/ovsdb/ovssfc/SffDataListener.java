/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.ovssfc;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

public class SffDataListener extends AbstractDataListener {
    private static final Logger logger = LoggerFactory.getLogger(SffDataListener.class);

    public SffDataListener (DataBroker dataBroker) {
        setDataBroker(dataBroker);
        setIID(InstanceIdentifierUtils.createServiceFunctionForwarderPath());
        registerAsDataChangeListener();
    }

    @Override
    public void onDataChanged (final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change ) {
        Map<InstanceIdentifier<?>, DataObject> dataObject;
        logger.trace("\nOVSSFC Enter: {}", Thread.currentThread().getStackTrace()[1]);

        dataObject = change.getCreatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionForwarder) {
                ServiceFunctionForwarder serviceFunctionForwarder = (ServiceFunctionForwarder) entry.getValue();
                logger.trace("\nOVSSFC CREATE: sff:\n   {}", serviceFunctionForwarder.toString());
            }
        }

        dataObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionForwarder) {
                ServiceFunctionForwarder serviceFunctionForwarder = (ServiceFunctionForwarder) entry.getValue();
                logger.trace("\nOVSSFC UPDATE: sff:\n   {}", serviceFunctionForwarder.toString());
            }
        }

        logger.trace("\nOVSSFC Exit: {}", Thread.currentThread().getStackTrace()[1]);
    }
}
