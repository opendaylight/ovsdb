/*
 * Copyright © 2016 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.netvirt.renderers.neutron;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.common.rev151227.NetworkTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l2.networks.rev151227.l2.networks.L2Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l2.networks.rev151227.l2.networks.L2NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l2.networks.rev151227.l2.networks.L2NetworkKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.networks.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.provider.ext.rev150712.NetworkProviderExtension;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data processor for Neutron Network
 */
public class NeutronNetworkDataProcessor implements DataProcessor<Network> {
    private static final Logger LOG = LoggerFactory.getLogger(NeutronNetworkDataProcessor.class);
    private MdsalUtils mdsalUtils;
    private final NeutronProvider provider;

    /**
     *
     * @param provider - Neutron provider
     * @param db - mdsal
     */
    public NeutronNetworkDataProcessor(final NeutronProvider provider, DataBroker db) {
        this.provider = Preconditions.checkNotNull(provider, "Provider can not be null!");
        mdsalUtils = new MdsalUtils(db);
    }

    /**
     * Remove a netvirt network from mdsal
     *
     * @param identifier - the whole path to DataObject
     * @param change - port to be removed
     */
    @Override
    public void remove(final InstanceIdentifier<Network> identifier,
                       final Network change) {
        Preconditions.checkNotNull(change, "Removed object can not be null!");
        LOG.debug("Delete Neutron Network model data changes for key: {} delete: {}", identifier, change);

        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l2.networks.rev151227.l2.networks.L2Network> networkIid =
                MdsalHelper.createL2NetworkInstanceIdentifier(change.getUuid());

        LOG.debug("Remove netvirt network uuid {} from mdsal", change.getUuid());
        try {
            boolean result = mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, networkIid);
            if (result) {
                LOG.debug("Remove netvirt network from mdsal success. Result: {}", result);
            } else {
                LOG.warn("Remove nevtirt network failed. Result: {}", result);
            }
        } catch (Exception e) {
            LOG.warn("Remove netvirt network failed: exception {}", e);
        }
    }

    /**
     * Update a netvirt network in mdsal
     *
     * @param identifier - the whole path to DataObject
     * @param original - original DataObject (for update)
     * @param change - network to be updated
     */

    @Override
    public void update(final InstanceIdentifier<Network> identifier,
                       final Network original, final Network change) {
        Preconditions.checkNotNull(original, "Updated original object can not be null!");
        Preconditions.checkNotNull(original, "Updated update object can not be null!");
        remove(identifier, original);
        LOG.debug("Update Neutron Network model data changes for key: {} delete: {}", identifier, change);
        remove(identifier, original);
        add(identifier, change);
    }

    private void addExtensions(L2NetworkBuilder l2NetworkBuilder, Network network)
    {
        NetworkProviderExtension  providerExtension = network.getAugmentation(NetworkProviderExtension.class);
        if (providerExtension != null) {
            if (providerExtension.getSegmentationId() != null) {
                l2NetworkBuilder.setSegmentationId(providerExtension.getSegmentationId());
            }

            if (Constants.NETVIRT_NEUTRON_NETWORK_TYPE_MAP.get(providerExtension.getNetworkType().getSimpleName()) != null) {
                l2NetworkBuilder.setNetworkType(Constants.NETVIRT_NEUTRON_NETWORK_TYPE_MAP.get(providerExtension.getNetworkType().getSimpleName()));
            } else {
                LOG.warn("Neutron Network Type not supported.. using Flat for network {}", network);
                l2NetworkBuilder.setNetworkType(NetworkTypeBase.class);
            }
        }
    }

    /**
     * Add a netvirt Network to mdsal
     *
     * @param identifier - the whole path to new DataObject
     * @param change - port to be added
     */
    @Override
    public void add(final InstanceIdentifier<Network> identifier,
                    final Network change) {
        Preconditions.checkNotNull(change, "Added object can not be null!");
        LOG.debug("Create Neutron Network model data changes for identifier: {} change: {}", identifier, change);
        L2NetworkBuilder l2NetworkBuilder = new L2NetworkBuilder();

        InstanceIdentifier<L2Network> networkIid = MdsalHelper.createL2NetworkInstanceIdentifier(change.getUuid());
        addExtensions(l2NetworkBuilder, change);

        if (change.isAdminStateUp() != null) {
            l2NetworkBuilder.setAdminStateUp(change.isAdminStateUp());
        }
        if (change.getName() != null) {
            l2NetworkBuilder.setName(change.getName());
        }
        if (change.isShared() != null) {
            l2NetworkBuilder.setShared(change.isShared());
        }
        if (change.getStatus() != null) {
            l2NetworkBuilder.setStatus(change.getStatus());
        }
        if (change.getTenantId() != null) {
            // TODO
            //l2NetworkBuilder.setTenantId(change.getTenantId());
        }
        if (change.getUuid() != null) {
            l2NetworkBuilder.setUuid(change.getUuid());
            l2NetworkBuilder.setKey(new L2NetworkKey(change.getUuid()));
        }

        LOG.debug("Add Netvirt network {} to mdsal", l2NetworkBuilder.build().toString());
        try {
            boolean result = mdsalUtils.put(LogicalDatastoreType.CONFIGURATION, networkIid, l2NetworkBuilder.build());
            if (result) {
                LOG.debug("createNetwork:addToMdSal success. Result: {}", result);
            } else {
                LOG.warn("createNetwork:addToMdSal failed. Result: {}", result);
            }
        } catch (Exception e) {
            LOG.warn("create Netvirt Network : addToMdSal exception {}", e);
        }
    }
}
