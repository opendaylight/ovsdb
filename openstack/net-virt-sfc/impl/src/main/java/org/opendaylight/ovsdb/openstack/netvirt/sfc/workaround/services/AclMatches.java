/*
 * Copyright © 2016 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc.workaround.services;

import org.opendaylight.ovsdb.utils.mdsal.openflow.MatchUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.matches.ace.type.AceEth;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AclMatches {
    private static final Logger LOG = LoggerFactory.getLogger(AclMatches.class);
    MatchBuilder matchBuilder;
    Matches matches;

    public AclMatches(Matches matches) {
        matchBuilder = new MatchBuilder();
        this.matches = matches;
    }

    /**
     * Convert the ACL into an OpenFlow {@link MatchBuilder}
     * @return {@link MatchBuilder}
     */
    //TODO: Matches will overwrite previous matches for ethernet and ip since these methods
    // can be called successively for the same ACL.
    // This requires fixing the MatchUtils to preserve previously set fields.
    protected MatchBuilder buildMatch() {
        if (matches.getAceType() instanceof AceEth) {
            addEthMatch();
        } else if (matches.getAceType() instanceof AceIp) {
            addIpMatch();
        }

        LOG.info("buildMatch: {}", matchBuilder.build());
        return matchBuilder;
    }

    private void addEthMatch() {
        AceEth aceEth = (AceEth) matches.getAceType();
        MatchUtils.createEthSrcDstMatch(matchBuilder, aceEth.getSourceMacAddress(),
                aceEth.getDestinationMacAddress());
    }

    private void addIpMatch() {
        AceIp aceIp = (AceIp)matches.getAceType();

        if (aceIp.getDscp() != null) {
            MatchUtils.addDscp(matchBuilder, aceIp.getDscp().getValue());
        }

        if (aceIp.getProtocol() != null) {
            addIpProtocolMatch(aceIp);
        }

        if (aceIp.getAceIpVersion() instanceof AceIpv4) {
            addIpV4Match(aceIp);
        }

        if (aceIp.getAceIpVersion() instanceof AceIpv6) {
            addIpV6Match(aceIp);
        }
    }

    private void addIpProtocolMatch(AceIp aceIp) {
        int srcPort = 0;
        int dstPort = 0;

        // TODO Ranges are not supported yet
        if (aceIp.getSourcePortRange() != null && aceIp.getSourcePortRange().getLowerPort() != null) {
            srcPort = aceIp.getSourcePortRange().getLowerPort().getValue();
        }
        if (aceIp.getDestinationPortRange() != null && aceIp.getDestinationPortRange().getLowerPort() != null) {
            dstPort = aceIp.getDestinationPortRange().getLowerPort().getValue();
        }
        MatchUtils.createIpProtocolMatch(matchBuilder, aceIp.getProtocol());
        MatchUtils.addLayer4Match(matchBuilder, aceIp.getProtocol().intValue(), srcPort, dstPort);
    }

    private void addIpV4Match(AceIp aceIp) {
        AceIpv4 aceIpv4 = (AceIpv4)aceIp.getAceIpVersion();

        MatchUtils.createEtherTypeMatch(matchBuilder, new EtherType(MatchUtils.ETHERTYPE_IPV4));
        matchBuilder = MatchUtils.addRemoteIpPrefix(matchBuilder, aceIpv4.getSourceIpv4Network(),
                aceIpv4.getDestinationIpv4Network());
    }

    private void addIpV6Match(AceIp aceIp) {
        AceIpv6 aceIpv6 = (AceIpv6)aceIp.getAceIpVersion();

        MatchUtils.createEtherTypeMatch(matchBuilder, new EtherType(MatchUtils.ETHERTYPE_IPV6));
        matchBuilder = MatchUtils.addRemoteIpv6Prefix(matchBuilder, aceIpv6.getSourceIpv6Network(),
                aceIpv6.getDestinationIpv6Network());
    }
}
