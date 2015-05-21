package org.opendaylight.ovsdb.openstack.netvirt.providers;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public interface ConfigInterface {
    void setDependencies(BundleContext bundleContext, ServiceReference serviceReference);
    void setDependencies(Object impl);
}
