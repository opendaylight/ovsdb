/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */
package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13;

public abstract class AbstractServiceInstance {
    Service service;
    public AbstractServiceInstance (Service service) {
        this.service = service;
    }

    public int getTable() {
        return service.getTable();
    }
}
