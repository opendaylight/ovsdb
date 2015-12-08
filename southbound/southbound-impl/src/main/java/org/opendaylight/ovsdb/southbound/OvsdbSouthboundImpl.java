/*
 * Copyright (c) 2015 Intel and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.IsNodeDpdkEnabledInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.IsNodeDpdkEnabledOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.IsNodeDpdkEnabledOutputBuilder;

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;

import java.util.List;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeDpdk;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeDpdkr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeDpdkvhost;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeDpdkvhostuser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.InterfaceTypeEntry;

import java.util.concurrent.Future;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Optional;

public class OvsdbSouthboundImpl implements OvsdbService {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbSouthboundImpl.class);
    private DataBroker dataBroker;

    public OvsdbSouthboundImpl(DataBroker db) {
        this.dataBroker = db;
    }

    private <D extends org.opendaylight.yangtools.yang.binding.DataObject> D read(
            final LogicalDatastoreType store, final InstanceIdentifier<D> path)  {
        D result = null;
        final ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        Optional<D> optionalDataObject;
        CheckedFuture<Optional<D>, ReadFailedException> future = transaction.read(store, path);
        try {
            optionalDataObject = future.checkedGet();
            if (optionalDataObject.isPresent()) {
                result = optionalDataObject.get();
            } else {
                LOG.debug("{}: Failed to read {}",
                        Thread.currentThread().getStackTrace()[1], path);
            }
        } catch (ReadFailedException e) {
            LOG.warn("Failed to read {} ", path, e);
        }
        transaction.close();
        return result;
    }

    private List<InterfaceTypeEntry> readInterfaceTypesByNodeConnectionIp(IpAddress ipAddr) {
        List<InterfaceTypeEntry> interfaceTypes = null;
        TopologyId OVSDB_TOPOLOGY_ID = new TopologyId(new Uri("ovsdb:1"));
        InstanceIdentifier<Topology> OVSDB_NODE_IID = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(OVSDB_TOPOLOGY_ID));

        Topology topology = read(LogicalDatastoreType.OPERATIONAL, OVSDB_NODE_IID);
        if (topology != null && topology.getNode() != null) {
            for (Node node : topology.getNode()) {
                OvsdbNodeAugmentation ovsdbNodeAugmentation = node.getAugmentation(OvsdbNodeAugmentation.class);
                if (ovsdbNodeAugmentation != null) {
                    if(ovsdbNodeAugmentation.getConnectionInfo().getRemoteIp() == ipAddr) {
                        interfaceTypes = ovsdbNodeAugmentation.getInterfaceTypeEntry();
                        break;
                    }
                }
            }
        }

        return interfaceTypes;
    }

    @Override
    public Future<RpcResult<IsNodeDpdkEnabledOutput>> isNodeDpdkEnabled(IsNodeDpdkEnabledInput input) {
        IsNodeDpdkEnabledOutputBuilder isDpdkEnabledBuilder = new IsNodeDpdkEnabledOutputBuilder();
        isDpdkEnabledBuilder.setDpdkEnabled(false);

        IpAddress nodeIpAddr = input.getNodeIp();
        if (nodeIpAddr == null) {
            LOG.info("Input Error, return default false.");
            return RpcResultBuilder.success(isDpdkEnabledBuilder.build()).buildFuture();
        }

        LOG.info("Trying to check is DPDK enabled for node {}", nodeIpAddr);
        List<InterfaceTypeEntry> interfaceTypesOfNode = readInterfaceTypesByNodeConnectionIp(nodeIpAddr);
        if (interfaceTypesOfNode != null) {
            /* Try to find the Dpdk interface types in the InterfaceTypeEntry */
            for (InterfaceTypeEntry interfaceType : interfaceTypesOfNode) {
                java.lang.Class<? extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase> type
                         = interfaceType.getInterfaceType();
                if (type == InterfaceTypeDpdk.class || type == InterfaceTypeDpdkr.class
                        || type == InterfaceTypeDpdkvhost.class || type == InterfaceTypeDpdkvhostuser.class) {
                    isDpdkEnabledBuilder.setDpdkEnabled(true);
                }
            }
        }

        return RpcResultBuilder.success(isDpdkEnabledBuilder.build()).buildFuture();
    }
}
