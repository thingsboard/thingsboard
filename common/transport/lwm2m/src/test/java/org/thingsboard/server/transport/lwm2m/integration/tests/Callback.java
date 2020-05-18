/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Zebra Technologies - initial API and implementation
 *******************************************************************************/
package org.thingsboard.server.transport.lwm2m.integration.tests;

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Callback<T extends LwM2mResponse> implements ResponseCallback<T>, ErrorCallback {

    private final CountDownLatch latch;
    private final AtomicBoolean called;
    private T response;
    private Exception exception;

    public Callback() {
        called = new AtomicBoolean(false);
        latch = new CountDownLatch(1);
    }

    @Override
    public void onResponse(T response) {
        this.response = response;
        called.set(true);
        latch.countDown();
    }

    @Override
    public void onError(Exception e) {
        exception = e;
        called.set(true);
        latch.countDown();
    }

    public AtomicBoolean isCalled() {
        return called;
    }

    public boolean waitForResponse(long timeout) throws InterruptedException {
        return latch.await(timeout, TimeUnit.MILLISECONDS);
    }

    public ResponseCode getResponseCode() {
        return response.getCode();
    }

    public void reset() {
        called.set(false);
    }

    public T getResponse() {
        return response;
    }

    public Exception getException() {
        return exception;
    }
}
