/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.arp;

import org.opendaylight.controller.liblldp.EtherTypes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.arp.match.fields.ArpTargetHardwareAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatchBuilder;

/**
 * Contains methods creating flow part for ARP flow.
 */
public class ArpFlowFactory {

    private static final String HOST_MASK = "/32";

    /**
     * Creates {@link EthernetMatch} containing ARP ether-type and the given destination MAC address
     */
    public static EthernetMatch createEthernetMatch(MacAddress destinationMacAddress) {
        return new EthernetMatchBuilder().setEthernetType(
                new EthernetTypeBuilder().setType(new EtherType(Long.valueOf(EtherTypes.ARP.intValue()))).build())
            .setEthernetDestination(new EthernetDestinationBuilder().setAddress(destinationMacAddress).build())
            .build();
    }

    /**
     * Creates {@link ArpMatch} containing Reply ARP operation, THA and TPA for the given target
     * address and SPA for the given sender protocol address
     */
    public static ArpMatch createArpMatch(ArpMessageAddress targetAddress, Ipv4Address senderProtocolAddress) {
        return new ArpMatchBuilder().setArpOp(ArpOperation.REPLY.intValue())
            .setArpTargetHardwareAddress(
                    new ArpTargetHardwareAddressBuilder().setAddress(targetAddress.getHardwareAddress()).build())
            .setArpTargetTransportAddress(new Ipv4Prefix(targetAddress.getProtocolAddress().getValue() + HOST_MASK))
            .setArpSourceTransportAddress(new Ipv4Prefix(senderProtocolAddress.getValue() + HOST_MASK))
            .build();
    }

    /**
     * Creates {@link Action} representing output to the controller
     *
     * @param order the order for the action
     */
    public static Action createSendToControllerAction(int order) {
        return new ActionBuilder().setOrder(order)
            .setKey(new ActionKey(order))
            .setAction(
                    new OutputActionCaseBuilder().setOutputAction(
                            new OutputActionBuilder().setMaxLength(0xffff)
                                .setOutputNodeConnector(new Uri(OutputPortValues.CONTROLLER.toString()))
                                .build()).build())
            .build();
    }

}
