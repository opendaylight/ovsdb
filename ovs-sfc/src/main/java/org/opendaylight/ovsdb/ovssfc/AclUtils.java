/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.ovssfc;

//import com.google.common.base.Optional;
//import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
//import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.Actions1;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.Actions1Builder;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.access.lists.access.list.access.list.entries.actions.SfcAction;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.access.lists.access.list.access.list.entries.actions.sfc.action.AclServiceFunctionPath;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.access.lists.access.list.access.list.entries.actions.sfc.action.AclServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.AccessListsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.AccessList;
//import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.AccessListBuilder;
//import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.AccessListEntries;
//import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.AccessListEntriesBuilder;
//import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.ActionsBuilder;
//import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.MatchesBuilder;
//import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.matches.ace.type.AceIpBuilder;
//import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
//import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.packet.fields.rev140625.acl.transport.header.fields.DestinationPortRangeBuilder;
//import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.ExecutionException;

public class AclUtils {
    private static final Logger logger = LoggerFactory.getLogger(AclUtils.class);
    private AccessList accessList;
    private AccessLists accessLists;

    public AclUtils () {
        // TODO: remove these when then acl restconf is fixed
        //setAccessList();
        //setAccessLists();
    }
/*
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
        // TODO: revert when reston fix is in
        //AccessLists accessLists = readAccessLists();
        AccessLists accessLists = this.accessLists;

        List<AccessList> accessListList = accessLists.getAccessList();
        for (AccessList accessList : accessListList) {
            List<AccessListEntries> accessListEntriesList = accessList.getAccessListEntries();
            for (AccessListEntries accessListEntries : accessListEntriesList) {
                SfcAction sfcAction = accessListEntries.getActions().getAugmentation(Actions1.class).getSfcAction();
                //String aclServicePathName = ((AclServiceFunctionPath)sfcAction).getServiceFunctionPath();
                //if (servicePathName.equals(aclServicePathName)) {
                //    accessListEntry = accessListEntries;
                //    break;
                //}
            }
        }

        return accessListEntry;
    }

    public AccessListEntries getAccessList () {
        AccessListEntries accessListEntry = null;

        setAccessList();

        List<AccessListEntries> accessListEntriesList = accessList.getAccessListEntries();
        for (AccessListEntries accessListEntries : accessListEntriesList) {
            accessListEntry = accessListEntries;
        }

        return accessListEntry;
    }

    private AccessList setAccessList () {
        PortNumber portNumber = new PortNumber(80);
        DestinationPortRangeBuilder destinationPortRangeBuilder = new DestinationPortRangeBuilder();
        destinationPortRangeBuilder.setLowerPort(portNumber);
        destinationPortRangeBuilder.setUpperPort(portNumber);

        AceIpBuilder aceIpBuilder = new AceIpBuilder();
        aceIpBuilder.setDestinationPortRange(destinationPortRangeBuilder.build());

        MatchesBuilder matchesBuilder = new MatchesBuilder();
        matchesBuilder.setAceType(aceIpBuilder.build());

        //AclServiceFunctionPathBuilder aclServiceFunctionPathBuilder = new AclServiceFunctionPathBuilder();
        //aclServiceFunctionPathBuilder.setServiceFunctionPath("sfp1");

        Actions1Builder actions1Builder = new Actions1Builder();
        //actions1Builder.setSfcAction(aclServiceFunctionPathBuilder.build());

        ActionsBuilder actionsBuilder = new ActionsBuilder();
        actionsBuilder.addAugmentation(Actions1.class, actions1Builder.build());

        AccessListEntriesBuilder accessListEntriesBuilder = new AccessListEntriesBuilder();
        accessListEntriesBuilder.setRuleName("http");
        accessListEntriesBuilder.setMatches(matchesBuilder.build());
        accessListEntriesBuilder.setActions(actionsBuilder.build());
        List<AccessListEntries> accessListEntriesList = new ArrayList<>();
        accessListEntriesList.add(accessListEntriesBuilder.build());

        AccessListBuilder accessListBuilder = new AccessListBuilder();
        accessListBuilder.setAclName("http");
        accessListBuilder.setAccessListEntries(accessListEntriesList);

        logger.trace("acl: {}", accessListBuilder.build());
        accessList = accessListBuilder.build();
        return accessListBuilder.build();
    }

    private AccessLists setAccessLists () {
        PortNumber portNumber = new PortNumber(80);
        DestinationPortRangeBuilder destinationPortRangeBuilder = new DestinationPortRangeBuilder();
        destinationPortRangeBuilder.setLowerPort(portNumber);
        destinationPortRangeBuilder.setUpperPort(portNumber);

        AceIpBuilder aceIpBuilder = new AceIpBuilder();
        aceIpBuilder.setDestinationPortRange(destinationPortRangeBuilder.build());

        MatchesBuilder matchesBuilder = new MatchesBuilder();
        matchesBuilder.setAceType(aceIpBuilder.build());

        //AclServiceFunctionPathBuilder aclServiceFunctionPathBuilder = new AclServiceFunctionPathBuilder();
        //aclServiceFunctionPathBuilder.setServiceFunctionPath("sfp1");

        Actions1Builder actions1Builder = new Actions1Builder();
        //actions1Builder.setSfcAction(aclServiceFunctionPathBuilder.build());

        ActionsBuilder actionsBuilder = new ActionsBuilder();
        actionsBuilder.addAugmentation(Actions1.class, actions1Builder.build());

        AccessListEntriesBuilder accessListEntriesBuilder = new AccessListEntriesBuilder();
        accessListEntriesBuilder.setRuleName("http");
        accessListEntriesBuilder.setMatches(matchesBuilder.build());
        accessListEntriesBuilder.setActions(actionsBuilder.build());
        List<AccessListEntries> accessListEntriesList = new ArrayList<>();
        accessListEntriesList.add(accessListEntriesBuilder.build());

        AccessListBuilder accessListBuilder = new AccessListBuilder();
        accessListBuilder.setAclName("http");
        accessListBuilder.setAccessListEntries(accessListEntriesList);
        List<AccessList> accessListList = new ArrayList<>();
        accessListList.add(accessListBuilder.build());

        AccessListsBuilder accessListsBuilder = new AccessListsBuilder();
        accessListsBuilder.setAccessList(accessListList);

        logger.trace("acls: {}", accessListsBuilder.build());
        accessLists = accessListsBuilder.build();
        return accessListsBuilder.build();
    }
*/
}
