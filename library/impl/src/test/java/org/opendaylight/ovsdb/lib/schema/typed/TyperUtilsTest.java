/*
 * Copyright © 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema.typed;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import java.util.Collections;
import org.junit.Test;
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchemaImpl;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;

/**
 * Test class for {@link TyperUtils}.
 */
public class TyperUtilsTest {
    @TypedTable(name = "TestTypedTable", database = "Open_vSwitch")
    private static final class TestTypedTable {

    }

    private static final class TestUntypedTable {

    }

    /**
     * Test that {@link TyperUtils#getTableSchema(DatabaseSchema, Class)} returns the appropriate schema when given a
     * table containing the appropriate schema, for a typed table (annotated).
     */
    @Test
    public void testGetTableSchemaWithIncludedTypedTable() {
        // Given ...
        GenericTableSchema testTableSchema = new GenericTableSchema("TestTypedTable");
        DatabaseSchema dbSchema = new DatabaseSchemaImpl("testDb", Version.NULL,
            ImmutableMap.of(testTableSchema.getName(), testTableSchema));

        // When ...
        GenericTableSchema tableSchema = TyperUtils.getTableSchema(dbSchema, TestTypedTable.class);

        // Then ...
        assertEquals(testTableSchema, tableSchema);
    }

    /**
     * Test that {@link TyperUtils#getTableSchema(DatabaseSchema, Class)} returns the appropriate schema when given a
     * table containing the appropriate schema, for an untyped table (non-annotated).
     */
    @Test
    public void testGetTableSchemaWithIncludedUntypedTable() {
        // Given ...
        GenericTableSchema testTableSchema = new GenericTableSchema("TestUntypedTable");
        DatabaseSchema dbSchema = new DatabaseSchemaImpl("testDb", Version.NULL,
            ImmutableMap.of(testTableSchema.getName(), testTableSchema));

        // When ...
        GenericTableSchema tableSchema = TyperUtils.getTableSchema(dbSchema, TestUntypedTable.class);

        // Then ...
        assertEquals(testTableSchema, tableSchema);
    }

    /**
     * Test that {@link TyperUtils#getTableSchema(DatabaseSchema, Class)} throws an {@link IllegalArgumentException}
     * when the appropriate table schema isn't present in the database schema (for a typed table).
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetTableSchemaWithoutIncludedTypedTable() {
        // Given ...
        DatabaseSchema dbSchema = new DatabaseSchemaImpl("testDb", Version.NULL, Collections.emptyMap());

        // When ...
        TyperUtils.getTableSchema(dbSchema, TestTypedTable.class);
    }

    /**
     * Test that {@link TyperUtils#getTableSchema(DatabaseSchema, Class)} throws an {@link IllegalArgumentException}
     * when the appropriate table schema isn't present in the database schema (for an untyped table).
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetTableSchemaWithoutIncludedUntypedTable() {
        // Given ...
        DatabaseSchema dbSchema = new DatabaseSchemaImpl("testDb", Version.NULL, Collections.emptyMap());

        // When ...
        TyperUtils.getTableSchema(dbSchema, TestUntypedTable.class);
    }

    /**
     * Test that {@link TyperUtils#checkVersion(Version, Range)} detects an old version. (The aim here isn't
     * to test {@link Version#compareTo(Version)}, that should be done in
     * {@link org.opendaylight.ovsdb.lib.notation.VersionTest}).
     */
    @Test(expected = SchemaVersionMismatchException.class)
    public void testCheckOldVersionFails() throws SchemaVersionMismatchException {
        callCheckVersion(new Version(1, 0, 0), new Version(1, 1, 0), Version.NULL);
    }

    /**
     * Test that {@link TyperUtils#checkVersion(Version, Range)} detects a new version.
     */
    @Test(expected = SchemaVersionMismatchException.class)
    public void testCheckNewVersionFails() throws SchemaVersionMismatchException {
        callCheckVersion(new Version(2, 0, 0), Version.NULL, new Version(1, 1, 0));
    }

    /**
     * Test that {@link TyperUtils#checkVersion(Version, Range)} accepts null boundaries.
     */
    @Test
    public void testCheckNullVersionsSucceed() throws SchemaVersionMismatchException {
        callCheckVersion(new Version(2, 0, 0), Version.NULL, Version.NULL);
        // This check succeeds in the absence of an exception
    }

    /**
     * Test that {@link TyperUtils#checkVersion(Version, Range)} accepts the lower boundary version.
     */
    @Test
    public void testCheckLowerVersionBoundarySucceeds() throws SchemaVersionMismatchException {
        callCheckVersion(new Version(2, 0, 0), new Version(2, 0, 0), Version.NULL);
        // This check succeeds in the absence of an exception
    }

    /**
     * Test that {@link TyperUtils#checkVersion(Version, Range)} accepts the upper boundary version.
     */
    @Test
    public void testCheckUpperVersionBoundarySucceeds() throws SchemaVersionMismatchException {
        callCheckVersion(new Version(2, 0, 0), Version.NULL, new Version(2, 0, 0));
        // This check succeeds in the absence of an exception
    }

    /**
     * Test that {@link TyperUtils#checkVersion(Version, Range)} accepts both boundary versions.
     */
    @Test
    public void testCheckSingleVersionBoundarySucceeds() throws SchemaVersionMismatchException {
        callCheckVersion(new Version(2, 0, 0), new Version(2, 0, 0), new Version(2, 0, 0));
        // This check succeeds in the absence of an exception
    }

    /**
     * Test that {@link TyperUtils#checkVersion(Version, Range)} accepts a version within the boundaries
     * (strictly).
     */
    @Test
    public void testCheckVersionWithinBoundariesSucceeds() throws SchemaVersionMismatchException {
        callCheckVersion(new Version(1, 1, 0), new Version(1, 0, 0), new Version(2, 0, 0));
        // This check succeeds in the absence of an exception
    }

    /**
     * Call {@link TyperUtils#checkVersion(Version, Range)}.
     *
     * @param schema The schema version (to be checked).
     * @param from The minimum supported version.
     * @param to The maximum supported version.
     * @throws SchemaVersionMismatchException if the schema version isn't supported.
     */
    private static void callCheckVersion(final Version schema, final Version from, final Version to) {
        TyperUtils.checkVersion(schema, Version.createRangeOf(from, to));
    }
}
