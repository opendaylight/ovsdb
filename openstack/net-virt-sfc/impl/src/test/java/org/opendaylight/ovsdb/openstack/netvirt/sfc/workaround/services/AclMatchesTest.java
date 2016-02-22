/*
 * Copyright © 2016 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc.workaround.services;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.ovsdb.utils.mdsal.openflow.MatchUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.MatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;

public class AclMatchesTest {
    @Test
    public void buildMatchTest() {
        AclUtils aclUtils = new AclUtils();
        MatchesBuilder matchesBuilder = aclUtils.matchesBuilder(new MatchesBuilder(), 80);
        AclMatches aclMatches = new AclMatches(matchesBuilder.build());
        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createIpProtocolMatch(matchBuilder, (short)6);
        MatchUtils.addLayer4Match(matchBuilder, 6, 0, 80);
        assertEquals(matchBuilder.build(), aclMatches.buildMatch().build());
    }
}
