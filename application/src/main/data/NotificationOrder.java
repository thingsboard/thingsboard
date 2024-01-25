/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.californium.core.observe;

import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.elements.util.ClockUtil;

import java.util.concurrent.TimeUnit;

public class NotificationOrder {

    /** The observe number */
    protected final int number;

    /** The timestamp of the response */
    protected final long nanoTimestamp;

    /**
     * Creates a new notification order for a given notification.
     *
     * @param observe observe of the notification
     * @since 3.0 changed type of observe from {@code Integer} to {@code int}.
     */
    public NotificationOrder(int observe) {
        this(observe, ClockUtil.nanoRealtime());
    }

    /**
     * Creates a new notification order for a given notification and nano-time.
     *
     * @param observe observe of the notification
     * @param nanoTime receive time of notification
     * @since 3.0 changed type of observe from {@code Integer} to {@code int}.
     */
    public NotificationOrder(int observe, long nanoTime) {
        number = observe;
        nanoTimestamp = nanoTime;
    }

    /**
     * Returns the notification number.
     *
     * @return the notification number.
     * @since 3.0 changed return type from {@code Integer} to {@code int}.
     */
    public int getObserve() {
        return number;
    }

    /**
     * Test, if the provided notification is newer than the current one.
     *
     * @param response the notification
     * @return {@code true} if the notification is new
     */
    public boolean isNew(Response response) {

        Integer observe = response.getOptions().getObserve();
        if (observe == null) {
            // this is a final response, e.g. error or proactive cancellation
            return true;
        }

        return isNew(nanoTimestamp, number, ClockUtil.nanoRealtime(), observe);
    }

    /**
     * Compare order of notifications.
     *
     * @param T1 nano realtimestamp of first notification
     * @param V1 observe number of first notification
     * @param T2 nano realtimestamp of second notification
     * @param V2 observe number of second notification
     * @return {@code true}, if second notification is newer.
     */
    public static boolean isNew(long T1, int V1, long T2, int V2) {
        // Multiple responses with different notification numbers might
        // arrive and be processed by different threads. We have to
        // ensure that only the most fresh one is being delivered.
        // We use the notation from the observe draft-08.
        if (V1 < V2 && (V2 - V1) < (1L << 23) || V1 > V2 && (V1 - V2) > (1L << 23)
                || T2 > (T1 + TimeUnit.SECONDS.toNanos(128))) {
            return true;
        } else {
            return false;
        }
    }
}
