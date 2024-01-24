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
 *
 * Copyright (c) 2015 Institute for Pervasive Computing, ETH Zurich and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *    Matthias Kovatsch - creator and main architect
 *    Martin Lanter - architect and re-implementation
 *    Dominique Im Obersteg - parsers and initial implementation
 *    Daniel Pauli - parsers and initial implementation
 *    Kai Hudalla - logging
 *    Achim Kraus (Bosch Software Innovation GmbH) - use nano time
 *                                                   remove unused methods
 */

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

