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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChains;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

public class SfcDataListener extends AbstractDataListener {
    private static final Logger logger = LoggerFactory.getLogger(SfcDataListener.class);

    public SfcDataListener (DataBroker dataBroker) {
        setDataBroker(dataBroker);
        setIID(InstanceIdentifierUtils.createServiceFunctionChainsPath());
        registerAsDataChangeListener();
    }

    @Override
    public void onDataChanged (final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change ) {
        logger.trace("\nOVSSFC Enter: {}", Thread.currentThread().getStackTrace()[1]);

        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionChains) {
                ServiceFunctionChains updatedServiceFunctionChains = (ServiceFunctionChains) entry.getValue();
                logger.trace("\nOVSSFC sfc:\n   {}", updatedServiceFunctionChains.toString());
            }
        }

        logger.trace("\nOVSSFC Exit: {}", Thread.currentThread().getStackTrace()[1]);
    }
}
