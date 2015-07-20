/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */
package org.opendaylight.ovsdb.schema.openvswitch;

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.MethodType;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedColumn;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;

import java.util.Map;
import java.util.Set;

/**
 * This class is a typed interface to the Controller Table
 */

@TypedTable (name="Controller", database="Open_vSwitch", fromVersion="1.0.0")
public interface Controller extends TypedBaseTable<GenericTableSchema> {

    @TypedColumn (name = "target", method = MethodType.GETCOLUMN, fromVersion = "1.0.0")
    Column<GenericTableSchema, String> getTargetColumn();

    @TypedColumn (name = "target", method = MethodType.SETDATA, fromVersion = "1.0.0")
    void setTarget(String target);

    @TypedColumn (name = "controller_burst_limit", method = MethodType.GETCOLUMN, fromVersion = "1.0.0")
    Column<GenericTableSchema, Long> getBurstLimitColumn();

    @TypedColumn (name = "controller_burst_limit", method = MethodType.SETDATA, fromVersion = "1.0.0")
    void setBurstLimit(Long burstLimit);

    @TypedColumn (name = "controller_rate_limit", method = MethodType.GETCOLUMN, fromVersion = "1.0.0")
    Column<GenericTableSchema, Long> getRateLimitColumn();

    @TypedColumn (name = "controller_rate_limit", method = MethodType.SETDATA, fromVersion = "1.0.0")
    void setRateLimit(Long rateLimit);

    @TypedColumn (name = "connection_mode", method = MethodType.GETCOLUMN, fromVersion = "1.0.0")
    Column<GenericTableSchema, Set<String>> getConnectionModeColumn();

    @TypedColumn (name = "connection_mode", method = MethodType.SETDATA, fromVersion = "1.0.0")
    void setConnectionMode(Set<String> connectionMode);

    @TypedColumn (name = "enable_async_messages", method = MethodType.GETCOLUMN, fromVersion = "6.7.0")
    Column<GenericTableSchema, Set<Boolean>> getEnableAsyncMessagesColumn();

    @TypedColumn (name = "enable_async_messages", method = MethodType.SETDATA, fromVersion = "6.7.0")
    void setEnableAsyncMessages(Set<Boolean> enableAsyncMessages);

    @TypedColumn (name = "external_ids", method = MethodType.GETCOLUMN, fromVersion = "1.0.0")
    Column<GenericTableSchema, Map<String, String>> getExternalIdsColumn();

    @TypedColumn (name = "external_ids", method = MethodType.SETDATA, fromVersion = "1.0.0")
    void setExternalIds(Map<String, String> externalIds);

    @TypedColumn (name = "local_netmask", method = MethodType.GETCOLUMN, fromVersion = "1.0.0")
    Column<GenericTableSchema, Set<String>> getLocalNetmaskColumn();

    @TypedColumn (name = "local_netmask", method = MethodType.SETDATA, fromVersion = "1.0.0")
    void setLocalNetmask(Set<String> localNetmask);

    @TypedColumn (name = "local_gateway", method = MethodType.GETCOLUMN, fromVersion = "1.0.0")
    Column<GenericTableSchema, Set<String>> getLocalGatewayColumn();

    @TypedColumn (name = "local_gateway", method = MethodType.SETDATA, fromVersion = "1.0.0")
    void setLocalGateway(Set<String> localGateway);

    @TypedColumn (name = "status", method = MethodType.GETCOLUMN, fromVersion = "1.1.0")
    Column<GenericTableSchema, Map<String, String>> getStatusColumn();

    @TypedColumn (name = "status", method = MethodType.SETDATA, fromVersion = "1.1.0")
    void setStatus(Map<String, String> status);

    @TypedColumn (name = "role", method = MethodType.GETCOLUMN, fromVersion = "1.1.0")
    Column<GenericTableSchema, Set<String>> getRoleColumn();

    @TypedColumn (name = "role", method = MethodType.SETDATA, fromVersion = "1.1.0")
    void setRole(Set<String> role);

    @TypedColumn (name = "inactivity_probe", method = MethodType.GETCOLUMN, fromVersion = "1.0.0")
    Column<GenericTableSchema, Set<Long>> getInactivityProbeColumn();

    @TypedColumn (name = "inactivity_probe", method = MethodType.SETDATA, fromVersion = "1.0.0")
    void setInactivityProbe(Set<Long> inactivityProbe);

    @TypedColumn (name = "is_connected", method = MethodType.GETCOLUMN, fromVersion = "1.1.0")
    Column<GenericTableSchema, Boolean> getIsConnectedColumn();

    @TypedColumn (name = "is_connected", method = MethodType.SETDATA, fromVersion = "1.1.0")
    void setIsConnected(Boolean isConnected);

    @TypedColumn (name = "other_config", method = MethodType.GETCOLUMN, fromVersion = "6.8.0")
    Column<GenericTableSchema, Map<String, String>> getOtherConfigColumn();

    @TypedColumn (name = "other_config", method = MethodType.SETDATA, fromVersion = "6.8.0")
    void setOtherConfig(Map<String, String> otherConfig);

    @TypedColumn (name = "max_backoff", method = MethodType.GETCOLUMN, fromVersion = "1.0.0")
    Column<GenericTableSchema, Long> getMaxBackoffColumn();

    @TypedColumn (name = "max_backoff", method = MethodType.SETDATA, fromVersion = "1.0.0")
    void setMaxBackoff(Long maxBackoff);

    @TypedColumn (name = "local_ip", method = MethodType.GETCOLUMN, fromVersion = "1.0.0")
    Column<GenericTableSchema, Set<String>> getLocalIpColumn();

    @TypedColumn (name = "local_ip", method = MethodType.SETDATA, fromVersion = "1.0.0")
    void setLocalIp(Set<String> localIp);

    @TypedColumn (name = "discover_update_resolv_conf", method = MethodType.GETCOLUMN,
                  fromVersion="1.0.0", untilVersion="3.0.0")
    Column<GenericTableSchema, Set<String>> getDiscoverUpdateResolvConfColumn();

    @TypedColumn (name = "discover_update_resolv_conf", method = MethodType.SETDATA,
                  fromVersion="1.0.0", untilVersion="3.0.0")
    void setDiscoverUpdateResolvConf(Set<String> discoverUpdateResolvConf);

    @TypedColumn (name = "discover_accept_regex", method = MethodType.GETCOLUMN,
                  fromVersion="1.0.0", untilVersion="3.0.0")
    Column<GenericTableSchema, Set<String>> getDiscoverAcceptRegexColumn();

    @TypedColumn (name = "discover_accept_regex", method = MethodType.SETDATA,
                  fromVersion="1.0.0", untilVersion="3.0.0")
    void setDiscoverAcceptRegex(Set<String> discoverAcceptRegex);
}
