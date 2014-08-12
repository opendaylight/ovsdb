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
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class SffDataListener implements DataChangeListener {
    private static final Logger logger = LoggerFactory.getLogger(SffDataListener.class);
    private DataBroker dataBroker;
    private ListenerRegistration<org.opendaylight.controller.md.sal.binding.api.DataChangeListener> sffDataChangeListenerRegistration;

    public SffDataListener (DataBroker dataBroker) {
        setDataBroker(dataBroker);
        registerAsDataChangeListener();
    }

    public void setDataBroker (DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change ) {
        logger.info("\n***OVSSFC Enter: {}", Thread.currentThread().getStackTrace()[1]);

        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();
        /*
         * when a SFF is created we will process and send it to southbound devices. But first we need
         * to make sure all info is present or we will pass.
         */
        //boolean sffready = false;
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionForwarders) {

                ServiceFunctionForwarders updatedServiceFunctionForwarders = (ServiceFunctionForwarders) entry.getValue();
                List<ServiceFunctionForwarder> serviceFunctionForwarderList = updatedServiceFunctionForwarders.getServiceFunctionForwarder();
                for (ServiceFunctionForwarder serviceFunctionForwarder : serviceFunctionForwarderList) {
                    logger.info("\n***OVSSFC sff: {}", serviceFunctionForwarder.getName());
                    //Object[] serviceForwarderObj = {updatedServiceFunctionForwarders};
                    //Class[] serviceForwarderClass = {ServiceFunctionForwarders.class};
                    //odlSfc.executor.execute(SfcProviderRestAPI.getPutServiceFunctionForwarders (serviceForwarderObj, serviceForwarderClass));
                }
            }
        }

        logger.info("\n***OVSSFC Exit: {}", Thread.currentThread().getStackTrace()[1]);
    }

    private void registerAsDataChangeListener() {
        logger.info("\n***OVSSFC Enter: {}", Thread.currentThread().getStackTrace()[1]);

        InstanceIdentifier<ServiceFunctionForwarders> sffIID =
                InstanceIdentifier.builder(ServiceFunctionForwarders.class).build();

        sffDataChangeListenerRegistration = dataBroker.registerDataChangeListener( LogicalDatastoreType.CONFIGURATION,
                sffIID, this, DataBroker.DataChangeScope.SUBTREE );

        logger.info("\n***OVSSFC Exit: {}", Thread.currentThread().getStackTrace()[1]);
    }
}
