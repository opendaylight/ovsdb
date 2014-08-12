/*
* Copyright (C) 2014 Red Hat, Inc.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*
* Authors : Sam Hague
*/
package org.opendaylight.controller.config.yang.config.ovssfc_provider.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.ovsdb.ovssfc.OvsSfcProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsSfcProviderModule extends org.opendaylight.controller.config.yang.config.ovssfc_provider.impl.AbstractOvsSfcProviderModule {
    private static final Logger LOG = LoggerFactory.getLogger(OvsSfcProviderModule.class);

    public OvsSfcProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public OvsSfcProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.ovssfc_provider.impl.OvsSfcProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        DataBroker dataBroker = getDataBrokerDependency();
        OvsSfcProvider ovsSfcProvider = new OvsSfcProvider(dataBroker);

        final class AutoCloseableSfc implements AutoCloseable {

            @Override
            public void close() throws Exception {
                //runtimeReg.close();
                //ovsSfcProvider.close();
                LOG.info("OVSSFC provider (instance {}) torn down.", this);
            }
        }

        AutoCloseable ret = new AutoCloseableSfc();
        LOG.info("OvsSfcProvider (instance {}) initialized.", ret);
        return ret;
    }

}
