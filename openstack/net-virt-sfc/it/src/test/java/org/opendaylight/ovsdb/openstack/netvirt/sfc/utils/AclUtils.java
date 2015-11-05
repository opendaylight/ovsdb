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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.AccessListsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.AclBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.AccessListEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.AceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.ActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.MatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.actions.packet.handling.PermitBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev150611.acl.transport.header.fields.DestinationPortRangeBuilder;

public class AclUtils {
    public MatchesBuilder createMatches (MatchesBuilder matchesBuilder, int destPort) {
        PortNumber portNumber = new PortNumber(destPort);
        DestinationPortRangeBuilder destinationPortRangeBuilder = new DestinationPortRangeBuilder();
        destinationPortRangeBuilder.setLowerPort(portNumber);
        destinationPortRangeBuilder.setUpperPort(portNumber);

        AceIpBuilder aceIpBuilder = new AceIpBuilder();
        aceIpBuilder.setDestinationPortRange(destinationPortRangeBuilder.build());
        aceIpBuilder.setProtocol((short)6);
        aceIpBuilder.setAceIpVersion(new AceIpv4Builder().build());
        matchesBuilder.setAceType(aceIpBuilder.build());

        return matchesBuilder;
    }

    public ActionsBuilder createActions (ActionsBuilder actionsBuilder, Boolean permit) {
        PermitBuilder permitBuilder = new PermitBuilder();
        permitBuilder.setPermit(Boolean.TRUE);
        actionsBuilder.setPacketHandling(permitBuilder.build());

        return actionsBuilder;
    }

    public AceBuilder createAccessListEntryBuilder(AceBuilder accessListEntryBuilder,
                                                   String ruleName,
                                                   MatchesBuilder matchesBuilder,
                                                   ActionsBuilder actionsBuilder) {
        accessListEntryBuilder.setRuleName(ruleName);
        accessListEntryBuilder.setMatches(matchesBuilder.build());
        accessListEntryBuilder.setActions(actionsBuilder.build());

        return accessListEntryBuilder;
    }

    public AccessListEntriesBuilder createAccessListEntries(AccessListEntriesBuilder accessListEntriesBuilder,
                                                            AceBuilder accessListEntryBuilder) {
        List<Ace> accessListEntriesList = new ArrayList<>();
        accessListEntriesList.add(accessListEntryBuilder.build());

        return accessListEntriesBuilder;
    }

    public AclBuilder createAccessList(AclBuilder accessListBuilder,
                                       String aclName,
                                       AccessListEntriesBuilder accessListEntriesBuilder) {
        accessListBuilder.setAclName(aclName);
        accessListBuilder.setAccessListEntries(accessListEntriesBuilder.build());

        return accessListBuilder;
    }

    public AccessListsBuilder createAccessLists(AccessListsBuilder accessListsBuilder,
                                                AclBuilder accessListBuilder) {
        List<Acl> accessListList = new ArrayList<>();
        accessListList.add(accessListBuilder.build());
        accessListsBuilder.setAcl(accessListList);

        return accessListsBuilder;
    }
}
