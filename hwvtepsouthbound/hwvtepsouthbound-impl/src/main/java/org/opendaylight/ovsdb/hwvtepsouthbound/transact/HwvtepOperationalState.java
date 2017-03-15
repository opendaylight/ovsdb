/*
 * Copyright (c) 2015, 2017 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepDeviceInfo;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.EncapsulationTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalPortAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.Acls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.AclsKey;
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
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

//TODO: need to be optimized, get entry by iid not name
public class HwvtepOperationalState {

    private static final Logger LOG = LoggerFactory.getLogger(HwvtepOperationalState.class);

    private Map<InstanceIdentifier<Node>, Node> operationalNodes = new HashMap<>();
    private ReadWriteTransaction transaction;
    HashMap<InstanceIdentifier<TerminationPoint>, UUID> inflightLocators = new HashMap<>();
    private HwvtepDeviceInfo deviceInfo;
    private HwvtepConnectionInstance connectionInstance;
    private Map<Class<? extends Identifiable>, Map<InstanceIdentifier, UUID>> currentTxUUIDs = new ConcurrentHashMap<>();
    private Map<Class<? extends Identifiable>, Map<InstanceIdentifier, Boolean>> currentTxDeletedKeys = new ConcurrentHashMap<>();

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

    public HwvtepOperationalState(DataBroker db, HwvtepConnectionInstance connectionInstance,
                                  Collection<DataTreeModification<Node>> changes) {
        this.connectionInstance = connectionInstance;
        this.deviceInfo = connectionInstance.getDeviceInfo();
        this.db = db;
        this.changes = changes;
        this.transaction = db.newReadWriteTransaction();
    }

    public HwvtepOperationalState(HwvtepConnectionInstance connectionInstance) {
        this.connectionInstance = connectionInstance;
        this.deviceInfo = connectionInstance.getDeviceInfo();
        this.db = connectionInstance.getDataBroker();
        this.changes = null;
        transaction = connectionInstance.getDataBroker().newReadWriteTransaction();
        Optional<Node> readNode = HwvtepSouthboundUtil.readNode(transaction,
                connectionInstance.getInstanceIdentifier());
        if (readNode.isPresent()) {
            operationalNodes.put(connectionInstance.getInstanceIdentifier(), readNode.get());
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
                Optional<Node> readNode = HwvtepSouthboundUtil.readNode(transaction, entry.getKey());
                //add related globalNode or physicalSwitchNode to operationalNodes map
                //for example, when creating physical port, logical switch is needed
                //but logical switch is in HwvtepGlobalAugmentation rather than PhysicalSwitchAugmentation
                if (readNode.isPresent()) {
                    operationalNodes.put(entry.getKey(), readNode.get());
                    HwvtepGlobalAugmentation hgAugmentation = readNode.get().getAugmentation(HwvtepGlobalAugmentation.class);
                    PhysicalSwitchAugmentation psAugmentation = readNode.get().getAugmentation(PhysicalSwitchAugmentation.class);
                    if (hgAugmentation != null && hgAugmentation.getSwitches() != null) {
                        for (Switches pswitch : hgAugmentation.getSwitches()) {
                            @SuppressWarnings("unchecked")
                            InstanceIdentifier<Node> psNodeIid = (InstanceIdentifier<Node>) pswitch.getSwitchRef().getValue();
                            Optional<Node> psNode = HwvtepSouthboundUtil.readNode(transaction, psNodeIid);
                            if (psNode.isPresent()) {
                                operationalNodes.put(psNodeIid, psNode.get());
                            }
                        }
                    }
                    if (psAugmentation != null) {
                        @SuppressWarnings("unchecked")
                        InstanceIdentifier<Node> hgNodeIid = (InstanceIdentifier<Node>) psAugmentation.getManagedBy().getValue();
                        Optional<Node> hgNode = HwvtepSouthboundUtil.readNode(transaction, hgNodeIid);
                        if (hgNode.isPresent()) {
                            operationalNodes.put(hgNodeIid, hgNode.get());
                        }
                    }
                }
            }
        }
    }

    public Optional<Node> getGlobalNode(InstanceIdentifier<?> iid) {
        InstanceIdentifier<Node> nodeIid = iid.firstIdentifierOf(Node.class);
        return Optional.fromNullable(operationalNodes.get(nodeIid));
    }

    public Optional<HwvtepGlobalAugmentation> getHwvtepGlobalAugmentation(InstanceIdentifier<?> iid) {
        Preconditions.checkNotNull(iid);
        Optional<Node> nodeOptional = getGlobalNode(iid);
        if (nodeOptional.isPresent()) {
            return Optional.fromNullable(nodeOptional.get().getAugmentation(HwvtepGlobalAugmentation.class));
        }
        return Optional.absent();
    }

    public Optional<PhysicalSwitchAugmentation> getPhysicalSwitchAugmentation(InstanceIdentifier<?> iid) {
        Preconditions.checkNotNull(iid);
        Optional<Node> nodeOptional = getGlobalNode(iid);
        if (nodeOptional.isPresent()) {
            return Optional.fromNullable(nodeOptional.get().getAugmentation(PhysicalSwitchAugmentation.class));
        }
        return Optional.absent();
    }

    public Optional<List<TerminationPoint>> getTerminationPointList(InstanceIdentifier<?> iid) {
        Preconditions.checkNotNull(iid);
        Optional<Node> nodeOptional = getGlobalNode(iid);
        if (nodeOptional.isPresent() && nodeOptional.get().getTerminationPoint() != null) {
            return Optional.fromNullable(nodeOptional.get().getTerminationPoint());
        }
        return Optional.absent();
    }

    public Optional<LogicalSwitches> getLogicalSwitches(InstanceIdentifier<?> iid, LogicalSwitchesKey logicalSwitchesKey) {
        Preconditions.checkNotNull(iid);
        Optional<HwvtepGlobalAugmentation> nodeOptional = getHwvtepGlobalAugmentation(iid);
        if (nodeOptional.isPresent()) {
            HwvtepGlobalAugmentation hgAugmentation = nodeOptional.get();
            List<LogicalSwitches> lswitchList = null;
            if (hgAugmentation != null) {
                lswitchList = hgAugmentation.getLogicalSwitches();
            }
            if (lswitchList != null) {
                for (LogicalSwitches lswitch: lswitchList) {
                    if (lswitch.getKey().equals(logicalSwitchesKey)) {
                        return Optional.fromNullable(lswitch);
                    }
                }
            }
        }
        return Optional.absent();
    }

    public Optional<Tunnels> getTunnels(InstanceIdentifier<?> iid, TunnelsKey tunnelsKey) {
        Preconditions.checkNotNull(iid);
        Optional<PhysicalSwitchAugmentation> psOptional = getPhysicalSwitchAugmentation(iid);
        if (psOptional.isPresent()) {
            PhysicalSwitchAugmentation psAugmentation = psOptional.get();
            List<Tunnels> tunnelList = null;
            if (psAugmentation != null) {
                tunnelList = psAugmentation.getTunnels();
            }
            if (tunnelList != null) {
                for (Tunnels tunnel: tunnelList) {
                    if (tunnel.getKey().equals(tunnelsKey)) {
                        return Optional.fromNullable(tunnel);
                    }
                }
            }
        }
        return Optional.absent();
    }

    public Optional<HwvtepPhysicalPortAugmentation> getPhysicalPortAugmentation(InstanceIdentifier<?> iid,
            HwvtepNodeName hwvtepNodeName) {
        Preconditions.checkNotNull(iid);
        Optional<List<TerminationPoint>> nodeOptional = getTerminationPointList(iid);
        if (nodeOptional.isPresent()) {
            List<TerminationPoint> tpList = nodeOptional.get();
            for (TerminationPoint tp : tpList) {
                HwvtepPhysicalPortAugmentation hppAugmentation = tp.getAugmentation(HwvtepPhysicalPortAugmentation.class);
                if (hppAugmentation != null && hppAugmentation.getHwvtepNodeName().equals(hwvtepNodeName)) {
                    return Optional.fromNullable(hppAugmentation);
                }
            }
        }
        return Optional.absent();
    }

    public Optional<HwvtepPhysicalLocatorAugmentation> getPhysicalLocatorAugmentation(InstanceIdentifier<?> iid,
            IpAddress dstIp, Class<? extends EncapsulationTypeBase> encapType) {
        Preconditions.checkNotNull(iid);
        Optional<List<TerminationPoint>> nodeOptional = getTerminationPointList(iid);
        if (nodeOptional.isPresent()) {
            List<TerminationPoint> tpList = nodeOptional.get();
            for (TerminationPoint tp : tpList) {
                HwvtepPhysicalLocatorAugmentation hppAugmentation = tp.getAugmentation(HwvtepPhysicalLocatorAugmentation.class);
                if (hppAugmentation != null && hppAugmentation.getDstIp().equals(dstIp)
                        && hppAugmentation.getEncapsulationType().equals(encapType)) {
                    return Optional.fromNullable(hppAugmentation);
                }
            }
        }
        return Optional.absent();
    }

    public Optional<LocalMcastMacs> getLocalMcastMacs(InstanceIdentifier<?> iid, LocalMcastMacsKey key) {
        Preconditions.checkNotNull(iid);
        Optional<HwvtepGlobalAugmentation> nodeOptional = getHwvtepGlobalAugmentation(iid);
        if (nodeOptional.isPresent()) {
            HwvtepGlobalAugmentation hgAugmentation = nodeOptional.get();
            List<LocalMcastMacs> macList = null;
            if (hgAugmentation != null) {
                macList = hgAugmentation.getLocalMcastMacs();
            }
            if (macList != null) {
                for (LocalMcastMacs mac: macList) {
                    if (mac.getKey().equals(key)) {
                        return Optional.fromNullable(mac);
                    }
                }
            }
        }
        return Optional.absent();
    }

    public Optional<RemoteMcastMacs> getRemoteMcastMacs(InstanceIdentifier<?> iid, RemoteMcastMacsKey key) {
        Preconditions.checkNotNull(iid);
        Optional<HwvtepGlobalAugmentation> nodeOptional = getHwvtepGlobalAugmentation(iid);
        if (nodeOptional.isPresent()) {
            HwvtepGlobalAugmentation hgAugmentation = nodeOptional.get();
            List<RemoteMcastMacs> macList = null;
            if (hgAugmentation != null) {
                macList = hgAugmentation.getRemoteMcastMacs();
            }
            if (macList != null) {
                for (RemoteMcastMacs mac: macList) {
                    if (mac.getKey().equals(key)) {
                        return Optional.fromNullable(mac);
                    }
                }
            }
        }
        return Optional.absent();
    }

    public Optional<LocalUcastMacs> getLocalUcastMacs(InstanceIdentifier<?> iid, LocalUcastMacsKey key) {
        Preconditions.checkNotNull(iid);
        Optional<HwvtepGlobalAugmentation> nodeOptional = getHwvtepGlobalAugmentation(iid);
        if (nodeOptional.isPresent()) {
            HwvtepGlobalAugmentation hgAugmentation = nodeOptional.get();
            List<LocalUcastMacs> macList = null;
            if (hgAugmentation != null) {
                macList = hgAugmentation.getLocalUcastMacs();
            }
            if (macList != null) {
                for (LocalUcastMacs mac: macList) {
                    if (mac.getKey().equals(key)) {
                        return Optional.fromNullable(mac);
                    }
                }
            }
        }
        return Optional.absent();
    }

    public Optional<RemoteUcastMacs> getRemoteUcastMacs(InstanceIdentifier<?> iid, RemoteUcastMacsKey key) {
        Preconditions.checkNotNull(iid);
        Optional<HwvtepGlobalAugmentation> nodeOptional = getHwvtepGlobalAugmentation(iid);
        if (nodeOptional.isPresent()) {
            HwvtepGlobalAugmentation hgAugmentation = nodeOptional.get();
            List<RemoteUcastMacs> macList = null;
            if (hgAugmentation != null) {
                macList = hgAugmentation.getRemoteUcastMacs();
            }
            if (macList != null) {
                for (RemoteUcastMacs mac: macList) {
                    if (mac.getKey().equals(key)) {
                        return Optional.fromNullable(mac);
                    }
                }
            }
        }
        return Optional.absent();
    }

    public Optional<LogicalRouters> getLogicalRouters(final InstanceIdentifier<?> iid,
            final LogicalRoutersKey logicalRoutersKey) {
        Preconditions.checkNotNull(iid);
        Optional<HwvtepGlobalAugmentation> nodeOptional = getHwvtepGlobalAugmentation(iid);
        if (nodeOptional.isPresent()) {
            HwvtepGlobalAugmentation hgAugmentation = nodeOptional.get();
            if (hgAugmentation != null && hgAugmentation.getLogicalRouters() != null) {
                for (LogicalRouters lrouter: hgAugmentation.getLogicalRouters()) {
                    if (lrouter.getKey().equals(logicalRoutersKey)) {
                        return Optional.fromNullable(lrouter);
                    }
                }
            }
        }
        return Optional.absent();
    }

    public Optional<HwvtepPhysicalLocatorAugmentation> getPhysicalLocatorAugmentation(InstanceIdentifier<TerminationPoint> iid) {
        Optional<TerminationPoint> tp = HwvtepSouthboundUtil.readNode(transaction, iid);
        if (tp.isPresent()) {
            return Optional.fromNullable(tp.get().getAugmentation(HwvtepPhysicalLocatorAugmentation.class));
        }
        return Optional.absent();
    }

    public Optional<LogicalSwitches> getLogicalSwitches(InstanceIdentifier<LogicalSwitches> iid) {
        Optional<LogicalSwitches> lswitch = HwvtepSouthboundUtil.readNode(transaction, iid);
        return lswitch;
    }

    public Optional<Tunnels> getTunnels(InstanceIdentifier<Tunnels> iid) {
        Optional<Tunnels> tunnels = HwvtepSouthboundUtil.readNode(transaction, iid);
        return tunnels;
    }

    public Optional<Acls> getAcls(InstanceIdentifier<Acls> iid) {
        Optional<Acls> acl = HwvtepSouthboundUtil.readNode(transaction, iid);
        return acl;
    }

    public ReadWriteTransaction getReadWriteTransaction() {
        return transaction;
    }

    public void setPhysicalLocatorInFlight(InstanceIdentifier<TerminationPoint> iid,
                                           UUID uuid) {
        inflightLocators.put(iid, uuid);
    }

    public UUID getPhysicalLocatorInFlight(InstanceIdentifier<TerminationPoint> iid) {
        return inflightLocators.get(iid);
    }

    public HwvtepConnectionInstance getConnectionInstance() {
        return connectionInstance;
    }

    public HwvtepDeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    public void updateCurrentTxData(Class<? extends Identifiable> cls, InstanceIdentifier key, UUID uuid) {
        HwvtepSouthboundUtil.updateData(currentTxUUIDs, cls, key, uuid);
    }

    public void updateCurrentTxDeleteData(Class<? extends Identifiable> cls, InstanceIdentifier key) {
        HwvtepSouthboundUtil.updateData(currentTxDeletedKeys, cls, key, Boolean.TRUE);
    }

    public UUID getUUIDFromCurrentTx(Class<? extends Identifiable> cls, InstanceIdentifier key) {
        return HwvtepSouthboundUtil.getData(currentTxUUIDs, cls, key);
    }

    public boolean isKeyPartOfCurrentTx(Class<? extends Identifiable> cls, InstanceIdentifier key) {
        return HwvtepSouthboundUtil.containsKey(currentTxUUIDs, cls, key);
    }

    public Set<InstanceIdentifier> getDeletedKeysInCurrentTx(Class<? extends Identifiable> cls) {
        if (currentTxDeletedKeys.containsKey(cls)) {
            return currentTxDeletedKeys.get(cls).keySet();
        }
        return Collections.EMPTY_SET;
    }

    public List<? extends Identifiable> getUpdatedData(final InstanceIdentifier<Node> key,
                                                       final Class<? extends Identifiable> cls) {
        List<Identifiable> result = null;
        if (modifiedData.get(key) != null && modifiedData.get(key).getLeft() != null) {
            result = modifiedData.get(key).getLeft().get(cls);
        }
        if (result == null) {
            result = Collections.EMPTY_LIST;
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
            result = Collections.EMPTY_LIST;
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

    public void setInReconciliation(boolean inReconciliation) {
        this.inReconciliation = inReconciliation;
    }
}
