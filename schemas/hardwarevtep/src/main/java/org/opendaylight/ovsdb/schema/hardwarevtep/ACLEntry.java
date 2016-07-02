/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.schema.hardwarevtep;

import java.util.Map;
import java.util.Set;

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.MethodType;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedColumn;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;

@TypedTable(name="ACL_entry", database="hardware_vtep", fromVersion="1.4.0")
public interface ACLEntry extends TypedBaseTable<GenericTableSchema> {
    @TypedColumn(name="sequence", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, Long> getSequenceColumn();

    @TypedColumn(name="sequence", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setSequence(Long sequence);


    @TypedColumn(name="source_mac", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, Set<String>> getSourceMacColumn();

    @TypedColumn(name="source_mac", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setSourceMac(Set<String> sourceMac);


    @TypedColumn(name="dest_mac", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, Set<String>> getDestMacColumn();

    @TypedColumn(name="dest_mac", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setDestMac(Set<String> destMac);


    @TypedColumn(name="ethertype", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, Set<String>> getEtherTypeColumn();

    @TypedColumn(name="ethertype", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setEtherType(Set<String> etherType);


    @TypedColumn(name="source_ip", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, Set<String>> getSourceIpColumn();

    @TypedColumn(name="source_ip", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setSourceIp(Set<String> sourceIp);


    @TypedColumn(name="source_mask", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, Set<String>> getSourceMaskColumn();

    @TypedColumn(name="source_mask", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setSourceMask(Set<String> sourceMask);


    @TypedColumn(name="dest_ip", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, Set<String>> getDestIpColumn();

    @TypedColumn(name="dest_ip", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setDestIp(Set<String> destIp);


    @TypedColumn(name="dest_mask", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, Set<String>> getDestMaskColumn();

    @TypedColumn(name="dest_mask", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setDestMask(Set<String> destMask);


    @TypedColumn(name="protocol", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, Set<Long>> getProtocolColumn();

    @TypedColumn(name="protocol", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setProtocol(Set<Long> protocol);


    @TypedColumn(name="source_port_min", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, Set<Long>> getSourcePortMinColumn();

    @TypedColumn(name="source_port_min", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setSourcePortMin(Set<Long> sourcePortMin);


    @TypedColumn(name="source_port_max", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, Set<Long>> getSourcePortMaxColumn();

    @TypedColumn(name="source_port_max", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setSourcePortMax(Set<Long> sourcePortMax);


    @TypedColumn(name="dest_port_min", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, Set<Long>> getDestPortMinColumn();

    @TypedColumn(name="dest_port_min", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setDestPortMin(Set<Long> destPortMin);


    @TypedColumn(name="dest_port_max", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, Set<Long>> getDestPortMaxColumn();

    @TypedColumn(name="dest_port_max", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setDestPortMax(Set<Long> destPortMax);


    @TypedColumn(name="tcp_flags", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, Set<Long>> getTcpFlagsColumn();

    @TypedColumn(name="tcp_flags", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setTcpFlags(Set<Long> tcpFlags);


    @TypedColumn(name="tcp_flags_mask", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, Set<Long>> getTcpFlagsMaskColumn();

    @TypedColumn(name="tcp_flags_mask", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setTcpFlagsMask(Set<Long> tcpFlagsMask);


    @TypedColumn(name="icmp_code", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, Set<Long>> getIcmpCodeColumn();

    @TypedColumn(name="icmp_code", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setIcmpCode(Set<Long> icmpCode);


    @TypedColumn(name="icmp_type", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, Set<Long>> getIcmpTypeColumn();

    @TypedColumn(name="icmp_type", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setIcmpType(Set<Long> icmpType);


    @TypedColumn(name="direction", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, Set<String>> getDirectionColumn();

    @TypedColumn(name="direction", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setDirection(Set<String> direction);


    @TypedColumn(name="action", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, Set<String>> getActionColumn();

    @TypedColumn(name="action", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setAction(Set<String> action);

    @TypedColumn(name="acle_fault_status", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, Map<String, String>> getAcleFaultStatusColumn();

    @TypedColumn(name="acle_fault_status", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setAcleFaultStatus(Map<String, String> acleFaultStatus);
}
