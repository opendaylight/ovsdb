package org.opendaylight.ovsdb.northbound;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NorthboundProvider implements BindingAwareProvider, AutoCloseable  {

    private static final Logger LOG = LoggerFactory.getLogger(NorthboundProvider.class);

    @Override
    public void close() throws Exception {
        LOG.info("NorthboundProvider Closed.");
    }

    @Override
    public void onSessionInitiated(ProviderContext arg0) {
        LOG.info("NorthboundProvider Session Initated.");
    }

}
