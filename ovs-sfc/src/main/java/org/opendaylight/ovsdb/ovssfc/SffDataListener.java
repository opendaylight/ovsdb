/*
* Copyright (C) 2014 Red Hat, Inc.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*
* Authors : Sam Hague
*/
package org.opendaylight.ovsdb.ovssfc;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;

public class SffDataListener extends AbstractDataListener {
    private static final Logger logger = LoggerFactory.getLogger(SffDataListener.class);

    public SffDataListener (DataBroker dataBroker) {
        setDataBroker(dataBroker);
        setIID(InstanceIdentifier.builder(ServiceFunctionForwarders.class).build());
        registerAsDataChangeListener();
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change ) {
        logger.trace("\nOVSSFC Enter: {}", Thread.currentThread().getStackTrace()[1]);

        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionForwarders) {

                ServiceFunctionForwarders updatedServiceFunctionForwarders = (ServiceFunctionForwarders) entry.getValue();
                List<ServiceFunctionForwarder> serviceFunctionForwarderList = updatedServiceFunctionForwarders.getServiceFunctionForwarder();
                for (ServiceFunctionForwarder serviceFunctionForwarder : serviceFunctionForwarderList) {
                    logger.trace("\nOVSSFC sff:\n   {}", serviceFunctionForwarder.toString());

                    /*
                    List<SffDataPlaneLocator> sffDataPlaneLocatorList = serviceFunctionForwarder.getSffDataPlaneLocator();
                    for (SffDataPlaneLocator sffDataPlaneLocator : sffDataPlaneLocatorList) {
                        logger.trace("\nsffdpl: {}", sffDataPlaneLocator.toString());
                        logger.trace("\nsffdpl: name: {}", sffDataPlaneLocator.getName());
                        logger.trace("\nsffdpl: transport: {}", sffDataPlaneLocator.getTransport().getName());
                        logger.trace("\nsffdpl: dpl: {}", sffDataPlaneLocator.getDataPlaneLocator().toString());
                    }
                    */
                }
            }
        }

        logger.trace("\nOVSSFC Exit: {}", Thread.currentThread().getStackTrace()[1]);
    }
}
