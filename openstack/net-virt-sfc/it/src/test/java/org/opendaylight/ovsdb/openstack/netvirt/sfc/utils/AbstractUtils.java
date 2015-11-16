package org.opendaylight.ovsdb.openstack.netvirt.sfc.utils;

import java.util.List;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yangtools.concepts.Builder;

public abstract class AbstractUtils {
    public <T> List<T> list(List<T> list, Builder<T> builder) {
        list.add(builder.build());
        return list;
    }

    public IpBuilder ipBuilder(String ip, int port) {
        return new IpBuilder()
                .setIp(new IpAddress(ip.toCharArray()))
                .setPort(new PortNumber(port));
    }

    public DataPlaneLocatorBuilder dataPlaneLocatorBuilder(DataPlaneLocatorBuilder dataPlaneLocatorBuilder,
                                                           String ip, int port) {
        return dataPlaneLocatorBuilder
                .setLocatorType(ipBuilder(ip, port).build())
                .setTransport(VxlanGpe.class);
    }
}
