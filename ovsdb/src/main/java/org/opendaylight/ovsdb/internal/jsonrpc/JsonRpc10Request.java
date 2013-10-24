package org.opendaylight.ovsdb.internal.jsonrpc;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.UUID;

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
