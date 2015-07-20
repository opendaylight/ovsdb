/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Dave Tucker
 */
package org.opendaylight.ovsdb.schema.openvswitch;

import java.util.Map;
import java.util.Set;

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.MethodType;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedColumn;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;

/**
 * This class is a typed interface to the Bridge Table
 */
@TypedTable(name="Interface", database="Open_vSwitch", fromVersion="1.0.0")
public interface Interface extends TypedBaseTable<GenericTableSchema> {

    @TypedColumn(name="name", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, String> getNameColumn();
    @TypedColumn(name="name", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setName(String name);
    @TypedColumn(name="name", method=MethodType.GETDATA, fromVersion="1.0.0")
    String getName();

    @TypedColumn(name="type", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, String> getTypeColumn();
    @TypedColumn(name="type", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setType(String type);

    @TypedColumn(name="options", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Map<String, String>> getOptionsColumn();
    @TypedColumn(name="options", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setOptions(Map<String, String> options);

    @TypedColumn(name="ingress_policing_rate", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<Long>> getIngressPolicingRateColumn();
    @TypedColumn(name="ingress_policing_rate", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setIngressPolicingRate(Set<Long> ingressPolicingRate);

    @TypedColumn(name="ingress_policing_burst", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<Long>> getIngressPolicingBurstColumn();
    @TypedColumn(name="ingress_policing_burst", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setIngressPolicingBurst(Set<Long> ingressPolicingBurst);

    @TypedColumn(name="mac_in_use", method=MethodType.GETCOLUMN, fromVersion="7.1.0")
    Column<GenericTableSchema,Set<String>> getMacInUseColumn();
    @TypedColumn(name="mac_in_use", method=MethodType.SETDATA, fromVersion="7.1.0")
    void setMacInUse(Set<String> macInUse);

    @TypedColumn(name="mac", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<String>> getMacColumn();
    @TypedColumn(name="mac", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setMac(Set<String> mac);

    @TypedColumn(name="ifindex", method=MethodType.GETCOLUMN, fromVersion="7.2.1")
    Column<GenericTableSchema, Long> getIfIndexColumn();
    @TypedColumn(name="ifindex", method=MethodType.SETDATA, fromVersion="7.2.1")
    void setIfIndex(Long ifIndex);

    @TypedColumn(name="external_ids", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Map<String, String>> getExternalIdsColumn();
    @TypedColumn(name="external_ids", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setExternalIds(Map<String, String> externalIds);

    @TypedColumn(name="ofport", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<Long>> getOpenFlowPortColumn();
    @TypedColumn(name="ofport", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setOpenFlowPort(Set<Long> openFlowPort);

    @TypedColumn(name="ofport_request", method=MethodType.GETCOLUMN, fromVersion="6.2.0")
    Column<GenericTableSchema, Set<Long>> getOpenFlowPortRequestColumn();
    @TypedColumn(name="ofport_request", method=MethodType.SETDATA, fromVersion="6.2.0")
    void setOpenFlowPortRequest(Set<Long> openFlowPortRequest);

    @TypedColumn(name="bfd", method=MethodType.GETCOLUMN, fromVersion="7.2.0")
    Column<GenericTableSchema, Map<String, String>> getBfdColumn();
    @TypedColumn(name="bfd", method=MethodType.SETDATA, fromVersion="7.2.0")
    void setBfd(Map<String, String> bfd);

    @TypedColumn(name="bfd_status", method=MethodType.GETCOLUMN, fromVersion="7.2.0")
    Column<GenericTableSchema, Map<String, String>> getBfdStatusColumn();
    @TypedColumn(name="bfd_status", method=MethodType.SETDATA, fromVersion="7.2.0")
    void setBfdStatus(Map<String, String> bfdStatus);

    @TypedColumn(name="monitor", method=MethodType.GETCOLUMN, fromVersion="1.0.0", untilVersion="3.5.0")
    Column<GenericTableSchema, String> getMonitorColumn();
    @TypedColumn(name="monitor", method=MethodType.SETDATA, fromVersion="1.0.0", untilVersion="3.5.0")
    void setMonitor(String monitor);

    @TypedColumn(name="cfm_mpid", method=MethodType.GETCOLUMN, fromVersion="4.0.0")
    Column<GenericTableSchema, Set<Long>> getCfmMpidColumn();
    @TypedColumn(name="cfm_mpid", method=MethodType.SETDATA)
    void setCfmMpid(Set<Long> cfmMpid);

    @TypedColumn(name="cfm_remote_mpid", method=MethodType.GETCOLUMN, fromVersion="4.0.0", untilVersion="5.2.0")
    Column<GenericTableSchema, Set<Long>> getCfmRemoteMpidColumn();
    @TypedColumn(name="cfm_remote_mpid", method=MethodType.SETDATA, fromVersion="4.0.0", untilVersion="5.2.0")
    void setCfmRemoteMpid(Set<Long> cfmRemoteMpid);

    @TypedColumn(name="cfm_remote_mpids", method=MethodType.GETCOLUMN, fromVersion="6.0.0")
    Column<GenericTableSchema, Set<Long>> getCfmRemoteMpidsColumn();
    @TypedColumn(name="cfm_remote_mpids", method=MethodType.SETDATA, fromVersion="6.0.0")
    void setCfmRemoteMpids(Set<Long> cfmRemoteMpids);

    @TypedColumn(name="cfm_flap_count", method=MethodType.GETCOLUMN, fromVersion="7.3.0")
    Column<GenericTableSchema, Set<Long>> getCfmFlapCountColumn();
    @TypedColumn(name="cfm_flap_count", method=MethodType.SETDATA, fromVersion="7.3.0")
    void setCfmFlapCount(Set<Long> cfmFlapCount);

    @TypedColumn(name="cfm_fault", method=MethodType.GETCOLUMN, fromVersion="4.0.0")
    Column<GenericTableSchema, Set<Boolean>> getCfmFaultColumn();
    @TypedColumn(name="cfm_fault", method=MethodType.SETDATA, fromVersion="4.0.0")
    void setCfmFault(Set<Boolean> cfmFault);

    @TypedColumn(name="cfm_fault_status", method=MethodType.GETCOLUMN, fromVersion="6.6.0")
    Column<GenericTableSchema, Set<String>> getCfmFaultStatusColumn();
    @TypedColumn(name="cfm_fault_status", method=MethodType.SETDATA, fromVersion="6.6.0")
    void setCfmFaultStatus(Set<String> cfmFaultStatus);

    @TypedColumn(name="cfm_remote_opstate", method=MethodType.GETCOLUMN, fromVersion="6.10.0")
    Column<GenericTableSchema, Set<String>> getCfmRemoteOpStateColumn();
    @TypedColumn(name="cfm_remote_opstate", method=MethodType.SETDATA, fromVersion="6.10.0")
    void setCfmRemoteOpState(Set<String> cfmRemoteOpState);

    @TypedColumn(name="cfm_health", method=MethodType.GETCOLUMN, fromVersion="6.9.0")
    Column<GenericTableSchema, Set<Long>> getCfmHealthColumn();
    @TypedColumn(name="cfmHealth", method=MethodType.SETDATA, fromVersion="6.9.0")
    void setCfmHealth(Set<Long> cfmHealth);

    @TypedColumn(name="lacp_current", method=MethodType.GETCOLUMN, fromVersion="3.3.0")
    Column<GenericTableSchema, Set<Boolean>> getLacpCurrentColumn();
    @TypedColumn(name="lacp_current", method=MethodType.SETDATA, fromVersion="3.3.0")
    void setLacpCurrent(Set<Boolean> lacpCurrent);

    @TypedColumn(name="other_config", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Map<String, String>> getOtherConfigColumn();
    @TypedColumn(name="other_config", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setOtherConfig(Map<String, String> otherConfig);

    @TypedColumn(name="statistics", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Map<String, Long>> getStatisticsColumn();
    @TypedColumn(name="statistics", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setStatistics(Map<String, Long> statistics);

    @TypedColumn(name="status", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Map<String, String>> getStatusColumn();
    @TypedColumn(name="status", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setStatus(Map<String, String> status);

    @TypedColumn(name="admin_state", method=MethodType.GETCOLUMN, fromVersion="1.0.6")
    Column<GenericTableSchema, Set<String>> getAdminStateColumn();
    @TypedColumn(name="admin_state", method=MethodType.SETDATA, fromVersion="1.0.6")
    void setAdminState(Set<String> adminState);

    @TypedColumn(name="link_state", method=MethodType.GETCOLUMN, fromVersion="1.0.6")
    Column<GenericTableSchema, Map<String, String>> getLinkStateColumn();
    @TypedColumn(name="link_state", method=MethodType.SETDATA, fromVersion="1.0.6")
    void setLinkState(Map<String, String> linkState);

    @TypedColumn(name="link_resets", method=MethodType.GETCOLUMN, fromVersion="6.2.0")
    Column<GenericTableSchema, Set<String>> getLinkResetsColumn();
    @TypedColumn(name="link_resets", method=MethodType.SETDATA, fromVersion="6.2.0")
    void setLinkResets(Set<String> linkResets);

    @TypedColumn(name="link_speed", method=MethodType.GETCOLUMN, fromVersion="1.0.6")
    Column<GenericTableSchema, Set<Long>> getLinkSpeedColumn();
    @TypedColumn(name="link_speed", method=MethodType.SETDATA, fromVersion="1.0.6")
    void setLinkSpeed(Set<Long> linkSpeed);

    @TypedColumn(name="duplex", method=MethodType.GETCOLUMN, fromVersion="1.0.6")
    Column<GenericTableSchema, Set<String>> getDuplexColumn();
    @TypedColumn(name="duplex", method=MethodType.SETDATA, fromVersion="1.0.6")
    void setDuplex(Set<Long> duplex);

    @TypedColumn(name="mtu", method=MethodType.GETCOLUMN, fromVersion="1.0.6")
    Column<GenericTableSchema, Set<Long>> getMtuColumn();
    @TypedColumn(name="mtu", method=MethodType.SETDATA, fromVersion="1.0.6")
    void setMtu(Set<Long> mtu);

    @TypedColumn(name="error", method=MethodType.GETCOLUMN, fromVersion="7.7.0")
    Column<GenericTableSchema, Set<String>> getErrorColumn();
    @TypedColumn(name="error", method=MethodType.SETDATA, fromVersion="7.7.0")
    void setError(Set<String> error);

}
