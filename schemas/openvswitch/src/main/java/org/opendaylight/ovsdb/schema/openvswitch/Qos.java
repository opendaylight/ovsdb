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
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.MethodType;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedColumn;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;

import java.util.Map;
import java.util.Set;

/**
 *  This class is a typed interface to the QoS Table
 */
@TypedTable (name="QoS", database="Open_vSwitch", fromVersion="1.0.0")
public interface Qos extends TypedBaseTable<GenericTableSchema> {

    @TypedColumn (name="queues", method= MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Map<Long, UUID>> getQueuesColumn() ;

    @TypedColumn (name="queues", method= MethodType.SETDATA, fromVersion="1.0.0")
    void setQueues(Map<Long, UUID> queues) ;

    @TypedColumn (name="type", method= MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<String>> getTypeColumn() ;

    @TypedColumn (name="type", method= MethodType.SETDATA, fromVersion="1.0.0")
    void setType(Set<String> type) ;

    @TypedColumn (name="other_config", method= MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Map<String, String>> getOtherConfigColumn() ;

    @TypedColumn (name="other_config", method= MethodType.SETDATA, fromVersion="1.0.0")
    void setOtherConfig(Map<String, String> otherConfig) ;

    @TypedColumn (name="external_ids", method= MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Map<String, String>> getExternalIdsColumn() ;

    @TypedColumn (name="external_ids", method= MethodType.SETDATA, fromVersion="1.0.0")
    void setExternalIds(Map<String, String> externalIds) ;
}