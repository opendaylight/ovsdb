/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.ovsdb.southbound.OvsdbSchemaContants.OvsdbSchemaTables;

public class OvsdbSchemaContantsTest {

    private OvsdbSchemaContants ovsdbSchemaContants;

    @Before
    public void setUp() {
        ovsdbSchemaContants = new OvsdbSchemaContants();
    }

    @Test
    public void testDatabaseName() {
        assertEquals("Error databaseName did not return correct value","Open_vSwitch",OvsdbSchemaContants.databaseName);
    }
    @Test
    public void testGetTableName() {
        assertEquals("Error getTableName() did not return correct value", "Open_vSwitch", OvsdbSchemaTables.OPENVSWITCH.getTableName());
    }

    @Test
    public void testGetParentTableName() {
        assertEquals("Error getTableName() did not return correct value", null, OvsdbSchemaTables.OPENVSWITCH.getParentTableName());
    }

    @Test
    public void testGetColumnNameInParentTable() {
        assertEquals("Error getTableName() did not return correct value", null, OvsdbSchemaTables.OPENVSWITCH.getColumnNameInParentTable());
    }
}
