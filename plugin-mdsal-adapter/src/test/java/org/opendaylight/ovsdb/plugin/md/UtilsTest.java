package org.opendaylight.ovsdb.plugin.md;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;

import org.junit.Assert;
import org.junit.Test;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class UtilsTest {

    static final String IPV4_ADDRESS = "10.10.10.10";
    static final String IPV6_ADDRESS = "2001:db8:0:0:0:ff00:42:8329";

    @Test
    public void testConvertIpAddress() throws UnknownHostException {

        InetAddress addressV4 = Inet4Address.getByName(IPV4_ADDRESS);
        InetAddress addressV6 = Inet6Address.getByName(IPV6_ADDRESS);

        IpAddress ipAddresV4 = Utils.convertIpAddress(addressV4);
        IpAddress ipAddresV6 = Utils.convertIpAddress(addressV6);

        Assert.assertEquals(IPV4_ADDRESS, ipAddresV4.getIpv4Address().getValue());
        Assert.assertEquals(IPV6_ADDRESS, ipAddresV6.getIpv6Address().getValue());
    }
}