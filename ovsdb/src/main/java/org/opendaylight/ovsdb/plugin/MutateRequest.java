package org.opendaylight.ovsdb.plugin;

import java.util.ArrayList;
import java.util.List;

public class MutateRequest {
    public String op = "mutate";
    public String table;
    public List<Object> where;
    public List<Object> mutations;

    public MutateRequest(String table, List<Object> where, List<Object> mutations){
        this.table = table;
        this.where = where;
        this.mutations = mutations;
    }
}
