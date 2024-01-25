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

import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.network.KeyToken;
import org.eclipse.californium.core.server.resources.ObservableResource;
import org.eclipse.californium.elements.config.Configuration;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class ObserveManager {

    /** The mapping from endpoint addresses to ObservingEndpoints */
    private final ConcurrentHashMap<InetSocketAddress, ObservingEndpoint> endpoints;
    /**
     * The mapping from endpoint addresses to ObservingEndpoints.
     *
     * @since 3.6
     */
    private final ConcurrentHashMap<KeyToken, ObserveRelation> relations;
    /**
     * Maximum number of server-side observes.
     *
     * @since 3.6
     */
    private final int maxObserves;
    /**
     * Configuration.
     *
     * @since 3.7
     */
    private final Configuration config;
    /**
     * Observe health status.
     *
     * @since 3.6
     */
    private volatile ObserveHealth observeHealth;

    /**
     * Constructs a new ObserveManager for this server.
     *
     * @deprecated use {@link #ObserveManager(Configuration)} instead
     */
    @Deprecated
    public ObserveManager() {
        this(null);
    }

    /**
     * Constructs a new ObserveManager for this server.
     *
     * @param config configuration.  May be {@code null}.
     * @since 3.6
     */
    public ObserveManager(Configuration config) {
        this.endpoints = new ConcurrentHashMap<>();
        this.relations = new ConcurrentHashMap<>();
        this.config = config;
        int maxObserves = 0;
        if (config != null) {
            maxObserves = config.get(CoapConfig.MAX_SERVER_OBSERVES);
        }
        this.maxObserves = maxObserves;
    }

    /**
     * Set observe health status.
     *
     * @param observeHealth health status for observe.
     * @since 3.6
     */
    public void setObserveHealth(ObserveHealth observeHealth) {
        this.observeHealth = observeHealth;
    }

    /**
     * Find the ObservingEndpoint for the specified endpoint address or create a
     * new one if none exists yet. Does not return {@code null}.
     *
     * @param address the address
     * @return the ObservingEndpoint for the address
     * @deprecated obsolete
     */
    @Deprecated
    public ObservingEndpoint findObservingEndpoint(InetSocketAddress address) {
        ObservingEndpoint ep = endpoints.get(address);
        if (ep == null) {
            ep = createObservingEndpoint(address);
        }
        return ep;
    }

    /**
     * Add observer relation.
     *
     * @param exchange The initial exchange.
     * @param resource The resource
     * @since 3.6
     */
    public void addObserveRelation(Exchange exchange, ObservableResource resource) {
        ObserveRelation relation = new ObserveRelation(this, resource, exchange);
        ObserveRelation previous;
        synchronized (this) {
            previous = relations.get(relation.getKeyToken());
            if (previous != null || maxObserves == 0 || relations.size() < maxObserves) {
                relations.put(relation.getKeyToken(), relation);
                ObservingEndpoint remoteEndpoint = endpoints.get(relation.getSource());
                if (remoteEndpoint == null) {
                    remoteEndpoint = new ObservingEndpoint(relation.getSource());
                    relation.setEndpoint(remoteEndpoint);
                    endpoints.put(relation.getSource(), remoteEndpoint);
                } else {
                    relation.setEndpoint(remoteEndpoint);
                }
            }
        }
        if (previous != null) {
            previous.cancel();
        }
        ObserveHealth observeHealth = this.observeHealth;
        if (observeHealth != null) {
            observeHealth.receivingObserveRequest();
            observeHealth.setObserveRelations(relations.size());
            observeHealth.setObserveEndpoints(endpoints.size());
        }
    }

    /**
     * Cancel observe relation.
     *
     * @param exchange The exchange to cancel a observe relation
     * @since 3.6
     */
    public void cancelObserveRelation(Exchange exchange) {
        KeyToken keyToken = ObserveRelation.getKeyToken(exchange);
        ObserveRelation relation = relations.get(keyToken);
        if (relation != null) {
            relation.cancel();
        }
        ObserveHealth observeHealth = this.observeHealth;
        if (observeHealth != null) {
            observeHealth.receivingCancelRequest();
        }
    }

    /**
     * Report rejected notification.
     *
     * @param relation observe relation of rejected notification
     * @since 3.6
     */
    public void onRejectedNotification(ObserveRelation relation) {
        ObserveHealth observeHealth = this.observeHealth;
        if (observeHealth != null) {
            observeHealth.receivingReject();
        }
        relation.cancel();
    }

    /**
     * Remove observe relation.
     *
     * @param relation observe relation to remove
     * @since 3.6
     */
    public void removeObserveRelation(ObserveRelation relation) {
        boolean change = relations.remove(relation.getKeyToken(), relation);
        ObservingEndpoint endpoint = relation.getEndpoint();
        if (endpoint != null) {
            endpoint.removeObserveRelation(relation);
            synchronized (this) {
                if (endpoint.isEmpty()) {
                    change = endpoints.remove(relation.getSource(), endpoint) || change;
                }
            }
        }
        ObserveHealth observeHealth = this.observeHealth;
        if (change && observeHealth != null) {
            observeHealth.setObserveRelations(relations.size());
            observeHealth.setObserveEndpoints(endpoints.size());
        }
    }

    /**
     * Get number of observing endpoints.
     *
     * @return number of observing endpoints
     * @since 3.6
     */
    public int getNumberOfEndpoints() {
        return endpoints.size();
    }

    /**
     * Get number of observe relations.
     *
     * @return number of observe relations
     * @since 3.6
     */
    public int getNumberOfObserverRelations() {
        return relations.size();
    }

    /**
     * Get configuration.
     *
     * @return configuration, may be {@code null}.
     * @since 3.7
     */
    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Checks, whether the maximum number of observe relation is reached.
     *
     * @return {@code true}, if the maximum number of observe relation is
     *         reached, {@code false}, otherwise
     * @since 3.6
     */
    public boolean isFull() {
        return maxObserves > 0 && relations.size() >= maxObserves;
    }

    /**
     * Return the ObservingEndpoint for the specified endpoint address or
     * {@code null}, if none exists.
     *
     * @param address the address
     * @return the ObservingEndpoint or {@code null}
     * @deprecated obsolete
     */
    @Deprecated
    public ObservingEndpoint getObservingEndpoint(InetSocketAddress address) {
        return endpoints.get(address);
    }

    /**
     * Atomically creates a new ObservingEndpoint for the specified address.
     *
     * @param address the address
     * @return the ObservingEndpoint
     * @deprecated obsolete
     */
    @Deprecated
    private ObservingEndpoint createObservingEndpoint(InetSocketAddress address) {
        ObservingEndpoint ep = new ObservingEndpoint(address);

        // Make sure, there is exactly one ep with the specified address (atomic
        // creation)
        ObservingEndpoint previous = endpoints.putIfAbsent(address, ep);
        if (previous != null) {
            return previous; // and forget ep again
        } else {
            return ep;
        }
    }

    /**
     * Get observe relation.
     *
     * @param address the address
     * @param token token of relation
     * @return the observe relation, or {@code null}, if not available
     * @deprecated obsolete
     */
    @Deprecated
    public ObserveRelation getRelation(InetSocketAddress address, Token token) {
        ObservingEndpoint remote = getObservingEndpoint(address);
        if (remote != null) {
            return remote.getObserveRelation(token);
        } else {
            return null;
        }
    }

}
