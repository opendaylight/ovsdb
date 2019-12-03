/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocator;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalSwitch;
import org.opendaylight.ovsdb.schema.hardwarevtep.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.Tunnels;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdLocalConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdLocalConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdParams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdParamsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdRemoteConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdRemoteConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdStatusBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HwvtepTunnelUpdateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(HwvtepTunnelUpdateCommand.class);

    private Map<UUID, Tunnel> updatedTunnelRows;

    public HwvtepTunnelUpdateCommand(final HwvtepConnectionInstance key, final TableUpdates updates,
            final DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        try {
            updatedTunnelRows = extractRowsUpdated(Tunnel.class);
        } catch (IllegalArgumentException e) {
            LOG.debug("Tunnel Table not supported on this HWVTEP device", e);
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void execute(final ReadWriteTransaction transaction) {
        if (updatedTunnelRows != null && !updatedTunnelRows.isEmpty()) {
            for (Tunnel tunnel : updatedTunnelRows.values()) {
                try {
                    updateTunnel(transaction, tunnel);
                } catch (RuntimeException e) {
                    LOG.warn("Exception updating tunnel {}", tunnel, e);
                }
            }
        }
    }

    private void updateTunnel(final ReadWriteTransaction transaction, final Tunnel tunnel) {
        Preconditions.checkNotNull(tunnel.getLocalColumn().getData());
        Preconditions.checkNotNull(tunnel.getRemoteColumn().getData());
        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        //TODO remove these reads
        Optional<Node> connection = HwvtepSouthboundUtil.readNode(transaction, connectionIId);
        PhysicalSwitch phySwitch =
                        getOvsdbConnectionInstance().getDeviceInfo().getPhysicalSwitchForTunnel(tunnel.getUuid());

        InstanceIdentifier<Tunnels> tunnelIid = null;
        if (phySwitch != null) {
            InstanceIdentifier<Node> psIid =
                    HwvtepSouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(), phySwitch);
            tunnelIid = getInstanceIdentifier(psIid, tunnel);
        }

        if (connection.isPresent() && tunnelIid != null) {
            TunnelsBuilder builder = new TunnelsBuilder();
            builder.setLocalLocatorRef(new HwvtepPhysicalLocatorRef(getPhysicalLocatorRefFromUUID(
                    getOvsdbConnectionInstance().getInstanceIdentifier(), tunnel.getLocalColumn().getData())));
            builder.setRemoteLocatorRef(new HwvtepPhysicalLocatorRef(getPhysicalLocatorRefFromUUID(
                    getOvsdbConnectionInstance().getInstanceIdentifier(), tunnel.getRemoteColumn().getData())));
            builder.setTunnelUuid(new Uuid(tunnel.getUuid().toString()));
            setBfdLocalConfigs(builder, tunnel);
            setBfdRemoteConfigs(builder, tunnel);
            setBfdParams(builder, tunnel);
            setBfdStatus(builder, tunnel);
            Tunnels updatedTunnel = builder.build();
            LOG.trace("Built with the intent to store tunnel data {}", updatedTunnel);
            transaction.merge(LogicalDatastoreType.OPERATIONAL, tunnelIid, updatedTunnel);
            // TODO: Deletion of Tunnel BFD config and params
        } else {
            LOG.warn("Insuficient information. Unable to update tunnel {}", tunnel.getUuid());
        }
    }

    private static void setBfdLocalConfigs(final TunnelsBuilder tunnelsBuilder, final Tunnel tunnel) {
        Map<String, String> localConfigs = tunnel.getBfdConfigLocalColumn().getData();
        if (localConfigs != null && !localConfigs.isEmpty()) {
            List<BfdLocalConfigs> localConfigsList = localConfigs.entrySet().stream().map(
                entry -> new BfdLocalConfigsBuilder().setBfdLocalConfigKey(entry.getKey())
                    .setBfdLocalConfigValue(entry.getValue()).build()).collect(Collectors.toList());

            tunnelsBuilder.setBfdLocalConfigs(localConfigsList);
        }
    }

    private static void setBfdRemoteConfigs(final TunnelsBuilder tunnelsBuilder, final Tunnel tunnel) {
        Map<String, String> remoteConfigs = tunnel.getBfdConfigRemoteColumn().getData();
        if (remoteConfigs != null && !remoteConfigs.isEmpty()) {
            List<BfdRemoteConfigs> remoteConfigsList = remoteConfigs.entrySet().stream().map(
                entry -> new BfdRemoteConfigsBuilder().setBfdRemoteConfigKey(entry.getKey())
                    .setBfdRemoteConfigValue(entry.getValue()).build()).collect(Collectors.toList());

            tunnelsBuilder.setBfdRemoteConfigs(remoteConfigsList);
        }
    }

    private static void setBfdParams(final TunnelsBuilder tunnelsBuilder, final Tunnel tunnel) {
        Map<String, String> params = tunnel.getBfdParamsColumn().getData();
        if (params != null && !params.isEmpty()) {
            List<BfdParams> paramsList = params.entrySet().stream().map(
                entry -> new BfdParamsBuilder().setBfdParamKey(entry.getKey())
                    .setBfdParamValue(entry.getValue()).build()).collect(Collectors.toList());

            tunnelsBuilder.setBfdParams(paramsList);
        }
    }

    private static void setBfdStatus(final TunnelsBuilder tunnelsBuilder, final Tunnel tunnel) {
        Map<String, String> status = tunnel.getBfdStatusColumn().getData();
        if (status != null && !status.isEmpty()) {
            List<BfdStatus> statusList = status.entrySet().stream().map(
                entry -> new BfdStatusBuilder().setBfdStatusKey(entry.getKey())
                    .setBfdStatusValue(entry.getValue()).build()).collect(Collectors.toList());

            tunnelsBuilder.setBfdStatus(statusList);
        }
    }

    private InstanceIdentifier<Tunnels> getInstanceIdentifier(final InstanceIdentifier<Node> psIid,
            final Tunnel tunnel) {
        InstanceIdentifier<Tunnels> result = null;
        InstanceIdentifier<TerminationPoint> localTpPath =
                        getPhysicalLocatorRefFromUUID(getOvsdbConnectionInstance().getInstanceIdentifier(),
                                                        tunnel.getLocalColumn().getData());
        InstanceIdentifier<TerminationPoint> remoteTpPath =
                        getPhysicalLocatorRefFromUUID(getOvsdbConnectionInstance().getInstanceIdentifier(),
                                                        tunnel.getRemoteColumn().getData());
        if (remoteTpPath != null && localTpPath != null) {
            result = HwvtepSouthboundMapper.createInstanceIdentifier(psIid, localTpPath, remoteTpPath);
        }
        return result;
    }

    private InstanceIdentifier<TerminationPoint> getPhysicalLocatorRefFromUUID(final InstanceIdentifier<Node> nodeIid,
                                                                               final UUID uuid) {
        PhysicalLocator locator = getOvsdbConnectionInstance().getDeviceInfo().getPhysicalLocator(uuid);
        if (locator == null) {
            LOG.trace("Available PhysicalLocators: {}",
                            getOvsdbConnectionInstance().getDeviceInfo().getPhysicalLocators());
            return null;
        }
        return HwvtepSouthboundMapper.createInstanceIdentifier(nodeIid, locator);
    }
}
