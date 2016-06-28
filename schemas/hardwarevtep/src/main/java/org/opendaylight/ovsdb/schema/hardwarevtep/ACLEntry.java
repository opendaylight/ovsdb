/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.schema.hardwarevtep;

import java.util.Map;
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
    Column<GenericTableSchema, String> getSourceMacColumn();

    @TypedColumn(name="source_mac", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setSourceMac(String sourceMac);


    @TypedColumn(name="dest_mac", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, String> getDestMacColumn();

    @TypedColumn(name="dest_mac", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setDestMac(String destMac);


    @TypedColumn(name="ether_type", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, String> getEtherTypeColumn();

    @TypedColumn(name="ether_type", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setEtherType(String etherType);


    @TypedColumn(name="source_ip", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, String> getSourceIpColumn();

    @TypedColumn(name="source_ip", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setSourceIp(String sourceIp);


    @TypedColumn(name="source_mask", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, String> getSourceMaskColumn();

    @TypedColumn(name="source_mask", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setSourceMask(String sourceMask);


    @TypedColumn(name="dest_ip", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, String> getDestIpColumn();

    @TypedColumn(name="dest_ip", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setDestIp(String destIp);


    @TypedColumn(name="dest_mask", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, String> getDestMaskColumn();

    @TypedColumn(name="dest_mask", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setDestMask(String destMask);


    @TypedColumn(name="protocol", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, Long> getProtocolColumn();

    @TypedColumn(name="protocol", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setProtocol(Long protocol);


    @TypedColumn(name="source_port_min", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, Long> getSourcePortMinColumn();

    @TypedColumn(name="source_port_min", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setSourcePortMin(Long sourcePortMin);


    @TypedColumn(name="source_port_max", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, Long> getSourcePortMaxColumn();

    @TypedColumn(name="source_port_max", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setSourcePortMax(Long sourcePortMax);

    
    @TypedColumn(name="dest_port_min", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, Long> getDestPortMinColumn();

    @TypedColumn(name="dest_port_min", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setDestPortMin(Long destPortMin);


    @TypedColumn(name="dest_port_max", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, Long> getDestPortMaxColumn();

    @TypedColumn(name="dest_port_max", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setDestPortMax(Long destPortMax);


    @TypedColumn(name="tcp_flags", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, Long> getTcpFlagsColumn();

    @TypedColumn(name="tcp-flags", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setTcpFlags(Long tcpFlags);


    @TypedColumn(name="tcp_flags_mask", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, Long> getTcpFlagsMaskColumn();

    @TypedColumn(name="tcp_flags_mask", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setTcpFlagsMask(Long tcpFlagsMask);


    @TypedColumn(name="icmp_code", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, Long> getIcmpCodeColumn();

    @TypedColumn(name="icmp_code", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setIcmpCode(Long icmpCode);


    @TypedColumn(name="icmp_type", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, Long> getIcmpTypeColumn();

    @TypedColumn(name="icmp_type", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setIcmpType(Long icmpType);


    @TypedColumn(name="direction", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, String> getDirectionColumn();

    @TypedColumn(name="direction", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setDirection(String direction);


    @TypedColumn(name="action", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, String> getActionColumn();

    @TypedColumn(name="action", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setAction(String action);

    @TypedColumn(name="acle_fault_status", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<GenericTableSchema, Map<String, String>> getAcleFaultStatusColumn();

    @TypedColumn(name="acle_fault_status", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setAcleFaultStatus(Map<String, String> acleFaultStatus);
}
