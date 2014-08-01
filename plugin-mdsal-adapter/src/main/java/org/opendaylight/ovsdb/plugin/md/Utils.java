package org.opendaylight.ovsdb.plugin.md;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by dave on 01/08/2014.
 */
public final class Utils {

    static final Logger logger = LoggerFactory.getLogger(Utils.class);

    /**
     * Returns an IP address Given an AD-SAL style Node ID "OVS:172.6.2.4:3456"
     */
    public static IpAddress getOvsdbIpAddress(String nodeId){
        logger.info("Attempting to get IP Address from {}", nodeId);
        String[] split = nodeId.split(":");
        String ipAddressString = split[0];
        Ipv4Address ipv4Address = new Ipv4Address(ipAddressString);
        return new IpAddress(ipv4Address);
    }
}
