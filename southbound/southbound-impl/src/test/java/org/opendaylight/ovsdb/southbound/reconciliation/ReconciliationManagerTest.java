/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.reconciliation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ReconciliationManagerTest {
    @Test
    void formatBridgeListTest() {
        assertEquals("[]", ReconciliationManager.formatBridgeList(List.of()));
        assertEquals("""
            [""]""", ReconciliationManager.formatBridgeList(List.of("")));
        assertEquals("""
            ["a"]""", ReconciliationManager.formatBridgeList(List.of("a")));
        assertEquals("""
            ["a", "b"]""", ReconciliationManager.formatBridgeList(List.of("a", "b")));
        assertEquals("""
            ["a b"]""", ReconciliationManager.formatBridgeList(List.of("a b")));
        assertEquals("""
            ["\\""]""", ReconciliationManager.formatBridgeList(List.of("\"")));
    }
}
