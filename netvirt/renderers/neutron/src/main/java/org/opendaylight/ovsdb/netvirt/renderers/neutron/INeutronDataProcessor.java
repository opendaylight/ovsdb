/*
 * Copyright (c) 2013, 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.netvirt.renderers.neutron;

import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * netvirt neutron data processor
 * org.opendaylight.ovsdb.netvirt
 */
public interface INeutronDataProcessor<D extends DataObject> {

    /**
     * Method removes DataObject which is identified by InstanceIdentifier.
     *
     * @param identifier - the whole path to DataObject
     * @param del - DataObject for removing
     */
    void remove(InstanceIdentifier<D> identifier, D del);

    /**
     * Method updates the original DataObject to the update DataObject.
     * Both are identified by same InstanceIdentifier.
     *
     * @param identifier - the whole path to DataObject
     * @param original - original DataObject (for update)
     * @param update - changed DataObject (contain updates)
     */
    void update(InstanceIdentifier<D> identifier, D original, D update);

    /**
     * Method adds the DataObject which is identified by InstanceIdentifier
     * to device.
     *
     * @param identifier - the whole path to new DataObject
     * @param add - new DataObject
     */
    void add(InstanceIdentifier<D> identifier, D add);

}
