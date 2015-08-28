package org.opendaylight.ovsdb.southbound;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.southbound.transactions.md.OvsdbOperationalCommandAggregator;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionInvoker;

@RunWith(MockitoJUnitRunner.class)

public class OvsdbMonitorCallbackTest {
    @InjectMocks private OvsdbMonitorCallback ovsdbMonitorCallback = mock(OvsdbMonitorCallback.class, Mockito.CALLS_REAL_METHODS);
    @Mock private TransactionInvoker txInvoker;
    @Mock private OvsdbConnectionInstance key;

    @Test
    public void testUpdate() {
        ovsdbMonitorCallback.update(mock(TableUpdates.class), mock(DatabaseSchema.class));
        verify(txInvoker).invoke(any(OvsdbOperationalCommandAggregator.class));
    }

    @Test
    public void testException() {
        ovsdbMonitorCallback.exception(mock(Throwable.class));
        verify(ovsdbMonitorCallback).exception(any(Throwable.class));
    }
}
