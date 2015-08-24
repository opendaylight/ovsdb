/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib;

/**
 * Applications interested in Passive ovsdb connection events should implement this interface.
 */
public interface OvsdbConnectionListener {
    /**
     * Event thrown to the connection listener when a new Passive connection is established.
     * @param client OvsdbClient that represents the connection.
     */
    void connected(OvsdbClient client);

    /**
     * Event thrown to the connection listener when an existing connection is terminated.
     * @param client OvsdbClient that represents the connection.
     */
    void disconnected(OvsdbClient client);
}
