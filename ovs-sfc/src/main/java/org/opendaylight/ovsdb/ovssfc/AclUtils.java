/*
* Copyright (C) 2014 Red Hat, Inc.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*
* Authors : Sam Hague
*/
/*
package org.opendaylight.ovsdb.ovssfc;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.AccessList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.AccessListEntries;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class AclUtils {
    private static final Logger logger = LoggerFactory.getLogger(AclUtils.class);

    public AccessLists readAccessLists () {
        InstanceIdentifier<AccessLists> iID =
                InstanceIdentifierUtils.createAccessListsPath();

        ReadOnlyTransaction readTx = OvsSfcProvider.getOvsSfcProvider().getDataBroker().newReadOnlyTransaction();
        Optional<AccessLists> dataObject = null;
        try {
            dataObject = readTx.read(LogicalDatastoreType.CONFIGURATION, iID).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        if ((dataObject != null) && (dataObject.get() != null)) {
            logger.trace("\nOVSSFC {}\n   acl: {}",
                    Thread.currentThread().getStackTrace()[1], dataObject.get().toString());
            return dataObject.get();
        } else {
            logger.trace("\nOVSSFC {}, acl: null", Thread.currentThread().getStackTrace()[1]);
            return null;
        }
    }

    public AccessListEntries getAccessList (String servicePathName) {
        AccessListEntries accessListEntry = null;
        AccessLists accessLists = readAccessLists();

        List<AccessList> accessListList = accessLists.getAccessList();
        for (AccessList accessList : accessListList) {
            List<AccessListEntries> accessListEntriesList = accessList.getAccessListEntries();
            for (AccessListEntries accessListEntries : accessListEntriesList) {
                logger.trace("accessListEntries: {}", accessListEntries);
                //String name = accessListEntries.getAugmentation(AccessListEntries.class). .getActions().getAugmentation(Actions1.class).;
                //Actions actions= accessListEntries.getActions();
                //String name = accessListEntries.getActions().getAugmentation(Actions.class) getServiceFunctionPath();
                //String name = accessListEntries.getAugmentation(Actions.class).getgetAugmentation(AclServiceFunctionPath.class).getServiceFunctionPath();
                /*
                if (accessListEntries.getAugmentation(AccessListEntries.class).getAugmentation(AclServiceFunctionPath.class).getServiceFunctionPath().equals(servicePathName)) {
                    accessListEntry = accessListEntries;
                    break;
                }
                *
            }
        }

        return accessListEntry;
    }
}
*/