package org.opendaylight.ovsdb.openstack.netvirt;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetVirtProvider implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(NetVirtProvider.class);

    private BundleContext bundleContext;

    private ActivatorV2 activatorV2;

    public NetVirtProvider(BundleContext bundleContext) {
        LOG.warn("BundleContext set to: {}",bundleContext);
        this.bundleContext = bundleContext;
    }

    @Override
    public void onSessionInitiated(ProviderContext arg0) {
        LOG.info("NetVirtProvider Session Initiated");
        this.activatorV2 = new ActivatorV2();
        try {
            LOG.warn("BundleContext found to be: {}",bundleContext);
            this.activatorV2.start(bundleContext);
        } catch (Exception e) {
            LOG.warn("Unable to start NetVirt because: ",e);
        }
    }
    

    @Override
    public void close() throws Exception {
        this.activatorV2.stop(bundleContext);
    }
}
