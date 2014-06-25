/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */

package org.opendaylight.ovsdb.lib;

/**
 * Applications interested in Passive ovsdb connection events should implement this interface.
 */
public interface OvsDBConnectionListener {
    /**
     * Event thrown to the connection listener when a new Passive connection is established.
     * @param OvsDBClient that represents the connection.
     */
    public void connected(OvsDBClient client);

    /**
     * Event thrown to the connection listener when an existing connection is terminated.
     * @param OvsDBClient that represents the connection.
     */
    public void disconnected(OvsDBClient client);
}
