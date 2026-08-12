/*
 * Copyright © 2013, 2017 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.jsonrpc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JsonRpc10Request {

    String id;
    String method;
    List<Object> params = new ArrayList<>();

    public JsonRpc10Request(final String id) {
        setId(id);
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(final String method) {
        this.method = method;
    }

    public List<Object> getParams() {
        return params;
    }

    public void setParams(final List<Object> params) {
        this.params = params;
    }

    public void setParams(final Object[] pararms) {
        params = new ArrayList<>(Arrays.asList(pararms));
    }

    @Override
    public String toString() {
        return "JsonRpc10Request [id=" + id + ", method=" + method + ", params=" + params + "]";
    }
}
