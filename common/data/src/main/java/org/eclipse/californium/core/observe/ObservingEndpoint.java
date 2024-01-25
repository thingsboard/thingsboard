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
 *    Achim Kraus (Bosch Software Innovations GmbH) - replace byte array token by Token
 */

package org.eclipse.californium.core.observe;

import org.eclipse.californium.core.coap.Token;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ObservingEndpoint {

    /** The endpoint's address */
    private final InetSocketAddress address;

    /** The list of relations the endpoint has established with this server */
    private final List<ObserveRelation> relations;

    /**
     * Constructs a new ObservingEndpoint.
     *
     * @param address the endpoint's address
     * @throws NullPointerException if address is {@code null}.
     */
    public ObservingEndpoint(InetSocketAddress address) {
        if (address == null) {
            throw new NullPointerException("Address must not be null!");
        }
        this.address = address;
        this.relations = new CopyOnWriteArrayList<ObserveRelation>();
    }

    /**
     * Adds the specified observe relation.
     *
     * @param relation the relation
     */
    public void addObserveRelation(ObserveRelation relation) {
        relations.add(relation);
    }

    /**
     * Removes the specified observe relations.
     *
     * @param relation the relation
     */
    public void removeObserveRelation(ObserveRelation relation) {
        relations.remove(relation);
    }

    /**
     * Cancels all observe relations that this endpoint has established with
     * resources from this server.
     */
    public void cancelAll() {
        for (ObserveRelation relation : relations) {
            if (relation.isEstablished()) {
                relation.cancel();
            }
        }
    }

    /**
     * Returns the address of this endpoint-
     *
     * @return the address
     */
    public InetSocketAddress getAddress() {
        return address;
    }

    /**
     * Get observer relation for provided token.
     *
     * @param token observe request's token
     * @return observe relation, or {@code null}, if not available.
     * @deprecated obsolete
     */
    @Deprecated
    public ObserveRelation getObserveRelation(Token token) {
        if (token != null) {
            for (ObserveRelation relation : relations) {
                if (token.equals(relation.getExchange().getRequest().getToken())) {
                    return relation;
                }
            }
        }
        return null;
    }

    public boolean isEmpty() {
        return relations.isEmpty();
    }
}
