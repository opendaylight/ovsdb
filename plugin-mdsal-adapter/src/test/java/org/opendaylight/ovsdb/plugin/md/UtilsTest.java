package org.opendaylight.ovsdb.plugin.md;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.INodeFactory;
import org.opendaylight.ovsdb.plugin.impl.NodeFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;

import org.junit.Assert;
import org.junit.Test;

public class UtilsTest {

    @Test
    public void testGetOvsdbIpAddress() throws Exception {
        IpAddress expectedIpAddress = new IpAddress(new Ipv4Address("172.16.1.1"));
        String nodeId = "172.16.1.1:54042";
        IpAddress ipAddress = Utils.getOvsdbIpAddress(nodeId);
        Assert.assertEquals(expectedIpAddress, ipAddress);
    }
}