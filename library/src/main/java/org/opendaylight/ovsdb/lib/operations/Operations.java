/*
 *
 *  * Copyright (C) 2014 EBay Software Foundation
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *  *
 *  * Authors : Ashwin Raveendran
 *
 */

package org.opendaylight.ovsdb.lib.operations;

import org.opendaylight.ovsdb.lib.schema.TableSchema;

public class Operations {
    public static Operations op = new Operations();

    public <E extends TableSchema<E>> Insert<E> insert(TableSchema<E> schema) {
        return new Insert<>(schema);
    }

    public  <E extends TableSchema<E>> Update<E> update(TableSchema<E> schema) {
        return new Update<>(schema);
    }


}
