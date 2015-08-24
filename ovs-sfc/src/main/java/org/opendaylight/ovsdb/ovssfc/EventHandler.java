/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.ovssfc;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventHandler {
    private static final Logger logger = LoggerFactory.getLogger(EventHandler.class);
    private ExecutorService eventHandler;
    private BlockingQueue<SfcEvent> events;

    void init () {
        eventHandler = Executors.newSingleThreadExecutor();
        this.events = new LinkedBlockingQueue<>();
    }

    void start () {
        eventHandler.submit(new Runnable() {
            @Override
            public void run() {
            while (true) {
                SfcEvent ev;
                try {
                    ev = events.take();
                } catch (InterruptedException e) {
                    logger.info("The event handler thread was interrupted, shutting down", e);
                    return;
                }
                logger.trace("\nOVSSFC: {}, event: {}", Thread.currentThread().getStackTrace()[1], ev);
                switch (ev.getType()) {
                case SFP:
                    try {
                        OvsSfcProvider.getOvsSfcProvider().sfp.processSfp(ev.getAction(), ev.serviceFunctionPath);
                    } catch (Exception e) {
                        logger.error("Exception caught in processSfp {}", ev, e);
                    }
                    break;
                case SFPS:
                    try {
                        OvsSfcProvider.getOvsSfcProvider().sfp.processSfps(ev.getAction(), ev.serviceFunctionPaths);
                    } catch (Exception e) {
                        logger.error("Exception caught in processSfps {}", ev, e);
                    }
                    break;
                default:
                    logger.warn("Unable to process {}", ev);
                }
            }
            }
        });
    }

    void stop () throws InterruptedException{
        // stop accepting new tasks
        eventHandler.shutdown();

        if (!eventHandler.awaitTermination(100, TimeUnit.MICROSECONDS)) {
            System.out.println("Still waiting...");
            //System.exit(0);
        }
        System.out.println("Exiting normally...");

/*
        try {
            // Wait a while for existing tasks to terminate
            if (!eventHandler.awaitTermination(10, TimeUnit.SECONDS)) {
                eventHandler.shutdownNow();
                // Wait a while for tasks to respond to being cancelled
                if (!eventHandler.awaitTermination(10, TimeUnit.SECONDS)) {
                    logger.error("EventHandler did not terminate");
                }
            }
        } catch (InterruptedException e) {
            // (Re-)Cancel if current thread also interrupted
            eventHandler.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
*/
    }

    public void enqueueSfpEvent (SfcEvent.Action action, ServiceFunctionPath serviceFunctionPath) {
        SfcEvent event = new SfcEvent(SfcEvent.Type.SFP, action, serviceFunctionPath);
        enqueueEvent(event);
    }

    public void enqueueSfpsEvent (SfcEvent.Action action, ServiceFunctionPaths serviceFunctionPaths) {
        SfcEvent event = new SfcEvent(SfcEvent.Type.SFPS, action, serviceFunctionPaths);
        enqueueEvent(event);
    }

    public void enqueueEvent (SfcEvent event) {
        try {
            events.put(event);
        } catch (InterruptedException e) {
            logger.error("Thread was interrupted while trying to enqueue event ", e);
        }
    }
}
