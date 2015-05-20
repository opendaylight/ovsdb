package org.opendaylight.ovsdb.openstack.netvirt;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public interface NetvirtInterface {
    void setDependencies(BundleContext bundleContext, ServiceReference serviceReference);
    void setDependencies(Object impl);
}
