/*
 * Copyright © 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc.utils;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.AccessListsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.AccessList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.AccessListBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.access.list.AccessListEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.access.list.access.list.entries.AccessListEntry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.access.list.access.list.entries.AccessListEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.access.list.access.list.entries.access.list.entry.ActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.access.list.access.list.entries.access.list.entry.MatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.access.list.access.list.entries.access.list.entry.actions.packet.handling.PermitBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.access.list.access.list.entries.access.list.entry.matches.ace.type.AceIpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.packet.fields.rev140625.acl.transport.header.fields.DestinationPortRangeBuilder;

public class AclUtils {
    public MatchesBuilder createMatches (MatchesBuilder matchesBuilder, int destPort) {
        PortNumber portNumber = new PortNumber(destPort);
        DestinationPortRangeBuilder destinationPortRangeBuilder = new DestinationPortRangeBuilder();
        destinationPortRangeBuilder.setLowerPort(portNumber);
        destinationPortRangeBuilder.setUpperPort(portNumber);

        AceIpBuilder aceIpBuilder = new AceIpBuilder();
        aceIpBuilder.setDestinationPortRange(destinationPortRangeBuilder.build());
        matchesBuilder.setAceType(aceIpBuilder.build());

        return matchesBuilder;
    }

    public ActionsBuilder createActions (ActionsBuilder actionsBuilder, Boolean permit) {
        PermitBuilder permitBuilder = new PermitBuilder();
        permitBuilder.setPermit(Boolean.TRUE);
        actionsBuilder.setPacketHandling(permitBuilder.build());

        return actionsBuilder;
    }

    public AccessListEntryBuilder createAccessListEntryBuilder(AccessListEntryBuilder accessListEntryBuilder,
                                                               String ruleName,
                                                               MatchesBuilder matchesBuilder,
                                                               ActionsBuilder actionsBuilder) {
        accessListEntryBuilder.setRuleName(ruleName);
        accessListEntryBuilder.setMatches(matchesBuilder.build());
        accessListEntryBuilder.setActions(actionsBuilder.build());

        return accessListEntryBuilder;
    }

    public AccessListEntriesBuilder createAccessListEntries(AccessListEntriesBuilder accessListEntriesBuilder,
                                                            AccessListEntryBuilder accessListEntryBuilder) {
        List<AccessListEntry> accessListEntriesList = new ArrayList<>();
        accessListEntriesList.add(accessListEntryBuilder.build());

        return accessListEntriesBuilder;
    }

    public AccessListBuilder createAccessList(AccessListBuilder accessListBuilder,
                                              String aclName,
                                              AccessListEntriesBuilder accessListEntriesBuilder) {
        accessListBuilder.setAclName(aclName);
        accessListBuilder.setAccessListEntries(accessListEntriesBuilder.build());

        return accessListBuilder;
    }

    public AccessListsBuilder createAccessLists(AccessListsBuilder accessListsBuilder,
                                                AccessListBuilder accessListBuilder) {
        List<AccessList> accessListList = new ArrayList<>();
        accessListList.add(accessListBuilder.build());
        accessListsBuilder.setAccessList(accessListList);

        return accessListsBuilder;
    }
}
