/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.ovssfc;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;

public class SfcEvent {
    protected ServiceFunctionPath serviceFunctionPath;
    protected ServiceFunctionPaths serviceFunctionPaths;
    private Type type;
    private Action action;
    public enum Type { SFP, SFPS };
    public enum Action { CREATE, UPDATE, DELETE };

    public SfcEvent (Type type) {
        this.type = type;
    }

    public SfcEvent (Type type, Action action, ServiceFunctionPath serviceFunctionPath) {
        this.type = type;
        this.action = action;
        this.serviceFunctionPath = serviceFunctionPath;
    }

    public SfcEvent (Type type, Action action, ServiceFunctionPaths serviceFunctionPaths) {
        this.type = type;
        this.action = action;
        this.serviceFunctionPaths = serviceFunctionPaths;
    }

    public Type getType () {
        return type;
    }

    public void setType (Type type) {
        this.type = type;
    }

    public Action getAction () {
        return action;
    }

    public void setAction (Action action) {
        this.action = action;
    }

    @Override
    public String toString () {
        switch (type) {
        case SFP:
            return "SfcEvent{" +
                    "type=" + type + ", action=" + action + ", " +
                    "ServiceFunctionPath=" + serviceFunctionPath +
                    '}';
        case SFPS:
            return "SfcEvent{" +
                    "type=" + type + ", action=" + action + ", " +
                    "ServiceFunctionPaths=" + serviceFunctionPaths +
                    '}';
        default:
            return "undefined";
        }
    }
}
