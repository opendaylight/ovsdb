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
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
//import java.util.Set;

public class SfpDataListener extends AbstractDataListener {
    private static final Logger logger = LoggerFactory.getLogger(SfpDataListener.class);

    public SfpDataListener (DataBroker dataBroker) {
        setDataBroker(dataBroker);
        //setIID(InstanceIdentifierUtils.createServiceFunctionPathsPath());
        setIID(InstanceIdentifierUtils.createServiceFunctionPathPath());
        registerAsDataChangeListener(DataBroker.DataChangeScope.BASE);
    }

    @Override
    public void onDataChanged (final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change ) {
        Map<InstanceIdentifier<?>, DataObject> dataObject;

        logger.trace("\nOVSSFC Enter: {}", Thread.currentThread().getStackTrace()[1]);
/* Keeping for reference in case I need to use it
        DataObject dom = change.getOriginalSubtree();
        if (dom instanceof ServiceFunctionPaths) {
            ServiceFunctionPaths serviceFunctionPaths = (ServiceFunctionPaths) dom;
            logger.trace("\nOVSSFC ORIGINAL SUBTREE: sfps:\n   {}", serviceFunctionPaths.toString());

            //OvsSfcProvider.getOvsSfcProvider().eventHandler.enqueueSfpEvent(SfcEvent.Action.CREATE, serviceFunctionPaths);
        }
        dom = change.getUpdatedSubtree();
        if (dom instanceof ServiceFunctionPaths) {
            ServiceFunctionPaths serviceFunctionPaths = (ServiceFunctionPaths) dom;
            logger.trace("\nOVSSFC UPDATED SUBTREE: sfps:\n   {}", serviceFunctionPaths.toString());

            OvsSfcProvider.getOvsSfcProvider().eventHandler.enqueueSfpEvent(SfcEvent.Action.UPDATE, serviceFunctionPaths);
        }

        dataObject = change.getOriginalData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionPaths) {
                ServiceFunctionPaths serviceFunctionPaths = (ServiceFunctionPaths) entry.getValue();
                logger.trace("\nOVSSFC ORIGINAL: sfps:\n   {}", serviceFunctionPaths.toString());

                //OvsSfcProvider.getOvsSfcProvider().eventHandler.enqueueSfpEvent(SfcEvent.Action.CREATE, serviceFunctionPaths);
            }
        }

        Map<InstanceIdentifier<?>, DataObject> originalDataObject = change.getOriginalData();
        Set<InstanceIdentifier<?>> iID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : iID) {
            DataObject dObject = originalDataObject.get(instanceIdentifier);
            if (dObject instanceof ServiceFunctionPath) {
                ServiceFunctionPath serviceFunctionPath = (ServiceFunctionPath) dataObject;
                logger.trace("\nOVSSFC DELETE: sfp:\n   {}", serviceFunctionPath.toString());

                OvsSfcProvider.getOvsSfcProvider().eventHandler.enqueueSfpEvent(SfcEvent.Action.DELETE, serviceFunctionPath);
            }
        }

        dataObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionPaths) {
                ServiceFunctionPaths updatedServiceFunctionPaths = (ServiceFunctionPaths) entry.getValue();
                logger.trace("\nOVSSFC UPDATE: sfps:\n   {}", updatedServiceFunctionPaths.toString());

                OvsSfcProvider.getOvsSfcProvider().eventHandler.enqueueSfpsEvent(SfcEvent.Action.UPDATE, updatedServiceFunctionPaths);
            }
        }
*/
/*
        dataObject = change.getCreatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionPaths) {
                ServiceFunctionPaths serviceFunctionPaths = (ServiceFunctionPaths) entry.getValue();
                logger.trace("\nOVSSFC CREATE: sfps:\n   {}", serviceFunctionPaths.toString());

                OvsSfcProvider.getOvsSfcProvider().eventHandler.enqueueSfpsEvent(SfcEvent.Action.CREATE, serviceFunctionPaths);
            }
        }
*/
        dataObject = change.getCreatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionPath) {
                ServiceFunctionPath serviceFunctionPath = (ServiceFunctionPath) entry.getValue();
                logger.trace("\nOVSSFC CREATE: sfp:\n   {}", serviceFunctionPath.toString());

                OvsSfcProvider.getOvsSfcProvider().eventHandler.enqueueSfpEvent(SfcEvent.Action.CREATE, serviceFunctionPath);
            }
        }

        dataObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionPath) {
                ServiceFunctionPath updatedServiceFunctionPath = (ServiceFunctionPath) entry.getValue();
                logger.trace("\nOVSSFC UPDATE: sfp:\n   {}", updatedServiceFunctionPath.toString());

                OvsSfcProvider.getOvsSfcProvider().eventHandler.enqueueSfpEvent(SfcEvent.Action.UPDATE, updatedServiceFunctionPath);
            }
        }

       logger.trace("\nOVSSFC Exit: {}", Thread.currentThread().getStackTrace()[1]);
    }
}
