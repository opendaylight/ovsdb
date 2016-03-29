/*
 * Copyright (c) 2016 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.netvirt.renderers.neutron;

import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipState;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.osgi.framework.BundleContext;

public class NeutronProvider implements BindingAwareProvider, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(NeutronProvider.class);
    private BundleContext bundleContext = null;
    private static DataBroker dataBroker = null;
    private ConfigActivator activator;
    private static EntityOwnershipService entityOwnershipService;
    private static final Entity ownerInstanceEntity = new Entity(Constants.NETVIRT_NEUTRON_OWNER_ENTITY_TYPE,
            Constants.NETVIRT_NEUTRON_OWNER_ENTITY_TYPE);

    public NeutronProvider(BundleContext bundleContext, EntityOwnershipService eos) {
        LOG.info("Netvirt NeutronProvider: bundleContext: {}", bundleContext);
        this.bundleContext = bundleContext;
        entityOwnershipService = eos;
    }

    public NeutronProvider() {
        LOG.info("Netvirt NeutronProvider created");
    }

    public static boolean isMasterProviderInstance() {
        if (entityOwnershipService != null) {
            Optional<EntityOwnershipState> state = entityOwnershipService.getOwnershipState(ownerInstanceEntity);
            return state.isPresent() && state.get().isOwner();
        }
        return false;
    }


    @Override
    public void onSessionInitiated(ProviderContext providerContext) {
        LOG.info("Netvirt NeutronProvider Session Initiated");
        dataBroker = providerContext.getSALService(DataBroker.class);
        LOG.info("Netvirt NeutronProvider: onSessionInitiated dataBroker: {}", dataBroker);
        this.activator = new ConfigActivator(providerContext);
        try {
            activator.start(bundleContext);
        } catch (Exception e) {
            LOG.warn("Failed to start Netvirt Neutron: ", e);
        }

    }

    @Override
    public void close() throws Exception {
        LOG.info("Netvirt NeutronProvider Closed");
        activator.stop(bundleContext);
    }

}
