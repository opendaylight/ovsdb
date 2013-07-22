package org.opendaylight.ovsdb.internal;

import java.util.ArrayList;

public class MutateRequest {
    public String op = "mutate";
    public String table;
    public ArrayList<Object> where;
    public ArrayList<Object> mutations;

    public MutateRequest(String table, ArrayList<Object> where, ArrayList<Object> mutations){
        this.table = table;
        this.where = where;
        this.mutations = mutations;
    }
}
