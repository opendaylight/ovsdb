/*
 * Copyright © 2015, 2017 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import static org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil.schemaMismatchLog;
import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.Global;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalSwitch;
import org.opendaylight.ovsdb.schema.hardwarevtep.Tunnel;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.ManagementIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.Tunnels;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdLocalConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdParams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdRemoteConfigs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhysicalSwitchUpdateCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(PhysicalSwitchUpdateCommand.class);

    public PhysicalSwitchUpdateCommand(HwvtepOperationalState state,
            Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> created =
                extractCreated(getChanges(),PhysicalSwitchAugmentation.class);
        if (!created.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> physicalSwitchEntry:
                created.entrySet()) {
                updatePhysicalSwitch(transaction,  physicalSwitchEntry.getKey(), physicalSwitchEntry.getValue());
            }
        }
        Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> updated =
                extractUpdatedSwitches(getChanges(),PhysicalSwitchAugmentation.class);
        if (!updated.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> physicalSwitchEntry:
                updated.entrySet()) {
                updatePhysicalSwitch(transaction,  physicalSwitchEntry.getKey(), physicalSwitchEntry.getValue());
            }
        }
    }


    private void updatePhysicalSwitch(TransactionBuilder transaction,
            InstanceIdentifier<Node> iid, PhysicalSwitchAugmentation physicalSwitchAugmentation) {
        LOG.debug("Creating a physical switch named: {}", physicalSwitchAugmentation.getHwvtepNodeName());
        Optional<PhysicalSwitchAugmentation> operationalPhysicalSwitchOptional =
                getOperationalState().getPhysicalSwitchAugmentation(iid);
        PhysicalSwitch physicalSwitch = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(),
                PhysicalSwitch.class);
        setDescription(physicalSwitch, physicalSwitchAugmentation);
        setManagementIps(physicalSwitch, physicalSwitchAugmentation);
        setTunnuleIps(physicalSwitch, operationalPhysicalSwitchOptional.get());
        try {
            setTunnels(transaction, iid, physicalSwitch, physicalSwitchAugmentation,
                            operationalPhysicalSwitchOptional.isPresent());
        } catch (SchemaVersionMismatchException e) {
            schemaMismatchLog("tunnels", "Physical_Switch", e);
        }
        if (!operationalPhysicalSwitchOptional.isPresent()) {
            //create a physical switch
            setName(physicalSwitch, physicalSwitchAugmentation, operationalPhysicalSwitchOptional);
            String pswitchUuid = "PhysicalSwitch_" + HwvtepSouthboundMapper.getRandomUUID();
            transaction.add(op.insert(physicalSwitch).withId(pswitchUuid));
            transaction.add(op.comment("Physical Switch: Creating "
                    + physicalSwitchAugmentation.getHwvtepNodeName().getValue()));
            //update global table
            Global global = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Global.class);
            global.setSwitches(Collections.singleton(new UUID(pswitchUuid)));

            LOG.trace("execute: create physical switch: {}", physicalSwitch);
            transaction.add(op.mutate(global)
                    .addMutation(global.getSwitchesColumn().getSchema(), Mutator.INSERT,
                            global.getSwitchesColumn().getData()));
            transaction.add(op.comment("Global: Mutating "
                            + physicalSwitchAugmentation.getHwvtepNodeName().getValue() + " " + pswitchUuid));
        } else {
            PhysicalSwitchAugmentation updatedPhysicalSwitch = operationalPhysicalSwitchOptional.get();
            String existingPhysicalSwitchName = updatedPhysicalSwitch.getHwvtepNodeName().getValue();
            /* In case TOR devices don't allow creation of PhysicalSwitch name might be null
             * as user is only adding configurable parameters to MDSAL like BFD params
             *
             * TODO Note: Consider handling tunnel udpate/remove in separate command
             */
            if (existingPhysicalSwitchName == null) {
                existingPhysicalSwitchName = operationalPhysicalSwitchOptional.get().getHwvtepNodeName().getValue();
            }
            // Name is immutable, and so we *can't* update it.  So we use extraPhysicalSwitch for the schema stuff
            PhysicalSwitch extraPhysicalSwitch = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(),
                    PhysicalSwitch.class);
            extraPhysicalSwitch.setName("");
            LOG.trace("execute: updating physical switch: {}", physicalSwitch);
            transaction.add(op.update(physicalSwitch)
                    .where(extraPhysicalSwitch.getNameColumn().getSchema().opEqual(existingPhysicalSwitchName))
                    .build());
            transaction.add(op.comment("Physical Switch: Updating " + existingPhysicalSwitchName));
        }
    }

    private void setName(PhysicalSwitch physicalSwitch, PhysicalSwitchAugmentation physicalSwitchAugmentation,
            Optional<PhysicalSwitchAugmentation> operationalPhysicalSwitchOptional) {
        if (physicalSwitchAugmentation.getHwvtepNodeName() != null) {
            physicalSwitch.setName(physicalSwitchAugmentation.getHwvtepNodeName().getValue());
        } else if (operationalPhysicalSwitchOptional.isPresent()
                && operationalPhysicalSwitchOptional.get().getHwvtepNodeName() != null) {
            physicalSwitch.setName(operationalPhysicalSwitchOptional.get().getHwvtepNodeName().getValue());
        }
    }

    private void setDescription(PhysicalSwitch physicalSwitch, PhysicalSwitchAugmentation physicalSwitchAugmentation) {
        if (physicalSwitchAugmentation.getHwvtepNodeDescription() != null) {
            physicalSwitch.setDescription(physicalSwitchAugmentation.getHwvtepNodeDescription());
        }
    }

    private void setManagementIps(PhysicalSwitch physicalSwitch,
            PhysicalSwitchAugmentation physicalSwitchAugmentation) {
        Set<String> ipSet = new HashSet<>();
        if (physicalSwitchAugmentation.getManagementIps() != null) {
            for (ManagementIps ip: physicalSwitchAugmentation.getManagementIps()) {
                ipSet.add(ip.getManagementIpsKey().getIpv4Address().getValue());
            }
            physicalSwitch.setManagementIps(ipSet);
        }
    }

    private void setTunnuleIps(PhysicalSwitch physicalSwitch, PhysicalSwitchAugmentation physicalSwitchAugmentation) {
        Set<String> ipSet = new HashSet<>();
        if (physicalSwitchAugmentation.getTunnelIps() != null) {
            for (TunnelIps ip: physicalSwitchAugmentation.getTunnelIps()) {
                ipSet.add(ip.getTunnelIpsKey().getIpv4Address().getValue());
            }
            physicalSwitch.setTunnelIps(ipSet);
        }
    }

    @SuppressWarnings("unchecked")
    private void setTunnels(TransactionBuilder transaction, InstanceIdentifier<Node> iid,
                    PhysicalSwitch physicalSwitch, PhysicalSwitchAugmentation physicalSwitchAugmentation,
                    boolean switchExists) {
        //TODO: revisit this code for optimizations
        //TODO: needs more testing
        if (physicalSwitchAugmentation.getTunnels() != null) {
            for (Tunnels tunnel : physicalSwitchAugmentation.getTunnels()) {
                Optional<Tunnels> opTunnelOpt = getOperationalState().getTunnels(iid, tunnel.getKey());
                Tunnel newTunnel = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Tunnel.class);

                UUID localUUID = getLocatorUUID(transaction,
                                (InstanceIdentifier<TerminationPoint>) tunnel.getLocalLocatorRef().getValue());
                UUID remoteUUID = getLocatorUUID(transaction,
                                (InstanceIdentifier<TerminationPoint>) tunnel.getRemoteLocatorRef().getValue());
                if (localUUID != null && remoteUUID != null) {
                    UUID uuid;
                    // local and remote must exist
                    newTunnel.setLocal(localUUID);
                    newTunnel.setRemote(remoteUUID);
                    setBfdParams(newTunnel, tunnel);
                    setBfdLocalConfigs(newTunnel, tunnel);
                    setBfdRemoteConfigs(newTunnel, tunnel);
                    if (!opTunnelOpt.isPresent()) {
                        String tunnelUuid = "Tunnel_" + HwvtepSouthboundMapper.getRandomUUID();
                        transaction.add(op.insert(newTunnel).withId(tunnelUuid));
                        transaction.add(op.comment("Tunnel: Creating " + tunnelUuid));
                        if (!switchExists) {
                            //TODO: Figure out a way to handle this
                            LOG.warn("Tunnel configuration requires pre-existing physicalSwitch");
                        } else {
                            // TODO: Can we reuse physicalSwitch instead?
                            PhysicalSwitch phySwitch =
                                            TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(),
                                                            PhysicalSwitch.class);
                            phySwitch.setTunnels(Collections.singleton(new UUID(tunnelUuid)));
                            phySwitch.setName(physicalSwitchAugmentation.getHwvtepNodeName().getValue());
                            transaction.add(op.mutate(phySwitch)
                                            .addMutation(phySwitch.getTunnels().getSchema(), Mutator.INSERT,
                                                    phySwitch.getTunnels().getData())
                                            .where(phySwitch.getNameColumn().getSchema()
                                                            .opEqual(phySwitch.getNameColumn().getData()))
                                            .build());
                            transaction.add(op.comment("PhysicalSwitch: Mutating " + tunnelUuid));
                        }
                        uuid = new UUID(tunnelUuid);
                    } else {
                        uuid = new UUID(opTunnelOpt.get().getTunnelUuid().getValue());
                        Tunnel extraTunnel =
                                TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Tunnel.class, null);
                        extraTunnel.getUuidColumn().setData(uuid);
                        transaction.add(op.update(newTunnel)
                                        .where(extraTunnel.getUuidColumn().getSchema().opEqual(uuid))
                                        .build());
                        transaction.add(op.comment("Tunnel: Updating " + uuid));
                    }
                }
            }
        }
    }

    private void setBfdParams(Tunnel tunnel, Tunnels psAugTunnel) {
        List<BfdParams> bfdParams = psAugTunnel.getBfdParams();
        if (bfdParams != null) {
            Map<String, String> bfdParamMap = new HashMap<>();
            for (BfdParams bfdParam : bfdParams) {
                bfdParamMap.put(bfdParam.getBfdParamKey(), bfdParam.getBfdParamValue());
            }
            try {
                tunnel.setBfdParams(ImmutableMap.copyOf(bfdParamMap));
            } catch (NullPointerException e) {
                LOG.warn("Incomplete BFD Params for tunnel", e);
            }
        }
    }

    private void setBfdLocalConfigs(Tunnel tunnel, Tunnels psAugTunnel) {
        List<BfdLocalConfigs> bfdLocalConfigs = psAugTunnel.getBfdLocalConfigs();
        if (bfdLocalConfigs != null) {
            Map<String, String> configLocalMap = new HashMap<>();
            for (BfdLocalConfigs localConfig : bfdLocalConfigs) {
                configLocalMap.put(localConfig.getBfdLocalConfigKey(), localConfig.getBfdLocalConfigValue());
            }
            try {
                tunnel.setBfdConfigLocal(ImmutableMap.copyOf(configLocalMap));
            } catch (NullPointerException e) {
                LOG.warn("Incomplete BFD LocalConfig for tunnel", e);
            }
        }
    }

    private void setBfdRemoteConfigs(Tunnel tunnel, Tunnels psAugTunnel) {
        List<BfdRemoteConfigs> bfdRemoteConfigs = psAugTunnel.getBfdRemoteConfigs();
        if (bfdRemoteConfigs != null) {
            Map<String, String> configRemoteMap = new HashMap<>();
            for (BfdRemoteConfigs remoteConfig : bfdRemoteConfigs) {
                configRemoteMap.put(remoteConfig.getBfdRemoteConfigKey(), remoteConfig.getBfdRemoteConfigValue());
            }
            try {
                tunnel.setBfdConfigRemote(ImmutableMap.copyOf(configRemoteMap));
            } catch (NullPointerException e) {
                LOG.warn("Incomplete BFD RemoteConfig for tunnel", e);
            }
        }
    }

    private UUID getLocatorUUID(TransactionBuilder transaction, InstanceIdentifier<TerminationPoint> iid) {
        UUID locatorUUID = null;
        Optional<HwvtepPhysicalLocatorAugmentation> opLocOptional =
                        getOperationalState().getPhysicalLocatorAugmentation(iid);
        if (opLocOptional.isPresent()) {
            // Get Locator UUID from operational
            HwvtepPhysicalLocatorAugmentation locatorAug = opLocOptional.get();
            locatorUUID = new UUID(locatorAug.getPhysicalLocatorUuid().getValue());
        } else {
            // TODO/FIXME: Not in operational, do we create a new one?
            LOG.warn("Trying to create tunnel without creating physical locators first");
            Optional<TerminationPoint> confLocOptional = new MdsalUtils(getOperationalState().getDataBroker())
                    .readOptional(LogicalDatastoreType.CONFIGURATION, iid);
            if (confLocOptional.isPresent()) {
                HwvtepPhysicalLocatorAugmentation locatorAugmentation =
                                confLocOptional.get().getAugmentation(HwvtepPhysicalLocatorAugmentation.class);
                locatorUUID = TransactUtils.createPhysicalLocator(transaction, getOperationalState(), iid);
            } else {
                LOG.warn("Unable to find endpoint for tunnel. Endpoint indentifier is {}", iid);
            }
        }
        return locatorUUID;
    }

    private Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> extractCreated(
            Collection<DataTreeModification<Node>> changes, Class<PhysicalSwitchAugmentation> class1) {
        Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> result = new HashMap<>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node created = TransactUtils.getCreated(mod);
                if (created != null) {
                    PhysicalSwitchAugmentation physicalSwitch =
                            created.getAugmentation(PhysicalSwitchAugmentation.class);
                    if (physicalSwitch != null) {
                        result.put(key, physicalSwitch);
                    }
                }
            }
        }
        return result;
    }

    private Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> extractUpdatedSwitches(
            Collection<DataTreeModification<Node>> changes, Class<PhysicalSwitchAugmentation> class1) {
        Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> result = new HashMap<>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node updated = TransactUtils.getUpdated(mod);
                if (updated != null) {
                    PhysicalSwitchAugmentation physicalSwitch =
                            updated.getAugmentation(PhysicalSwitchAugmentation.class);
                    if (physicalSwitch != null) {
                        result.put(key, physicalSwitch);
                    }
                }
            }
        }
        return result;
    }

}
