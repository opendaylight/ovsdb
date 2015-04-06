package org.opendaylight.ovsdb.northbound.transactions;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class TransactionInvokerImpl implements TransactionInvoker, TransactionChainListener, Runnable, AutoCloseable{

    private ExecutorService executor;
    private DataBroker db;
    private BindingTransactionChain chain;

    public TransactionInvokerImpl(DataBroker db) {
        this.db = db;
        this.chain = db.createTransactionChain(this);
        ThreadFactory threadFact = new ThreadFactoryBuilder().setNameFormat("transaction-invoker-impl-%d").build();
        executor = Executors.newSingleThreadExecutor(threadFact);
        executor.submit(this);
    }

    @Override
    public void invoke(TransactionCommand command) {
        // TODO Auto-generated method stub
    }

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onTransactionChainFailed(TransactionChain<?, ?> arg0,
            AsyncTransaction<?, ?> arg1, Throwable arg2) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onTransactionChainSuccessful(TransactionChain<?, ?> arg0) {
        // TODO Auto-generated method stub
    }

}
