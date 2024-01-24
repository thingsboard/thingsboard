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
 * Copyright (c) 2016, 2017 Sierra Wireless and others.
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
 *    Achim Kraus (Bosch Software Innovations GmbH) - precalculated hashCode
 *    Bosch Software Innovations GmbH - migrate to SLF4J
 *    Achim Kraus (Bosch Software Innovations GmbH) - adjust to use Token,
 *                                                    token generator and
 *                                                    ensure putIfAbsent semantic
 *    Achim Kraus (Bosch Software Innovations GmbH) - add observation to logging
 *    Achim Kraus (Bosch Software Innovations GmbH) - start status logging with first
 *                                                    stored observation.
 *    Achim Kraus (Bosch Software Innovations GmbH) - use ExecutorsUtil.getScheduledExecutor()
 *                                                    for health status instead of own executor.
 */

package org.eclipse.californium.core.observe;

import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.SystemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class InMemoryObservationStore implements ObservationStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryObservationStore.class);
    private static final Logger HEALTH_LOGGER = LoggerFactory.getLogger(LOGGER.getName() + ".health");
    private final ConcurrentMap<Token, Observation> map = new ConcurrentHashMap<>();
    private volatile boolean enableStatus;
    private final Configuration config;
    private ScheduledFuture<?> statusLogger;
    private ScheduledExecutorService executor;

    public InMemoryObservationStore(Configuration config) {
        this.config = config;
    }

    @Override
    public void setExecutor(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public Observation putIfAbsent(Token key, Observation obs) {
        if (key == null) {
            throw new NullPointerException("token must not be null");
        } else if (obs == null) {
            throw new NullPointerException("observation must not be null");
        } else {
            enableStatus = true;
            Observation result = map.putIfAbsent(key, obs);
            if (result == null) {
                LOGGER.debug("added observation for {}", key);
            } else {
                LOGGER.debug("kept observation {} for {}", result, key);
            }
            return result;
        }
    }

    @Override
    public Observation put(Token key, Observation obs) {
        if (key == null) {
            throw new NullPointerException("token must not be null");
        } else if (obs == null) {
            throw new NullPointerException("observation must not be null");
        } else {
            enableStatus = true;
            Observation result = map.put(key, obs);
            if (result == null) {
                LOGGER.debug("added observation for {}", key);
            } else {
                LOGGER.debug("replaced observation {} for {}", result, key);
            }
            return result;
        }
    }

    @Override
    public Observation get(Token token) {
        if (token == null) {
            return null;
        } else {
            Observation obs = map.get(token);
            LOGGER.debug("looking up observation for token {}: {}", token, obs);
            // clone request in order to prevent accumulation of
            // message observers on original request
            return ObservationUtil.shallowClone(obs);
        }
    }

    @Override
    public void remove(Token token) {
        if (token != null) {
            if (map.remove(token) != null) {
                LOGGER.debug("removed observation for token {}", token);
            } else {
                LOGGER.debug("Already removed observation for token {}", token);
            }
        }
    }

    /**
     * Checks if this store is empty.
     *
     * @return {@code true} if this store does not contain any observations.
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Gets the number of observations currently held in this store.
     *
     * @return The number of observations.
     */
    public int getSize() {
        return map.size();
    }

    /**
     * Removes all observations from this store.
     */
    public void clear() {
        map.clear();
    }

    @Override
    public void setContext(Token token, final EndpointContext ctx) {

        if (token != null && ctx != null) {
            Observation obs = map.get(token);
            if (obs != null) {
                map.replace(token, obs, new Observation(obs.getRequest(), ctx));
            }
        }
    }

    @Override
    public synchronized void start() {
        long healthStatusInterval = config.get(SystemConfig.HEALTH_STATUS_INTERVAL, TimeUnit.MILLISECONDS);

        if (healthStatusInterval > 0 && HEALTH_LOGGER.isDebugEnabled() && executor != null) {
            statusLogger = executor.scheduleAtFixedRate(new Runnable() {

                @Override
                public void run() {
                    if (enableStatus) {
                        HEALTH_LOGGER.debug("{} observes", map.size());
                        Iterator<Token> iterator = map.keySet().iterator();
                        int max = 5;
                        while (iterator.hasNext()) {
                            HEALTH_LOGGER.debug("   observe {}", iterator.next());
                            --max;
                            if (max == 0) {
                                break;
                            }
                        }
                    }
                }
            }, healthStatusInterval, healthStatusInterval, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public synchronized void stop() {
        if (statusLogger != null) {
            statusLogger.cancel(false);
            statusLogger = null;
        }
    }
}
