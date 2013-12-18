/*
 * Copyright (C) 2013 Ebay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran
 */
package org.opendaylight.ovsdb.lib.table.internal;

import org.opendaylight.ovsdb.lib.table.*;

import java.util.ArrayList;
import java.util.List;

public class Tables {
    public static List<Table> tables = new ArrayList<Table>();

    static {
        tables.add(new Bridge());
        tables.add(new Port());
        tables.add(new Capability());
        tables.add(new Interface());
        tables.add(new Controller());
        tables.add(new Manager());
        tables.add(new Mirror());
        tables.add(new NetFlow());
        tables.add(new Open_vSwitch());
        tables.add(new Qos());
        tables.add(new Queue());
        tables.add(new SFlow());
        tables.add(new SSL());
        tables.add(new Flow_Sample_Collector_Set());
        tables.add(new Flow_Table());
        tables.add(new IPFIX());
    }

    public static List<Table> getTables() {
        return tables;
    }
}
