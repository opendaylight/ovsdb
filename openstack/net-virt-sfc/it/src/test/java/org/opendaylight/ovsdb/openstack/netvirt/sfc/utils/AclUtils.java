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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev150611.acl.transport.header.fields.DestinationPortRangeBuilder;

public class AclUtils extends AbstractUtils {
    public MatchesBuilder matchesBuilder(MatchesBuilder matchesBuilder, int destPort) {
        PortNumber portNumber = new PortNumber(destPort);
        DestinationPortRangeBuilder destinationPortRangeBuilder = new DestinationPortRangeBuilder()
                .setLowerPort(portNumber)
                .setUpperPort(portNumber);

        AceIpBuilder aceIpBuilder = new AceIpBuilder()
                .setDestinationPortRange(destinationPortRangeBuilder.build())
                .setProtocol((short)6)
                .setAceIpVersion(new AceIpv4Builder().build());

        return matchesBuilder.setAceType(aceIpBuilder.build());
    }

    public ActionsBuilder actionsBuilder(ActionsBuilder actionsBuilder, Boolean permit) {
        return actionsBuilder.setPacketHandling(new PermitBuilder().setPermit(permit).build());
    }

    public AceBuilder aceBuilder(AceBuilder accessListEntryBuilder,
                                 String ruleName,
                                 MatchesBuilder matchesBuilder,
                                 ActionsBuilder actionsBuilder) {
        return accessListEntryBuilder
                .setRuleName(ruleName)
                .setMatches(matchesBuilder.build())
                .setActions(actionsBuilder.build());
    }

    public AccessListEntriesBuilder accessListEntriesBuidler(AccessListEntriesBuilder accessListEntriesBuilder,
                                                             AceBuilder accessListEntryBuilder) {
        List<Ace> accessListEntriesList = new ArrayList<>();
        accessListEntriesList.add(accessListEntryBuilder.build());

        return accessListEntriesBuilder;
    }

    public AclBuilder aclBuilder(AclBuilder accessListBuilder,
                                 String aclName,
                                 AccessListEntriesBuilder accessListEntriesBuilder) {
        return accessListBuilder
                .setAclName(aclName)
                .setAccessListEntries(accessListEntriesBuilder.build());
    }

    public AccessListsBuilder accessListsBuidler(AccessListsBuilder accessListsBuilder,
                                                 AclBuilder accessListBuilder) {
        List<Acl> accessListList = new ArrayList<>();
        accessListList.add(accessListBuilder.build());
        accessListsBuilder.setAcl(accessListList);

        return accessListsBuilder;
    }
}
