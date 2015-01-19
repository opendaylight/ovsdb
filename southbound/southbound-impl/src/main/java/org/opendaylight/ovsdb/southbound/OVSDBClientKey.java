package org.opendaylight.ovsdb.southbound;

import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.IpPortLocator;

public class OVSDBClientKey {
    /*
     * This class is immutable.  If you are in anyway changing its fields after
     * creation, your are doing it wrong :)
     */
    private IpAddress ipaddress;
    private PortNumber port;

    OVSDBClientKey(IpPortLocator locator) {
        ipaddress = locator.getIp();
        port = locator.getPort();
    }

    OVSDBClientKey(OvsdbClient client) {
        ipaddress = SouthboundMapper.createIpAddress(client.getConnectionInfo().getRemoteAddress());
        port = new PortNumber(client.getConnectionInfo().getRemotePort());
    }

    public IpAddress getIp() {
        return ipaddress;
    }

    public PortNumber getPort() {
        return port;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((ipaddress == null) ? 0 : ipaddress.hashCode());
        result = prime * result + ((port == null) ? 0 : port.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OVSDBClientKey other = (OVSDBClientKey) obj;
        if (ipaddress == null) {
            if (other.ipaddress != null)
                return false;
        } else if (!ipaddress.equals(other.ipaddress))
            return false;
        if (port == null) {
            if (other.port != null)
                return false;
        } else if (!port.equals(other.port))
            return false;
        return true;
    }
}
