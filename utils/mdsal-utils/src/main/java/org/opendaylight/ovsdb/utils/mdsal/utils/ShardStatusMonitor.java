/*
 * Copyright (c) 2020 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.utils.mdsal.utils;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ShardStatusMonitor {

    private static final Logger LOG = LoggerFactory.getLogger(ShardStatusMonitor.class);
    private static final String TOPOLOGY_CONFIG_SHARD = "topology:config";
    private static final String TOPOLOGY_OPER_SHARD = "topology:oper";
    private static final String STATUS_OPERATIONAL = "OPERATIONAL";

    private static final String JMX_OBJECT_NAME_LIST_OF_CONFIG_SHARDS =
        "org.opendaylight.controller:type=DistributedConfigDatastore,Category=ShardManager,name=shard-manager-config";
    private static final String JMX_OBJECT_NAME_LIST_OF_OPER_SHARDS =
        "org.opendaylight.controller:type=DistributedOperationalDatastore,"
                + "Category=ShardManager,name=shard-manager-operational";

    public static final Collection<String> TOPOLOGY_SHARDS =
            Collections.unmodifiableList(Arrays.asList(TOPOLOGY_CONFIG_SHARD, TOPOLOGY_OPER_SHARD));

    //To avoid the checkstyle errors
    private ShardStatusMonitor() {

    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static String getLeaderJMX(String objectName, String atrName) {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        String leader = "";
        if (mbs != null) {
            try {
                leader  = (String)mbs.getAttribute(new ObjectName(objectName), atrName);
            } catch (Exception e) {
                LOG.error("Failed to get leader jmx {}", e.getMessage());
            }
        }
        return leader;
    }

    public static boolean getShardStatus(Collection<String> shards) {
        boolean status = true;
        for (String shard : shards) {
            String[] params = shard.split(":");
            if (!getDataStoreStatus(params[0], params[1]).equalsIgnoreCase(STATUS_OPERATIONAL)) {
                status = false;
                break;
            }
        }
        return status;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static String getDataStoreStatus(String name, String type) {
        boolean statusResult = true;
        try {
            ArrayList listOfShards;
            if (type.equalsIgnoreCase("config")) {
                listOfShards = getAttributeJMXCommand(JMX_OBJECT_NAME_LIST_OF_CONFIG_SHARDS, "LocalShards");
            } else  {
                listOfShards = getAttributeJMXCommand(JMX_OBJECT_NAME_LIST_OF_OPER_SHARDS, "LocalShards");
            }
            if (listOfShards != null) {
                for (int i = 0; i < listOfShards.size(); i++) {
                    if (listOfShards.get(i).toString().contains(name)) {
                        String jmxObjectShardStatus;
                        if (type.equalsIgnoreCase("config")) {
                            jmxObjectShardStatus = "org.opendaylight.controller:Category=Shards,name="
                                    + listOfShards.get(i) + ",type=DistributedConfigDatastore";
                        } else {
                            jmxObjectShardStatus = "org.opendaylight.controller:Category=Shards,name="
                                    + listOfShards.get(i) + ",type=DistributedOperationalDatastore";
                        }
                        String leader = getLeaderJMX(jmxObjectShardStatus,"Leader");
                        if (leader != null && leader.length() > 1) {
                            if (type.equalsIgnoreCase("config")) {
                                LOG.info("{} ::Config DS has the Leader as:: {}", listOfShards.get(i), leader);
                            } else {
                                LOG.info("{} ::Oper DS has the Leader as:: {}", listOfShards.get(i), leader);
                            }
                        } else {
                            statusResult = false;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("ERROR::", e);
            statusResult = false;
        }
        if (statusResult) {
            return STATUS_OPERATIONAL;
        } else {
            return "ERROR";
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static ArrayList getAttributeJMXCommand(String objectName, String attributeName) {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ArrayList listOfShards = new ArrayList();
        if (mbs != null) {
            try {
                listOfShards = (ArrayList) mbs.getAttribute(new ObjectName(objectName), attributeName);
            } catch (MalformedObjectNameException monEx) {
                LOG.error("CRITICAL EXCEPTION : Malformed Object Name Exception");
            } catch (MBeanException mbEx) {
                LOG.error("CRITICAL EXCEPTION : MBean Exception");
            } catch (InstanceNotFoundException infEx) {
                LOG.error("CRITICAL EXCEPTION : Instance Not Found Exception");
            } catch (ReflectionException rEx) {
                LOG.error("CRITICAL EXCEPTION : Reflection Exception");
            } catch (Exception e) {
                LOG.error("Attribute not found");
            }
        }
        return listOfShards;
    }

}
