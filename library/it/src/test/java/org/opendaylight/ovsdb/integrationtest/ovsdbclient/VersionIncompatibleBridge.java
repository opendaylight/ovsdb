/*
 * Copyright (C) 2014 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */

package org.opendaylight.ovsdb.integrationtest.ovsdbclient;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;

/**
 * VersionIncompatibleBridge is used to test the Version Compatibility logic in the Library
 * with an absurdly low fromVersion and untilVersion which will fail for all the OVS versions.
 */
@TypedTable(name="Bridge", database="Open_vSwitch", fromVersion="0.0.1", untilVersion="0.0.2")
public interface VersionIncompatibleBridge extends TypedBaseTable {
}
