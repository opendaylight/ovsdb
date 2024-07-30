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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class TransactionHistory extends ArrayList<TransactionElement> {
    private static final long serialVersionUID = 1L;

    private final int capacity;
    private final int watermark;

    public TransactionHistory(int initialCapacity, int watermark) {
        super(initialCapacity);
        this.capacity = initialCapacity;
        this.watermark = watermark;
    }

    public void addToHistory(TransactionType updateType, Object object) {
        add(new TransactionElement(updateType, object));
    }

    public void addToHistory(TransactionType updateType, InstanceIdentifier iid, DataObject dataObject) {
        add(new TransactionElement(updateType, new MdsalObject(iid, dataObject)));
    }

    @Override
    public boolean add(TransactionElement element) {
        if (size() >= watermark) {
            removeRange(0, capacity - watermark);
        }
        return super.add(element);
    }

    public ArrayList<TransactionElement> getElements() {
        return new ArrayList<>(this);
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        super.removeRange(fromIndex, toIndex);
    }
}
