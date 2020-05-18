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

import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SynchronousRegistrationListener implements RegistrationListener {

    private CountDownLatch registerLatch = new CountDownLatch(1);
    private CountDownLatch updateLatch = new CountDownLatch(1);
    private CountDownLatch deregisterLatch = new CountDownLatch(1);
    private Registration lastRegistration;

    @Override
    public void registered(Registration reg, Registration previousReg, Collection<Observation> previousObsersations) {
        if (accept(reg)) {
            lastRegistration = reg;
            registerLatch.countDown();
        }
    }

    @Override
    public void updated(RegistrationUpdate update, Registration updatedReg, Registration previousReg) {
        if (accept(updatedReg))
            updateLatch.countDown();
    }

    @Override
    public void unregistered(Registration reg, Collection<Observation> observations, boolean expired,
                             Registration newReg) {
        if (accept(reg))
            deregisterLatch.countDown();
    }

    /**
     * Wait until next register event.
     * 
     * @throws TimeoutException if wait timeouts
     */
    public void waitForRegister(long timeout, TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        try {
            if (!registerLatch.await(timeout, timeUnit))
                throw new TimeoutException("wait for register timeout");
        } finally {
            registerLatch = new CountDownLatch(1);
        }
    }

    /**
     * Wait until next update event.
     * 
     * @throws TimeoutException if wait timeouts
     */
    public void waitForUpdate(long timeout, TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        try {
            if (!updateLatch.await(timeout, timeUnit))
                throw new TimeoutException("wait for update timeout");
        } finally {
            updateLatch = new CountDownLatch(1);
        }
    }

    /**
     * Wait until next de-register event.
     * 
     * @throws TimeoutException if wait timeouts
     */
    public void waitForDeregister(long timeout, TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        try {
            if (!deregisterLatch.await(timeout, timeUnit))
                throw new TimeoutException("wait for deregister timeout");
        } finally {
            deregisterLatch = new CountDownLatch(1);
        }
    }

    public boolean accept(Registration registration) {
        return true;
    }

    public Registration getLastRegistration() {
        return lastRegistration;
    }
}
