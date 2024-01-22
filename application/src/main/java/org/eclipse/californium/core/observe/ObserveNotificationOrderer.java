package org.eclipse.californium.core.observe;

import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.elements.util.ClockUtil;

import java.util.concurrent.atomic.AtomicInteger;

public class ObserveNotificationOrderer {

    /** The counter for observe numbers */
    private final AtomicInteger number = new AtomicInteger();

    /** The timestamp of the last response */
    private long nanoTimestamp;

    /**
     * Creates a new notification orderer.
     */
    public ObserveNotificationOrderer() {
    }

    /**
     * Creates a new notification orderer for a given notification.
     *
     * @param observe value of observe option
     * @throws NullPointerException if observe is {@code null}
     */
    public ObserveNotificationOrderer(Integer observe) {
        if (observe == null) {
            throw new NullPointerException("observe option must not be null!");
        }
        number.set(observe);
        nanoTimestamp = ClockUtil.nanoRealtime();
    }

    /**
     * Return a new observe option number. This method is thread-safe as it
     * increases the option number atomically.
     *
     * @return a new observe option number
     */
    public int getNextObserveNumber() {
        int next = number.incrementAndGet();
        while (next >= 1 << 24) {
            number.compareAndSet(next, 0);
            next = number.incrementAndGet();
        }
        // assert 0 <= next && next < 1<<24;
        return next;
    }

    /**
     * Returns the current notification number.
     *
     * @return the current notification number
     */
    public int getCurrent() {
        return number.get();
    }

    /**
     * Check, if the provided notification is newer than the current one.
     *
     * @param response the notification
     * @return {@code true}, if the notification is new, or the response is no
     *         notify
     */
    public synchronized boolean isNew(Response response) {

        Integer observe = response.getOptions().getObserve();
        if (observe == null) {
            // this is a final response, e.g., error or proactive cancellation
            return true;
        }

        // Multiple responses with different notification numbers might
        // arrive and be processed by different threads. We have to
        // ensure that only the most fresh one is being delivered.
        // We use the notation from the observe draft-08.
        long T2 = ClockUtil.nanoRealtime();
        if (NotificationOrder.isNew(nanoTimestamp, number.get(), T2, observe)) {
            nanoTimestamp = T2;
            number.set(observe);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Reset state.
     *
     * @since 3.8
     */
    public synchronized void reset() {
        number.set(0);
        nanoTimestamp = 0;
    }

}

