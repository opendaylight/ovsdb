/*
 * (c) Copyright 2013 Hewlett-Packard Development Company, L.P.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.util.packet;

import static org.opendaylight.util.junit.TestTools.EOL;
import static org.opendaylight.util.junit.TestTools.print;
import static org.opendaylight.util.net.EthernetType.IPv4;
import static org.opendaylight.util.packet.Dns.ClassType.INTERNET;
import static org.opendaylight.util.packet.Dns.RecordType.A;
import static org.opendaylight.util.packet.Dns.RecordType.CNAME;
import static org.opendaylight.util.packet.ProtocolId.DNS;
import static org.opendaylight.util.packet.ProtocolId.ETHERNET;
import static org.opendaylight.util.packet.ProtocolId.IP;
import static org.opendaylight.util.packet.ProtocolId.UDP;
import static org.opendaylight.util.packet.ProtocolUtils.hex;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.opendaylight.util.net.IpAddress;
import org.opendaylight.util.net.MacAddress;
import org.opendaylight.util.net.TcpUdpPort;
import org.opendaylight.util.packet.Dns.OpCode;
import org.opendaylight.util.packet.Dns.Record;
import org.opendaylight.util.packet.Dns.ResRecord;


/**
 * DNS A #2 (IPv4) response unit tests.
 *
 * @author Frank Wood
 */
public class DnsAResp2Test extends PacketTest {

    private static final String TEST_DATA_FILE
        = "eth2-ip-udp-dns-a2-resp.hex";
    
    private static final int PKT_LEN = 135;

    private static final MacAddress SRC_MAC = mac("1c:7e:e5:58:4c:5d");
    private static final MacAddress DST_MAC = mac("00:24:81:b3:e4:de");

    private static final IpAddress DST_IP = ip("192.168.0.100");
    private static final IpAddress SRC_IP = ip("192.168.0.1");
    private static final int IP_TTL = 64;
    private static final int IP_IDENT = 0;
    private static final int IP_CHECK_SUM = 0xb8be;
    
    private static final TcpUdpPort DST_PORT = TcpUdpPort.udpPort(43015);
    private static final TcpUdpPort SRC_PORT = TcpUdpPort.udpPort(53);
    private static final int UDP_LEN = 101;
    private static final int UDP_CHECK_SUM = 0x240c;

    private static final int TXID = 0xde05;
    private static final int ANS_TTL0 = 99;
    private static final int ANS_TTL1 = 119;
    
    private static final String QUERY_NAME = "www.hp.com";
    private static final IpAddress ANS_IP0 =
            IpAddress.valueOf("15.217.49.140");
    private static final IpAddress ANS_IP1 =
            IpAddress.valueOf("15.217.49.142");

    private static final String PNAME = "www.hpgtm.nsatc.net";
    
    private Packet decodeFile() {
        return Codec.decodeEthernet(getPacketReader(TEST_DATA_FILE));
    }

    private void verify(Packet pkt) {
        Ethernet eth = pkt.get(ETHERNET);
        assertEquals(DST_MAC, eth.dstAddr());
        assertEquals(SRC_MAC, eth.srcAddr());
        assertEquals(IPv4, eth.type());

        Ip ip = pkt.get(IP);
        assertEquals(SRC_IP, ip.srcAddr());
        assertEquals(DST_IP, ip.dstAddr());
        assertEquals(IP_TTL, ip.ttl());
        assertEquals(IP_CHECK_SUM, ip.checkSum());

        Udp udp = pkt.get(UDP);
        assertEquals(SRC_PORT, udp.srcPort());
        assertEquals(DST_PORT, udp.dstPort());
        assertEquals(UDP_LEN, udp.len());
        assertEquals(UDP_CHECK_SUM, udp.checkSum());

        Dns dns = pkt.get(DNS);
        assertEquals(TXID, dns.txId());
        assertFalse(dns.query());
        assertEquals(OpCode.QUERY, dns.opCode());
        assertTrue(dns.recurDesired());
        assertTrue(dns.svrRecurAvail());
        assertFalse(dns.trunc());
        assertFalse(dns.responderAuth());
        assertFalse(dns.authData());
        assertFalse(dns.checkDisabled());
        assertEquals(0, dns.authorities().length);
        assertEquals(0, dns.additionals().length);
        
        Record[] queries = dns.queries();
        assertEquals(1, queries.length);
        Record q = queries[0];
        assertEquals(QUERY_NAME, q.name());
        assertEquals(INTERNET, q.clsType());
        assertEquals(A, q.recType());        
        
        assertEquals(3, dns.answers().length);
        
        ResRecord<?> r = dns.answers()[0];
        assertEquals(QUERY_NAME, r.name());
        assertEquals(INTERNET, r.clsType());
        assertEquals(CNAME, r.recType());
        assertEquals(ANS_TTL0, r.ttl());
        String name = (String) r.data();
        assertEquals(PNAME, name);
        
        r = dns.answers()[1];
        assertEquals(PNAME, r.name());
        assertEquals(INTERNET, r.clsType());
        assertEquals(A, r.recType());
        assertEquals(ANS_TTL1, r.ttl());
        IpAddress addr = (IpAddress) r.data();
        assertEquals(ANS_IP0, addr);
        
        r = dns.answers()[2];
        assertEquals(PNAME, r.name());
        assertEquals(INTERNET, r.clsType());
        assertEquals(A, r.recType());
        assertEquals(ANS_TTL1, r.ttl());
        addr = (IpAddress) r.data();
        assertEquals(ANS_IP1, addr);
    }

    private Packet create() {
        Ethernet eth = new Ethernet.Builder()
            .dstAddr(DST_MAC)
            .srcAddr(SRC_MAC)
            .type(IPv4)
            .build();

        Ip ip = new Ip.Builder()
            .ttl(IP_TTL)
            .doNotFrag(true)
            .type(IpType.UDP)
            .srcAddr(SRC_IP)
            .dstAddr(DST_IP)
            .ident(IP_IDENT)
            .build();

        Udp udp = new Udp.Builder()
            .srcPort(SRC_PORT)
            .dstPort(DST_PORT)
            .build();

        Dns dns = new Dns.Builder()
            .txId(TXID)
            .opCode(OpCode.QUERY)
            .recurDesired(true)
            .svrRecurAvail(true)
            .query(QUERY_NAME, A)
            .answers(
                new ResRecord<String>(QUERY_NAME, CNAME, ANS_TTL0, PNAME),                    
                new ResRecord<IpAddress>(PNAME, A, ANS_TTL1, ANS_IP0),                    
                new ResRecord<IpAddress>(PNAME, A, ANS_TTL1, ANS_IP1)                    
            )
            .build();

        return new Packet(eth, ip, udp, dns);
    }

    @Test
    public void decode() {
        print(EOL + "decode()");
        Packet pkt = decodeFile();
        print(pkt.toDebugString());
        verify(pkt);
    }

    @Test
    public void encode() {
        print(EOL + "encode()");
        Packet pkt = create();
        print(pkt.toDebugString());

        byte[] encoding = Codec.encode(pkt);
        print("enc=" + hex(encoding));

        PacketReader r = getPacketReader(TEST_DATA_FILE);
        byte[] expected = r.readBytes(PKT_LEN);
        print("exp=" + hex(expected));
        
        assertArrayEquals(expected, encoding);
    }

}
