/*
 * Copyright (c) 2013, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.jsonrpc;

import com.google.common.collect.Lists;

import java.util.List;

public class JsonRpc10Request {

    String id;
    String method;
    List<Object> params = Lists.newArrayList();

    public JsonRpc10Request(String id) {
        setId(id);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public List<Object> getParams() {
        return params;
    }

    public void setParams(List<Object> params) {
        this.params = params;
    }

    public void setParams(Object[] pararms) {
        this.params = Lists.newArrayList(pararms);
    }

    @Override
    public String toString() {
        return "JsonRpc10Request [id=" + id + ", method=" + method
                + ", params=" + params + "]";
    }
}
