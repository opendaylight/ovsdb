/*
 * Copyright © 2016 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc.workaround.services;

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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev150611.acl.transport.header.fields.SourcePortRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.acl.rev150105.RedirectToSfc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.acl.rev150105.RedirectToSfcBuilder;

public class AclUtils {
    private static final String ACLNAME= "httpAcl";
    private static final String RULENAME= "httpRule";
    private static final String SFCNAME = "SFC";

    public MatchesBuilder matchesBuilder(MatchesBuilder matchesBuilder, int dstPort) {
        SourcePortRangeBuilder sourcePortRangeBuilder = new SourcePortRangeBuilder()
                .setLowerPort(PortNumber.getDefaultInstance("0"))
                .setUpperPort(PortNumber.getDefaultInstance("0"));

        PortNumber portNumber = new PortNumber(dstPort);
        DestinationPortRangeBuilder destinationPortRangeBuilder = new DestinationPortRangeBuilder()
                .setLowerPort(portNumber)
                .setUpperPort(portNumber);

        AceIpBuilder aceIpBuilder = new AceIpBuilder()
                .setSourcePortRange(sourcePortRangeBuilder.build())
                .setDestinationPortRange(destinationPortRangeBuilder.build())
                .setProtocol((short)6);

        return matchesBuilder.setAceType(aceIpBuilder.build());
    }

    public ActionsBuilder actionsBuilder(ActionsBuilder actionsBuilder, Boolean permit) {
        return actionsBuilder.setPacketHandling(new PermitBuilder().setPermit(permit).build());
    }

    public ActionsBuilder actionsBuilder(ActionsBuilder actionsBuilder, String sfcName, boolean renderRsp) {
        RedirectToSfcBuilder redirectToSfcBuilder = new RedirectToSfcBuilder()
                .setSfcName(sfcName)
                .setRenderRsp(renderRsp);

        return actionsBuilder.addAugmentation(RedirectToSfc.class, redirectToSfcBuilder.build());
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
                                                             AceBuilder aceBuilder) {
        List<Ace> aceList = new ArrayList<>();
        aceList.add(aceBuilder.build());

        return accessListEntriesBuilder.setAce(aceList);
    }

    public AclBuilder aclBuilder(AclBuilder aclBuilder,
                                 String aclName,
                                 AccessListEntriesBuilder accessListEntriesBuilder) {
        return aclBuilder
                .setAclName(aclName)
                .setAccessListEntries(accessListEntriesBuilder.build());
    }

    public AccessListsBuilder accessListsbuilder(AccessListsBuilder accessListsBuilder,
                                                 AclBuilder aclBuilder) {
        List<Acl> aclList = new ArrayList<>();
        aclList.add(aclBuilder.build());

        return accessListsBuilder.setAcl(aclList);
    }

    public AccessListsBuilder accessListsBuilder(boolean renderRsp) {
        String ruleName = RULENAME;
        String sfcName = SFCNAME;
        MatchesBuilder matchesBuilder = matchesBuilder(new MatchesBuilder(), 80);
        ActionsBuilder actionsBuilder = actionsBuilder(new ActionsBuilder(), sfcName, renderRsp);
        AceBuilder accessListEntryBuilder =
                aceBuilder(new AceBuilder(), ruleName, matchesBuilder, actionsBuilder);
        AccessListEntriesBuilder accessListEntriesBuilder =
                accessListEntriesBuidler(new AccessListEntriesBuilder(), accessListEntryBuilder);
        AclBuilder accessListBuilder =
                aclBuilder(new AclBuilder(), ACLNAME, accessListEntriesBuilder);
        AccessListsBuilder accessListsBuilder =
                accessListsbuilder(new AccessListsBuilder(), accessListBuilder);
        return accessListsBuilder;
    }
}
