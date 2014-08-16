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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

public class SfpDataListener extends AbstractDataListener {
    private static final Logger logger = LoggerFactory.getLogger(SfpDataListener.class);

    public SfpDataListener(DataBroker dataBroker) {
        setDataBroker(dataBroker);
        setIID(InstanceIdentifier.builder(ServiceFunctionPaths.class).build());
        registerAsDataChangeListener();
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change ) {
        logger.trace("\nOVSSFC Enter: {}", Thread.currentThread().getStackTrace()[1]);

        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionPaths) {
                ServiceFunctionPaths updatedServiceFunctionPaths = (ServiceFunctionPaths) entry.getValue();
                logger.trace("\nOVSSFC sfp:\n   {}", updatedServiceFunctionPaths.toString());
            }
        }

        logger.trace("\nOVSSFC Exit: {}", Thread.currentThread().getStackTrace()[1]);
    }
}
