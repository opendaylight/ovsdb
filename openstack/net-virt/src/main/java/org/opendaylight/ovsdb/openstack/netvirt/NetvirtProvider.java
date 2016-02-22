/*
 * Copyright (c) 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipState;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sam Hague (shague@redhat.com)
 */
public class NetvirtProvider implements BindingAwareProvider, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(NetvirtProvider.class);
    private BundleContext bundleContext = null;
    private static DataBroker dataBroker = null;
    private ConfigActivator activator;
    private static EntityOwnershipService entityOwnershipService;
    private static final Entity ownerInstanceEntity = new Entity(
            Constants.NETVIRT_OWNER_ENTITY_TYPE, Constants.NETVIRT_OWNER_ENTITY_TYPE);
    private boolean conntrackEnabled = false;

    public NetvirtProvider(BundleContext bundleContext, EntityOwnershipService eos) {
        LOG.info("NetvirtProvider: bundleContext: {}", bundleContext);
        this.bundleContext = bundleContext;
        entityOwnershipService = eos;
    }

    public static boolean isMasterProviderInstance() {
        if (entityOwnershipService != null) {
            Optional<EntityOwnershipState> state = entityOwnershipService.getOwnershipState(ownerInstanceEntity);
            return state.isPresent() && state.get().isOwner();
        }
        return false;
    }

    @Override
    public void close() throws Exception {
        LOG.info("NetvirtProvider closed");
        activator.stop(bundleContext);
    }

    @Override
    public void onSessionInitiated(ProviderContext providerContext) {
        dataBroker = providerContext.getSALService(DataBroker.class);
        LOG.info("NetvirtProvider: onSessionInitiated dataBroker: {}", dataBroker);
        LOG.info("NetvirtProvider: onSessionInitiated isConntrackEnabled: {}", this.conntrackEnabled);
        this.activator = new ConfigActivator(providerContext);
        activator.setConntrackEnabled(this.conntrackEnabled);
        try {
            activator.start(bundleContext);
        } catch (Exception e) {
            LOG.warn("Failed to start Netvirt: ", e);
        }
    }

    public boolean isConntrackEnabled() {
        return conntrackEnabled;
    }

    public void setConntrackEnabled(boolean conntackEnabled) {
        this.conntrackEnabled = conntackEnabled;
    }
}
