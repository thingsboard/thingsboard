/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.server;

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

