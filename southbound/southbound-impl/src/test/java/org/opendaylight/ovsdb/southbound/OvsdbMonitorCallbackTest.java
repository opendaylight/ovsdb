/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.southbound.transactions.md.OvsdbOperationalCommandAggregator;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionInvoker;


@RunWith(MockitoJUnitRunner.class)

public class OvsdbMonitorCallbackTest {

    @InjectMocks
    OvsdbMonitorCallback ovsdbMonitorCallback = mock(OvsdbMonitorCallback.class, Mockito.CALLS_REAL_METHODS);

    @Mock
    private TransactionInvoker txInvoker;
    @Mock
    private OvsdbConnectionInstance key;
    @Mock
    private AtomicBoolean intialUpdate;

    @Test
    public void testUpdate() {
        DatabaseSchema dbSchema = mock(DatabaseSchema.class);
        when(dbSchema.getVersion())
                .thenReturn(Version.fromString(SouthboundConstants.AUTOATTACH_SUPPORTED_OVS_SCHEMA_VERSION));
        ovsdbMonitorCallback.update(mock(TableUpdates.class), dbSchema);
        verify(txInvoker).invoke(any(OvsdbOperationalCommandAggregator.class));
    }

    @Test
    public void testException() {
        ovsdbMonitorCallback.exception(mock(Throwable.class));
        verify(ovsdbMonitorCallback).exception(any(Throwable.class));
    }
}
