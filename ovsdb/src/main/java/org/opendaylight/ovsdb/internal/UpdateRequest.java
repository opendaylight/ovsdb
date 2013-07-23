package org.opendaylight.ovsdb.internal;


import java.util.ArrayList;
import java.util.Map;

public class UpdateRequest {
    public String op;
    public String table;
    public ArrayList<Object> where;
    public Map<String, Object> row;

    public UpdateRequest(String op, String table, ArrayList<Object> where, Map<String, Object> row){
        this.op = op;
        this.table = table;
        this.where = where;
        this.row = row;
    }
}
