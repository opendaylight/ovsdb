package org.opendaylight.ovsdb;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.ovsdb.lib.message.OvsdbRPC;
import org.opendaylight.ovsdb.lib.message.TransactBuilder;
import org.opendaylight.ovsdb.lib.message.operations.ConditionalOperation;
import org.opendaylight.ovsdb.lib.message.operations.Operation;
import org.opendaylight.ovsdb.lib.message.operations.OperationResult;
import org.opendaylight.ovsdb.lib.meta.ColumnSchema;
import org.opendaylight.ovsdb.lib.meta.DatabaseSchema;
import org.opendaylight.ovsdb.lib.meta.TableSchema;
import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.notation.Function;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;

/**
 * @author araveendrann
 */
public class OpenVswitch {

    ExecutorService executorService;
    String schemaName;
    OvsdbRPC rpc;
    volatile DatabaseSchema schema;
    Queue<Throwable> exceptions;

    public OpenVswitch(OvsdbRPC rpc, ExecutorService executorService) {
        this.rpc = rpc;
        this.executorService = executorService;
    }

    public OpenVswitch() {
    }


    public void populateSchemaFromDevice() {
        final ListenableFuture<JsonNode> fOfSchema = rpc.get_schema(Lists.newArrayList(DatabaseSchema.OPEN_VSWITCH_SCHEMA_NAME));
        fOfSchema.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    JsonNode jsonNode = fOfSchema.get();
                    schema =  DatabaseSchema.fromJson(jsonNode);

                } catch (Exception e) {
                    exceptions.add(e);
                }
            }
        }, executorService);
    }

    public Transaction transact(){
        return new Transaction(this);
    }

    public ListenableFuture<List<OperationResult>> transact(List<Operation> operations) {

        //todo, we may not need transactionbuilder if we can have JSON objects
        TransactBuilder builder = new TransactBuilder();
        for (Operation o : operations) {
           builder.addOperation(o);
        }

        ListenableFuture<List<OperationResult>> transact = rpc.transact(builder);
        return transact;
    }

    public boolean isReady(long timeout) {
        //todo implement timeout
        return null != schema;
    }

    public DatabaseSchema schema() {
        return schema;
    }


    public static class Transaction {

        private  DatabaseSchema eDatabaseSchema;
        OpenVswitch ovs;
        ArrayList<Operation> operations = Lists.newArrayList();

        public Transaction(OpenVswitch ovs) {
            this.ovs = ovs;
        }

        public Transaction(DatabaseSchema eDatabaseSchema) {
            this.eDatabaseSchema = eDatabaseSchema;
        }

        public Transaction add(Operation operation) {
            operations.add(operation);
            return this;
        }

        public List<Operation> build() {
            return operations;
        }

        public ListenableFuture<List<OperationResult>> execute() {
            return ovs.transact(operations);
        }
    }

    public static class Update<E extends TableSchema<E>> extends Operation<E> implements ConditionalOperation {

        Map<String, Object> row = Maps.newHashMap();
        String uuid;
        //Where where;
        List<Condition> where = Lists.newArrayList();

        private String uuidName;

        public Update(TableSchema<E> schema) {
            super(schema, "update");
        }

        public Update<E> on(TableSchema schema){
            return this;
        }

        public <T extends TableSchema<T>, D> Update<E> set(ColumnSchema<T, D> columnSchema, D value) {
            columnSchema.validate(value);
            this.row.put(columnSchema.getName(), value);
            return this;
        }

        public Where where(Condition condition) {
            return new Where(this);
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getUuidName() {
            return uuidName;
        }

        public void setUuidName(String uuidName) {
            this.uuidName = uuidName;
        }

        public Map<String, Object> getRow() {
            return row;
        }

        public void setRow(Map<String, Object> row) {
            this.row = row;
        }

        @Override
        public void addCondition(Condition condition) {
            this.where.add(condition);
        }

        public List<Condition> getWhere() {
            return where;
        }

        public void setWhere(List<Condition> where) {
            this.where = where;
        }
    }


    public static class Where {

        @JsonIgnore
        ConditionalOperation operation;

        public Where() { }  public Where(ConditionalOperation operation) {
            this.operation = operation;
        }

        public Where condition(Condition condition) {
            operation.addCondition(condition);
            return this;
        }

        public Where condition(ColumnSchema column, Function function, Object value) {
            this.condition(new Condition(column.getName(), function, value));
            return this;
        }

        public Where and(ColumnSchema column, Function function, Object value) {
            condition(column, function, value);
            return this;
        }

        public Where and(Condition condition) {
           condition(condition);
            return this;
        }

        public Operation operation() {
            return (Operation) this.operation;
        }

    }


    public static class Operations {
        public static Operations op = new Operations();

        public <E extends TableSchema<E>> Insert<E> insert(TableSchema<E> schema) {
            return new Insert<>(schema);
        }

        public  <E extends TableSchema<E>> Update<E> update(TableSchema<E> schema) {
            return new Update<>(schema);
        }
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public OvsdbRPC getRpc() {
        return rpc;
    }

    public void setRpc(OvsdbRPC rpc) {
        this.rpc = rpc;
    }

    public DatabaseSchema getSchema() {
        return schema;
    }

    public void setSchema(DatabaseSchema schema) {
        this.schema = schema;
    }

    public Queue<Throwable> getExceptions() {
        return exceptions;
    }

    public void setExceptions(Queue<Throwable> exceptions) {
        this.exceptions = exceptions;
    }


}
