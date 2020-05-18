/*******************************************************************************
 * Copyright (c) 2018 Sierra Wireless and others.
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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.thingsboard.server.transport.lwm2m.integration.tests;

import org.eclipse.leshan.server.queue.PresenceListener;
import org.eclipse.leshan.server.registration.Registration;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SynchronousPresenceListener implements PresenceListener {
    private CountDownLatch awakeLatch = new CountDownLatch(1);
    private CountDownLatch sleepingLatch = new CountDownLatch(1);

    @Override
    public void onAwake(Registration registration) {
        if (accept(registration)) {
            awakeLatch.countDown();
        }
    }

    @Override
    public void onSleeping(Registration registration) {
        if (accept(registration)) {
            sleepingLatch.countDown();
        }
    }

    /**
     * Wait until next Awake event.
     * 
     * @throws TimeoutException if wait timeouts
     */
    public void waitForAwake(long timeout, TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        try {
            if (!awakeLatch.await(timeout, timeUnit))
                throw new TimeoutException("wait for awake timeout");
        } finally {
            awakeLatch = new CountDownLatch(1);
        }
    }

    /**
     * Wait until next sleep event.
     * 
     * @throws TimeoutException if wait timeouts
     */
    public void waitForSleep(long timeout, TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        try {
            if (!sleepingLatch.await(timeout, timeUnit))
                throw new TimeoutException("wait for sleep timeout");
        } finally {
            sleepingLatch = new CountDownLatch(1);
        }
    }

    public boolean accept(Registration registration) {
        return true;
    }
}
