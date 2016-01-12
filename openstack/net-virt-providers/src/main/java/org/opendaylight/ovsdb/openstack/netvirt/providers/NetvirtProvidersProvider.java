/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers;

import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.CandidateAlreadyRegisteredException;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipCandidateRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipChange;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListener;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListenerRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TableId;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Sam Hague (shague@redhat.com)
 */
public class NetvirtProvidersProvider implements BindingAwareProvider, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(NetvirtProvidersProvider.class);

    private BundleContext bundleContext = null;
    private static DataBroker dataBroker = null;
    private ConfigActivator activator;
    private static ProviderContext providerContext = null;
    private static EntityOwnershipService entityOwnershipService;
    private ProviderEntityListener providerEntityListener = null;
    private static AtomicBoolean hasProviderEntityOwnership = new AtomicBoolean(false);
    private static short tableOffset;
    private NetvirtProvidersConfigImpl netvirtProvidersConfig = null;

    public NetvirtProvidersProvider(BundleContext bundleContext, EntityOwnershipService eos, short tableOffset) {
        LOG.info("NetvirtProvidersProvider: bundleContext: {}", bundleContext);
        this.bundleContext = bundleContext;
            entityOwnershipService = eos;
        setTableOffset(tableOffset);
    }

    public static DataBroker getDataBroker() {
        return dataBroker;
    }

    public static ProviderContext getProviderContext() {
        return providerContext;
    }

    public static boolean isMasterProviderInstance() {
        return hasProviderEntityOwnership.get();
    }

    public static void setTableOffset(short tableOffset) {
        try {
            new TableId((short) (tableOffset + Service.L2_FORWARDING.getTable()));
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid table offset: {}", tableOffset, e);
            return;
        }

        LOG.info("setTableOffset: changing from {} to {}",
                NetvirtProvidersProvider.tableOffset, tableOffset);
        NetvirtProvidersProvider.tableOffset = tableOffset;
    }

    public static short getTableOffset() {
        return tableOffset;
    }

    @Override
    public void close() throws Exception {
        LOG.info("NetvirtProvidersProvider closed");
        activator.stop(bundleContext);
        providerEntityListener.close();
    }

    @Override
    public void onSessionInitiated(ProviderContext providerContextRef) {
        dataBroker = providerContextRef.getSALService(DataBroker.class);
        providerContext = providerContextRef;
        LOG.info("NetvirtProvidersProvider: onSessionInitiated dataBroker: {}", dataBroker);
        this.activator = new ConfigActivator(providerContextRef);
        try {
            activator.start(bundleContext);
        } catch (Exception e) {
            LOG.warn("Failed to start Netvirt: ", e);
        }
        providerEntityListener = new ProviderEntityListener(this, entityOwnershipService);
    }

    private void handleOwnershipChange(EntityOwnershipChange ownershipChange) {
        if (ownershipChange.isOwner()) {
            LOG.info("*This* instance of OVSDB netvirt provider is a MASTER instance");
            hasProviderEntityOwnership.set(true);
        } else {
            LOG.info("*This* instance of OVSDB netvirt provider is a SLAVE instance");
            hasProviderEntityOwnership.set(false);
        }
    }

    private class ProviderEntityListener implements EntityOwnershipListener {
        private NetvirtProvidersProvider provider;
        private EntityOwnershipListenerRegistration listenerRegistration;
        private EntityOwnershipCandidateRegistration candidateRegistration;

        ProviderEntityListener(NetvirtProvidersProvider provider,
                               EntityOwnershipService entityOwnershipService) {
            this.provider = provider;
            this.listenerRegistration =
                    entityOwnershipService.registerListener(Constants.NETVIRT_OWNER_ENTITY_TYPE, this);

            //register instance entity to get the ownership of the netvirt provider
            Entity instanceEntity = new Entity(
                    Constants.NETVIRT_OWNER_ENTITY_TYPE, Constants.NETVIRT_OWNER_ENTITY_TYPE);
            try {
                this.candidateRegistration = entityOwnershipService.registerCandidate(instanceEntity);
            } catch (CandidateAlreadyRegisteredException e) {
                LOG.warn("OVSDB Netvirt Provider instance entity {} was already "
                        + "registered for ownership", instanceEntity, e);
            }
        }

        public void close() {
            this.listenerRegistration.close();
            this.candidateRegistration.close();
        }

        @Override
        public void ownershipChanged(EntityOwnershipChange ownershipChange) {
            provider.handleOwnershipChange(ownershipChange);
        }
    }
}
