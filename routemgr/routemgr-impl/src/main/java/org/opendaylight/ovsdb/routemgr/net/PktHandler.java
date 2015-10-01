/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.routemgr.net;

import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.ovsdb.routemgr.utils.BitBufferHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.opendaylight.controller.md.sal.binding.api.DataBroker;


public class PktHandler implements PacketProcessingListener {
    private static final Logger LOG = LoggerFactory.getLogger(PktHandler.class);
    public static final int IPv6_ETHTYPE = 34525;
    public static final int ICMP_v6 = 1;

    public static final int ETHTYPE_START = 96;
    public static final int ONE_BYTE  = 8;
    public static final int TWO_BYTES = 16;
    public static final int IPv6_HDR_START = 112;
    public static final int IPv6_NEXT_HDR = 48;

    private DataBroker dataService;

    public void setDataBrokerService(DataBroker dataService) {
        this.dataService = dataService;
    }

    @Override
    public void onPacketReceived(PacketReceived packetReceived) {
        boolean result = false;
        int     ethType;
        int     v6NxtHdr;

        if (packetReceived == null) {
            LOG.debug("receiving null packet. returning without any processing");
            return;
        }
        byte[] data = packetReceived.getPayload();
        if (data.length <= 0) {
            LOG.debug("received packet with invalid length {}", data.length);
            return;
        }
        try {
            ethType = BitBufferHelper.getInt(BitBufferHelper.getBits(data, ETHTYPE_START, TWO_BYTES));
            if (ethType == IPv6_ETHTYPE) {
                v6NxtHdr = BitBufferHelper.getInt(BitBufferHelper.getBits(data,
                            IPv6_HDR_START + IPv6_NEXT_HDR, TWO_BYTES));
                if (v6NxtHdr == ICMP_v6) {
                    LOG.debug("ICMPv6 Pdu received on port {} ", packetReceived.getIngress());
                } else {
                    LOG.debug("IPv6 Pdu received on port {} ", packetReceived.getIngress());
                }
            } else {
                LOG.debug("Pdu received on port {} ", packetReceived.getIngress());
                return;
            }
        } catch (Exception e) {
            LOG.warn("Failed to decode packet: {}", e.getMessage());
            return;
        }
    }

}
