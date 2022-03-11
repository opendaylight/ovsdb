/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import java.util.Collection;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocator;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalSwitch;
import org.opendaylight.ovsdb.schema.hardwarevtep.Tunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.Tunnels;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HwvtepTunnelRemoveCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(HwvtepTunnelRemoveCommand.class);

    Collection<Tunnel> deletedTunnelRows = null;

    public HwvtepTunnelRemoveCommand(HwvtepConnectionInstance key, TableUpdates updates, DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        try {
            deletedTunnelRows = TyperUtils.extractRowsRemoved(Tunnel.class, getUpdates(), getDbSchema()).values();
        } catch (IllegalArgumentException e) {
            LOG.debug("Tunnel Table not supported on this HWVTEP device", e);
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void execute(ReadWriteTransaction transaction) {
        for (Tunnel tunnel : deletedTunnelRows) {
            try {
                InstanceIdentifier<Tunnels> tunnelIid = getInstanceIdentifier(getOvsdbConnectionInstance(), tunnel);
                if (tunnelIid != null) {
                    transaction.delete(LogicalDatastoreType.OPERATIONAL, tunnelIid);
                    LOG.trace("Deleting tunnel {}", tunnelIid);
                }
                getOvsdbConnectionInstance().getDeviceInfo().removePhysicalSwitchForTunnel(tunnel.getUuid());
            } catch (RuntimeException e) {
                LOG.warn("Failed to delete tunnel {}", tunnel, e);
            }
        }
    }

    private InstanceIdentifier<Tunnels> getInstanceIdentifier(HwvtepConnectionInstance client, Tunnel tunnel) {
        InstanceIdentifier<Tunnels> result = null;

        PhysicalSwitch phySwitch = client.getDeviceInfo().getPhysicalSwitchForTunnel(tunnel.getUuid());
        if (phySwitch == null) {
            //PhysicalSwitch has already been removed, nothing to do here
            return null;
        }
        InstanceIdentifier<Node> psIid = HwvtepSouthboundMapper.createInstanceIdentifier(client, phySwitch);
        PhysicalLocator plLocal = getPhysicalLocatorFromUUID(tunnel.getLocalColumn().getData());
        PhysicalLocator plRemote = getPhysicalLocatorFromUUID(tunnel.getRemoteColumn().getData());
        if (plLocal != null && plRemote != null) {
            InstanceIdentifier<TerminationPoint> localTpPath = HwvtepSouthboundMapper.createInstanceIdentifier(
                                                                client.getInstanceIdentifier(), plLocal);
            InstanceIdentifier<TerminationPoint> remoteTpPath = HwvtepSouthboundMapper.createInstanceIdentifier(
                                                                client.getInstanceIdentifier(), plRemote);
            result = HwvtepSouthboundMapper.createInstanceIdentifier(psIid, localTpPath, remoteTpPath);
        }
        return result;
    }

    private PhysicalLocator getPhysicalLocatorFromUUID(UUID uuid) {
        PhysicalLocator locator = getOvsdbConnectionInstance().getDeviceInfo().getPhysicalLocator(uuid);
        if (locator == null) {
            LOG.trace("Available PhysicalLocators: {}",
                            getOvsdbConnectionInstance().getDeviceInfo().getPhysicalLocators());
        }
        return locator;
    }
}
