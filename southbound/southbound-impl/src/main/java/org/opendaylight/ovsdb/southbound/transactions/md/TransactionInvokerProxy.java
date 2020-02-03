/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionInvokerProxy {

    static Logger LOG = LoggerFactory.getLogger(TransactionInvokerProxy.class);
    private static final int NUMBER_OF_TXINVOKER_THREAD = Integer.getInteger("hwvtep.thread.count", 4);
    public static final int PER_THREAD_QUEUE_SIZE
        = Integer.getInteger("hwvtep.queue.size", 40000) / NUMBER_OF_TXINVOKER_THREAD;

    private TransactionInvoker defaultInvoker;
    private List<TransactionInvoker> txInvokersOrderedByLoad = new ArrayList();
    private Map<TransactionInvoker, AtomicInteger> threadLoad = new ConcurrentHashMap<>();

    private LoadingCache<InstanceIdentifier<Node>, TransactionInvoker> threadAffinity = CacheBuilder.newBuilder()
        .expireAfterAccess(60, TimeUnit.MINUTES)//scaled in compute node gets removed only after 60 mins
        .removalListener((RemovalNotification<InstanceIdentifier<Node>, TransactionInvoker> removed)
            -> decrementThreadLoad(removed.getValue()))
        .build(new CacheLoader<InstanceIdentifier<Node>, TransactionInvoker>() {
            public TransactionInvoker load(InstanceIdentifier<Node> nodeIid) {
                synchronized (txInvokersOrderedByLoad) {
                    Collections.sort(txInvokersOrderedByLoad, new Comparator<TransactionInvoker>() {
                        @Override
                        public int compare(TransactionInvoker o1, TransactionInvoker o2) {
                            return getLoad(o1) - getLoad(o2);
                        }
                    });
                    TransactionInvoker txInvoker = txInvokersOrderedByLoad.iterator().next();
                    incrementThreadLoad(txInvoker);
                    return txInvoker;
                }
            }
        });

    public TransactionInvokerProxy() {
    }

    public int getLoad(TransactionInvoker invoker) {
        return threadLoad.get(invoker).get();
    }

    private void incrementThreadLoad(TransactionInvoker invoker) {
        threadLoad.computeIfAbsent(invoker, (invoker1) -> new AtomicInteger(0)).incrementAndGet();
    }

    private void decrementThreadLoad(TransactionInvoker invoker) {
        threadLoad.computeIfAbsent(invoker, (invoker1) -> new AtomicInteger(0)).decrementAndGet();
    }

    public void init(Supplier<TransactionInvoker> supplier) {
        for (int id = 0; id < NUMBER_OF_TXINVOKER_THREAD; id++) {
            TransactionInvoker invoker = supplier.get();
            txInvokersOrderedByLoad.add(invoker);
            incrementThreadLoad(invoker);
            defaultInvoker = invoker;//Last one is the default invoker
        }
    }

    @SuppressWarnings("IllegalCatch")
    public void close() {
        txInvokersOrderedByLoad.forEach(invoker -> {
            try {
                ((AutoCloseable) invoker).close();
            } catch (Exception e) {
                LOG.error("Failed to close SouthBound invoker ", e);
            }
        });
    }

    public TransactionInvoker getTransactionInvokerForNode(InstanceIdentifier<Node> node) {
        try {
            return threadAffinity.get(node);
        } catch (ExecutionException e) {
            LOG.error("Failed to get invoker ", e);
            return defaultInvoker;
        }
    }
}
