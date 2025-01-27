/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.utils.mdsal.utils;

import java.util.ArrayList;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

public class TransactionHistory extends ArrayList<TransactionElement> {
    private static final long serialVersionUID = 1L;

    private final int capacity;
    private final int watermark;

    public TransactionHistory(final int initialCapacity, final int watermark) {
        super(initialCapacity);
        capacity = initialCapacity;
        this.watermark = watermark;
    }

    public void addToHistory(final TransactionType updateType, final Object object) {
        add(new TransactionElement(updateType, object));
    }

    public <T extends DataObject> void addToHistory(final TransactionType updateType, final DataObjectIdentifier<T> iid,
            final T dataObject) {
        add(new TransactionElement(updateType, new MdsalObject<>(iid, dataObject)));
    }

    @Override
    public boolean add(final TransactionElement element) {
        if (size() >= watermark) {
            removeRange(0, capacity - watermark);
        }
        return super.add(element);
    }

    public ArrayList<TransactionElement> getElements() {
        return new ArrayList<>(this);
    }

    @Override
    protected void removeRange(final int fromIndex, final int toIndex) {
        super.removeRange(fromIndex, toIndex);
    }
}
