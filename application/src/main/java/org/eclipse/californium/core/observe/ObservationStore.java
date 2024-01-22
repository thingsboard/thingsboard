package org.eclipse.californium.core.observe;

import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.elements.EndpointContext;

import java.util.concurrent.ScheduledExecutorService;

public interface ObservationStore {

    /**
     * Adds an observation to the store using the provided token, if not already
     * added with that token.
     *
     * Preserve previous stored observation with that token.
     *
     * @param token unique token to add the provided observation.
     * @param obs The observation to add.
     * @return the previous value associated with the specified key, or
     *         {@code null} if there was no mapping for the key.
     * @throws NullPointerException if token or observation is {@code null}.
     * @throws ObservationStoreException if observation isn't stored.
     */
    Observation putIfAbsent(Token token, Observation obs);

    /**
     * Adds an observation to the store using the provided token.
     *
     * Potentially replaces previous stored observation with that token.
     *
     * @param token unique token to add the provided observation.
     * @param obs The observation to add.
     * @return the previous value associated with the specified key, or
     *         {@code null} if there was no mapping for the key.
     * @throws NullPointerException if token or observation is {@code null}.
     * @throws ObservationStoreException if observation isn't stored.
     */
    Observation put(Token token, Observation obs);

    /**
     * Removes the observation initiated by the request with the given token.
     *
     * @param token The token of the observation to remove.
     */
    void remove(Token token);

    /**
     * Gets the observation initiated by the request with the given token.
     *
     * @param token The token of the initiating request.
     * @return The corresponding observation or {@code null} if no observation
     *         is registered for the given token.
     */
    Observation get(Token token);

    /**
     * Sets the endpoint context on the observation initiated by the request
     * with the given token.
     * <p>
     * This method is necessary because the endpoint context may not be known
     * when the observation is originally registered. This is due to the fact
     * that the information contained in the endpoint context is gathered by the
     * transport layer when the request establishing the observation is sent to
     * the peer.
     * </p>
     *
     * @param token The token of the observation to set the context on.
     * @param endpointContext The context to set.
     */
    void setContext(Token token, EndpointContext endpointContext);

    /**
     * Set executor for this store.
     *
     * Executor is not managed by the store, it must be shutdown
     * externally, if the resource should be freed.
     *
     * @param executor intended to be used for rare executing timers (e.g. cleanup tasks).
     */
    void setExecutor(ScheduledExecutorService executor);

    void start();

    void stop();
}

