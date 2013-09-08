package org.opendaylight.ovsdb.internal;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;

/**
 * Class which will monitor the completion of a FlowEntryDistributionOrder it
 * implements a Future interface so it can be inspected by who is waiting for
 * it.
 */
final class MessageHandlerFuture implements Future<Object> {
    private final Long id;
    private Object response;
    private boolean amICancelled;
    private CountDownLatch waitingLatch;
    private Status retStatus;

    /**
     * @param order
     *            for which we are monitoring the execution
     */
    public MessageHandlerFuture(Long id) {
        // Order being monitored
        this.id = id;
        this.response = null;
        this.amICancelled = false;
        // We need to wait for one completion to happen
        this.waitingLatch = new CountDownLatch(1);
        // No return status yet!
        this.retStatus = new Status(StatusCode.UNDEFINED);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        // If i'm done lets return the status as many times as caller wants
        if (this.waitingLatch.getCount() == 0L) {
            return response;
        }

        // Wait till someone signal that we are done
        this.waitingLatch.await();

        // Return the known status
        return response;
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
        // If i'm done lets return the status as many times as caller wants
        if (this.waitingLatch.getCount() == 0L) {
            return response;
        }

        // Wait till someone signal that we are done
        this.waitingLatch.await(timeout, unit);

        // Return the known status, could also be null if didn't return
        return response;
    }

    @Override
    public boolean isCancelled() {
        return this.amICancelled;
    }

    @Override
    public boolean isDone() {
        return (this.waitingLatch.getCount() == 0L);
    }

    /**
     * Used by the thread that gets back the status for the order so can unblock
     * an eventual caller waiting on the result to comes back
     *
     * @param order
     * @param retStatus
     */
    void gotResponse(Long id, Object response) {
        if (id != this.id) {
            // Weird we got a call for an order we didn't make
            return;
        }
        this.response = response;
        // Now we are not waiting any longer
        this.waitingLatch.countDown();
    }
}
