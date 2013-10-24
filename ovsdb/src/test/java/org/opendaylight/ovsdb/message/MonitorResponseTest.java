package org.opendaylight.ovsdb.message;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import com.google.common.io.InputSupplier;
import com.google.common.io.Resources;

import junit.framework.TestCase;

import org.opendaylight.ovsdb.datatype.OvsDBMap;
import org.opendaylight.ovsdb.datatype.OvsDBSet;
import org.opendaylight.ovsdb.datatype.UUID;
import org.opendaylight.ovsdb.table.Bridge;
import org.opendaylight.ovsdb.table.Interface;
import org.opendaylight.ovsdb.table.Port;
import org.opendaylight.ovsdb.table.internal.Table;
import org.sonatype.inject.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MonitorResponseTest extends TestCase {

    public void testDeser() throws IOException {
        URL resource = Resources.getResource(MonitorResponseTest.class, "monitor_response1.json");
        InputSupplier<InputStream> inputStreamInputSupplier = Resources.newInputStreamSupplier(resource);
        InputStream input = inputStreamInputSupplier.getInput();
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.ANY);
        TableUpdates updates = mapper.readValue(input, TableUpdates.class);

        Set<Table.Name> available = updates.availableUpdates();
        for (Table.Name name : available) {
            if (Bridge.NAME.equals(name)) {
                verifyBridge(updates);
            } else if (Port.NAME.equals(name)) {
                veriftyPort(updates);
            } else if (Interface.NAME.equals(name)) {
                verifyInterface(updates);
            }
        }
    }

    private void verifyInterface(TableUpdates updates) {
        TableUpdate<Interface> update = updates.getUpdate(Interface.NAME);
        for (TableUpdate.Row<Interface> interfaceRow : update.getRows()) {
            System.out.println("interfaceRow = " + interfaceRow);
            Interface aNew = interfaceRow.getNew();
            if (null != aNew) {
                OvsDBMap<String, String> options = aNew.getOptions();
                if (options != null) {
                    for (Map.Entry<String, String> optE : options.entrySet()) {
                        System.out.println("optE.getKey() = " + optE.getKey());
                        System.out.println("optE.getValue() = " + optE.getValue());
                    }
                }
            }
        }
    }

    private void verifyBridge(TableUpdates updates) {
        TableUpdate<Bridge> update = updates.getUpdate(Bridge.NAME);
        for (TableUpdate.Row<Bridge> row : update.getRows()) {
            assertEquals("788de61c-0e4f-43d8-a068-259e75aabbba", row.getId());
            Bridge bridge = row.getNew();
            assertNotNull(bridge);
            OvsDBSet<UUID> ports = bridge.getPorts();
            assertEquals(2, ports.size());
            List<UUID> uuids =  Ordering.usingToString().sortedCopy(ports);
            assertEquals("f6018e7a-7ca5-4e72-a744-a9b434f47011", uuids.get(0).toString());
            assertEquals("fe3c89fd-2ff3-44d8-9f27-f9c7ac2a693d", uuids.get(1).toString());
            bridge = row.getOld();
            assertNull(bridge);
        }
    }

    private void veriftyPort(TableUpdates updates) {
        TableUpdate<Port> update = updates.getUpdate(Port.NAME);
        Collection<TableUpdate.Row<Port>> rows = update.getRows();
        assertEquals(2, rows.size());
        List<TableUpdate.Row<Port>> sorted = Ordering.natural().onResultOf(new Function<TableUpdate.Row<Port>, String>() {
            @Override
            public String apply(@Nullable org.opendaylight.ovsdb.message.TableUpdate.Row<Port> input) {
                return input.getId();
            }
        }).sortedCopy(rows);

        TableUpdate.Row<Port> portRow = sorted.get(0);
        assertEquals("f6018e7a-7ca5-4e72-a744-a9b434f47011", portRow.getId());
        Port port = portRow.getNew();
        assertNotNull(port);
        List<UUID> interfaces = Ordering.usingToString().sortedCopy(port.getInterfaces());
        assertEquals("13548b08-dca3-4d4b-9e9b-f50c237dcb9e", interfaces.get(0).toString());
        port = portRow.getOld();
        assertNull(port);

        portRow = sorted.get(1);
        assertEquals("fe3c89fd-2ff3-44d8-9f27-f9c7ac2a693d", portRow.getId());
        port = portRow.getNew();
        assertNotNull(port);
        interfaces = Ordering.usingToString().sortedCopy(port.getInterfaces());
        assertEquals("88ae29fb-8c91-41a9-a14f-a74126e790c0", interfaces.get(0).toString());
        port = portRow.getOld();
        assertNull(port);
    }

}
