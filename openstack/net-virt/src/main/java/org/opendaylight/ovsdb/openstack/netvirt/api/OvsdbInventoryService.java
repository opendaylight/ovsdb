/*
 * Copyright (c) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.api;

/**
 * OvsdbInventoryService is the interface to the southbound for netvirt.
 *
 * @author Sam Hague (shague@redhat.com)
 */
public interface OvsdbInventoryService {
    void listenerAdded(OvsdbInventoryListener listener);
    void listenerRemoved(OvsdbInventoryListener listener);
    void providersReady();
}
