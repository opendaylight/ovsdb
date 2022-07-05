/*
 * Copyright (c) 2015, 2017 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepDeviceInfo;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.EncapsulationTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalPortAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.Acls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalMcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalUcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalRouters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalRoutersKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitchesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.Switches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.Tunnels;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO: need to be optimized, get entry by iid not name
public class HwvtepOperationalState {

    private static final Logger LOG = LoggerFactory.getLogger(HwvtepOperationalState.class);

    private final Map<InstanceIdentifier<Node>, Node> operationalNodes = new HashMap<>();
    private ReadWriteTransaction transaction;
    HashMap<InstanceIdentifier<TerminationPoint>, UUID> inflightLocators = new HashMap<>();
    private final HwvtepDeviceInfo deviceInfo;
    private final HwvtepConnectionInstance connectionInstance;
    private final Map<Class<? extends Identifiable>, Map<InstanceIdentifier, UUID>> currentTxUUIDs =
            new ConcurrentHashMap<>();
    private final Map<Class<? extends Identifiable>, Map<InstanceIdentifier, Boolean>> currentTxDeletedKeys =
            new ConcurrentHashMap<>();

    /* stores the modified and deleted data for each child type of each node id
       Map<nodeid , Pair < updated, deleted >
       each updated/ deleted contains Map < child type, List<ChildData>>
       child type is the child of hwvtep Global augmentation
     */
    private Map<InstanceIdentifier<Node>,
            Pair<Map<Class<? extends Identifiable>, List<Identifiable>>,
                    Map<Class<? extends Identifiable>, List<Identifiable>>>> modifiedData = new HashMap<>();
    private boolean inReconciliation = false;
    private final DataBroker db;
    private final Collection<DataTreeModification<Node>> changes;
    long transactionId = 0;

    public HwvtepOperationalState(final DataBroker db, final HwvtepConnectionInstance connectionInstance,
                                  final Collection<DataTreeModification<Node>> changes) {
        this.connectionInstance = connectionInstance;
        deviceInfo = connectionInstance.getDeviceInfo();
        this.db = db;
        this.changes = changes;
        transaction = db.newReadWriteTransaction();
    }

    public HwvtepOperationalState(final HwvtepConnectionInstance connectionInstance) {
        this.connectionInstance = connectionInstance;
        deviceInfo = connectionInstance.getDeviceInfo();
        db = connectionInstance.getDataBroker();
        changes = null;
        transaction = connectionInstance.getDataBroker().newReadWriteTransaction();
        Optional<Node> readNode = new MdsalUtils(db).readOptional(LogicalDatastoreType.OPERATIONAL,
                connectionInstance.getInstanceIdentifier());
        if (readNode.isPresent()) {
            operationalNodes.put(connectionInstance.getInstanceIdentifier(), readNode.get());
        }
    }

    public HwvtepOperationalState(final DataBroker db,
                                  final HwvtepConnectionInstance connectionInstance,
                                  final Collection<DataTreeModification<Node>> changes,
                                  final Node globalOperNode,
                                  final Node psNode) {
        this(db, connectionInstance, changes);
        operationalNodes.put(connectionInstance.getInstanceIdentifier(), globalOperNode);
        HwvtepGlobalAugmentation globalAugmentation = globalOperNode.augmentation(HwvtepGlobalAugmentation.class);
        if (globalAugmentation != null) {
            if (!HwvtepSouthboundUtil.isEmptyMap(globalAugmentation.getSwitches())) {
                operationalNodes.put((InstanceIdentifier<Node>)
                        globalAugmentation.getSwitches().values().iterator().next().getSwitchRef().getValue(), psNode);
            }
        }
    }

    public void readOperationalNodes() {
        if (inReconciliation) {
            return;
        }
        if (changes == null) {
            LOG.warn("Could not read operational nodes for {} as changes is",
                    connectionInstance.getNodeId().getValue());
            return;
        }
        Map<InstanceIdentifier<Node>, Node> nodeCreateOrUpdate =
                TransactUtils.extractCreatedOrUpdatedOrRemoved(changes, Node.class);
        if (nodeCreateOrUpdate != null) {
            transaction = db.newReadWriteTransaction();
            for (Entry<InstanceIdentifier<Node>, Node> entry: nodeCreateOrUpdate.entrySet()) {
                Optional<Node> readNode = new MdsalUtils(db).readOptional(LogicalDatastoreType.OPERATIONAL,
                        entry.getKey());
                //add related globalNode or physicalSwitchNode to operationalNodes map
                //for example, when creating physical port, logical switch is needed
                //but logical switch is in HwvtepGlobalAugmentation rather than PhysicalSwitchAugmentation
                if (readNode.isPresent()) {
                    operationalNodes.put(entry.getKey(), readNode.get());
                    HwvtepGlobalAugmentation hgAugmentation =
                            readNode.get().augmentation(HwvtepGlobalAugmentation.class);
                    PhysicalSwitchAugmentation psAugmentation =
                            readNode.get().augmentation(PhysicalSwitchAugmentation.class);
                    if (hgAugmentation != null && hgAugmentation.getSwitches() != null) {
                        for (Switches pswitch : hgAugmentation.getSwitches().values()) {
                            @SuppressWarnings("unchecked")
                            InstanceIdentifier<Node> psNodeIid =
                                    (InstanceIdentifier<Node>) pswitch.getSwitchRef().getValue();
                            Optional<Node> psNode =
                                new MdsalUtils(db).readOptional(LogicalDatastoreType.OPERATIONAL, psNodeIid);
                            if (psNode.isPresent()) {
                                operationalNodes.put(psNodeIid, psNode.get());
                            }
                        }
                    }
                    if (psAugmentation != null) {
                        @SuppressWarnings("unchecked")
                        InstanceIdentifier<Node> hgNodeIid =
                                (InstanceIdentifier<Node>) psAugmentation.getManagedBy().getValue();
                        Optional<Node> hgNode = new MdsalUtils(db).readOptional(
                                LogicalDatastoreType.OPERATIONAL, hgNodeIid);
                        if (hgNode.isPresent()) {
                            operationalNodes.put(hgNodeIid, hgNode.get());
                        }
                    }
                }
            }
        }
    }

    public Optional<Node> getGlobalNode(final InstanceIdentifier<?> iid) {
        InstanceIdentifier<Node> nodeIid = iid.firstIdentifierOf(Node.class);
        return Optional.ofNullable(operationalNodes.get(nodeIid));
    }

    public Optional<HwvtepGlobalAugmentation> getHwvtepGlobalAugmentation(final InstanceIdentifier<?> iid) {
        Optional<Node> nodeOptional = getGlobalNode(requireNonNull(iid));
        if (nodeOptional.isPresent()) {
            return Optional.ofNullable(nodeOptional.get().augmentation(HwvtepGlobalAugmentation.class));
        }
        return Optional.empty();
    }

    public Optional<PhysicalSwitchAugmentation> getPhysicalSwitchAugmentation(final InstanceIdentifier<?> iid) {
        Optional<Node> nodeOptional = getGlobalNode(requireNonNull(iid));
        if (nodeOptional.isPresent()) {
            return Optional.ofNullable(nodeOptional.get().augmentation(PhysicalSwitchAugmentation.class));
        }
        return Optional.empty();
    }

    public Optional<Map<TerminationPointKey, TerminationPoint>> getTerminationPointList(
            final InstanceIdentifier<?> iid) {
        Optional<Node> nodeOptional = getGlobalNode(requireNonNull(iid));
        if (nodeOptional.isPresent() && nodeOptional.get().getTerminationPoint() != null) {
            return Optional.ofNullable(nodeOptional.get().getTerminationPoint());
        }
        return Optional.empty();
    }

    public Optional<LogicalSwitches> getLogicalSwitches(final InstanceIdentifier<?> iid,
            final LogicalSwitchesKey logicalSwitchesKey) {
        Optional<HwvtepGlobalAugmentation> nodeOptional = getHwvtepGlobalAugmentation(requireNonNull(iid));
        if (nodeOptional.isPresent()) {
            LogicalSwitches lswitch = nodeOptional.get().nonnullLogicalSwitches().get(logicalSwitchesKey);
            if (lswitch != null) {
                return Optional.of(lswitch);
            }
        }
        return Optional.empty();
    }

    public Optional<LogicalSwitches> getLogicalSwitches(final InstanceIdentifier<LogicalSwitches> iid) {
        return new MdsalUtils(db).readOptional(LogicalDatastoreType.OPERATIONAL, iid);
    }

    public Optional<Tunnels> getTunnels(final InstanceIdentifier<?> iid, final TunnelsKey tunnelsKey) {
        Optional<PhysicalSwitchAugmentation> psOptional = getPhysicalSwitchAugmentation(requireNonNull(iid));
        if (psOptional.isPresent()) {
            Tunnels tunnel = psOptional.get().nonnullTunnels().get(tunnelsKey);
            if (tunnel != null) {
                return Optional.of(tunnel);
            }
        }
        return Optional.empty();
    }

    public Optional<Tunnels> getTunnels(final InstanceIdentifier<Tunnels> iid) {
        Optional<Tunnels> tunnels = new MdsalUtils(db).readOptional(LogicalDatastoreType.OPERATIONAL, iid);
        return tunnels;
    }

    public Optional<HwvtepPhysicalPortAugmentation> getPhysicalPortAugmentation(final InstanceIdentifier<?> iid,
            final HwvtepNodeName hwvtepNodeName) {
        Optional<Map<TerminationPointKey, TerminationPoint>> nodeOptional =
                getTerminationPointList(requireNonNull(iid));
        if (nodeOptional.isPresent()) {
            for (TerminationPoint tp : nodeOptional.get().values()) {
                HwvtepPhysicalPortAugmentation hppAugmentation =
                        tp.augmentation(HwvtepPhysicalPortAugmentation.class);
                if (hppAugmentation != null && hppAugmentation.getHwvtepNodeName().equals(hwvtepNodeName)) {
                    return Optional.ofNullable(hppAugmentation);
                }
            }
        }
        return Optional.empty();
    }

    public Optional<HwvtepPhysicalLocatorAugmentation> getPhysicalLocatorAugmentation(final InstanceIdentifier<?> iid,
            final IpAddress dstIp, final EncapsulationTypeBase encapType) {
        Optional<Map<TerminationPointKey, TerminationPoint>> nodeOptional =
                getTerminationPointList(requireNonNull(iid));
        if (nodeOptional.isPresent()) {
            for (TerminationPoint tp : nodeOptional.get().values()) {
                HwvtepPhysicalLocatorAugmentation hppAugmentation =
                        tp.augmentation(HwvtepPhysicalLocatorAugmentation.class);
                if (hppAugmentation != null && hppAugmentation.getDstIp().equals(dstIp)
                        && hppAugmentation.getEncapsulationType().equals(encapType)) {
                    return Optional.ofNullable(hppAugmentation);
                }
            }
        }
        return Optional.empty();
    }

    public Optional<HwvtepPhysicalLocatorAugmentation>
            getPhysicalLocatorAugmentation(final InstanceIdentifier<TerminationPoint> iid) {
        Optional<TerminationPoint> tp =
            new MdsalUtils(db).readOptional(LogicalDatastoreType.OPERATIONAL, iid);
        if (tp.isPresent()) {
            return Optional.ofNullable(tp.get().augmentation(HwvtepPhysicalLocatorAugmentation.class));
        }
        return Optional.empty();
    }

    public Optional<LocalMcastMacs> getLocalMcastMacs(final InstanceIdentifier<?> iid, final LocalMcastMacsKey key) {
        Optional<HwvtepGlobalAugmentation> nodeOptional = getHwvtepGlobalAugmentation(requireNonNull(iid));
        if (nodeOptional.isPresent()) {
            LocalMcastMacs mac = nodeOptional.get().nonnullLocalMcastMacs().get(key);
            if (mac != null) {
                return Optional.of(mac);
            }
        }
        return Optional.empty();
    }

    public Optional<RemoteMcastMacs> getRemoteMcastMacs(final InstanceIdentifier<?> iid, final RemoteMcastMacsKey key) {
        Optional<HwvtepGlobalAugmentation> nodeOptional = getHwvtepGlobalAugmentation(requireNonNull(iid));
        if (nodeOptional.isPresent()) {
            RemoteMcastMacs mac = nodeOptional.get().nonnullRemoteMcastMacs().get(key);
            if (mac != null) {
                return Optional.of(mac);
            }
        }
        return Optional.empty();
    }

    public Optional<LocalUcastMacs> getLocalUcastMacs(final InstanceIdentifier<?> iid, final LocalUcastMacsKey key) {
        Optional<HwvtepGlobalAugmentation> nodeOptional = getHwvtepGlobalAugmentation(requireNonNull(iid));
        if (nodeOptional.isPresent()) {
            LocalUcastMacs mac = nodeOptional.get().nonnullLocalUcastMacs().get(key);
            if (mac != null) {
                return Optional.of(mac);
            }
        }
        return Optional.empty();
    }

    public Optional<RemoteUcastMacs> getRemoteUcastMacs(final InstanceIdentifier<?> iid, final RemoteUcastMacsKey key) {
        Optional<HwvtepGlobalAugmentation> nodeOptional = getHwvtepGlobalAugmentation(requireNonNull(iid));
        if (nodeOptional.isPresent()) {
            RemoteUcastMacs mac = nodeOptional.get().nonnullRemoteUcastMacs().get(key);
            if (mac != null) {
                return Optional.of(mac);
            }
        }
        return Optional.empty();
    }

    public Optional<LogicalRouters> getLogicalRouters(final InstanceIdentifier<?> iid,
            final LogicalRoutersKey logicalRoutersKey) {
        Optional<HwvtepGlobalAugmentation> nodeOptional = getHwvtepGlobalAugmentation(requireNonNull(iid));
        if (nodeOptional.isPresent()) {
            LogicalRouters lrouter = nodeOptional.get().nonnullLogicalRouters().get(logicalRoutersKey);
            if (lrouter != null) {
                return Optional.of(lrouter);
            }
        }
        return Optional.empty();
    }

    public Optional<Acls> getAcls(final InstanceIdentifier<Acls> iid) {
        Optional<Acls> acl = new MdsalUtils(db).readOptional(LogicalDatastoreType.OPERATIONAL, iid);
        return acl;
    }

    public ReadWriteTransaction getReadWriteTransaction() {
        return transaction;
    }

    public void setPhysicalLocatorInFlight(final InstanceIdentifier<TerminationPoint> iid,
                                           final UUID uuid) {
        inflightLocators.put(iid, uuid);
    }

    public UUID getPhysicalLocatorInFlight(final InstanceIdentifier<TerminationPoint> iid) {
        return inflightLocators.get(iid);
    }

    public HwvtepConnectionInstance getConnectionInstance() {
        return connectionInstance;
    }

    public HwvtepDeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    public void updateCurrentTxData(final Class<? extends Identifiable> cls, final InstanceIdentifier key,
            final UUID uuid) {
        HwvtepSouthboundUtil.updateData(currentTxUUIDs, cls, key, uuid);
    }

    public void updateCurrentTxDeleteData(final Class<? extends Identifiable> cls, final InstanceIdentifier key) {
        HwvtepSouthboundUtil.updateData(currentTxDeletedKeys, cls, key, Boolean.TRUE);
    }

    public UUID getUUIDFromCurrentTx(final Class<? extends Identifiable> cls, final InstanceIdentifier key) {
        return HwvtepSouthboundUtil.getData(currentTxUUIDs, cls, key);
    }

    public boolean isKeyPartOfCurrentTx(final Class<? extends Identifiable> cls, final InstanceIdentifier key) {
        return HwvtepSouthboundUtil.containsKey(currentTxUUIDs, cls, key);
    }

    public Set<InstanceIdentifier> getDeletedKeysInCurrentTx(final Class<? extends Identifiable> cls) {
        if (currentTxDeletedKeys.containsKey(cls)) {
            return currentTxDeletedKeys.get(cls).keySet();
        }
        return Collections.emptySet();
    }

    public List<? extends Identifiable> getUpdatedData(final InstanceIdentifier<Node> key,
                                                       final Class<? extends Identifiable> cls) {
        List<Identifiable> result = null;
        if (modifiedData.get(key) != null && modifiedData.get(key).getLeft() != null) {
            result = modifiedData.get(key).getLeft().get(cls);
        }
        if (result == null) {
            result = Collections.emptyList();
        }
        return result;
    }

    public List<? extends Identifiable> getDeletedData(final InstanceIdentifier<Node> key,
                                                       final Class<? extends Identifiable> cls) {
        List<Identifiable> result = null;
        if (modifiedData.get(key) != null && modifiedData.get(key).getRight() != null) {
            result = modifiedData.get(key).getRight().get(cls);
        }
        if (result == null) {
            result = Collections.emptyList();
        }
        return result;
    }

    public void setModifiedData(final Map<InstanceIdentifier<Node>,
            Pair<Map<Class<? extends Identifiable>, List<Identifiable>>,
                    Map<Class<? extends Identifiable>, List<Identifiable>>>> modifiedData) {
        this.modifiedData = modifiedData;
    }

    public boolean isInReconciliation() {
        return inReconciliation;
    }

    public void setInReconciliation(final boolean inReconciliation) {
        this.inReconciliation = inReconciliation;
    }

    public DataBroker getDataBroker() {
        return db;
    }


    public void clearIntransitKeys() {
        currentTxUUIDs.forEach((cls, map) -> {
            map.forEach((iid, uuid) -> deviceInfo.clearInTransit(cls, iid));
        });
        currentTxDeletedKeys.forEach((cls, map) -> {
            map.forEach((iid, val) -> deviceInfo.clearInTransit(cls, iid));
        });
        currentTxUUIDs.clear();
        currentTxDeletedKeys.clear();
        deviceInfo.onOperDataAvailable();
    }

    public long getTransactionId() {
        return transactionId;
    }
}
