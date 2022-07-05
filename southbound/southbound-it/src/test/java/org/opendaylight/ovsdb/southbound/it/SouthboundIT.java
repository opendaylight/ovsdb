/*
 * Copyright © 2015, 2017 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.propagateSystemProperties;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.inject.Inject;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.internal.AssumptionViolatedException;
import org.junit.runner.RunWith;
import org.opendaylight.controller.mdsal.it.base.AbstractMdsalTestBase;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.ovsdb.utils.southbound.utils.SouthboundUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbFailModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbPortInterfaceAttributes.VlanMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbQueueRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.QosTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeExternalIdsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.Autoattach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.AutoattachBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.AutoattachKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.DatapathTypeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.DatapathTypeEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.InterfaceTypeEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntriesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.Queues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QueuesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QueuesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.autoattach.AutoattachExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.autoattach.AutoattachExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.autoattach.AutoattachExternalIdsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.autoattach.Mappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.autoattach.MappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.autoattach.MappingsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosExternalIdsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosOtherConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosOtherConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosOtherConfigKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QueueList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QueueListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QueueListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesExternalIdsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesOtherConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesOtherConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesOtherConfigKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIdsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceLldp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceLldpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceLldpKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceOtherConfigsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortExternalIdsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortOtherConfigsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Trunks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.TrunksBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration tests for southbound-impl.
 *
 * @author Sam Hague (shague@redhat.com)
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SouthboundIT extends AbstractMdsalTestBase {
    private static final String NETDEV_DP_TYPE = "netdev";
    private static final Logger LOG = LoggerFactory.getLogger(SouthboundIT.class);
    private static final int OVSDB_UPDATE_TIMEOUT = 1000;
    private static final int OVSDB_ROUNDTRIP_TIMEOUT = 10000;
    private static final String FORMAT_STR = "%s_%s_%d";
    private static final Version AUTOATTACH_FROM_VERSION = Version.fromString("7.11.2");
    private static final Version IF_INDEX_FROM_VERSION = Version.fromString("7.2.1");
    private static final Uint32 MAX_BACKOFF = Uint32.valueOf(10000);
    private static final Uint32 INACTIVITY_PROBE = Uint32.valueOf(30000);
    private static String addressStr;
    private static Uint16 portNumber;
    private static String connectionType;
    private static boolean setup = false;
    private static MdsalUtils mdsalUtils = null;
    private static Node ovsdbNode;
    private static int testMethodsRemaining;
    private static Version schemaVersion;
    @Inject @Filter(timeout = 60000)
    private static DataBroker dataBroker = null;

    @Inject
    private BundleContext bundleContext;

    private static final NotifyingDataChangeListener CONFIGURATION_LISTENER =
            new NotifyingDataChangeListener(LogicalDatastoreType.CONFIGURATION);
    private static final NotifyingDataChangeListener OPERATIONAL_LISTENER =
            new NotifyingDataChangeListener(LogicalDatastoreType.OPERATIONAL);


    private static final class NotifyingDataChangeListener implements DataTreeChangeListener<DataObject> {
        private static final int RETRY_WAIT = 100;

        private final LogicalDatastoreType type;
        private final Set<InstanceIdentifier<?>> createdIids = new HashSet<>();
        private final Set<InstanceIdentifier<?>> removedIids = new HashSet<>();
        private final Set<InstanceIdentifier<?>> updatedIids = new HashSet<>();
        private final InstanceIdentifier<?> iid;

        private NotifyingDataChangeListener(final LogicalDatastoreType type) {
            this.type = type;
            iid = null;
        }

        private NotifyingDataChangeListener(final LogicalDatastoreType type, final InstanceIdentifier<?> iid) {
            this.type = type;
            this.iid = iid;
        }

        @Override
        public void onDataTreeChanged(final Collection<DataTreeModification<DataObject>> changes) {
            for (DataTreeModification<DataObject> change: changes) {
                DataObjectModification<DataObject> rootNode = change.getRootNode();
                final InstanceIdentifier<DataObject> identifier = change.getRootPath().getRootIdentifier();
                switch (rootNode.getModificationType()) {
                    case SUBTREE_MODIFIED:
                    case WRITE:
                        if (rootNode.getDataBefore() == null) {
                            LOG.info("{} DataTreeChanged: created {}", type, identifier);
                            createdIids.add(identifier);

                            final DataObject obj = rootNode.getDataAfter();
                            if (obj instanceof ManagedNodeEntry managedNodeEntry) {
                                LOG.info("{} DataChanged: created managed {}",
                                        managedNodeEntry.getBridgeRef().getValue());
                                createdIids.add(managedNodeEntry.getBridgeRef().getValue());
                            }
                        } else {
                            LOG.info("{} DataTreeChanged: updated {}", type, identifier);
                            updatedIids.add(identifier);
                        }
                        break;
                    case DELETE:
                        LOG.info("{} DataTreeChanged: removed {}", type, identifier);
                        removedIids.add(identifier);
                        break;
                    default:
                        break;
                }
            }

            synchronized (this) {
                notifyAll();
            }
        }

        public boolean isCreated(final InstanceIdentifier<?> path) {
            return createdIids.remove(path);
        }

        public boolean isRemoved(final InstanceIdentifier<?> path) {
            return removedIids.remove(path);
        }

        public boolean isUpdated(final InstanceIdentifier<?> path) {
            return updatedIids.remove(path);
        }

        public void clear() {
            createdIids.clear();
            removedIids.clear();
            updatedIids.clear();
        }

        public void registerDataChangeListener() {
            dataBroker.registerDataTreeChangeListener(DataTreeIdentifier.create(type,
                    (InstanceIdentifier)iid), this);
        }

        public void waitForCreation(final long timeout) throws InterruptedException {
            synchronized (this) {
                long start = System.currentTimeMillis();
                LOG.info("Waiting for {} DataChanged creation on {}", type, iid);
                while (!isCreated(iid) && System.currentTimeMillis() - start < timeout) {
                    wait(RETRY_WAIT);
                }
                LOG.info("Woke up, waited {}ms for creation of {}", System.currentTimeMillis() - start, iid);
            }
        }

        public void waitForDeletion(final long timeout) throws InterruptedException {
            synchronized (this) {
                long start = System.currentTimeMillis();
                LOG.info("Waiting for {} DataChanged deletion on {}", type, iid);
                while (!isRemoved(iid) && System.currentTimeMillis() - start < timeout) {
                    wait(RETRY_WAIT);
                }
                LOG.info("Woke up, waited {}ms for deletion of {}", System.currentTimeMillis() - start, iid);
            }
        }

        public void waitForUpdate(final long timeout) throws InterruptedException {
            synchronized (this) {
                long start = System.currentTimeMillis();
                LOG.info("Waiting for {} DataChanged update on {}", type, iid);
                while (!isUpdated(iid) && System.currentTimeMillis() - start < timeout) {
                    wait(RETRY_WAIT);
                }
                LOG.info("Woke up, waited {}ms for update of {}", System.currentTimeMillis() - start, iid);
            }
        }
    }

    @Override
    @Configuration
    public Option[] config() {
        Option[] options = super.config();
        Option[] propertyOptions = getPropertiesOptions();
        Option[] otherOptions = getOtherOptions();
        Option[] combinedOptions = new Option[options.length + propertyOptions.length + otherOptions.length];
        System.arraycopy(options, 0, combinedOptions, 0, options.length);
        System.arraycopy(propertyOptions, 0, combinedOptions, options.length, propertyOptions.length);
        System.arraycopy(otherOptions, 0, combinedOptions, options.length + propertyOptions.length,
                otherOptions.length);
        return combinedOptions;
    }

    private static Option[] getOtherOptions() {
        return new Option[] {
                vmOption("-javaagent:../jars/org.jacoco.agent.jar=destfile=../../jacoco-it.exec"),
                keepRuntimeFolder()
        };
    }

    @Override
    public String getKarafDistro() {
        return maven()
                .groupId("org.opendaylight.ovsdb")
                .artifactId("southbound-karaf")
                .versionAsInProject()
                .type("zip")
                .getURL();
    }

    @Override
    public MavenUrlReference getFeatureRepo() {
        return maven()
                .groupId("org.opendaylight.ovsdb")
                .artifactId("southbound-features")
                .classifier("features")
                .type("xml")
                .versionAsInProject();
    }

    @Override
    public String getFeatureName() {
        return "odl-ovsdb-southbound-test";
    }

    protected String usage() {
        return """
            Integration Test needs a valid connection configuration as follows :
            active connection : mvn -Dovsdbserver.ipaddress=x.x.x.x -Dovsdbserver.port=yyyy verify
            passive connection : mvn -Dovsdbserver.connection=passive verify
            """;
    }

    @Override
    public Option getLoggingOption() {
        return composite(
                editConfigurationFilePut(SouthboundITConstants.ORG_OPS4J_PAX_LOGGING_CFG,
                        "log4j.logger.org.opendaylight.ovsdb",
                        LogLevelOption.LogLevel.TRACE.name()),
                super.getLoggingOption());
    }

    private static Option[] getPropertiesOptions() {
        Properties props = new Properties(System.getProperties());
        String ipAddressStr = props.getProperty(SouthboundITConstants.SERVER_IPADDRESS,
                SouthboundITConstants.DEFAULT_SERVER_IPADDRESS);
        String portStr = props.getProperty(SouthboundITConstants.SERVER_PORT,
                SouthboundITConstants.DEFAULT_SERVER_PORT);
        String connectionTypeStr = props.getProperty(SouthboundITConstants.CONNECTION_TYPE,
                SouthboundITConstants.CONNECTION_TYPE_ACTIVE);

        LOG.info("getPropertiesOptions: Using the following properties: mode= {}, ip:port= {}:{}",
                connectionTypeStr, ipAddressStr, portStr);

        return new Option[] {
                propagateSystemProperties(
                        SouthboundITConstants.SERVER_IPADDRESS,
                        SouthboundITConstants.SERVER_PORT,
                        SouthboundITConstants.CONNECTION_TYPE),
                editConfigurationFilePut(SouthboundITConstants.CUSTOM_PROPERTIES,
                        SouthboundITConstants.SERVER_IPADDRESS, ipAddressStr),
                editConfigurationFilePut(SouthboundITConstants.CUSTOM_PROPERTIES,
                        SouthboundITConstants.SERVER_PORT, portStr),
                editConfigurationFilePut(SouthboundITConstants.CUSTOM_PROPERTIES,
                        SouthboundITConstants.CONNECTION_TYPE, connectionTypeStr),
        };
    }

    @Before
    @Override
    public void setup() throws Exception {
        if (setup) {
            LOG.info("Skipping setup, already initialized");
            return;
        }

        super.setup();
        assertNotNull("db should not be null", dataBroker);

        LOG.info("sleeping for 10s to let the features finish installing");
        Thread.sleep(10000);

        addressStr = bundleContext.getProperty(SouthboundITConstants.SERVER_IPADDRESS);
        String portStr = bundleContext.getProperty(SouthboundITConstants.SERVER_PORT);
        try {
            portNumber = Uint16.valueOf(portStr);
        } catch (IllegalArgumentException e) {
            fail("Invalid port number " + portStr + System.lineSeparator() + usage());
        }
        connectionType = bundleContext.getProperty(SouthboundITConstants.CONNECTION_TYPE);

        LOG.info("setUp: Using the following properties: mode= {}, ip:port= {}:{}",
                connectionType, addressStr, portNumber);
        if (connectionType.equalsIgnoreCase(SouthboundITConstants.CONNECTION_TYPE_ACTIVE)) {
            if (addressStr == null) {
                fail(usage());
            }
        }

        mdsalUtils = new MdsalUtils(dataBroker);
        assertTrue("Did not find " + SouthboundUtils.OVSDB_TOPOLOGY_ID.getValue(), getOvsdbTopology());
        final ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        final InstanceIdentifier<Node> iid = SouthboundUtils.createInstanceIdentifier(connectionInfo);
        dataBroker.registerDataTreeChangeListener(DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION,
                (InstanceIdentifier)iid), CONFIGURATION_LISTENER);
        dataBroker.registerDataTreeChangeListener(DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL,
                (InstanceIdentifier)iid), OPERATIONAL_LISTENER);

        ovsdbNode = connectOvsdbNode(connectionInfo);
        OvsdbNodeAugmentation ovsdbNodeAugmentation = ovsdbNode.augmentation(OvsdbNodeAugmentation.class);
        assertNotNull("The OvsdbNodeAugmentation cannot be null", ovsdbNodeAugmentation);
        schemaVersion = Version.fromString(ovsdbNodeAugmentation.getDbVersion());
        LOG.info("schemaVersion = {}", schemaVersion);

        // Let's count the test methods (we need to use this instead of @AfterClass on teardown() since the latter is
        // useless with pax-exam)
        for (Method method : getClass().getMethods()) {
            boolean testMethod = false;
            boolean ignoreMethod = false;
            for (Annotation annotation : method.getAnnotations()) {
                if (Test.class.equals(annotation.annotationType())) {
                    testMethod = true;
                }
                if (Ignore.class.equals(annotation.annotationType())) {
                    ignoreMethod = true;
                }
            }
            if (testMethod && !ignoreMethod) {
                testMethodsRemaining++;
            }
        }
        LOG.info("{} test methods to run", testMethodsRemaining);

        setup = true;
    }

    @After
    public void teardown() {
        testMethodsRemaining--;
        LOG.info("{} test methods remaining", testMethodsRemaining);
        if (testMethodsRemaining == 0) {
            try {
                disconnectOvsdbNode(getConnectionInfo(addressStr, portNumber));
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while disconnecting", e);
            }
        }
    }

    private static Boolean getOvsdbTopology() {
        LOG.info("getOvsdbTopology: looking for {}...", SouthboundUtils.OVSDB_TOPOLOGY_ID.getValue());
        Boolean found = false;
        final TopologyId topologyId = SouthboundUtils.OVSDB_TOPOLOGY_ID;
        InstanceIdentifier<Topology> path =
                InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, new TopologyKey(topologyId));
        for (int i = 0; i < 60; i++) {
            Topology topology = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, path);
            if (topology != null) {
                LOG.info("getOvsdbTopology: found {}...", SouthboundUtils.OVSDB_TOPOLOGY_ID.getValue());
                found = true;
                break;
            } else {
                LOG.info("getOvsdbTopology: still looking ({})...", i);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOG.warn("Interrupted while waiting for {}", SouthboundUtils.OVSDB_TOPOLOGY_ID.getValue(), e);
                }
            }
        }
        return found;
    }

    /**
     * Test passive connection mode. The southbound starts in a listening mode waiting for connections on port
     * 6640. This test will wait for incoming connections for {@link SouthboundITConstants#CONNECTION_INIT_TIMEOUT} ms.
     */
    @Test
    public void testPassiveNode() throws InterruptedException {
        if (connectionType.equalsIgnoreCase(SouthboundITConstants.CONNECTION_TYPE_PASSIVE)) {
            //Wait for CONNECTION_INIT_TIMEOUT for the Passive connection to be initiated by the ovsdb-server.
            Thread.sleep(SouthboundITConstants.CONNECTION_INIT_TIMEOUT);
        }
    }

    private static ConnectionInfo getConnectionInfo(final String ipAddressStr, final Uint16 portNum) {
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getByName(ipAddressStr);
        } catch (UnknownHostException e) {
            fail("Could not resolve " + ipAddressStr + ": " + e);
        }

        IpAddress address = SouthboundMapper.createIpAddress(inetAddress);
        PortNumber port = new PortNumber(portNum);

        final ConnectionInfo connectionInfo = new ConnectionInfoBuilder()
                .setRemoteIp(address)
                .setRemotePort(port)
                .build();
        LOG.info("connectionInfo: {}", connectionInfo);
        return connectionInfo;
    }

    @Test
    public void testNetworkTopology() throws InterruptedException {
        NetworkTopology networkTopology = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(NetworkTopology.class));
        assertNotNull("NetworkTopology could not be found in " + LogicalDatastoreType.CONFIGURATION, networkTopology);

        networkTopology = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.create(NetworkTopology.class));
        assertNotNull("NetworkTopology could not be found in " + LogicalDatastoreType.OPERATIONAL, networkTopology);
    }

    @Test
    public void testOvsdbTopology() throws InterruptedException {
        InstanceIdentifier<Topology> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID));

        Topology topology = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, path);
        assertNotNull("Topology could not be found in " + LogicalDatastoreType.CONFIGURATION, topology);

        topology = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, path);

        assertNotNull("Topology could not be found in " + LogicalDatastoreType.OPERATIONAL, topology);
    }

    private static Node connectOvsdbNode(final ConnectionInfo connectionInfo) throws InterruptedException {
        final InstanceIdentifier<Node> iid = SouthboundUtils.createInstanceIdentifier(connectionInfo);
        assertTrue(
                mdsalUtils.put(LogicalDatastoreType.CONFIGURATION, iid, SouthboundUtils.createNode(connectionInfo)));
        waitForOperationalCreation(iid);
        Node node = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, iid);
        assertNotNull(node);
        LOG.info("Connected to {}", SouthboundUtils.connectionInfoToString(connectionInfo));
        return node;
    }

    private static void waitForOperationalCreation(final InstanceIdentifier<Node> iid) throws InterruptedException {
        synchronized (OPERATIONAL_LISTENER) {
            long start = System.currentTimeMillis();
            LOG.info("Waiting for OPERATIONAL DataChanged creation on {}", iid);
            while (!OPERATIONAL_LISTENER.isCreated(
                    iid) && System.currentTimeMillis() - start < OVSDB_ROUNDTRIP_TIMEOUT) {
                OPERATIONAL_LISTENER.wait(OVSDB_UPDATE_TIMEOUT);
            }
            LOG.info("Woke up, waited {} for creation of {}", System.currentTimeMillis() - start, iid);
        }
    }

    private static void waitForOperationalDeletion(final InstanceIdentifier<Node> iid) throws InterruptedException {
        synchronized (OPERATIONAL_LISTENER) {
            long start = System.currentTimeMillis();
            LOG.info("Waiting for OPERATIONAL DataChanged deletion on {}", iid);
            while (!OPERATIONAL_LISTENER.isRemoved(
                    iid) && System.currentTimeMillis() - start < OVSDB_ROUNDTRIP_TIMEOUT) {
                OPERATIONAL_LISTENER.wait(OVSDB_UPDATE_TIMEOUT);
            }
            LOG.info("Woke up, waited {} for deletion of {}", System.currentTimeMillis() - start, iid);
        }
    }

    private static void waitForOperationalUpdate(final InstanceIdentifier<Node> iid) throws InterruptedException {
        synchronized (OPERATIONAL_LISTENER) {
            long start = System.currentTimeMillis();
            LOG.info("Waiting for OPERATIONAL DataChanged update on {}", iid);
            while (!OPERATIONAL_LISTENER.isUpdated(
                    iid) && System.currentTimeMillis() - start < OVSDB_ROUNDTRIP_TIMEOUT) {
                OPERATIONAL_LISTENER.wait(OVSDB_UPDATE_TIMEOUT);
            }
            LOG.info("Woke up, waited {} for update of {}", System.currentTimeMillis() - start, iid);
        }
    }

    private static void disconnectOvsdbNode(final ConnectionInfo connectionInfo) throws InterruptedException {
        final InstanceIdentifier<Node> iid = SouthboundUtils.createInstanceIdentifier(connectionInfo);
        assertTrue(mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, iid));
        waitForOperationalDeletion(iid);
        Node node = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, iid);
        assertNull(node);
        LOG.info("Disconnected from {}", SouthboundUtils.connectionInfoToString(connectionInfo));
    }

    @Test
    public void testAddDeleteOvsdbNode() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        // At this point we're connected, disconnect and reconnect (the connection will be removed at the very end)
        disconnectOvsdbNode(connectionInfo);
        connectOvsdbNode(connectionInfo);
    }

    @Test
    public void testDpdkSwitch() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        Map<DatapathTypeEntryKey, DatapathTypeEntry> datapathTypeEntries =
                ovsdbNode.augmentation(OvsdbNodeAugmentation.class).nonnullDatapathTypeEntry();
        if (datapathTypeEntries == null) {
            LOG.info("DPDK not supported on this node.");
        } else {
            for (DatapathTypeEntry dpTypeEntry : datapathTypeEntries.values()) {
                DatapathTypeBase dpType = dpTypeEntry.getDatapathType();
                String dpTypeStr = SouthboundMapper.DATAPATH_TYPE_MAP.get(dpType);
                LOG.info("dp type is {}", dpTypeStr);
                if (dpTypeStr.equals(NETDEV_DP_TYPE)) {
                    LOG.info("Found a DPDK node; adding a corresponding netdev device");
                    InstanceIdentifier<Node> bridgeIid = SouthboundUtils.createInstanceIdentifier(connectionInfo,
                            new OvsdbBridgeName(SouthboundITConstants.BRIDGE_NAME));
                    NodeId bridgeNodeId = SouthboundUtils.createManagedNodeId(bridgeIid);
                    try (TestBridge testBridge = new TestBridge(connectionInfo, bridgeIid,
                            SouthboundITConstants.BRIDGE_NAME, bridgeNodeId, false, null, true, dpType, null, null,
                            null)) {
                        // Verify that the device is netdev
                        OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
                        assertNotNull(bridge);
                        assertEquals(dpType, bridge.getDatapathType());

                        // Add port for all dpdk interface types (dpdkvhost not supported in existing dpdk ovs)
                        List<String> dpdkTypes = new ArrayList<>();
                        dpdkTypes.add("dpdk");
                        dpdkTypes.add("dpdkr");
                        dpdkTypes.add("dpdkvhostuser");
                        //dpdkTypes.add("dpdkvhost");

                        for (String dpdkType : dpdkTypes) {
                            String testPortname = "test" + dpdkType + "port";
                            LOG.info("DPDK portname and type is {}, {}", testPortname, dpdkType);
                            OvsdbTerminationPointAugmentationBuilder ovsdbTerminationpointBuilder =
                                    createSpecificDpdkOvsdbTerminationPointAugmentationBuilder(testPortname,
                                            SouthboundMapper.OVSDB_INTERFACE_TYPE_MAP.get(dpdkType));
                            assertTrue(addTerminationPoint(bridgeNodeId, testPortname, ovsdbTerminationpointBuilder));
                        }

                        // Verify that all DPDK ports are created
                        InstanceIdentifier<Node> terminationPointIid = getTpIid(connectionInfo, bridge);
                        Node terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL,
                                terminationPointIid);
                        assertNotNull(terminationPointNode);

                        // Verify that each termination point has the specific DPDK ifType
                        for (String dpdkType : dpdkTypes) {
                            String testPortname = "test" + dpdkType + "port";
                            InterfaceTypeBase dpdkIfType = SouthboundMapper.OVSDB_INTERFACE_TYPE_MAP.get(dpdkType);
                            for (TerminationPoint terminationPoint
                                    : terminationPointNode.nonnullTerminationPoint().values()) {
                                OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation = terminationPoint
                                        .augmentation(OvsdbTerminationPointAugmentation.class);
                                if (ovsdbTerminationPointAugmentation.getName().equals(testPortname)) {
                                    assertEquals(dpdkIfType, ovsdbTerminationPointAugmentation.getInterfaceType());
                                }
                            }
                        }
                    }
                }
                break;
            }
        }
    }

    @Test
    public void testOvsdbNodeOvsVersion() throws InterruptedException {
        OvsdbNodeAugmentation ovsdbNodeAugmentation = ovsdbNode.augmentation(OvsdbNodeAugmentation.class);
        assertNotNull(ovsdbNodeAugmentation);
        assertNotNull(ovsdbNodeAugmentation.getOvsVersion());
    }

    @Test
    public void testOvsdbNodeDbVersion() throws InterruptedException {
        OvsdbNodeAugmentation ovsdbNodeAugmentation = ovsdbNode.augmentation(OvsdbNodeAugmentation.class);
        assertNotNull(ovsdbNodeAugmentation);
        assertNotNull(ovsdbNodeAugmentation.getDbVersion());
    }

    @Test
    public void testOpenVSwitchOtherConfig() throws InterruptedException {
        OvsdbNodeAugmentation ovsdbNodeAugmentation = ovsdbNode.augmentation(OvsdbNodeAugmentation.class);
        assertNotNull(ovsdbNodeAugmentation);
        Map<OpenvswitchOtherConfigsKey, OpenvswitchOtherConfigs> otherConfigsList =
                ovsdbNodeAugmentation.getOpenvswitchOtherConfigs();
        if (otherConfigsList != null) {
            for (OpenvswitchOtherConfigs otherConfig : otherConfigsList.values()) {
                if (otherConfig.getOtherConfigKey().equals("local_ip")) {
                    LOG.info("local_ip: {}", otherConfig.getOtherConfigValue());
                    break;
                } else {
                    LOG.info("other_config {}:{}", otherConfig.getOtherConfigKey(), otherConfig.getOtherConfigValue());
                }
            }
        } else {
            LOG.info("other_config is not present");
        }
    }

    @Test
    public void testOvsdbBridgeControllerInfo() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr,portNumber);
        String controllerTarget = SouthboundUtil.getControllerTarget(ovsdbNode);
        assertNotNull("Failed to get controller target", controllerTarget);
        ControllerEntry setControllerEntry = createControllerEntry(controllerTarget);
        Uri setUri = new Uri(controllerTarget);
        try (TestBridge testBridge = new TestBridge(connectionInfo, null, SouthboundITConstants.BRIDGE_NAME,null, true,
                SouthboundConstants.OVSDB_FAIL_MODE_MAP.inverse().get("secure"), true, null, null,
                BindingMap.of(setControllerEntry), null)) {
            OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
            assertNotNull("bridge was not found: " + SouthboundITConstants.BRIDGE_NAME,  bridge);
            assertNotNull("ControllerEntry was not found: " + setControllerEntry, bridge.getControllerEntry());
            for (ControllerEntry entry : bridge.getControllerEntry().values()) {
                if (entry.getTarget() != null) {
                    assertEquals(setUri.toString(), entry.getTarget().toString());
                }
                if (entry.getMaxBackoff() != null) {
                    assertEquals(MAX_BACKOFF, entry.getMaxBackoff());
                }
                if (entry.getInactivityProbe() != null) {
                    assertEquals(INACTIVITY_PROBE, entry.getInactivityProbe());
                }
            }
        }
    }

    private static @NonNull ControllerEntry createControllerEntry(final String controllerTarget) {
        return new ControllerEntryBuilder()
                .setTarget(new Uri(controllerTarget))
                .setMaxBackoff(MAX_BACKOFF)
                .setInactivityProbe(INACTIVITY_PROBE)
                .build();
    }

    private static void setManagedBy(final OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder,
                              final ConnectionInfo connectionInfo) {
        InstanceIdentifier<Node> connectionNodePath = SouthboundUtils.createInstanceIdentifier(connectionInfo);
        ovsdbBridgeAugmentationBuilder.setManagedBy(new OvsdbNodeRef(connectionNodePath));
    }

    private static Map<ProtocolEntryKey, ProtocolEntry> createMdsalProtocols() {
        ImmutableBiMap<String, OvsdbBridgeProtocolBase> mapper = SouthboundConstants.OVSDB_PROTOCOL_MAP.inverse();
        return BindingMap.of(new ProtocolEntryBuilder().setProtocol(mapper.get("OpenFlow13")).build());
    }

    private static OvsdbTerminationPointAugmentationBuilder createGenericOvsdbTerminationPointAugmentationBuilder() {
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointAugmentationBuilder =
                new OvsdbTerminationPointAugmentationBuilder();
        ovsdbTerminationPointAugmentationBuilder.setInterfaceType(
                new InterfaceTypeEntryBuilder()
                        .setInterfaceType(
                                SouthboundMapper.createInterfaceType("internal"))
                        .build().getInterfaceType());
        return ovsdbTerminationPointAugmentationBuilder;
    }

    private static OvsdbTerminationPointAugmentationBuilder createGenericDpdkOvsdbTerminationPointAugmentationBuilder(
            final String portName) {
        return createGenericOvsdbTerminationPointAugmentationBuilder()
            .setName(portName)
            .setInterfaceType(SouthboundMapper.OVSDB_INTERFACE_TYPE_MAP.get("dpdk"));
    }

    private static OvsdbTerminationPointAugmentationBuilder createSpecificDpdkOvsdbTerminationPointAugmentationBuilder(
            final String testPortname, final InterfaceTypeBase dpdkIfType) {
        return createGenericOvsdbTerminationPointAugmentationBuilder()
            .setName(testPortname)
            .setInterfaceType(dpdkIfType);
    }

    private static boolean addTerminationPoint(final NodeId bridgeNodeId, final String portName,
                                               final OvsdbTerminationPointAugmentationBuilder
                                                   ovsdbTerminationPointAugmentationBuilder)
            throws InterruptedException {

        InstanceIdentifier<Node> portIid = SouthboundMapper.createInstanceIdentifier(bridgeNodeId);
        NodeBuilder portNodeBuilder = new NodeBuilder();
        NodeId portNodeId = SouthboundMapper.createManagedNodeId(portIid);
        portNodeBuilder.setNodeId(portNodeId);
        TerminationPointBuilder entry = new TerminationPointBuilder()
                .withKey(new TerminationPointKey(new TpId(portName)))
                .addAugmentation(ovsdbTerminationPointAugmentationBuilder.build());
        portNodeBuilder.setTerminationPoint(BindingMap.of(entry.build()));
        boolean result = mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION, portIid, portNodeBuilder.build());
        Thread.sleep(OVSDB_UPDATE_TIMEOUT);
        return result;
    }

    private static class TestBridge implements AutoCloseable {
        private final ConnectionInfo connectionInfo;
        private final String bridgeName;

        /**
         * Creates a test bridge which can be automatically removed when no longer necessary.
         *
         * @param connectionInfo The connection information.
         * @param bridgeIid The bridge identifier; if {@code null}, one is created based on {@code bridgeName}.
         * @param bridgeName The bridge name; must be provided.
         * @param bridgeNodeId The bridge node identifier; if {@code null}, one is created based on {@code bridgeIid}.
         * @param setProtocolEntries {@code true} to set default protocol entries for the bridge.
         * @param failMode The fail mode to set for the bridge.
         * @param setManagedBy {@code true} to specify {@code setManagedBy} for the bridge.
         * @param dpType The datapath type.
         * @param externalIds The external identifiers if any.
         * @param otherConfigs The other configuration items if any.
         */
        TestBridge(final ConnectionInfo connectionInfo, @Nullable InstanceIdentifier<Node> bridgeIid,
                                  final String bridgeName, NodeId bridgeNodeId, final boolean setProtocolEntries,
                                  final OvsdbFailModeBase failMode, final boolean setManagedBy,
                                  @Nullable final DatapathTypeBase dpType,
                                  @Nullable final Map<BridgeExternalIdsKey, BridgeExternalIds> externalIds,
                                  @Nullable final Map<ControllerEntryKey, ControllerEntry> controllerEntries,
                                  @Nullable final Map<BridgeOtherConfigsKey, BridgeOtherConfigs> otherConfigs) {
            this.connectionInfo = connectionInfo;
            this.bridgeName = bridgeName;
            NodeBuilder bridgeNodeBuilder = new NodeBuilder();
            if (bridgeIid == null) {
                bridgeIid = SouthboundUtils.createInstanceIdentifier(connectionInfo, new OvsdbBridgeName(bridgeName));
            }
            if (bridgeNodeId == null) {
                bridgeNodeId = SouthboundMapper.createManagedNodeId(bridgeIid);
            }
            bridgeNodeBuilder.setNodeId(bridgeNodeId);
            OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
            ovsdbBridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName(bridgeName));
            if (setProtocolEntries) {
                ovsdbBridgeAugmentationBuilder.setProtocolEntry(createMdsalProtocols());
            }
            ovsdbBridgeAugmentationBuilder.setFailMode(failMode);
            if (setManagedBy) {
                setManagedBy(ovsdbBridgeAugmentationBuilder, connectionInfo);
            }
            ovsdbBridgeAugmentationBuilder.setDatapathType(dpType);
            ovsdbBridgeAugmentationBuilder.setBridgeExternalIds(externalIds);
            ovsdbBridgeAugmentationBuilder.setControllerEntry(controllerEntries);
            ovsdbBridgeAugmentationBuilder.setBridgeOtherConfigs(otherConfigs);
            bridgeNodeBuilder.addAugmentation(ovsdbBridgeAugmentationBuilder.build());
            LOG.debug("Built with the intent to store bridge data {}", ovsdbBridgeAugmentationBuilder);
            assertTrue(mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION, bridgeIid, bridgeNodeBuilder.build()));
            try {
                Thread.sleep(OVSDB_UPDATE_TIMEOUT);
            } catch (InterruptedException e) {
                LOG.warn("Sleep interrupted while waiting for bridge creation (bridge {})", bridgeName, e);
            }
        }

        TestBridge(final ConnectionInfo connectionInfo, final String bridgeName) {
            this(connectionInfo, null, bridgeName, null, true,
                    SouthboundConstants.OVSDB_FAIL_MODE_MAP.inverse().get("secure"), true, null, null, null, null);
        }

        @Override
        public void close() {
            final InstanceIdentifier<Node> iid =
                    SouthboundUtils.createInstanceIdentifier(connectionInfo, new OvsdbBridgeName(bridgeName));
            assertTrue(mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, iid));
            try {
                Thread.sleep(OVSDB_UPDATE_TIMEOUT);
            } catch (InterruptedException e) {
                LOG.warn("Sleep interrupted while waiting for bridge deletion (bridge {})", bridgeName, e);
            }
        }
    }

    private static class TestAutoAttach implements AutoCloseable {
        private final ConnectionInfo connectionInfo;
        private final Uri autoattachId;
        private final Uri bridgeId;

        TestAutoAttach(final ConnectionInfo connectionInfo,
                final Uri autoattachId,
                final Uri bridgeId,
                @Nullable final String systemName,
                @Nullable final String systemDescription,
                @Nullable final Map<MappingsKey, Mappings> mappings,
                @Nullable final Map<AutoattachExternalIdsKey, AutoattachExternalIds> externalIds) {
            this.connectionInfo = connectionInfo;
            this.autoattachId = autoattachId;
            this.bridgeId = bridgeId;

            Autoattach aaEntry = new AutoattachBuilder()
                    .setAutoattachId(autoattachId)
                    .setBridgeId(bridgeId)
                    .setSystemName(systemName)
                    .setSystemDescription(systemDescription)
                    .setMappings(mappings)
                    .setAutoattachExternalIds(externalIds)
                    .build();
            InstanceIdentifier<Autoattach> iid = SouthboundUtils.createInstanceIdentifier(connectionInfo)
                    .augmentation(OvsdbNodeAugmentation.class)
                    .child(Autoattach.class, aaEntry.key());
            final NotifyingDataChangeListener aaOperationalListener =
                    new NotifyingDataChangeListener(LogicalDatastoreType.OPERATIONAL, iid);
            aaOperationalListener.registerDataChangeListener();

            assertTrue(mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION, iid, aaEntry));
            try {
                aaOperationalListener.waitForCreation(OVSDB_ROUNDTRIP_TIMEOUT);
            } catch (InterruptedException e) {
                LOG.warn("Sleep interrupted while waiting for queue {}", iid, e);
            }
        }

        @Override
        public void close() {
            final InstanceIdentifier<Autoattach> iid = SouthboundUtils.createInstanceIdentifier(connectionInfo)
                    .augmentation(OvsdbNodeAugmentation.class)
                    .child(Autoattach.class, new AutoattachKey(autoattachId));
            final NotifyingDataChangeListener aaOperationalListener =
                    new NotifyingDataChangeListener(LogicalDatastoreType.OPERATIONAL, iid);
            aaOperationalListener.registerDataChangeListener();

            assertTrue(mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, iid));
            try {
                aaOperationalListener.waitForDeletion(OVSDB_ROUNDTRIP_TIMEOUT);
            } catch (InterruptedException e) {
                LOG.warn("Sleep interrupted while waiting for qos deletion (qos {})", iid, e);
            }
        }
    }

    @Test
    public void testCRUDAutoAttach() throws InterruptedException {
        final boolean isOldSchema = schemaVersion.compareTo(AUTOATTACH_FROM_VERSION) < 0;

        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        String testAutoattachId = "testAutoattachEntry";
        String testSystemName = "testSystemName";
        String testSystemDescription = "testSystemDescription";
        String testAutoattachExternalKey = "testAutoattachExternalKey";
        String testAutoattachExternalValue = "testAutoattachExternalValue";

        try (TestBridge testBridge = new TestBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME)) {
            OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
            assertNotNull(bridge);

            // CREATE: Create Autoattach table
            NodeId nodeId = SouthboundUtils.createManagedNodeId(SouthboundUtils.createInstanceIdentifier(
                    connectionInfo, bridge.getBridgeName()));
            String bridgeId = nodeId.getValue();
            try (TestAutoAttach testAutoattach = new TestAutoAttach(connectionInfo, new Uri(testAutoattachId),
                    new Uri(bridgeId), testSystemName, testSystemDescription, null, null)) {
                // READ: Read md-sal operational datastore to see if the AutoAttach table was created
                // and if Bridge table was updated with AutoAttach Uuid
                OvsdbNodeAugmentation ovsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                        LogicalDatastoreType.OPERATIONAL);
                Autoattach operAa = getAutoAttach(ovsdbNodeAugmentation, new Uri(testAutoattachId));

                // skip tests after verifying that Autoattach doesn't break with unsupported schema
                assumeFalse(isOldSchema);

                // FIXME: Remove once CRUD is supported
                assumeFalse(operAa == null);

                assertNotNull(operAa);
                assertEquals(testSystemName, operAa.getSystemName());
                bridge = getBridge(connectionInfo);
                Uuid aaUuid = new Uuid(operAa.getAutoattachUuid().getValue());
                assertEquals(aaUuid, bridge.getAutoAttach());

                // UPDATE: Update mappings column of AutoAttach table that was created
                Map<MappingsKey, Mappings> mappings = BindingMap.of(new MappingsBuilder()
                        .setMappingsKey(Uint32.valueOf(100))
                        .setMappingsValue(Uint16.valueOf(200))
                        .build());
                Autoattach updatedAa = new AutoattachBuilder()
                        .setAutoattachId(new Uri(testAutoattachId))
                        .setMappings(mappings)
                        .build();
                InstanceIdentifier<Autoattach> iid = SouthboundUtils.createInstanceIdentifier(connectionInfo)
                        .augmentation(OvsdbNodeAugmentation.class)
                        .child(Autoattach.class, updatedAa.key());
                final NotifyingDataChangeListener aaOperationalListener =
                        new NotifyingDataChangeListener(LogicalDatastoreType.OPERATIONAL, iid);
                aaOperationalListener.registerDataChangeListener();
                assertTrue(mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION, iid, updatedAa));
                aaOperationalListener.waitForUpdate(OVSDB_UPDATE_TIMEOUT);

                // UPDATE: Update external_ids column of AutoAttach table that was created
                BindingMap.Builder<AutoattachExternalIdsKey, AutoattachExternalIds> externalIds = BindingMap.builder();
                externalIds.add(new AutoattachExternalIdsBuilder()
                        .setAutoattachExternalIdKey(testAutoattachExternalKey)
                        .setAutoattachExternalIdValue(testAutoattachExternalValue)
                        .build());
                updatedAa = new AutoattachBuilder()
                        .setAutoattachId(new Uri(testAutoattachId))
                        .setAutoattachExternalIds(externalIds.build())
                        .build();
                assertTrue(mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION, iid, updatedAa));
                aaOperationalListener.waitForUpdate(OVSDB_UPDATE_TIMEOUT);

                // READ: Read the updated AutoAttach table for latest mappings and external_ids column value
                ovsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                        LogicalDatastoreType.OPERATIONAL);
                operAa = getAutoAttach(ovsdbNodeAugmentation, new Uri(testAutoattachId));
                assertNotNull(operAa);
                Map<MappingsKey, Mappings> operMappingsList = operAa.getMappings();
                for (Mappings operMappings : operMappingsList.values()) {
                    assertTrue(mappings.containsValue(operMappings));
                }
                Map<AutoattachExternalIdsKey, AutoattachExternalIds> operExternalIds =
                        operAa.getAutoattachExternalIds();
                final Collection<AutoattachExternalIds> ids = externalIds.add(new AutoattachExternalIdsBuilder()
                    .setAutoattachExternalIdKey(SouthboundConstants.AUTOATTACH_ID_EXTERNAL_ID_KEY)
                    .setAutoattachExternalIdValue(operAa.getAutoattachId().getValue())
                    .build())
                    .build().values();

                for (AutoattachExternalIds operExternalId : operExternalIds.values()) {
                    assertTrue(ids.contains(operExternalId));
                }

                // DELETE: Delete AutoAttach table
                assertTrue(mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, iid));
                aaOperationalListener.waitForUpdate(OVSDB_UPDATE_TIMEOUT);
                ovsdbNodeAugmentation = getOvsdbNode(connectionInfo, LogicalDatastoreType.OPERATIONAL);
                operAa = getAutoAttach(ovsdbNodeAugmentation, new Uri(testAutoattachId));
                assertNull(operAa);
            } catch (AssumptionViolatedException e) {
                LOG.warn("Skipped test for Autoattach due to unsupported schema", e);
            }
        }
    }

    private static Autoattach getAutoAttach(final OvsdbNodeAugmentation ovsdbNodeAugmentation, final Uri uri) {
        for (Autoattach aa : ovsdbNodeAugmentation.nonnullAutoattach().values()) {
            if (aa.key().getAutoattachId().equals(uri)) {
                return aa;
            }
        }
        return null;
    }

    private static class TestQos implements AutoCloseable {
        private final ConnectionInfo connectionInfo;
        private final Uri qosId;

        /**
         * Creates a test qos entry which can be automatically removed when no longer necessary.
         *
         * @param connectionInfo The connection information.
         * @param qosId The Qos identifier.
         * @param qosType The qos type.
         * @param externalIds The external identifiers if any.
         * @param otherConfigs The other configuration items if any.
         */
        TestQos(final ConnectionInfo connectionInfo, final Uri qosId, final QosTypeBase qosType,
                final @Nullable Map<QosExternalIdsKey, QosExternalIds> externalIds,
                final @Nullable Map<QosOtherConfigKey, QosOtherConfig> otherConfigs) {
            this.connectionInfo = connectionInfo;
            this.qosId = qosId;

            QosEntries qosEntry = new QosEntriesBuilder()
                .setQosId(qosId)
                .setQosType(qosType)
                .setQosExternalIds(externalIds)
                .setQosOtherConfig(otherConfigs)
                .build();
            InstanceIdentifier<QosEntries> qeIid = SouthboundUtils.createInstanceIdentifier(connectionInfo)
                    .augmentation(OvsdbNodeAugmentation.class)
                    .child(QosEntries.class, qosEntry.key());
            final NotifyingDataChangeListener qosOperationalListener =
                    new NotifyingDataChangeListener(LogicalDatastoreType.OPERATIONAL, qeIid);
            qosOperationalListener.registerDataChangeListener();

            assertTrue(mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION, qeIid, qosEntry));

            try {
                qosOperationalListener.waitForCreation(OVSDB_ROUNDTRIP_TIMEOUT);
            } catch (InterruptedException e) {
                LOG.warn("Sleep interrupted while waiting for queue {}", qeIid, e);
            }

        }

        @Override
        public void close() {
            final InstanceIdentifier<QosEntries> qeIid = SouthboundUtils.createInstanceIdentifier(connectionInfo)
                    .augmentation(OvsdbNodeAugmentation.class)
                    .child(QosEntries.class, new QosEntriesKey(qosId));
            final NotifyingDataChangeListener qosOperationalListener =
                    new NotifyingDataChangeListener(LogicalDatastoreType.OPERATIONAL, qeIid);
            qosOperationalListener.registerDataChangeListener();

            assertTrue(mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, qeIid));
            try {
                qosOperationalListener.waitForDeletion(OVSDB_ROUNDTRIP_TIMEOUT);
            } catch (InterruptedException e) {
                LOG.warn("Sleep interrupted while waiting for qos deletion (qos {})", qeIid, e);
            }
        }
    }

    private static class TestQueue implements AutoCloseable {
        private final ConnectionInfo connectionInfo;
        private final Uri queueId;
        private final InstanceIdentifier<Queues> queueIid;

        /**
         * Creates a test queue entry which can be automatically removed when no longer necessary.
         *
         * @param connectionInfo The connection information.
         * @param queueId The Queue identifier.
         * @param queueDscp The queue dscp value.
         * @param externalIds The external identifiers if any.
         * @param otherConfigs The other configuration items if any.
         */
        TestQueue(final ConnectionInfo connectionInfo, final Uri queueId, final Uint8 queueDscp,
                  final @Nullable Map<QueuesExternalIdsKey, QueuesExternalIds> externalIds,
                  final @Nullable Map<QueuesOtherConfigKey, QueuesOtherConfig> otherConfigs) {
            this.connectionInfo = connectionInfo;
            this.queueId = queueId;

            Queues queue = new QueuesBuilder()
                .setQueueId(queueId)
                .setDscp(queueDscp)
                .setQueuesExternalIds(externalIds)
                .setQueuesOtherConfig(otherConfigs)
                .build();
            queueIid = SouthboundUtils.createInstanceIdentifier(connectionInfo)
                    .augmentation(OvsdbNodeAugmentation.class)
                    .child(Queues.class, queue.key());
            final NotifyingDataChangeListener queueOperationalListener =
                    new NotifyingDataChangeListener(LogicalDatastoreType.OPERATIONAL, queueIid);
            queueOperationalListener.registerDataChangeListener();

            assertTrue(mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION, queueIid, queue));

            try {
                queueOperationalListener.waitForCreation(OVSDB_ROUNDTRIP_TIMEOUT);
            } catch (InterruptedException e) {
                LOG.warn("Sleep interrupted while waiting for queue {}", queueId, e);
            }
        }

        public InstanceIdentifier<Queues> getInstanceIdentifier() {
            return queueIid;
        }

        @Override
        public void close() {
            InstanceIdentifier<Queues> queuesIid = SouthboundUtils.createInstanceIdentifier(connectionInfo)
                    .augmentation(OvsdbNodeAugmentation.class)
                    .child(Queues.class, new QueuesKey(queueId));
            final NotifyingDataChangeListener queueOperationalListener =
                    new NotifyingDataChangeListener(LogicalDatastoreType.OPERATIONAL, queuesIid);
            queueOperationalListener.registerDataChangeListener();

            assertTrue(mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, queuesIid));
            try {
                queueOperationalListener.waitForDeletion(OVSDB_ROUNDTRIP_TIMEOUT);
            } catch (InterruptedException e) {
                LOG.warn("Sleep interrupted while waiting for queue deletion (queue {})", queueId, e);
            }
        }
    }

    private static OvsdbNodeAugmentation getOvsdbNode(final ConnectionInfo connectionInfo,
            final LogicalDatastoreType store) {
        InstanceIdentifier<Node> nodeIid = SouthboundUtils.createInstanceIdentifier(connectionInfo);
        Node node = mdsalUtils.read(store, nodeIid);
        assertNotNull(node);
        OvsdbNodeAugmentation ovsdbNodeAugmentation = node.augmentation(OvsdbNodeAugmentation.class);
        assertNotNull(ovsdbNodeAugmentation);
        return ovsdbNodeAugmentation;
    }

    private static OvsdbBridgeAugmentation getBridge(final ConnectionInfo connectionInfo) {
        return getBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME);
    }

    /**
     * Extract the <code>store</code> type data store contents for the particular bridge identified by
     * <code>bridgeName</code>.
     *
     * @param connectionInfo the connection information
     * @param bridgeName the bridge name
     * @param store defined by the <code>LogicalDatastoreType</code> enumeration
     * @return <code>store</code> type data store contents
     */
    private static OvsdbBridgeAugmentation getBridge(final ConnectionInfo connectionInfo, final String bridgeName,
                                                     final LogicalDatastoreType store) {
        Node bridgeNode = getBridgeNode(connectionInfo, bridgeName, store);
        assertNotNull(bridgeNode);
        OvsdbBridgeAugmentation ovsdbBridgeAugmentation = bridgeNode.augmentation(OvsdbBridgeAugmentation.class);
        assertNotNull(ovsdbBridgeAugmentation);
        return ovsdbBridgeAugmentation;
    }

    /**
     * extract the <code>LogicalDataStoreType.OPERATIONAL</code> type data store contents for the particular bridge
     * identified by <code>bridgeName</code>.
     *
     * @param connectionInfo the connection information
     * @param bridgeName the bridge name
     * @see SouthboundIT#getBridge(ConnectionInfo, String, LogicalDatastoreType)
     * @return <code>LogicalDatastoreType.OPERATIONAL</code> type data store contents
     */
    private static OvsdbBridgeAugmentation getBridge(final ConnectionInfo connectionInfo, final String bridgeName) {
        return getBridge(connectionInfo, bridgeName, LogicalDatastoreType.OPERATIONAL);
    }

    /**
     * Extract the node contents from <code>store</code> type data store for the
     * bridge identified by <code>bridgeName</code>.
     *
     * @param connectionInfo the connection information
     * @param bridgeName the bridge name
     * @param store defined by the <code>LogicalDatastoreType</code> enumeration
     * @return <code>store</code> type data store contents
     */
    private static Node getBridgeNode(final ConnectionInfo connectionInfo, final String bridgeName,
            final LogicalDatastoreType store) {
        InstanceIdentifier<Node> bridgeIid =
                SouthboundUtils.createInstanceIdentifier(connectionInfo, new OvsdbBridgeName(bridgeName));
        return mdsalUtils.read(store, bridgeIid);
    }

    /**
     * Extract the node contents from <code>LogicalDataStoreType.OPERATIONAL</code> data store for the
     * bridge identified by <code>bridgeName</code>.
     *
     * @param connectionInfo the connection information
     * @param bridgeName the bridge name
     * @return <code>LogicalDatastoreType.OPERATIONAL</code> type data store contents
     */
    private static Node getBridgeNode(final ConnectionInfo connectionInfo, final String bridgeName) {
        return getBridgeNode(connectionInfo, bridgeName, LogicalDatastoreType.OPERATIONAL);
    }

    @Test
    public void testAddDeleteBridge() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);

        try (TestBridge testBridge = new TestBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME)) {
            OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
            assertNotNull(bridge);
            LOG.info("bridge: {}", bridge);
        }
    }

    private static InstanceIdentifier<Node> getTpIid(final ConnectionInfo connectionInfo,
            final OvsdbBridgeAugmentation bridge) {
        return SouthboundUtils.createInstanceIdentifier(connectionInfo, bridge.getBridgeName());
    }

    /**
     * Extracts the <code>TerminationPointAugmentation</code> for the <code>index</code> <code>TerminationPoint</code>
     * on <code>bridgeName</code>.
     *
     * @param connectionInfo the connection information
     * @param bridgeName the bridge name
     * @param store defined by the <code>LogicalDatastoreType</code> enumeration
     * @param index the index we're interested in
     * @return the augmentation (or {@code null} if none)
     */
    private static OvsdbTerminationPointAugmentation getOvsdbTerminationPointAugmentation(
            final ConnectionInfo connectionInfo, final String bridgeName, final LogicalDatastoreType store,
            final int index) {

        Map<TerminationPointKey, TerminationPoint> tpList = getBridgeNode(connectionInfo, bridgeName, store)
                .getTerminationPoint();
        if (tpList == null) {
            return null;
        }
        return Iterables.get(tpList.values(), index).augmentation(OvsdbTerminationPointAugmentation.class);
    }

    @Test
    public void testCRUDTerminationPointIfIndex() throws InterruptedException {
        final boolean isOldSchema = schemaVersion.compareTo(IF_INDEX_FROM_VERSION) < 0;
        assumeFalse(isOldSchema);
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);

        // Test create ifIndex
        try (TestBridge testBridge = new TestBridge(connectionInfo, null, SouthboundITConstants.BRIDGE_NAME, null, true,
                SouthboundConstants.OVSDB_FAIL_MODE_MAP.inverse().get("secure"),
                true, SouthboundMapper.createDatapathType("netdev"), null, null, null)) {
            OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
            assertNotNull(bridge);
            LOG.info("bridge: {}", bridge);
            NodeId nodeId = SouthboundMapper.createManagedNodeId(SouthboundUtils.createInstanceIdentifier(
                    connectionInfo, bridge.getBridgeName()));
            OvsdbTerminationPointAugmentationBuilder ovsdbTerminationBuilder =
                    createGenericOvsdbTerminationPointAugmentationBuilder();
            String portName = "testIfIndex";
            ovsdbTerminationBuilder.setName(portName);

            assertTrue(addTerminationPoint(nodeId, portName, ovsdbTerminationBuilder));
            InstanceIdentifier<Node> terminationPointIid = getTpIid(connectionInfo, bridge);
            Node terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, terminationPointIid);
            assertNotNull(terminationPointNode);

            // Test read ifIndex
            for (TerminationPoint terminationPoint : terminationPointNode.nonnullTerminationPoint().values()) {
                OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                        terminationPoint.augmentation(OvsdbTerminationPointAugmentation.class);
                if (ovsdbTerminationPointAugmentation.getName().equals(portName)) {
                    Long ifIndex = ovsdbTerminationPointAugmentation.getIfindex().toJava();
                    assertNotNull(ifIndex);
                    LOG.info("ifIndex: {} for the port:{}", ifIndex, portName);
                }
            }
        }
    }

    @Test
    public void testCRDTerminationPointOfPort() throws InterruptedException {
        final Uint32 ofportExpected = Uint32.valueOf(45002);

        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);

        // CREATE
        try (TestBridge testBridge = new TestBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME)) {
            OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
            assertNotNull(bridge);
            LOG.info("bridge: {}", bridge);
            NodeId nodeId = SouthboundMapper.createManagedNodeId(SouthboundUtils.createInstanceIdentifier(
                    connectionInfo, bridge.getBridgeName()));
            OvsdbTerminationPointAugmentationBuilder ovsdbTerminationBuilder =
                    createGenericOvsdbTerminationPointAugmentationBuilder();
            String portName = "testOfPort";
            ovsdbTerminationBuilder.setName(portName);

            ovsdbTerminationBuilder.setOfport(ofportExpected);
            assertTrue(addTerminationPoint(nodeId, portName, ovsdbTerminationBuilder));
            InstanceIdentifier<Node> terminationPointIid = getTpIid(connectionInfo, bridge);
            Node terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, terminationPointIid);
            assertNotNull(terminationPointNode);

            // READ
            for (TerminationPoint terminationPoint : terminationPointNode.nonnullTerminationPoint().values()) {
                OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                        terminationPoint.augmentation(OvsdbTerminationPointAugmentation.class);
                if (ovsdbTerminationPointAugmentation.getName().equals(portName)) {
                    Uint32 ofPort = ovsdbTerminationPointAugmentation.getOfport();
                    // if ephemeral port 45002 is in use, ofPort is set to 1
                    assertTrue(ofPort.equals(ofportExpected) || ofPort.equals(Uint32.ONE));
                    LOG.info("ofPort: {}", ofPort);
                }
            }

            // UPDATE- Not Applicable.  From the OpenVSwitch Documentation:
            //   "A client should ideally set this column’s value in the same database transaction that it uses to
            //   create the interface."

            // DELETE handled by TestBridge
        }
    }

    @Test
    public void testCRDTerminationPointOfPortRequest() throws InterruptedException {
        final Uint32 ofportExpected = Uint32.valueOf(45008);
        final Uint32 ofportInput = Uint32.valueOf(45008);

        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);

        // CREATE
        try (TestBridge testBridge = new TestBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME)) {
            OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
            assertNotNull(bridge);
            final NodeId nodeId = SouthboundUtils.createManagedNodeId(SouthboundUtils.createInstanceIdentifier(
                    connectionInfo, bridge.getBridgeName()));
            OvsdbTerminationPointAugmentationBuilder ovsdbTerminationBuilder =
                    createGenericOvsdbTerminationPointAugmentationBuilder();
            String portName = "testOfPortRequest";
            ovsdbTerminationBuilder.setName(portName);
            Uint16 ofPortRequestExpected = ofportExpected.toUint16();
            ovsdbTerminationBuilder.setOfport(ofportInput);
            ovsdbTerminationBuilder.setOfportRequest(ofPortRequestExpected);
            assertTrue(addTerminationPoint(nodeId, portName, ovsdbTerminationBuilder));
            InstanceIdentifier<Node> terminationPointIid = getTpIid(connectionInfo, bridge);
            Node terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, terminationPointIid);
            assertNotNull(terminationPointNode);

            // READ
            for (TerminationPoint terminationPoint : terminationPointNode.nonnullTerminationPoint().values()) {
                OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                        terminationPoint.augmentation(OvsdbTerminationPointAugmentation.class);
                if (ovsdbTerminationPointAugmentation.getName().equals(portName)) {
                    Uint32 ofPort = ovsdbTerminationPointAugmentation.getOfport();
                    // if ephemeral port 45008 is in use, ofPort is set to 1
                    assertTrue(ofPort.equals(ofportExpected) || ofPort.equals(Uint32.ONE));
                    LOG.info("ofPort: {}", ofPort);

                    Uint16 ofPortRequest = ovsdbTerminationPointAugmentation.getOfportRequest();
                    assertEquals(ofPortRequestExpected, ofPortRequest);
                    LOG.info("ofPortRequest: {}", ofPortRequest);
                }
            }

            // UPDATE- Not Applicable.  From the OpenVSwitch documentation:
            //   "A client should ideally set this column’s value in the same database transaction that it uses to
            //   create the interface. "

            // DELETE handled by TestBridge
        }
    }

    private static <I extends Identifier<T>, T extends Identifiable<I>> void assertExpectedExist(
            final Map<I, T> expected, final Map<I, T> test) {
        if (expected != null && test != null) {
            for (T exp : expected.values()) {
                assertTrue("The retrieved values don't contain " + exp, test.containsValue(exp));
            }
        }
    }

    private interface SouthboundTerminationPointHelper<I extends Identifier<T>, T extends Identifiable<I>> {
        void writeValues(OvsdbTerminationPointAugmentationBuilder builder, Map<I, T> values);

        Map<I, T> readValues(OvsdbTerminationPointAugmentation augmentation);
    }

    /*
     * Tests the CRUD operations for <code>Port</code> <code>external_ids</code>.
     *
     * @see <code>SouthboundIT.generatePortExternalIdsTestCases()</code> for specific test case information
     */
    private static <I extends Identifier<T>, T extends Identifiable<I>> void testCRUDTerminationPoint(
            final KeyValueBuilder<T> builder, final String prefix, final SouthboundTerminationPointHelper<I, T> helper)
            throws InterruptedException {
        final int terminationPointTestIndex = 0;

        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);

        // updateFromTestCases represent the original test case value.  updateToTestCases represent the new value after
        // the update has been performed.
        List<SouthboundTestCase<I, T>> updateFromTestCases = generateKeyValueTestCases(builder, prefix + "From");
        List<SouthboundTestCase<I, T>> updateToTestCases = generateKeyValueTestCases(builder, prefix + "To");

        for (SouthboundTestCase<I, T> updateFromTestCase : updateFromTestCases) {
            for (SouthboundTestCase<I, T> updateToTestCase : updateToTestCases) {
                String testBridgeAndPortName = String.format("%s_%s", prefix, updateToTestCase.name);

                // CREATE: Create the test bridge
                try (TestBridge testBridge = new TestBridge(connectionInfo, null, testBridgeAndPortName, null, true,
                        SouthboundConstants.OVSDB_FAIL_MODE_MAP.inverse().get("secure"), true, null, null, null,
                        null)) {
                    NodeId testBridgeNodeId = SouthboundUtils.createManagedNodeId(
                            SouthboundUtils.createInstanceIdentifier(connectionInfo,
                                    new OvsdbBridgeName(testBridgeAndPortName)));
                    OvsdbTerminationPointAugmentationBuilder tpCreateAugmentationBuilder =
                            createGenericOvsdbTerminationPointAugmentationBuilder();
                    tpCreateAugmentationBuilder.setName(testBridgeAndPortName);
                    helper.writeValues(tpCreateAugmentationBuilder, updateFromTestCase.inputValues);
                    assertTrue(
                            addTerminationPoint(testBridgeNodeId, testBridgeAndPortName, tpCreateAugmentationBuilder));

                    // READ: Read the test port and ensure changes are propagated to the CONFIGURATION data store,
                    // then repeat for OPERATIONAL data store
                    OvsdbTerminationPointAugmentation updateFromConfigurationTerminationPointAugmentation =
                            getOvsdbTerminationPointAugmentation(connectionInfo, testBridgeAndPortName,
                                    LogicalDatastoreType.CONFIGURATION, terminationPointTestIndex);
                    if (updateFromConfigurationTerminationPointAugmentation != null) {
                        Map<I, T> updateFromConfigurationValues =
                                helper.readValues(updateFromConfigurationTerminationPointAugmentation);
                        assertExpectedExist(updateFromTestCase.expectedValues, updateFromConfigurationValues);
                    }
                    OvsdbTerminationPointAugmentation updateFromOperationalTerminationPointAugmentation =
                            getOvsdbTerminationPointAugmentation(connectionInfo, testBridgeAndPortName,
                                    LogicalDatastoreType.OPERATIONAL, terminationPointTestIndex);
                    if (updateFromOperationalTerminationPointAugmentation != null) {
                        Map<I, T> updateFromOperationalValues =
                                helper.readValues(updateFromOperationalTerminationPointAugmentation);
                        assertExpectedExist(updateFromTestCase.expectedValues, updateFromOperationalValues);
                    }

                    // UPDATE:  update the values
                    testBridgeNodeId = getBridgeNode(connectionInfo, testBridgeAndPortName).getNodeId();
                    OvsdbTerminationPointAugmentationBuilder tpUpdateAugmentationBuilder =
                            new OvsdbTerminationPointAugmentationBuilder();
                    helper.writeValues(tpUpdateAugmentationBuilder, updateToTestCase.inputValues);
                    InstanceIdentifier<Node> portIid = SouthboundMapper.createInstanceIdentifier(testBridgeNodeId);
                    NodeBuilder portUpdateNodeBuilder = new NodeBuilder();
                    NodeId portUpdateNodeId = SouthboundUtils.createManagedNodeId(portIid);
                    portUpdateNodeBuilder.setNodeId(portUpdateNodeId);
                    TerminationPointBuilder tpUpdateBuilder = new TerminationPointBuilder();
                    tpUpdateBuilder.withKey(new TerminationPointKey(new TpId(testBridgeAndPortName)));
                    tpUpdateBuilder.addAugmentation(tpUpdateAugmentationBuilder.build());
                    portUpdateNodeBuilder.setTerminationPoint(BindingMap.of(tpUpdateBuilder.build()));
                    assertTrue(mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                            portIid, portUpdateNodeBuilder.build()));
                    Thread.sleep(OVSDB_UPDATE_TIMEOUT);

                    // READ: the test port and ensure changes are propagated to the CONFIGURATION data store,
                    // then repeat for OPERATIONAL data store
                    OvsdbTerminationPointAugmentation updateToConfigurationTerminationPointAugmentation =
                            getOvsdbTerminationPointAugmentation(connectionInfo, testBridgeAndPortName,
                                    LogicalDatastoreType.CONFIGURATION, terminationPointTestIndex);
                    if (updateToConfigurationTerminationPointAugmentation != null) {
                        Map<I, T> updateToConfigurationValues =
                                helper.readValues(updateToConfigurationTerminationPointAugmentation);
                        assertExpectedExist(updateToTestCase.expectedValues, updateToConfigurationValues);
                        assertExpectedExist(updateFromTestCase.expectedValues, updateToConfigurationValues);
                    }
                    OvsdbTerminationPointAugmentation updateToOperationalTerminationPointAugmentation =
                            getOvsdbTerminationPointAugmentation(connectionInfo, testBridgeAndPortName,
                                    LogicalDatastoreType.OPERATIONAL, terminationPointTestIndex);
                    if (updateToOperationalTerminationPointAugmentation != null) {
                        Map<I, T> updateToOperationalValues =
                                helper.readValues(updateToOperationalTerminationPointAugmentation);
                        if (updateFromTestCase.expectedValues != null) {
                            assertExpectedExist(updateToTestCase.expectedValues, updateToOperationalValues);
                            assertExpectedExist(updateFromTestCase.expectedValues, updateToOperationalValues);
                        }
                    }

                    // DELETE handled by TestBridge
                }
            }
        }
    }

    /*
     * Tests the CRUD operations for <code>Port</code> <code>external_ids</code>.
     *
     * @see <code>SouthboundIT.generatePortExternalIdsTestCases()</code> for specific test case information
     */
    @Test
    public void testCRUDTerminationPointPortExternalIds() throws InterruptedException {
        testCRUDTerminationPoint(new SouthboundPortExternalIdsBuilder(), "TPPortExternalIds",
                new PortExternalIdsSouthboundHelper());
    }

    /*
     * Tests the CRUD operations for <code>Interface</code> <code>external_ids</code>.
     *
     * @see <code>SouthboundIT.generateInterfaceExternalIdsTestCases()</code> for specific test case information
     */
    @Test
    public void testCRUDTerminationPointInterfaceExternalIds() throws InterruptedException {
        testCRUDTerminationPoint(new SouthboundInterfaceExternalIdsBuilder(), "TPInterfaceExternalIds",
                new InterfaceExternalIdsSouthboundHelper());
    }

    /*
     * Tests the CRUD operations for <code>Interface</code> <code>lldp</code>.
     *
     * @see <code>SouthboundIT.generateInterfaceLldpTestCases()</code> for specific test case information
     */
    @Test
    public void testCRUDTerminationPointInterfaceLldp() throws InterruptedException {
        testCRUDTerminationPoint(new SouthboundInterfaceLldpBuilder(), "TPInterfaceLldp",
                new InterfaceLldpSouthboundHelper());
    }

    /*
     * Tests the CRUD operations for <code>TerminationPoint</code> <code>options</code>.
     *
     * @see <code>SouthboundIT.generateTerminationPointOptions()</code> for specific test case information
     */
    @Test
    public void testCRUDTerminationPointOptions() throws InterruptedException {
        testCRUDTerminationPoint(new SouthboundOptionsBuilder(), "TPOptions", new OptionsSouthboundHelper());
    }

    /*
     * Tests the CRUD operations for <code>Interface</code> <code>other_configs</code>.
     *
     * @see <code>SouthboundIT.generateInterfaceExternalIdsTestCases()</code> for specific test case information
     */
    @Test
    public void testCRUDTerminationPointInterfaceOtherConfigs() throws InterruptedException {
        testCRUDTerminationPoint(new SouthboundInterfaceOtherConfigsBuilder(), "TPInterfaceOtherConfigs",
                new InterfaceOtherConfigsSouthboundHelper());
    }

    /*
     * Tests the CRUD operations for <code>Port</code> <code>other_configs</code>.
     *
     * @see <code>SouthboundIT.generatePortExternalIdsTestCases()</code> for specific test case information
     */
    @Test
    public void testCRUDTerminationPointPortOtherConfigs() throws InterruptedException {
        testCRUDTerminationPoint(new SouthboundPortOtherConfigsBuilder(), "TPPortOtherConfigs",
                new PortOtherConfigsSouthboundHelper());
    }

    @Test
    public void testCRUDTerminationPoints() throws InterruptedException {
        String port1 = "vx1";
        String port2 = "vxlanport";
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);

        try (TestBridge testBridge = new TestBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME)) {
            OvsdbBridgeAugmentation bridge = getBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME);
            assertNotNull(bridge);
            NodeId nodeId = SouthboundUtils.createManagedNodeId(SouthboundUtils.createInstanceIdentifier(
                    connectionInfo, bridge.getBridgeName()));
            OvsdbTerminationPointAugmentationBuilder ovsdbTerminationBuilder =
                    createGenericOvsdbTerminationPointAugmentationBuilder();

            // add and delete a single port
            String portName = port1;
            ovsdbTerminationBuilder.setName(portName);
            assertTrue(addTerminationPoint(nodeId, portName, ovsdbTerminationBuilder));
            InstanceIdentifier<Node> terminationPointIid = getTpIid(connectionInfo, bridge);
            Node terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, terminationPointIid);
            assertNotNull(terminationPointNode);

            SouthboundUtils.createInstanceIdentifier(connectionInfo,
                    new OvsdbBridgeName(SouthboundITConstants.BRIDGE_NAME));
            portName = port1;
            InstanceIdentifier<TerminationPoint> nodePath =
                    SouthboundUtils.createInstanceIdentifier(connectionInfo,
                            new OvsdbBridgeName(SouthboundITConstants.BRIDGE_NAME))
                            .child(TerminationPoint.class, new TerminationPointKey(new TpId(portName)));

            assertTrue("failed to delete port " + portName,
                    mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, nodePath));
            LOG.info("shague: waiting for delete {}", portName);
            Thread.sleep(1000);
            TerminationPoint terminationPoint = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, nodePath);
            assertNull(terminationPoint);

            // add two ports, then delete them
            portName = port1;
            ovsdbTerminationBuilder.setName(portName);
            assertTrue(addTerminationPoint(nodeId, portName, ovsdbTerminationBuilder));
            terminationPointIid = getTpIid(connectionInfo, bridge);
            terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, terminationPointIid);
            assertNotNull(terminationPointNode);

            portName = port2;
            ovsdbTerminationBuilder.setName(portName);
            assertTrue(addTerminationPoint(nodeId, portName, ovsdbTerminationBuilder));
            terminationPointIid = getTpIid(connectionInfo, bridge);
            terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, terminationPointIid);
            assertNotNull(terminationPointNode);

            SouthboundUtils.createInstanceIdentifier(connectionInfo,
                    new OvsdbBridgeName(SouthboundITConstants.BRIDGE_NAME));
            portName = port1;
            nodePath =
                    SouthboundUtils.createInstanceIdentifier(connectionInfo,
                            new OvsdbBridgeName(SouthboundITConstants.BRIDGE_NAME))
                            .child(TerminationPoint.class, new TerminationPointKey(new TpId(portName)));

            assertTrue("failed to delete port " + portName,
                    mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, nodePath));
            LOG.info("shague: waiting for delete {}", portName);
            Thread.sleep(1000);
            terminationPoint = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, nodePath);
            assertNull(terminationPoint);

            portName = port2;
            nodePath = SouthboundUtils.createInstanceIdentifier(connectionInfo,
                    new OvsdbBridgeName(SouthboundITConstants.BRIDGE_NAME))
                    .child(TerminationPoint.class, new TerminationPointKey(new TpId(portName)));

            assertTrue("failed to delete port " + portName,
                    mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, nodePath));
            LOG.info("shague: waiting for delete {}", portName);
            Thread.sleep(1000);
            terminationPoint = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, nodePath);
            assertNull(terminationPoint);

            // DELETE handled by TestBridge
        }
    }

    @Test
    public void testCRUDTerminationPointVlan() throws InterruptedException {
        final Uint16 createdVlanId = Uint16.valueOf(4000);
        final Uint16 updatedVlanId = Uint16.valueOf(4001);

        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);

        // CREATE
        try (TestBridge testBridge = new TestBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME)) {
            OvsdbBridgeAugmentation bridge = getBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME);
            assertNotNull(bridge);
            NodeId nodeId = SouthboundUtils.createManagedNodeId(SouthboundUtils.createInstanceIdentifier(
                    connectionInfo, bridge.getBridgeName()));
            OvsdbTerminationPointAugmentationBuilder ovsdbTerminationBuilder =
                    createGenericOvsdbTerminationPointAugmentationBuilder();
            String portName = "testTerminationPointVlanId";
            ovsdbTerminationBuilder.setName(portName);
            ovsdbTerminationBuilder.setVlanTag(new VlanId(createdVlanId));
            assertTrue(addTerminationPoint(nodeId, portName, ovsdbTerminationBuilder));
            InstanceIdentifier<Node> terminationPointIid = getTpIid(connectionInfo, bridge);
            Node terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, terminationPointIid);
            assertNotNull(terminationPointNode);

            // READ
            OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation;
            for (TerminationPoint terminationPoint : terminationPointNode.nonnullTerminationPoint().values()) {
                ovsdbTerminationPointAugmentation = terminationPoint.augmentation(
                        OvsdbTerminationPointAugmentation.class);
                if (ovsdbTerminationPointAugmentation.getName().equals(portName)) {
                    VlanId actualVlanId = ovsdbTerminationPointAugmentation.getVlanTag();
                    assertNotNull(actualVlanId);
                    assertEquals(createdVlanId, actualVlanId.getValue());
                }
            }

            // UPDATE
            NodeId testBridgeNodeId = getBridgeNode(connectionInfo, SouthboundITConstants.BRIDGE_NAME).getNodeId();
            OvsdbTerminationPointAugmentationBuilder tpUpdateAugmentationBuilder =
                    new OvsdbTerminationPointAugmentationBuilder();
            tpUpdateAugmentationBuilder.setVlanTag(new VlanId(updatedVlanId));
            InstanceIdentifier<Node> portIid = SouthboundMapper.createInstanceIdentifier(testBridgeNodeId);
            NodeBuilder portUpdateNodeBuilder = new NodeBuilder();
            NodeId portUpdateNodeId = SouthboundUtils.createManagedNodeId(portIid);
            portUpdateNodeBuilder.setNodeId(portUpdateNodeId);
            TerminationPointBuilder tpUpdateBuilder = new TerminationPointBuilder();
            tpUpdateBuilder.withKey(new TerminationPointKey(new TpId(portName)));
            tpUpdateBuilder.addAugmentation(tpUpdateAugmentationBuilder.build());
            tpUpdateBuilder.setTpId(new TpId(portName));
            portUpdateNodeBuilder.setTerminationPoint(BindingMap.of(tpUpdateBuilder.build()));
            assertTrue(
                    mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION, portIid, portUpdateNodeBuilder.build()));
            Thread.sleep(OVSDB_UPDATE_TIMEOUT);

            terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, terminationPointIid);
            for (TerminationPoint terminationPoint :  terminationPointNode.nonnullTerminationPoint().values()) {
                ovsdbTerminationPointAugmentation = terminationPoint.augmentation(
                        OvsdbTerminationPointAugmentation.class);
                if (ovsdbTerminationPointAugmentation.getName().equals(portName)) {
                    VlanId actualVlanId = ovsdbTerminationPointAugmentation.getVlanTag();
                    assertNotNull(actualVlanId);
                    assertEquals(updatedVlanId, actualVlanId.getValue());
                }
            }

            // DELETE handled by TestBridge
        }
    }

    @Test
    public void testCRUDTerminationPointVlanModes() throws InterruptedException {
        final VlanMode updatedVlanMode = VlanMode.Access;
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        VlanMode []vlanModes = VlanMode.values();
        for (VlanMode vlanMode : vlanModes) {
            // CREATE
            try (TestBridge testBridge = new TestBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME)) {
                OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
                assertNotNull(bridge);
                NodeId nodeId = SouthboundUtils.createManagedNodeId(SouthboundUtils.createInstanceIdentifier(
                        connectionInfo, bridge.getBridgeName()));
                OvsdbTerminationPointAugmentationBuilder ovsdbTerminationBuilder =
                        createGenericOvsdbTerminationPointAugmentationBuilder();
                String portName = "testTerminationPointVlanMode" + vlanMode.toString();
                ovsdbTerminationBuilder.setName(portName);
                ovsdbTerminationBuilder.setVlanMode(vlanMode);
                assertTrue(addTerminationPoint(nodeId, portName, ovsdbTerminationBuilder));
                InstanceIdentifier<Node> terminationPointIid = getTpIid(connectionInfo, bridge);
                Node terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, terminationPointIid);
                assertNotNull(terminationPointNode);

                // READ
                for (TerminationPoint terminationPoint : terminationPointNode.nonnullTerminationPoint().values()) {
                    OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                            terminationPoint.augmentation(OvsdbTerminationPointAugmentation.class);
                    if (ovsdbTerminationPointAugmentation.getName().equals(portName)) {
                        //test
                        assertTrue(ovsdbTerminationPointAugmentation.getVlanMode().equals(vlanMode));
                    }
                }

                // UPDATE
                NodeId testBridgeNodeId = getBridgeNode(connectionInfo, SouthboundITConstants.BRIDGE_NAME).getNodeId();
                OvsdbTerminationPointAugmentationBuilder tpUpdateAugmentationBuilder =
                        new OvsdbTerminationPointAugmentationBuilder();
                tpUpdateAugmentationBuilder.setVlanMode(updatedVlanMode);
                InstanceIdentifier<Node> portIid = SouthboundMapper.createInstanceIdentifier(testBridgeNodeId);
                NodeBuilder portUpdateNodeBuilder = new NodeBuilder();
                NodeId portUpdateNodeId = SouthboundUtils.createManagedNodeId(portIid);
                portUpdateNodeBuilder.setNodeId(portUpdateNodeId);
                TerminationPointBuilder tpUpdateBuilder = new TerminationPointBuilder();
                tpUpdateBuilder.withKey(new TerminationPointKey(new TpId(portName)));
                tpUpdateBuilder.addAugmentation(tpUpdateAugmentationBuilder.build());
                tpUpdateBuilder.setTpId(new TpId(portName));
                portUpdateNodeBuilder.setTerminationPoint(BindingMap.of(tpUpdateBuilder.build()));
                assertTrue(
                        mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION, portIid, portUpdateNodeBuilder.build()));
                Thread.sleep(OVSDB_UPDATE_TIMEOUT);

                terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, terminationPointIid);
                for (TerminationPoint terminationPoint : terminationPointNode.nonnullTerminationPoint().values()) {
                    OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                            terminationPoint.augmentation(OvsdbTerminationPointAugmentation.class);
                    if (ovsdbTerminationPointAugmentation.getName().equals(portName)) {
                        //test
                        assertEquals(updatedVlanMode, ovsdbTerminationPointAugmentation.getVlanMode());
                    }
                }

                // DELETE handled by TestBridge
            }
        }
    }

    private static List<Set<Uint16>> generateVlanSets() {
        int min = 0;
        int max = 4095;
        return Lists.newArrayList(
                Collections.<Uint16>emptySet(),
                Collections.singleton(Uint16.valueOf(2222)),
                Sets.newHashSet(Uint16.valueOf(min), Uint16.valueOf(max), Uint16.valueOf(min + 1),
                    Uint16.valueOf(max - 1), Uint16.valueOf((max - min) / 2)));
    }

    private static List<Trunks> buildTrunkList(final Set<Uint16> trunkSet) {
        List<Trunks> trunkList = new ArrayList<>();
        for (Uint16 trunk : trunkSet) {
            trunkList.add(new TrunksBuilder().setTrunk(new VlanId(trunk)).build());
        }
        return trunkList;
    }

    @Test
    public void testCRUDTerminationPointVlanTrunks() throws InterruptedException {
        final List<Trunks> updatedTrunks = buildTrunkList(Collections.singleton(Uint16.valueOf(2011)));
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        Iterable<Set<Uint16>> vlanSets = generateVlanSets();
        int testCase = 0;
        for (Set<Uint16> vlanSet : vlanSets) {
            ++testCase;
            // CREATE
            try (TestBridge testBridge = new TestBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME)) {
                OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
                assertNotNull(bridge);
                NodeId nodeId = SouthboundUtils.createManagedNodeId(connectionInfo, bridge.getBridgeName());
                OvsdbTerminationPointAugmentationBuilder ovsdbTerminationBuilder =
                        createGenericOvsdbTerminationPointAugmentationBuilder();
                String portName = "testTerminationPointVlanTrunks" + testCase;
                ovsdbTerminationBuilder.setName(portName);
                List<Trunks> trunks = buildTrunkList(vlanSet);
                ovsdbTerminationBuilder.setTrunks(trunks);
                assertTrue(addTerminationPoint(nodeId, portName, ovsdbTerminationBuilder));
                InstanceIdentifier<Node> terminationPointIid = getTpIid(connectionInfo, bridge);
                Node terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, terminationPointIid);
                assertNotNull(terminationPointNode);

                // READ
                Collection<TerminationPoint> terminationPoints =
                        terminationPointNode.nonnullTerminationPoint().values();
                for (TerminationPoint terminationPoint : terminationPoints) {
                    OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                            terminationPoint.augmentation(OvsdbTerminationPointAugmentation.class);
                    if (ovsdbTerminationPointAugmentation.getName().equals(portName)) {
                        List<Trunks> actualTrunks = ovsdbTerminationPointAugmentation.getTrunks();
                        for (Trunks trunk : trunks) {
                            assertTrue(actualTrunks.contains(trunk));
                        }
                    }
                }


                // UPDATE
                NodeId testBridgeNodeId = getBridgeNode(connectionInfo, SouthboundITConstants.BRIDGE_NAME).getNodeId();
                OvsdbTerminationPointAugmentationBuilder tpUpdateAugmentationBuilder =
                        new OvsdbTerminationPointAugmentationBuilder();
                tpUpdateAugmentationBuilder.setTrunks(updatedTrunks);
                InstanceIdentifier<Node> portIid = SouthboundMapper.createInstanceIdentifier(testBridgeNodeId);
                NodeBuilder portUpdateNodeBuilder = new NodeBuilder();
                NodeId portUpdateNodeId = SouthboundUtils.createManagedNodeId(portIid);
                portUpdateNodeBuilder.setNodeId(portUpdateNodeId);
                TerminationPointBuilder tpUpdateBuilder = new TerminationPointBuilder();
                tpUpdateBuilder.withKey(new TerminationPointKey(new TpId(portName)));
                tpUpdateBuilder.addAugmentation(tpUpdateAugmentationBuilder.build());
                tpUpdateBuilder.setTpId(new TpId(portName));
                portUpdateNodeBuilder.setTerminationPoint(BindingMap.of(tpUpdateBuilder.build()));
                assertTrue(
                        mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION, portIid, portUpdateNodeBuilder.build()));
                Thread.sleep(OVSDB_UPDATE_TIMEOUT);

                terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, terminationPointIid);
                for (TerminationPoint terminationPoint : terminationPointNode.nonnullTerminationPoint().values()) {
                    OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                            terminationPoint.augmentation(OvsdbTerminationPointAugmentation.class);
                    if (ovsdbTerminationPointAugmentation.getName().equals(portName)) {
                        //test
                        assertEquals(updatedTrunks, ovsdbTerminationPointAugmentation.getTrunks());
                    }
                }

                // DELETE handled by TestBridge
            }
        }
    }

    /*
     * Tests setting and deleting <code>qos</code> field in a <code>port</code>.
     */
    @Test
    public void testCRUDTerminationPointQos() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        String testQosId = "testQosEntry";

        // CREATE
        try (TestBridge testBridge = new TestBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME);
                TestQos testQos = new TestQos(connectionInfo, new Uri(testQosId),
                        SouthboundMapper.createQosType(SouthboundConstants.QOS_LINUX_HFSC), null, null)) {
            OvsdbNodeAugmentation ovsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                    LogicalDatastoreType.OPERATIONAL);
            QosEntries operQos = getQos(new Uri(testQosId), ovsdbNodeAugmentation);
            assertNotNull(operQos);
            OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
            assertNotNull(bridge);
            NodeId nodeId = SouthboundUtils.createManagedNodeId(connectionInfo, bridge.getBridgeName());
            OvsdbTerminationPointAugmentationBuilder ovsdbTerminationBuilder =
                    createGenericOvsdbTerminationPointAugmentationBuilder();
            String portName = "testTerminationPointQos";
            ovsdbTerminationBuilder.setName(portName);
            assertTrue(addTerminationPoint(nodeId, portName, ovsdbTerminationBuilder));


           // READ and check that qos uuid has been added to the port
            InstanceIdentifier<TerminationPoint> tpEntryIid = getTpIid(connectionInfo, bridge)
                    .child(TerminationPoint.class, new TerminationPointKey(new TpId(portName)));
            TerminationPoint terminationPoint = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, tpEntryIid);
            assertNotNull(terminationPoint);

            // UPDATE - remove the qos entry from the port
            OvsdbTerminationPointAugmentationBuilder tpUpdateAugmentationBuilder =
                    new OvsdbTerminationPointAugmentationBuilder();
            tpUpdateAugmentationBuilder.setName(portName);
            TerminationPointBuilder tpUpdateBuilder = new TerminationPointBuilder();
            tpUpdateBuilder.withKey(new TerminationPointKey(new TpId(portName)));
            tpUpdateBuilder.addAugmentation(tpUpdateAugmentationBuilder.build());
            tpUpdateBuilder.setTpId(new TpId(portName));

            assertTrue(
                    mdsalUtils.put(LogicalDatastoreType.CONFIGURATION, tpEntryIid, tpUpdateBuilder.build()));
            Thread.sleep(OVSDB_UPDATE_TIMEOUT);

            // READ and verify that qos uuid has been removed from port
            TerminationPoint terminationPointUpdate = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, tpEntryIid);
            assertNotNull(terminationPointUpdate);

            // DELETE handled by TestBridge
        }
    }

    @Test
    public void testGetOvsdbNodes() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        InstanceIdentifier<Topology> topologyPath = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID));

        Topology topology = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, topologyPath);
        InstanceIdentifier<Node> expectedNodeIid = SouthboundUtils.createInstanceIdentifier(connectionInfo);
        NodeId expectedNodeId = expectedNodeIid.firstKeyOf(Node.class).getNodeId();
        Node foundNode = null;
        assertNotNull("Expected to find topology: " + topologyPath, topology);
        assertNotNull("Expected to find some nodes" + topology.getNode());
        LOG.info("expectedNodeId: {}, getNode: {}", expectedNodeId, topology.getNode());
        for (Node node : topology.nonnullNode().values()) {
            if (node.getNodeId().getValue().equals(expectedNodeId.getValue())) {
                foundNode = node;
                break;
            }
        }
        assertNotNull("Expected to find Node: " + expectedNodeId, foundNode);
    }

    /*
     * @see <code>SouthboundIT.generateBridgeOtherConfigsTestCases()</code> for specific test case information.
     */
    @Test
    public void testCRUDBridgeOtherConfigs() throws InterruptedException {
        testCRUDBridge("BridgeOtherConfigs", new SouthboundBridgeOtherConfigsBuilder(),
                new BridgeOtherConfigsSouthboundHelper());
    }

    private interface SouthboundBridgeHelper<I extends Identifier<T>, T extends Identifiable<I>> {
        void writeValues(OvsdbBridgeAugmentationBuilder builder, Map<I, T> values);

        Map<I, T> readValues(OvsdbBridgeAugmentation augmentation);
    }

    private static <I extends Identifier<T>, T extends Identifiable<I>> void testCRUDBridge(final String prefix,
            final KeyValueBuilder<T> builder, final SouthboundBridgeHelper<I, T> helper) throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        // updateFromTestCases represent the original test case value.  updateToTestCases represent the new value after
        // the update has been performed.
        List<SouthboundTestCase<I, T>> updateFromTestCases = generateKeyValueTestCases(builder, prefix + "From");
        List<SouthboundTestCase<I, T>> updateToTestCases = generateKeyValueTestCases(builder, prefix + "To");
        for (SouthboundTestCase<I, T> updateFromTestCase : updateFromTestCases) {
            for (SouthboundTestCase<I, T> updateToTestCase : updateToTestCases) {
                String testBridgeName = String.format("%s_%s", prefix, updateToTestCase.name);

                // CREATE: Create the test bridge
                final OvsdbBridgeName ovsdbBridgeName = new OvsdbBridgeName(testBridgeName);
                final InstanceIdentifier<Node> bridgeIid =
                        SouthboundUtils.createInstanceIdentifier(connectionInfo, ovsdbBridgeName);
                final NodeId bridgeNodeId = SouthboundMapper.createManagedNodeId(bridgeIid);
                final NodeBuilder bridgeCreateNodeBuilder = new NodeBuilder();
                bridgeCreateNodeBuilder.setNodeId(bridgeNodeId);
                OvsdbBridgeAugmentationBuilder bridgeCreateAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
                bridgeCreateAugmentationBuilder.setBridgeName(ovsdbBridgeName);
                bridgeCreateAugmentationBuilder.setProtocolEntry(createMdsalProtocols());
                bridgeCreateAugmentationBuilder.setFailMode(
                        SouthboundConstants.OVSDB_FAIL_MODE_MAP.inverse().get("secure"));
                setManagedBy(bridgeCreateAugmentationBuilder, connectionInfo);
                helper.writeValues(bridgeCreateAugmentationBuilder, updateFromTestCase.inputValues);
                bridgeCreateNodeBuilder.addAugmentation(bridgeCreateAugmentationBuilder.build());
                LOG.debug("Built with the intent to store bridge data {}", bridgeCreateAugmentationBuilder);
                assertTrue(mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION, bridgeIid,
                        bridgeCreateNodeBuilder.build()));
                Thread.sleep(OVSDB_UPDATE_TIMEOUT);

                // READ: Read the test bridge and ensure changes are propagated to the CONFIGURATION data store,
                // then repeat for OPERATIONAL data store
                Map<I, T> updateFromConfigurationExternalIds = helper.readValues(getBridge(connectionInfo,
                        testBridgeName, LogicalDatastoreType.CONFIGURATION));
                assertExpectedExist(updateFromTestCase.expectedValues, updateFromConfigurationExternalIds);
                Map<I, T> updateFromOperationalExternalIds = helper.readValues(getBridge(connectionInfo,
                        testBridgeName));
                assertExpectedExist(updateFromTestCase.expectedValues, updateFromOperationalExternalIds);

                // UPDATE:  update the values
                final OvsdbBridgeAugmentationBuilder bridgeUpdateAugmentationBuilder =
                        new OvsdbBridgeAugmentationBuilder();
                helper.writeValues(bridgeUpdateAugmentationBuilder, updateToTestCase.inputValues);
                final NodeBuilder bridgeUpdateNodeBuilder = new NodeBuilder();
                final Node bridgeNode = getBridgeNode(connectionInfo, testBridgeName);
                bridgeUpdateNodeBuilder.setNodeId(bridgeNode.getNodeId());
                bridgeUpdateNodeBuilder.withKey(bridgeNode.key());
                bridgeUpdateNodeBuilder.addAugmentation(bridgeUpdateAugmentationBuilder.build());
                assertTrue(mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION, bridgeIid,
                        bridgeUpdateNodeBuilder.build()));
                Thread.sleep(OVSDB_UPDATE_TIMEOUT);

                // READ: the test bridge and ensure changes are propagated to the CONFIGURATION data store,
                // then repeat for OPERATIONAL data store
                Map<I, T> updateToConfigurationExternalIds = helper.readValues(getBridge(connectionInfo, testBridgeName,
                        LogicalDatastoreType.CONFIGURATION));
                assertExpectedExist(updateToTestCase.expectedValues, updateToConfigurationExternalIds);
                assertExpectedExist(updateFromTestCase.expectedValues, updateToConfigurationExternalIds);
                Map<I, T> updateToOperationalExternalIds = helper.readValues(getBridge(connectionInfo, testBridgeName));
                if (updateFromTestCase.expectedValues != null) {
                    assertExpectedExist(updateToTestCase.expectedValues, updateToOperationalExternalIds);
                    assertExpectedExist(updateFromTestCase.expectedValues, updateToOperationalExternalIds);
                }

                // DELETE
                assertTrue(mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, bridgeIid));
                Thread.sleep(OVSDB_UPDATE_TIMEOUT);
            }
        }
    }

    /*
     * @see <code>SouthboundIT.generateBridgeExternalIdsTestCases()</code> for specific test case information
     */
    @Test
    public void testCRUDBridgeExternalIds() throws InterruptedException {
        testCRUDBridge("BridgeExternalIds", new SouthboundBridgeExternalIdsBuilder(),
                new BridgeExternalIdsSouthboundHelper());
    }

    @Test
    public void testAddDeleteQos() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        OvsdbNodeAugmentation ovsdbNodeAugmentation;
        Uri qosUri = new Uri("QOS-ROW");
        List<String> typeList = new ArrayList<>();
        typeList.add(SouthboundConstants.QOS_LINUX_HTB);
        typeList.add(SouthboundConstants.QOS_LINUX_HFSC);

        for (String qosType : typeList) {
            try (TestQos testQos = new TestQos(connectionInfo, qosUri, SouthboundMapper.createQosType(qosType),
                    null, null)) {
                ovsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                        LogicalDatastoreType.OPERATIONAL);
                QosEntries operQosHtb = getQos(qosUri, ovsdbNodeAugmentation);
                assertNotNull(operQosHtb);
            }
            ovsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                    LogicalDatastoreType.OPERATIONAL);
            QosEntries operQosHtb = getQos(qosUri, ovsdbNodeAugmentation);
            assertNull(operQosHtb);
        }
    }

    @Test
    public void testAddDeleteQueue() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        Uri queueUri = new Uri("QUEUE-A1");

        try (TestQueue testQueue = new TestQueue(connectionInfo, queueUri, Uint8.valueOf(25), null, null)) {
            OvsdbNodeAugmentation ovsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                    LogicalDatastoreType.OPERATIONAL);
            Queues operQueue = getQueue(queueUri, ovsdbNodeAugmentation);
            assertNotNull(operQueue);
        }
        OvsdbNodeAugmentation ovsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                LogicalDatastoreType.OPERATIONAL);
        Queues operQueue = getQueue(queueUri, ovsdbNodeAugmentation);
        assertNull(operQueue);
    }

    private static class SouthboundQueuesExternalIdsHelper
            implements SouthboundQueueHelper<QueuesExternalIdsKey, QueuesExternalIds> {
        @Override
        public void writeValues(final QueuesBuilder builder,
                final Map<QueuesExternalIdsKey, QueuesExternalIds> values) {
            builder.setQueuesExternalIds(values);
        }

        @Override
        public Map<QueuesExternalIdsKey, QueuesExternalIds> readValues(final Queues queue) {
            return queue.getQueuesExternalIds();
        }
    }

    private static class SouthboundQueuesOtherConfigHelper
            implements SouthboundQueueHelper<QueuesOtherConfigKey, QueuesOtherConfig> {
        @Override
        public void writeValues(final QueuesBuilder builder,
                final Map<QueuesOtherConfigKey, QueuesOtherConfig> values) {
            builder.setQueuesOtherConfig(values);
        }

        @Override
        public Map<QueuesOtherConfigKey, QueuesOtherConfig> readValues(final Queues queue) {
            return queue.getQueuesOtherConfig();
        }
    }

    private interface SouthboundQueueHelper<I extends Identifier<T>, T extends Identifiable<I>> {
        void writeValues(QueuesBuilder builder, Map<I, T> values);

        Map<I, T> readValues(Queues queue);
    }

    private static Queues getQueue(final Uri queueId, final OvsdbNodeAugmentation node) {
        for (Queues queue : node.nonnullQueues().values()) {
            if (queue.key().getQueueId().getValue().equals(queueId.getValue())) {
                return queue;
            }
        }
        return null;
    }

    private static class SouthboundQosExternalIdsHelper
            implements SouthboundQosHelper<QosExternalIdsKey, QosExternalIds> {
        @Override
        public void writeValues(final QosEntriesBuilder builder, final Map<QosExternalIdsKey, QosExternalIds> values) {
            builder.setQosExternalIds(values);
        }

        @Override
        public Map<QosExternalIdsKey, QosExternalIds> readValues(final QosEntries qos) {
            return qos.getQosExternalIds();
        }
    }

    private static class SouthboundQosOtherConfigHelper
            implements SouthboundQosHelper<QosOtherConfigKey, QosOtherConfig> {
        @Override
        public void writeValues(final QosEntriesBuilder builder, final Map<QosOtherConfigKey, QosOtherConfig> values) {
            builder.setQosOtherConfig(values);
        }

        @Override
        public Map<QosOtherConfigKey, QosOtherConfig> readValues(final QosEntries qos) {
            return qos.getQosOtherConfig();
        }
    }

    private interface SouthboundQosHelper<I extends Identifier<T>, T extends Identifiable<I>> {
        void writeValues(QosEntriesBuilder builder, Map<I, T> values);

        Map<I, T> readValues(QosEntries qos);
    }

    private static QosEntries getQos(final Uri qosId, final OvsdbNodeAugmentation node) {
        for (QosEntries qos : node.nonnullQosEntries().values()) {
            if (qos.key().getQosId().equals(qosId)) {
                return qos;
            }
        }
        return null;
    }

    private static <I extends Identifier<T>, T extends Identifiable<I>> void testCRUDQueue(
            final KeyValueBuilder<T> builder, final String prefix, final SouthboundQueueHelper<I, T> helper)
            throws InterruptedException {

        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);

        // updateFromTestCases represent the original test case value.  updateToTestCases represent the new value after
        // the update has been performed.
        List<SouthboundTestCase<I, T>> updateFromTestCases = generateKeyValueTestCases(builder, prefix + "From");
        List<SouthboundTestCase<I, T>> updateToTestCases = generateKeyValueTestCases(builder, prefix + "To");

        for (SouthboundTestCase<I, T> updateFromTestCase : updateFromTestCases) {
            for (SouthboundTestCase<I, T> updateToTestCase : updateToTestCases) {
                String testQueueId = String.format("%s_%s", prefix, updateToTestCase.name);

                // CREATE: and update the test queue with starting values.
                try (TestQueue testQueue = new TestQueue(connectionInfo, new Uri(testQueueId),
                        Uint8.valueOf(45), null, null)) {
                    QueuesBuilder queuesBuilder = new QueuesBuilder();
                    queuesBuilder.setQueueId(new Uri(testQueueId));
                    InstanceIdentifier<Queues> queueIid = SouthboundUtils.createInstanceIdentifier(connectionInfo)
                            .augmentation(OvsdbNodeAugmentation.class)
                            .child(Queues.class, queuesBuilder.build().key());
                    final NotifyingDataChangeListener queueConfigurationListener =
                            new NotifyingDataChangeListener(LogicalDatastoreType.CONFIGURATION, queueIid);
                    queueConfigurationListener.registerDataChangeListener();
                    final NotifyingDataChangeListener queueOperationalListener =
                            new NotifyingDataChangeListener(LogicalDatastoreType.OPERATIONAL, queueIid);
                    queueOperationalListener.registerDataChangeListener();

                    helper.writeValues(queuesBuilder, updateFromTestCase.inputValues);
                    assertTrue(mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                            queueIid, queuesBuilder.build()));
                    queueConfigurationListener.waitForUpdate(OVSDB_UPDATE_TIMEOUT);

                    // READ: Read the test queue and ensure changes are propagated to the CONFIGURATION data store,
                    // then repeat for OPERATIONAL data store
                    OvsdbNodeAugmentation updateFromConfigurationOvsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                            LogicalDatastoreType.CONFIGURATION);
                    Queues queueFromConfig =
                            getQueue(new Uri(testQueueId), updateFromConfigurationOvsdbNodeAugmentation);
                    if (queueFromConfig != null) {
                        assertExpectedExist(updateFromTestCase.expectedValues, helper.readValues(queueFromConfig));
                    }

                    queueOperationalListener.waitForUpdate(OVSDB_UPDATE_TIMEOUT);
                    OvsdbNodeAugmentation updateFromOperationalOvsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                            LogicalDatastoreType.OPERATIONAL);
                    Queues queueFromOper = getQueue(new Uri(testQueueId), updateFromOperationalOvsdbNodeAugmentation);
                    if (queueFromOper != null) {
                        assertExpectedExist(updateFromTestCase.expectedValues, helper.readValues(queueFromOper));
                    }

                    // UPDATE:  update the values
                    QueuesBuilder queuesUpdateBuilder = new QueuesBuilder();
                    queuesUpdateBuilder.setQueueId(new Uri(testQueueId));
                    helper.writeValues(queuesUpdateBuilder, updateToTestCase.inputValues);
                    assertTrue(mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                            queueIid, queuesUpdateBuilder.build()));
                    queueConfigurationListener.waitForUpdate(OVSDB_UPDATE_TIMEOUT);

                    // READ: the test queue and ensure changes are propagated to the CONFIGURATION data store,
                    // then repeat for OPERATIONAL data store
                    OvsdbNodeAugmentation updateToConfigurationOvsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                            LogicalDatastoreType.CONFIGURATION);
                    Queues queueToConfig = getQueue(new Uri(testQueueId), updateToConfigurationOvsdbNodeAugmentation);
                    if (queueToConfig != null) {
                        assertExpectedExist(updateToTestCase.expectedValues, helper.readValues(queueToConfig));
                    }

                    queueOperationalListener.waitForUpdate(OVSDB_UPDATE_TIMEOUT);
                    OvsdbNodeAugmentation updateToOperationalOvsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                            LogicalDatastoreType.OPERATIONAL);
                    Queues queueToOper = getQueue(new Uri(testQueueId), updateToOperationalOvsdbNodeAugmentation);
                    if (queueToOper != null) {
                        assertExpectedExist(updateToTestCase.expectedValues, helper.readValues(queueToOper));
                    }

                    // DELETE handled by TestQueue
                }
            }
        }
    }

    @Test
    public void testCRUDQueueExternalIds() throws InterruptedException {
        testCRUDQueue(new SouthboundQueuesExternalIdsBuilder(), "QueueExternalIds",
                new SouthboundQueuesExternalIdsHelper());
    }

    @Test
    public void testCRUDQueueOtherConfig() throws InterruptedException {
        testCRUDQueue(new SouthboundQueuesOtherConfigBuilder(), "QueueOtherConfig",
                new SouthboundQueuesOtherConfigHelper());
    }

    @Test
    public void testCRUDQueueDscp() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        String testQueueId = "testQueueDscp";

        // CREATE: and update the test queue with starting values.
        try (TestQueue testQueue = new TestQueue(connectionInfo, new Uri(testQueueId), Uint8.ZERO, null, null)) {
            for (short dscp = 1; dscp < 64; dscp++) {
                QueuesBuilder queuesBuilder = new QueuesBuilder();
                queuesBuilder.setQueueId(new Uri(testQueueId));
                InstanceIdentifier<Queues> queueIid = SouthboundUtils.createInstanceIdentifier(connectionInfo)
                        .augmentation(OvsdbNodeAugmentation.class)
                        .child(Queues.class, queuesBuilder.build().key());
                final NotifyingDataChangeListener queueOperationalListener =
                        new NotifyingDataChangeListener(LogicalDatastoreType.OPERATIONAL, queueIid);
                queueOperationalListener.registerDataChangeListener();

                queuesBuilder.setDscp(Uint8.valueOf(dscp));
                assertTrue(mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                        queueIid, queuesBuilder.build()));
                queueOperationalListener.waitForUpdate(OVSDB_ROUNDTRIP_TIMEOUT);

                // READ: Read the test queue and ensure changes are propagated to the OPERATIONAL data store
                // assumption is that CONFIGURATION was updated if OPERATIONAL is correct
                OvsdbNodeAugmentation ovsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                        LogicalDatastoreType.OPERATIONAL);
                Queues operQueue = getQueue(new Uri(testQueueId), ovsdbNodeAugmentation);
                assertNotNull(operQueue);
                assertEquals(dscp, operQueue.getDscp().toJava());
            }

            // DELETE handled by TestQueue
        }

    }

    private static <I extends Identifier<T>, T extends Identifiable<I>> void testCRUDQos(
            final KeyValueBuilder<T> builder, final String prefix, final SouthboundQosHelper<I, T> helper)
            throws InterruptedException {

        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);

        // updateFromTestCases represent the original test case value.  updateToTestCases represent the new value after
        // the update has been performed.
        List<SouthboundTestCase<I, T>> updateFromTestCases = generateKeyValueTestCases(builder, prefix + "From");
        List<SouthboundTestCase<I, T>> updateToTestCases = generateKeyValueTestCases(builder, prefix + "To");

        for (SouthboundTestCase<I, T> updateFromTestCase : updateFromTestCases) {
            for (SouthboundTestCase<I, T> updateToTestCase : updateToTestCases) {
                String testQosId = String.format("%s_%s", prefix, updateToTestCase.name);

                // CREATE: and update the test qos with starting values.
                try (TestQos testQos = new TestQos(connectionInfo, new Uri(testQosId),
                        SouthboundMapper.createQosType(SouthboundConstants.QOS_LINUX_HTB), null, null)) {
                    QosEntriesBuilder qosBuilder = new QosEntriesBuilder();
                    qosBuilder.setQosId(new Uri(testQosId));
                    InstanceIdentifier<QosEntries> qosIid = SouthboundUtils.createInstanceIdentifier(connectionInfo)
                            .augmentation(OvsdbNodeAugmentation.class)
                            .child(QosEntries.class, qosBuilder.build().key());
                    final NotifyingDataChangeListener qosConfigurationListener =
                            new NotifyingDataChangeListener(LogicalDatastoreType.CONFIGURATION, qosIid);
                    qosConfigurationListener.registerDataChangeListener();
                    final NotifyingDataChangeListener qosOperationalListener =
                            new NotifyingDataChangeListener(LogicalDatastoreType.OPERATIONAL, qosIid);
                    qosOperationalListener.registerDataChangeListener();

                    helper.writeValues(qosBuilder, updateFromTestCase.inputValues);
                    assertTrue(mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                            qosIid, qosBuilder.build()));
                    qosConfigurationListener.waitForUpdate(OVSDB_UPDATE_TIMEOUT);

                    // READ: Read the test queue and ensure changes are propagated to the CONFIGURATION data store,
                    // then repeat for OPERATIONAL data store
                    OvsdbNodeAugmentation updateFromConfigurationOvsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                            LogicalDatastoreType.CONFIGURATION);
                    QosEntries qosFromConfig = getQos(new Uri(testQosId), updateFromConfigurationOvsdbNodeAugmentation);
                    if (qosFromConfig != null) {
                        assertExpectedExist(updateFromTestCase.expectedValues, helper.readValues(qosFromConfig));
                    }

                    qosOperationalListener.waitForUpdate(OVSDB_UPDATE_TIMEOUT);
                    OvsdbNodeAugmentation updateFromOperationalOvsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                            LogicalDatastoreType.OPERATIONAL);
                    QosEntries qosFromOper = getQos(new Uri(testQosId), updateFromOperationalOvsdbNodeAugmentation);
                    if (qosFromOper != null) {
                        assertExpectedExist(updateFromTestCase.expectedValues, helper.readValues(qosFromOper));
                    }

                    // UPDATE:  update the values
                    QosEntriesBuilder qosUpdateBuilder = new QosEntriesBuilder();
                    qosUpdateBuilder.setQosId(new Uri(testQosId));
                    helper.writeValues(qosUpdateBuilder, updateToTestCase.inputValues);
                    assertTrue(mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                            qosIid, qosUpdateBuilder.build()));
                    qosConfigurationListener.waitForUpdate(OVSDB_UPDATE_TIMEOUT);

                    // READ: the test queue and ensure changes are propagated to the CONFIGURATION data store,
                    // then repeat for OPERATIONAL data store
                    OvsdbNodeAugmentation updateToConfigurationOvsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                            LogicalDatastoreType.CONFIGURATION);
                    QosEntries qosToConfig = getQos(new Uri(testQosId), updateToConfigurationOvsdbNodeAugmentation);
                    if (qosToConfig != null) {
                        assertExpectedExist(updateToTestCase.expectedValues, helper.readValues(qosToConfig));
                    }

                    qosOperationalListener.waitForUpdate(OVSDB_UPDATE_TIMEOUT);
                    OvsdbNodeAugmentation updateToOperationalOvsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                            LogicalDatastoreType.OPERATIONAL);
                    QosEntries qosToOper = getQos(new Uri(testQosId), updateToOperationalOvsdbNodeAugmentation);
                    if (qosToOper != null) {
                        assertExpectedExist(updateToTestCase.expectedValues, helper.readValues(qosToOper));
                    }

                    // DELETE handled by TestQueue
                }
            }
        }
    }

    @Test
    public void testCRUDQosExternalIds() throws InterruptedException {
        testCRUDQos(new SouthboundQosExternalIdsBuilder(), "QosExternalIds",
                new SouthboundQosExternalIdsHelper());
    }

    @Test
    public void testCRUDQosOtherConfig() throws InterruptedException {
        testCRUDQos(new SouthboundQosOtherConfigBuilder(), "QosOtherConfig",
                new SouthboundQosOtherConfigHelper());
    }

    @Test
    public void testCRUDQosQueues() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        String testQosId = "testQosQueues";

        // CREATE: and update the test queue with starting values.
        try (TestQos testQos = new TestQos(connectionInfo, new Uri(testQosId),
                SouthboundMapper.createQosType(SouthboundConstants.QOS_LINUX_HTB), null, null);
                TestQueue testQueue1 = new TestQueue(connectionInfo, new Uri("queue1"), Uint8.valueOf(12), null,
                    null);
                TestQueue testQueue2 = new TestQueue(connectionInfo, new Uri("queue2"), Uint8.valueOf(35), null,
                    null)) {
            QosEntriesBuilder qosBuilder = new QosEntriesBuilder();
            qosBuilder.setQosId(new Uri(testQosId));
            InstanceIdentifier<QosEntries> qosIid = SouthboundUtils.createInstanceIdentifier(connectionInfo)
                    .augmentation(OvsdbNodeAugmentation.class)
                    .child(QosEntries.class, qosBuilder.build().key());
            final NotifyingDataChangeListener qosOperationalListener =
                    new NotifyingDataChangeListener(LogicalDatastoreType.OPERATIONAL, qosIid);
            qosOperationalListener.registerDataChangeListener();

            // READ, UPDATE:  Read the UUIDs of the Queue rows and add them to the
            // configuration of the Qos row.
            OvsdbNodeAugmentation ovsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                    LogicalDatastoreType.OPERATIONAL);

            Queues operQueue1 = getQueue(new Uri("queue1"), ovsdbNodeAugmentation);

            assertNotNull(operQueue1);

            InstanceIdentifier<Queues> queue1Iid = testQueue1.getInstanceIdentifier();
            OvsdbQueueRef queue1Ref = new OvsdbQueueRef(queue1Iid);

            Queues operQueue2 = getQueue(new Uri("queue2"), ovsdbNodeAugmentation);
            assertNotNull(operQueue2);
            InstanceIdentifier<Queues> queue2Iid = testQueue2.getInstanceIdentifier();
            OvsdbQueueRef queue2Ref = new OvsdbQueueRef(queue2Iid);

            Map<QueueListKey, QueueList> queueList = BindingMap.of(
                new QueueListBuilder().setQueueNumber(Uint32.ONE).setQueueRef(queue1Ref).build(),
                new QueueListBuilder().setQueueNumber(Uint32.TWO).setQueueRef(queue2Ref).build());

            qosBuilder.setQueueList(queueList);

            assertTrue(mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                    qosIid, qosBuilder.build()));
            qosOperationalListener.waitForUpdate(OVSDB_UPDATE_TIMEOUT);

            // READ: Read the test qos and ensure changes are propagated to the OPERATIONAL data store
            // assumption is that CONFIGURATION was updated if OPERATIONAL is correct
            ovsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                    LogicalDatastoreType.OPERATIONAL);
            QosEntries operQos = getQos(new Uri(testQosId), ovsdbNodeAugmentation);
            assertNotNull(operQos);
            Map<QueueListKey, QueueList> operQueueList = operQos.getQueueList();
            assertNotNull(operQueueList);
            for (QueueList queueEntry : queueList.values()) {
                assertTrue(isQueueInList(operQueueList, queueEntry));
            }

            // DELETE one queue from queue list and check that one remains
            KeyedInstanceIdentifier<QueueList, QueueListKey> qosQueueIid = qosIid
                    .child(QueueList.class, new QueueListKey(Uint32.ONE));
            assertTrue(mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, qosQueueIid));
            qosOperationalListener.waitForUpdate(OVSDB_UPDATE_TIMEOUT);

            // READ: Read the test qos and ensure changes are propagated to the OPERATIONAL data store
            // assumption is that CONFIGURATION was updated if OPERATIONAL is correct
            ovsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                    LogicalDatastoreType.OPERATIONAL);
            operQos = getQos(new Uri(testQosId), ovsdbNodeAugmentation);
            assertNotNull(operQos);
            operQueueList = operQos.getQueueList();
            assertNotNull(operQueueList);

            for (QueueList queueEntry : queueList.values()) {
                if (queueEntry.getQueueRef().equals(queue2Ref)) {
                    assertTrue(isQueueInList(operQueueList, queueEntry));
                } else if (queueEntry.getQueueRef().equals(queue1Ref)) {
                    assertFalse(isQueueInList(operQueueList, queueEntry));
                } else {
                    assertTrue("Unknown queue entry in qos queue list", false);
                }
            }

            // DELETE  queue list and check that list is empty
            qosQueueIid = qosIid
                    .child(QueueList.class, new QueueListKey(Uint32.ONE));
            assertTrue(mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, qosQueueIid));
            qosOperationalListener.waitForUpdate(OVSDB_UPDATE_TIMEOUT);

            ovsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                    LogicalDatastoreType.OPERATIONAL);
            operQos = getQos(new Uri(testQosId), ovsdbNodeAugmentation);
            assertNotNull(operQos);
            operQueueList = operQos.getQueueList();
            assertNotNull(operQueueList);
            assertTrue(operQueueList.isEmpty());
        }
    }



    private static Boolean isQueueInList(final Map<QueueListKey, QueueList> queueList, final QueueList queue) {
        for (QueueList queueEntry : queueList.values()) {
            if (queueEntry.getQueueNumber().equals(queue.getQueueNumber())
                    && queueEntry.getQueueRef().equals(queue.getQueueRef())) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * Representation of a southbound test case. Each test case has a name, a list of input values and a list of
     * expected values. The input values are provided to the augmentation builder, and the expected values are checked
     * against the output of the resulting augmentation.
     * </p>
     * <p>
     * Instances of this class are immutable.
     * </p>
     *
     * @param <T> The type of data used for the test case.
     */
    private static final class SouthboundTestCase<I extends Identifier<T>, T extends Identifiable<I>> {
        private final String name;
        private final Map<I, T> inputValues;
        private final Map<I, T> expectedValues;

        /**
         * Creates an instance of a southbound test case.
         *
         * @param name The test case's name.
         * @param inputValues The input values (provided as input to the underlying augmentation builder).
         * @param expectedValues The expected values (checked against the output of the underlying augmentation).
         */
        SouthboundTestCase(final String name, final List<T> inputValues, final List<T> expectedValues) {
            this.name = name;
            this.inputValues = BindingMap.ordered(inputValues);
            this.expectedValues = BindingMap.of(expectedValues);
        }
    }

    /**
     * Southbound test case builder.
     *
     * @param <T> The type of data used for the test case.
     */
    private static final class SouthboundTestCaseBuilder<I extends Identifier<T>, T extends Identifiable<I>> {
        private String name;
        private List<T> inputValues;
        private List<T> expectedValues;

        /**
         * Creates a builder. Builders may be reused, the generated immutable instances are independent of the
         * builders. There are no default values.
         */
        SouthboundTestCaseBuilder() {
            // Nothing to do
        }

        /**
         * Sets the test case's name.
         *
         * @param value The test case's name.
         * @return The builder.
         */
        public SouthboundTestCaseBuilder<I, T> name(final String value) {
            this.name = value;
            return this;
        }

        /**
         * Sets the input values.
         *
         * @param values The input values.
         * @return The builder.
         */
        @SafeVarargs
        public final SouthboundTestCaseBuilder<I, T> input(final T... values) {
            this.inputValues = Lists.newArrayList(values);
            return this;
        }

        /**
         * Indicates that the provided input values should be expected as output values.
         *
         * @return The builder.
         */
        public SouthboundTestCaseBuilder<I, T> expectInputAsOutput() {
            this.expectedValues = this.inputValues;
            return this;
        }

        /**
         * Indicates that no output should be expected.
         *
         * @return The builder.
         */
        public SouthboundTestCaseBuilder<I, T> expectNoOutput() {
            this.expectedValues = null;
            return this;
        }

        /**
         * Builds an immutable instance representing the test case.
         *
         * @return The test case.
         */
        public SouthboundTestCase<I, T> build() {
            return new SouthboundTestCase<>(name, inputValues, expectedValues);
        }
    }

    private abstract static class KeyValueBuilder<T> {
        private static final int COUNTER_START = 0;
        private int counter = COUNTER_START;

        protected abstract boolean isValueMandatory();

        public final T build(final String testName, final String key, final String value) {
            counter++;
            return build(key == null ? null : String.format(FORMAT_STR, testName, key, counter),
                value != null ? null : String.format(FORMAT_STR, testName, value, counter));
        }

        abstract @NonNull T build(@Nullable String key, @Nullable String value);

        public final void reset() {
            counter = COUNTER_START;
        }
    }

    private static final class SouthboundQueuesExternalIdsBuilder extends KeyValueBuilder<QueuesExternalIds> {
        @Override
        QueuesExternalIds build(final String key, final String value) {
            return new QueuesExternalIdsBuilder()
                .setQueuesExternalIdKey(key)
                .setQueuesExternalIdValue(value)
                .build();
        }

        @Override
        protected boolean isValueMandatory() {
            return true;
        }
    }

    private static final class SouthboundQueuesOtherConfigBuilder extends KeyValueBuilder<QueuesOtherConfig> {
        @Override
        QueuesOtherConfig build(final String key, final String value) {
            return new QueuesOtherConfigBuilder()
                .setQueueOtherConfigKey(key)
                .setQueueOtherConfigValue(value)
                .build();
        }

        @Override
        protected boolean isValueMandatory() {
            return false;
        }
    }

    private static final class SouthboundQosExternalIdsBuilder extends KeyValueBuilder<QosExternalIds> {
        @Override
        QosExternalIds build(final String key, final String value) {
            return new QosExternalIdsBuilder()
                .setQosExternalIdKey(key)
                .setQosExternalIdValue(value)
                .build();
        }

        @Override
        protected boolean isValueMandatory() {
            return true;
        }
    }

    private static final class SouthboundQosOtherConfigBuilder extends KeyValueBuilder<QosOtherConfig> {
        @Override
        QosOtherConfig build(final String key, final String value) {
            return new QosOtherConfigBuilder()
                .setOtherConfigKey(key)
                .setOtherConfigValue(value)
                .build();
        }

        @Override
        protected boolean isValueMandatory() {
            return false;
        }
    }

    private static final class SouthboundPortExternalIdsBuilder extends KeyValueBuilder<PortExternalIds> {
        @Override
        PortExternalIds build(final String key, final String value) {
            return new PortExternalIdsBuilder()
                .setExternalIdKey(key)
                .setExternalIdValue(value)
                .build();
        }

        @Override
        protected boolean isValueMandatory() {
            return true;
        }
    }

    private static final class SouthboundInterfaceExternalIdsBuilder extends KeyValueBuilder<InterfaceExternalIds> {
        @Override
        InterfaceExternalIds build(final String key, final String value) {
            return new InterfaceExternalIdsBuilder()
                .setExternalIdKey(key)
                .setExternalIdValue(value)
                .build();
        }

        @Override
        protected boolean isValueMandatory() {
            return true;
        }
    }

    private static final class SouthboundInterfaceLldpBuilder extends KeyValueBuilder<InterfaceLldp> {
        @Override
        InterfaceLldp build(final String key, final String value) {
            return new InterfaceLldpBuilder()
                .setLldpKey(key)
                .setLldpValue(value)
                .build();
        }

        @Override
        protected boolean isValueMandatory() {
            return true;
        }
    }

    private static final class SouthboundOptionsBuilder extends KeyValueBuilder<Options> {
        @Override
        Options build(final String key, final String value) {
            return new OptionsBuilder()
                .setOption(key)
                .setValue(value)
                .build();
        }

        @Override
        protected boolean isValueMandatory() {
            return false;
        }
    }

    private static final class SouthboundInterfaceOtherConfigsBuilder extends KeyValueBuilder<InterfaceOtherConfigs> {
        @Override
        InterfaceOtherConfigs build(final String key, final String value) {
            return new InterfaceOtherConfigsBuilder()
                .setOtherConfigKey(key)
                .setOtherConfigValue(value)
                .build();
        }

        @Override
        protected boolean isValueMandatory() {
            return false;
        }
    }

    private static final class SouthboundPortOtherConfigsBuilder extends KeyValueBuilder<PortOtherConfigs> {
        @Override
        PortOtherConfigs build(final String key, final String value) {
            return new PortOtherConfigsBuilder()
                .setOtherConfigKey(key)
                .setOtherConfigValue(value)
                .build();
        }

        @Override
        protected boolean isValueMandatory() {
            return false;
        }
    }

    private static final class SouthboundBridgeOtherConfigsBuilder extends KeyValueBuilder<BridgeOtherConfigs> {
        @Override
        BridgeOtherConfigs build(final String key, final String value) {
            return new BridgeOtherConfigsBuilder()
                .setBridgeOtherConfigKey(key)
                .setBridgeOtherConfigValue(value)
                .build();
        }

        @Override
        protected boolean isValueMandatory() {
            return false;
        }
    }

    private static final class SouthboundBridgeExternalIdsBuilder extends KeyValueBuilder<BridgeExternalIds> {
        @Override
        BridgeExternalIds build(final String key, final String value) {
            return new BridgeExternalIdsBuilder()
                .setBridgeExternalIdKey(key)
                .setBridgeExternalIdValue(value)
                .build();
        }

        @Override
        protected boolean isValueMandatory() {
            return true;
        }
    }

    /*
     * Generates the test cases involved in testing key-value-based data.  See inline comments for descriptions of
     * the particular cases considered.
     */
    private static <I extends Identifier<T>, T extends Identifiable<I>> List<SouthboundTestCase<I, T>>
            generateKeyValueTestCases(final KeyValueBuilder<T> builder, final String testName) {
        List<SouthboundTestCase<I, T>> testCases = new ArrayList<>();

        final String goodKey = "GoodKey";
        final String goodValue = "GoodValue";
        final String noValueForKey = "NoValueForKey";

        final String idKey = testName + "Key";
        final String idValue = testName + "Value";

        // Test Case 1:  TestOne
        // Test Type:    Positive
        // Description:  Create a termination point with one value
        // Expected:     A port is created with the single value specified below
        final String testOneName = "TestOne" + testName;
        testCases.add(new SouthboundTestCaseBuilder<I, T>()
                .name(testOneName)
                .input(builder.build(testOneName, idKey, idValue))
                .expectInputAsOutput()
                .build());
        builder.reset();

        // Test Case 2:  TestFive
        // Test Type:    Positive
        // Description:  Create a termination point with multiple (five) values
        // Expected:     A port is created with the five values specified below
        final String testFiveName = "TestFive" + testName;
        testCases.add(new SouthboundTestCaseBuilder<I, T>()
                .name(testFiveName)
                .input(
                        builder.build(testFiveName, idKey, idValue),
                        builder.build(testFiveName, idKey, idValue),
                        builder.build(testFiveName, idKey, idValue),
                        builder.build(testFiveName, idKey, idValue),
                        builder.build(testFiveName, idKey, idValue))
                .expectInputAsOutput()
                .build());
        builder.reset();

        if (!builder.isValueMandatory()) {
            // Test Case 3:  TestOneGoodOneMalformedValue
            // Test Type:    Negative
            // Description:
            //     One perfectly fine input
            //        (TestOneGoodOneMalformedValue_GoodKey_1,
            //        TestOneGoodOneMalformedValue_GoodValue_1)
            //     and one malformed input which only has key specified
            //        (TestOneGoodOneMalformedValue_NoValueForKey_2,
            //        UNSPECIFIED)
            // Expected:     A port is created without any values
            final String testOneGoodOneMalformedValueName = "TestOneGoodOneMalformedValue" + testName;
            testCases.add(new SouthboundTestCaseBuilder<I, T>()
                    .name(testOneGoodOneMalformedValueName)
                    .input(
                            builder.build(testOneGoodOneMalformedValueName, goodKey, goodValue),
                            builder.build(testOneGoodOneMalformedValueName, noValueForKey, null))
                    .expectNoOutput()
                    .build());
            builder.reset();
        } else {
            LOG.info("generateKeyValueTestCases: skipping test case 3 for {}", builder.getClass().getSimpleName());
        }

        return testCases;
    }

    private static class PortExternalIdsSouthboundHelper
            implements SouthboundTerminationPointHelper<PortExternalIdsKey, PortExternalIds> {
        @Override
        public void writeValues(final OvsdbTerminationPointAugmentationBuilder builder,
                final Map<PortExternalIdsKey, PortExternalIds> values) {
            builder.setPortExternalIds(values);
        }

        @Override
        public Map<PortExternalIdsKey, PortExternalIds> readValues(
                final OvsdbTerminationPointAugmentation augmentation) {
            return augmentation.getPortExternalIds();
        }
    }

    private static class InterfaceExternalIdsSouthboundHelper
            implements SouthboundTerminationPointHelper<InterfaceExternalIdsKey, InterfaceExternalIds> {
        @Override
        public void writeValues(final OvsdbTerminationPointAugmentationBuilder builder,
                final Map<InterfaceExternalIdsKey, InterfaceExternalIds> values) {
            builder.setInterfaceExternalIds(values);
        }

        @Override
        public Map<InterfaceExternalIdsKey, InterfaceExternalIds> readValues(
                final OvsdbTerminationPointAugmentation augmentation) {
            return augmentation.getInterfaceExternalIds();
        }
    }

    private static class InterfaceLldpSouthboundHelper
            implements SouthboundTerminationPointHelper<InterfaceLldpKey, InterfaceLldp> {
        @Override
        public void writeValues(final OvsdbTerminationPointAugmentationBuilder builder,
                final Map<InterfaceLldpKey, InterfaceLldp> values) {
            builder.setInterfaceLldp(values);
        }

        @Override
        public Map<InterfaceLldpKey, InterfaceLldp> readValues(final OvsdbTerminationPointAugmentation augmentation) {
            return augmentation.getInterfaceLldp();
        }
    }

    private static class OptionsSouthboundHelper implements SouthboundTerminationPointHelper<OptionsKey, Options> {
        @Override
        public void writeValues(final OvsdbTerminationPointAugmentationBuilder builder,
                final Map<OptionsKey, Options> values) {
            builder.setOptions(values);
        }

        @Override
        public Map<OptionsKey, Options> readValues(final OvsdbTerminationPointAugmentation augmentation) {
            return augmentation.getOptions();
        }
    }

    private static class InterfaceOtherConfigsSouthboundHelper
            implements SouthboundTerminationPointHelper<InterfaceOtherConfigsKey, InterfaceOtherConfigs> {
        @Override
        public void writeValues(final OvsdbTerminationPointAugmentationBuilder builder,
                final Map<InterfaceOtherConfigsKey, InterfaceOtherConfigs> values) {
            builder.setInterfaceOtherConfigs(values);
        }

        @Override
        public Map<InterfaceOtherConfigsKey, InterfaceOtherConfigs> readValues(
                final OvsdbTerminationPointAugmentation augmentation) {
            return augmentation.getInterfaceOtherConfigs();
        }
    }

    private static class PortOtherConfigsSouthboundHelper implements
            SouthboundTerminationPointHelper<PortOtherConfigsKey, PortOtherConfigs> {
        @Override
        public void writeValues(final OvsdbTerminationPointAugmentationBuilder builder,
                final Map<PortOtherConfigsKey, PortOtherConfigs> values) {
            builder.setPortOtherConfigs(values);
        }

        @Override
        public Map<PortOtherConfigsKey, PortOtherConfigs> readValues(
                final OvsdbTerminationPointAugmentation augmentation) {
            return augmentation.getPortOtherConfigs();
        }
    }

    private static class BridgeExternalIdsSouthboundHelper
            implements SouthboundBridgeHelper<BridgeExternalIdsKey, BridgeExternalIds> {
        @Override
        public void writeValues(final OvsdbBridgeAugmentationBuilder builder,
                final Map<BridgeExternalIdsKey, BridgeExternalIds> values) {
            builder.setBridgeExternalIds(values);
        }

        @Override
        public Map<BridgeExternalIdsKey, BridgeExternalIds> readValues(final OvsdbBridgeAugmentation augmentation) {
            return augmentation.getBridgeExternalIds();
        }
    }

    private static class BridgeOtherConfigsSouthboundHelper
            implements SouthboundBridgeHelper<BridgeOtherConfigsKey, BridgeOtherConfigs> {
        @Override
        public void writeValues(final OvsdbBridgeAugmentationBuilder builder,
                final Map<BridgeOtherConfigsKey, BridgeOtherConfigs> values) {
            builder.setBridgeOtherConfigs(values);
        }

        @Override
        public Map<BridgeOtherConfigsKey, BridgeOtherConfigs> readValues(final OvsdbBridgeAugmentation augmentation) {
            return augmentation.getBridgeOtherConfigs();
        }
    }
}
