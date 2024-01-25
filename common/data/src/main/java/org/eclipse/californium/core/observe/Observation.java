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
 * Copyright (c) 2016 Sierra Wireless and others.
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
 *    initial implementation please refer gitlog
 *    Achim Kraus (Bosch Software Innovations GmbH) - add toString
 */

package org.eclipse.californium.core.observe;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.elements.EndpointContext;

public  final class Observation {

    /**
     * Initiate request for observation.
     */
    private final Request request;
    /**
     * Endpoint context the request was sent in.
     */
    private final EndpointContext context;

    /**
     * Creates a new observation for a request and a endpoint context.
     *
     * @param request The request that initiated the observation.
     * @param context The endpoint context of the request.
     * @throws NullPointerException if the request is {@code null}.
     * @throws IllegalArgumentException if the request doesn't have its observe option set to 0.
     */
    public Observation(final Request request, final EndpointContext context) {

        if (request == null) {
            throw new NullPointerException("request must not be null");
        } else if (!request.isObserve()) {
            throw new IllegalArgumentException("request has no observe=0 option");
        }
        this.request = request;
        this.context = context;
    }

    /**
     * @return the request which initiated the observation
     */
    public Request getRequest() {
        return request;
    }

    /**
     * Gets the endpoint context the requeste was sent in.
     *
     * @return the endpoint context for this observation
     */
    public EndpointContext getContext() {
        return context;
    }

    @Override
    public String toString() {
        return request.toString();
    }
}

