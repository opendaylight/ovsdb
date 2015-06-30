package org.opendaylight.ovsdb.openstack.netvirt.providers;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sam Hague (shague@redhat.com)
 */
public class NetvirtProvidersProvider implements BindingAwareProvider, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(NetvirtProvidersProvider.class);
    private BundleContext bundleContext = null;
    private static DataBroker dataBroker = null;
    private ConfigActivator activator;

    public NetvirtProvidersProvider(BundleContext bundleContext) {
        LOG.info("NetvirtProvidersProvider: bundleContext: {}", bundleContext);
        this.bundleContext = bundleContext;
    }

    public static DataBroker getDataBroker() {
        return dataBroker;
    }

    @Override
    public void close() throws Exception {
        activator.stop(bundleContext);
    }

    @Override
    public void onSessionInitiated(ProviderContext providerContext) {
        dataBroker = providerContext.getSALService(DataBroker.class);
        LOG.info("NetvirtProvidersProvider: onSessionInitiated dataBroker: {}", dataBroker);
        this.activator = new ConfigActivator(providerContext);
        try {
            activator.start(bundleContext);

            // Southbound southbound = (Southbound) ServiceHelper.getGlobalInstance(Southbound.class, this);
            // southbound.initializeNetvirtTopology();
        } catch (Exception e) {
            LOG.warn("Failed to start Netvirt: ", e);
        }
    }
}
