/*
 * Copyright (c) 2015 - 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.utils.mdsal.utils;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ActionableResourceImpl implements ActionableResource {
    private Object instance;
    private Object oldInstance;
    private String key;
    private InstanceIdentifier identifier;
    private short action;
    private List<ActionableResource> modifications = new ArrayList<>();
    private SettableFuture ft = SettableFuture.create();

    public ActionableResourceImpl(String key) {
        this.key = key;
    }

    public ActionableResourceImpl(InstanceIdentifier identifier, short action, Object updatedData, Object oldData) {
        this.instance = updatedData;
        this.oldInstance = oldData;
        this.identifier = identifier;
        this.action = action;
    }

    public ActionableResourceImpl(String key, InstanceIdentifier identifier, short action, Object updatedData,
                                  Object oldData) {
        this(identifier, action, updatedData, oldData);
        this.key = key;
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }

    public Object getInstance() {
        return this.instance;
    }

    public void setOldInstance(Object oldInstance) {
        this.oldInstance = oldInstance;
    }

    public Object getOldInstance() {
        return this.oldInstance;
    }

    public void setInstanceIdentifier(InstanceIdentifier instanceIdentifier) {
        this.identifier = instanceIdentifier;
    }

    public InstanceIdentifier getInstanceIdentifier() {
        return this.identifier;
    }

    public void setAction(short action) {
        this.action = action;
    }

    public short getAction() {
        return action;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public ListenableFuture<Void> getResultFt() {
        return ft;
    }

    public String getKey() {
        return this.identifier != null ? this.identifier.toString() : this.key;
    }

    public List<ActionableResource> getModifications() {
        return modifications;
    }

    public void setModifications(List<ActionableResource> modifications) {
        this.modifications = modifications;
    }
}