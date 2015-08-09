/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.operations;


public class Assert extends Operation {

    public static final String ASSERT = "assert";
    String lock;

    public Assert(String lock) {
        super(null, ASSERT);
        this.lock = lock;
    }

    public String getLock() {
        return lock;
    }
}
