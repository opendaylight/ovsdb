/*
 * Copyright © 2017 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.ACL;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalRouter;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepAclRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepLogicalSwitchRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.Acls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalRouters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalRoutersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalRoutersKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.logical.router.attributes.AclBindings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.logical.router.attributes.AclBindingsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.logical.router.attributes.AclBindingsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.logical.router.attributes.StaticRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.logical.router.attributes.StaticRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.logical.router.attributes.SwitchBindings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.logical.router.attributes.SwitchBindingsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class HwvtepLogicalRouterUpdateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(HwvtepLogicalRouterUpdateCommand.class);

    private final Map<UUID, LogicalRouter> updatedLRRows;

    public HwvtepLogicalRouterUpdateCommand(HwvtepConnectionInstance key, TableUpdates updates,
            DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedLRRows = TyperUtils.extractRowsUpdated(LogicalRouter.class, getUpdates(), getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        for (Entry<UUID, LogicalRouter> entry : updatedLRRows.entrySet()) {
            LOG.trace("Updating logical router {} with {}", entry.getKey(), entry.getValue());
            updateLogicalRouter(transaction, entry.getValue());
        }
    }

    private void updateLogicalRouter(ReadWriteTransaction transaction, final LogicalRouter router) {
        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        Optional<Node> connection = HwvtepSouthboundUtil.readNode(transaction, connectionIId);
        if (connection.isPresent()) {
            Node connectionNode = buildConnectionNode(router);
            transaction.merge(LogicalDatastoreType.OPERATIONAL, connectionIId, connectionNode);
            InstanceIdentifier<LogicalRouters> routerIid = getOvsdbConnectionInstance().getInstanceIdentifier()
                    .augmentation(HwvtepGlobalAugmentation.class)
                    .child(LogicalRouters.class, new LogicalRoutersKey(new HwvtepNodeName(router.getName())));
            getOvsdbConnectionInstance().getDeviceInfo().updateDeviceOperData(LogicalRouters.class, routerIid,
                    router.getUuid(), router);
        }
    }

    private Node buildConnectionNode(final LogicalRouter router) {
        //Update node with LogicalRouter reference
        NodeBuilder connectionNode = new NodeBuilder();
        connectionNode.setNodeId(getOvsdbConnectionInstance().getNodeId());
        LogicalRoutersBuilder lrBuilder = new LogicalRoutersBuilder();
        lrBuilder.setLogicalRouterUuid(new Uuid(router.getUuid().toString()));
        lrBuilder.setHwvtepNodeDescription(router.getDescription());
        HwvtepNodeName hwvtepName = new HwvtepNodeName(router.getName());
        lrBuilder.setHwvtepNodeName(hwvtepName);
        lrBuilder.withKey(new LogicalRoutersKey(hwvtepName));

        setSwitchBindings(router, lrBuilder);
        setStaticRoutes(router, lrBuilder);
        setAclBindings(router, lrBuilder);


        connectionNode.addAugmentation(new HwvtepGlobalAugmentationBuilder()
            .setLogicalRouters(BindingMap.of(lrBuilder.build()))
            .build());
        return connectionNode.build();
    }

    private static void setStaticRoutes(final LogicalRouter router, final LogicalRoutersBuilder lrBuilder) {
        if (isRouterHasStaticRoutes(router)) {
            List<StaticRoutes> routes = new ArrayList<>();
            for (Entry<String, String> entry : router.getStaticRoutesColumn().getData().entrySet()) {
                StaticRoutesBuilder staticRouteBuilder = new StaticRoutesBuilder();
                staticRouteBuilder.setDestinationAddress(new IpPrefix(new Ipv4Prefix(entry.getKey())));
                staticRouteBuilder.setNexthopAddress(new IpAddress(new Ipv4Address(entry.getKey())));
                routes.add(staticRouteBuilder.build());
            }
            lrBuilder.setStaticRoutes(routes);
        }
    }

    private static boolean isRouterHasStaticRoutes(final LogicalRouter router) {
        return router != null && router.getStaticRoutesColumn() != null
                && router.getStaticRoutesColumn().getData() != null
                && !router.getStaticRoutesColumn().getData().isEmpty();
    }

    private void setAclBindings(final LogicalRouter router, final LogicalRoutersBuilder builder) {
        if (isRouterHasAcls(router)) {
            var bindings = BindingMap.<AclBindingsKey, AclBindings>orderedBuilder();
            for (Entry<String, UUID> entry : router.getAclBindingColumn().getData().entrySet()) {
                AclBindingsBuilder aclBindingBuiler = new AclBindingsBuilder();
                UUID aclUUID = entry.getValue();
                ACL acl = (ACL)getOvsdbConnectionInstance().getDeviceInfo().getDeviceOperData(Acls.class, aclUUID);
                if (acl != null) {
                    InstanceIdentifier<Acls> aclIid =
                            HwvtepSouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(), acl);
                    aclBindingBuiler.setAclRef(new HwvtepAclRef(aclIid));
                    aclBindingBuiler.setRouterInterface(new IpPrefix(new Ipv4Prefix(entry.getKey())));
                    bindings.add(aclBindingBuiler.build());
                }
                builder.setAclBindings(bindings.build());
            }
        }
    }

    private static boolean isRouterHasAcls(final LogicalRouter router) {
        return router != null && router.getAclBindingColumn() != null
                && router.getAclBindingColumn().getData() != null
                && !router.getAclBindingColumn().getData().isEmpty();
    }

    private void setSwitchBindings(final LogicalRouter router, final LogicalRoutersBuilder builder) {
        if (isRouterHasSwitchBindings(router)) {
            List<SwitchBindings> bindings = new ArrayList<>();
            for (Entry<String, UUID> entry : router.getSwitchBindingColumn().getData().entrySet()) {
                SwitchBindingsBuilder switchBindingBuiler = new SwitchBindingsBuilder();
                UUID lsUUID = entry.getValue();
                LogicalSwitch logicalSwitch = getOvsdbConnectionInstance().getDeviceInfo().getLogicalSwitch(lsUUID);
                if (logicalSwitch != null) {
                    switchBindingBuiler.setDestinationAddress(new IpPrefix(new Ipv4Prefix(entry.getKey())));
                    InstanceIdentifier<LogicalSwitches> switchIid =
                        HwvtepSouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(), logicalSwitch);
                    switchBindingBuiler.setLogicalSwitchRef(new HwvtepLogicalSwitchRef(switchIid));
                    bindings.add(switchBindingBuiler.build());
                }
            }
            builder.setSwitchBindings(bindings);
        }
    }

    private static boolean isRouterHasSwitchBindings(final LogicalRouter router) {
        return router != null && router.getSwitchBindingColumn() != null
                && router.getSwitchBindingColumn().getData() != null
                && !router.getSwitchBindingColumn().getData().isEmpty();
    }
}
