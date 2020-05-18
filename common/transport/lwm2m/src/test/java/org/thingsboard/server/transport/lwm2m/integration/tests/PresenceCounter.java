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

import java.util.concurrent.atomic.AtomicInteger;

public class PresenceCounter implements PresenceListener {
    private AtomicInteger nbAwake = new AtomicInteger(0);
    private AtomicInteger nbSleeping = new AtomicInteger(0);

    @Override
    public void onAwake(Registration registration) {
        if (accept(registration))
            nbAwake.incrementAndGet();
    }

    @Override
    public void onSleeping(Registration registration) {
        if (accept(registration))
            nbSleeping.incrementAndGet();
    }

    public void resetCounter() {
        nbAwake.set(0);
        nbSleeping.set(0);
    }

    public int getNbAwake() {
        return nbAwake.get();
    }

    public int getNbSleeping() {
        return nbSleeping.get();
    }

    public boolean accept(Registration registration) {
        return true;
    }
}
