/*
 * Copyright © 2015, 2016 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.netvirt.renderers.neutron;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.PortTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.port.EndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.port.EndPointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.ports.PortBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.ports.PortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.port.attributes.FixedIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.Port;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data processor for Neutron Port
 */
public class NeutronPortDataProcessor implements DataProcessor<Port> {
    private static final Logger LOG = LoggerFactory.getLogger(NeutronPortDataProcessor.class);
    private MdsalUtils mdsalUtils;
    private final NeutronProvider provider;

    /**
     *
     * @param provider - Neutron provider
     * @param db - mdsal
     */
    public NeutronPortDataProcessor(final NeutronProvider provider, DataBroker db) {
        this.provider = Preconditions.checkNotNull(provider, "Provider can not be null!");
        mdsalUtils = new MdsalUtils(db);
    }

    /**
     * Remove a netvirt from mdsal
     *
     * @param identifier - the whole path to DataObject
     * @param change - port to be removed
     */
    @Override
    public void remove(final InstanceIdentifier<Port> identifier,
                       final Port change) {
        Preconditions.checkNotNull(change, "Removed object can not be null!");
        LOG.debug("Delete Neutron Port model data changes for key: {} delete: {}", identifier, change);

        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.ports.Port> portIid =
                MdsalHelper.createInstanceIdentifier(change.getUuid());

        LOG.debug("Remove netvirt Port uuid {} from mdsal", change.getUuid());
        try {
            boolean result = mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, portIid);
            if (result) {
                LOG.debug("Remove netvirt port from mdsal success. Result: {}", result);
            } else {
                LOG.warn("Remove nevtirt port failed. Result: {}", result);
            }
        } catch (Exception e) {
            LOG.warn("Remove netvirt port failed: exception {}", e);
        }
    }

    /**
     * Update a netvirt port in mdsal
     *
     * @param identifier - the whole path to DataObject
     * @param original - original DataObject (for update)
     * @param change - port to be updated
     */

    @Override
    public void update(final InstanceIdentifier<Port> identifier,
                       final Port original, final Port change) {
        Preconditions.checkNotNull(original, "Updated original object can not be null!");
        Preconditions.checkNotNull(original, "Updated update object can not be null!");
        remove(identifier, original);
        LOG.debug("Update Neutron Port model data changes for key: {} delete: {}", identifier, change);
        remove(identifier, original);
        add(identifier, change);
    }

    /**
     * Add a netvirt port to mdsal
     *
     * @param identifier - the whole path to new DataObject
     * @param change - port to be added
     */
    @Override
    public void add(final InstanceIdentifier<Port> identifier,
                    final Port change) {
        Preconditions.checkNotNull(change, "Added object can not be null!");
        LOG.debug("Create Neutron Port model data changes for identifier: {} change: {}", identifier, change);
        PortBuilder portBuilder = new PortBuilder();

        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.ports.Port> portIid =
                MdsalHelper.createInstanceIdentifier(change.getUuid());

        portBuilder.setStatus(change.getStatus());
        if (change.isAdminStateUp() != null) {
            portBuilder.setAdminStateUp(change.isAdminStateUp());
        }
        portBuilder.setName(change.getName());

        // TODO
        // Some neutron fields to consider for netvirt model
        // tenant-id
        // network-id
        // extra-dhcp-opts list
        // fixed-ips list
        // security groups
        // vif-details list
        // vif-type
        // vnic-type
        // profile
        // port-security-enabled
        // host-id

        if (Constants.NETVIRT_NEUTRON_PORT_TYPE_MAP.get(change.getDeviceOwner()) != null) {
            portBuilder.setPortType(Constants.NETVIRT_NEUTRON_PORT_TYPE_MAP.get(change.getDeviceOwner()));
        } else {
            LOG.warn("Unsupported device owner for neutron port identifier: {}  port: {}", identifier, change);
            portBuilder.setPortType(PortTypeBase.class);
        }

        // TODO - set parent when applicable
        //portB.setParent(change.getParent());

        portBuilder.setDeviceUuid(new Uuid(change.getDeviceId()));
        portBuilder.setDeviceLocatorUuid(change.getNetworkId());
        portBuilder.setKey(new PortKey(change.getUuid()));

        if (change.getFixedIps() != null) {
            List<IpAddress> ipAddressList = new ArrayList<>();
            for (FixedIps ips : change.getFixedIps()) {
                ipAddressList.add(new IpAddress(ips.getIpAddress().getValue()));
            }
            EndPoints endPoint = new EndPointsBuilder()
                    .setMacaddr(new MacAddress(change.getMacAddress()))
                    .setIpaddrs(ipAddressList)
                    .build();
            portBuilder.setEndPoints(Collections.singletonList(endPoint));
        }

        LOG.debug("Add Netvirt Port {} to mdsal", portBuilder.build().toString());
        try {
            boolean result = mdsalUtils.put(LogicalDatastoreType.CONFIGURATION, portIid, portBuilder.build());
            if (result) {
                LOG.debug("createPort:addToMdSal success. Result: {}", result);
            } else {
                LOG.warn("createPort:addToMdSal failed. Result: {}", result);
            }
        } catch (Exception e) {
            LOG.warn("create Netvirt Port : addToMdSal exception {}", e);
        }
    }
}
