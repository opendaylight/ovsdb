/*
 * Copyright (c) 2014, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib;

import java.io.Serializable;

public class MonitorHandle implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;

    public MonitorHandle(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
