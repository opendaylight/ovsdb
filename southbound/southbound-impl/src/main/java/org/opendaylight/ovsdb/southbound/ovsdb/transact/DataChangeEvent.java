/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import java.util.Map;
import java.util.Set;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

public interface DataChangeEvent {
    /**
     * Returns a map of paths and newly created objects, which were introduced by
     * this change into conceptual data tree, if no new objects were introduced
     * this map will be empty.
     *
     * @return map of paths and newly created objects
     */
    Map<DataObjectIdentifier<?>, DataObject> getCreatedData();

    /**
     * Returns a map of paths and objects which were updated by this change in the
     * conceptual data tree if no existing objects were updated
     * this map will be empty.
     *
     * @return map of paths and newly created objects
     */
    Map<DataObjectIdentifier<?>, DataObject> getUpdatedData();

    /**
     * Returns an immutable set of removed paths.
     *
     * @return set of removed paths
     */
    Set<DataObjectIdentifier<?>> getRemovedPaths();

    /**
     * Returns an immutable map of updated or removed paths and their original
     * states prior to this change.
     *
     * @return map of paths and original state of updated and removed objects.
     */
    Map<DataObjectIdentifier<?>, DataObject> getOriginalData();
}
