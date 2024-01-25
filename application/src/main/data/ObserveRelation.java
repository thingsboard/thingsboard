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

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.network.KeyToken;
import org.eclipse.californium.core.server.resources.ObservableResource;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.util.ClockUtil;
import org.eclipse.leshan.client.californium.RootResource;
import org.eclipse.leshan.client.californium.object.ObjectResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class ObserveRelation {

    /** The logger. */
    private final static Logger LOGGER = LoggerFactory.getLogger(ObserveRelation.class);

    /**
     * State of a {@link ObserveRelation}.
     *
     * @since 3.6
     */
    public enum State {
        /**
         * No observe relation.
         */
        NONE,
        /**
         * Observe relation initialized.
         *
         * Observe request received, response pending.
         */
        INIT,
        /**
         * Observe relation established.
         *
         * Observe request received, response/notify sent.
         */
        ESTABILSHED,
        /**
         * Observe relation canceled.
         */
        CANCELED
    }

    private final long checkIntervalTime;
    private final int checkIntervalCount;

    /**
     * Observe manager.
     *
     * If used with deprecated
     * {@link #ObserveRelation(ObservingEndpoint, Resource, Exchange)}, the
     * manager is {@code null}.
     *
     * @since 3.6
     */
    private final ObserveManager manager;

    /**
     * The resource that is observed.
     *
     * @since 3.6 adapted into the new {@link ObservableResource} type
     */
    private final ObservableResource resource;

    /** The exchange that has initiated the observe relationship */
    private final Exchange exchange;

    /**
     * The request type of the observer request.
     *
     * @since 3.6
     */
    private final Type requestType;

    private final InetSocketAddress source;

    /**
     * The key token.
     *
     * @since 3.6 adapted the type
     */
    private final KeyToken key;

    private Response recentControlNotification;
    private Response nextControlNotification;

    private volatile ObservingEndpoint remoteEndpoint;
    /*
     * This value is false at first and must be set to true by the resource if
     * it accepts the observe relation (the response code must be successful).
     */
    /** Indicates if the relation is established */
    private volatile boolean established;
    /** Indicates if the relation is canceled */
    private volatile boolean canceled;

    private long interestCheckTimer = ClockUtil.nanoRealtime();
    private int interestCheckCounter = 1;

    /**
     * Constructs a new observe relation.
     *
     * @param endpoint the observing endpoint
     * @param resource the observed resource
     * @param exchange the exchange that tries to establish the observe relation
     * @throws ClassCastException if {@link Resource} does not implement
     *             {@link ObservableResource} as well
     * @deprecated use
     *             {@link #ObserveRelation(ObserveManager, ObservableResource, Exchange)}
     *             instead.
     */
    @Deprecated
    public ObserveRelation(ObservingEndpoint endpoint, Resource resource, Exchange exchange) {
        this(null, (ObservableResource) resource, exchange);
        setEndpoint(endpoint);
    }

    /**
     * Constructs a new observe relation.
     *
     * @param manager the observe manager
     * @param resource the observed resource
     * @param exchange the exchange that tries to establish the observe relation
     * @since 3.6
     */
    public ObserveRelation(ObserveManager manager, ObservableResource resource, Exchange exchange) {
        if (manager == null) {
            throw new NullPointerException("Observe manager must not be null!");
        } else if (resource == null) {
            throw new NullPointerException("Observing resource must not be null!");
        } else if (exchange == null) {
            throw new NullPointerException("Exchange must not be null!");
        }
        this.manager = manager;
        this.resource = resource;
        this.exchange = exchange;
        this.requestType = exchange.getRequest().getType();
        Configuration config = manager.getConfiguration();
        Endpoint coapEndpoint = exchange.getEndpoint();
        if (coapEndpoint != null) {
            config = coapEndpoint.getConfig();
        }
        if (config == null) {
            throw new IllegalArgumentException("Either the ObserveManager or the Exchange must provide a Configuration!");
        }
        checkIntervalTime = config.get(CoapConfig.NOTIFICATION_CHECK_INTERVAL_TIME, TimeUnit.NANOSECONDS);
        checkIntervalCount = config.get(CoapConfig.NOTIFICATION_CHECK_INTERVAL_COUNT);
        Request request = exchange.getRequest();
        this.source = request.getSourceContext().getPeerAddress();
        this.key = getKeyToken(exchange);
        LOGGER.debug("Observe-relation, checks every {}ns or {} notifications.", checkIntervalTime, checkIntervalCount);
    }

    /**
     * Set observing endpoint.
     *
     * @param endpoint observing endpoint
     * @since 3.6
     */
    public void setEndpoint(ObservingEndpoint endpoint) {
        if (endpoint == null) {
            throw new NullPointerException("Observing endpoint must not be null!");
        }
        this.remoteEndpoint = endpoint;
        this.remoteEndpoint.addObserveRelation(this);
        this.exchange.setRelation(this);
    }

    /**
     * Returns his relation established state.
     *
     * @return {@code true}, if this relation has been established,
     *         {@code false}, otherwise
     */
    public boolean isEstablished() {
        return established;
    }

    /**
     * Sets the established field.
     *
     * Adds this relations to the resource
     *
     * @throws IllegalStateException if the relation was already canceled.
     */
    public void setEstablished() {
        boolean fail;
        synchronized (this) {
            fail = canceled;
            if (!fail) {
                established = true;
            }
        }
        if (fail) {
            throw new IllegalStateException(
                    String.format("Could not establish observe relation %s with %s, already canceled (%s)!", getKey(),
                            resource.getURI(), exchange));
        }
    }

    /**
     * Check, if this relation is canceled.
     *
     * @return {@code true}, if relation was canceled, {@code false}, otherwise.
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Cleanup relation.
     *
     * {@link #cancel()}, if {@link #isEstablished()}.
     *
     * @since 3.0
     * @deprecated use {@link #cancel()} also for not established relations
     */
    @Deprecated
    public void cleanup() {
        cancel();
    }

    public void reject() {
        if (manager != null) {
            manager.onRejectedNotification(this);
        } else {
            cancel();
        }
    }

    /**
     * Cancel this observe relation.
     *
     * This methods invokes the cancel methods of the resource and the endpoint.
     * A last response must have been sent before. Otherwise the
     * {@link Exchange} will be completed and fail that sending.
     *
     * Note: calling this method outside the execution of the related
     * {@link #exchange} may naturally cause indeterministic behavior.
     *
     * @throws IllegalStateException if relation wasn't established.
     */
    public void cancel() {
        cancel(true);
    }

    /**
     * Cancel all observer relations that this server has established with this
     * relation's endpoint.
     */
    public void cancelAll() {
        remoteEndpoint.cancelAll();
    }

    /**
     * Notifies the observing endpoint that the resource has been changed. This
     * method makes the resource process the same request again.
     *
     * Note: the {@link ObservableResource} must implement {@link Resource} as
     * well in order to call this method
     *
     * @throws ClassCastException if {@link ObservableResource} does not
     *             implement {@link Resource} as well
     * @deprecated obsolete
     */
    @Deprecated
    public void notifyObservers() {
        ((Resource) resource).handleRequest(exchange);
    }

    /**
     * Gets the resource.
     *
     * Note: the {@link ObservableResource} must implement {@link Resource} as
     * well in order to call this method
     *
     * @return the resource
     * @throws ClassCastException if {@link ObservableResource} does not
     *             implement {@link Resource} as well
     * @deprecated obsolete
     */
    @Deprecated
    public Resource getResource() {
        return (Resource) resource;
    }

    /**
     * Gets the exchange.
     *
     * @return the exchange
     */
    public Exchange getExchange() {
        return exchange;
    }

    /**
     * Gets the source address of the observing endpoint.
     *
     * @return the source address
     */
    public InetSocketAddress getSource() {
        return source;
    }

    /**
     * Gets the source address of the observing endpoint.
     *
     * @return the source address
     */
    public ObservingEndpoint getEndpoint() {
        return remoteEndpoint;
    }

    /**
     * Get the type of the notifications that will be sent.
     *
     * Uses {@link ObservableResource#getObserveType()}, or the request's type,
     * if no observe-type is provided. If this results in {@link Type#NON}, then
     * {@link #check()} is used, to determine, if the notification is adapted to
     * {@link Type#CON} to verify, that the client is still interested.
     *
     * @return the type of the notifications
     * @since 3.6
     */
    public Type getObserveType() {
        Type observeType = resource.getObserveType();
        if (observeType == null) {
            observeType = requestType;
        }
        if (observeType != Type.CON && !check()) {
            return Type.NON;
        }
        return Type.CON;
    }

    /**
     * Process response using this observe relation.
     *
     * The first response will {@link #setEstablished()} the relation and
     * {@link ObservableResource#addObserveRelation(ObserveRelation)} it. If not
     * {@link #isCanceled()}, and the response {@link Response#isSuccess()},
     * {@link ObservableResource#getNotificationSequenceNumber()} will be set.
     *
     * @param response response
     * @return current relation state.
     * @since 3.6
     */
    public State onResponse(Response response) {
        boolean canceled = isCanceled();
        if (canceled) {
            return State.CANCELED;
        } else if (isEstablished()) {
            exchange.retransmitResponse();
            if (response.isSuccess()) {
                if (resource instanceof ObjectResource || resource instanceof RootResource) {
                    response.getOptions().setObserve(resource.getNotificationSequenceNumber());
                } else {
                    ((CoapResource)resource).checkObserveRelation(exchange, response);
                }
            }
            return State.ESTABILSHED;
        } else {
            boolean established = false;
            if (response.isSuccess()) {
                setEstablished();
                resource.addObserveRelation(this);
                established = !isCanceled();
            }
            if (established) {
                if (resource instanceof ObjectResource || resource instanceof RootResource) {
                    response.getOptions().setObserve(resource.getNotificationSequenceNumber());
                } else {
                    ((CoapResource)resource).checkObserveRelation(exchange, response);
                }

                return State.INIT;
            } else {
                return State.CANCELED;
            }
        }
    }

    /**
     * Check, if notification is sent to test, if it is still requested by the
     * client.
     *
     * Send notification as CON response in order to challenge the client to
     * acknowledge the message.
     *
     * @return {@code true}, to check the observer relation with a
     *         CON-notification, {@code false}, otherwise.
     */
    public boolean check() {
        long now = ClockUtil.nanoRealtime();
        boolean check;
        synchronized (this) {
            check = (++interestCheckCounter >= checkIntervalCount);
            if (check) {
                this.interestCheckTimer = now;
                this.interestCheckCounter = 0;
            }
        }
        if (check) {
            LOGGER.trace("Observe-relation check, {} notifications reached.", checkIntervalCount);
            return check;
        }
        synchronized (this) {
            check = (now - interestCheckTimer - checkIntervalTime) > 0;
            if (check) {
                this.interestCheckTimer = now;
                this.interestCheckCounter = 0;
            }
        }
        if (check) {
            LOGGER.trace("Observe-relation check, {}s interval reached.",
                    TimeUnit.NANOSECONDS.toSeconds(checkIntervalTime));
        }
        return check;
    }

    /**
     * Check, if sending the provided notification is postponed.
     *
     * Postponed notification are kept and sent after the current notification.
     * Calls {@link #onSend(Response)}, if not postponed.
     *
     * @param response notification to check.
     * @return {@code true}, if sending the notification is postponed,
     *         {@code false}, if not.
     * @since 3.0
     */
    public boolean isPostponedNotification(Response response) {
        if (isInTransit(recentControlNotification)) {
            LOGGER.trace("in transit {}", recentControlNotification);
            if (nextControlNotification != null) {
                if (!nextControlNotification.isNotification()) {
                    return true;
                }
                // complete deprecated response
                nextControlNotification.onTransferComplete();
            }
            nextControlNotification = response;
            return true;
        } else {
            recentControlNotification = response;
            nextControlNotification = null;
            onSend(response);
            return false;
        }
    }

    /**
     * Get next notification.
     *
     * Calls {@link #onSend(Response)} for next notification.
     *
     * @param response current notification
     * @param acknowledged {@code true}, if the current notification was
     *            acknowledged, or {@code false}, on retransmission (caused by
     *            missing acknowledged)
     * @return next notification, or {@code null}, if no next notification is
     *         available.
     * @since 3.0
     */
    public Response getNextNotification(Response response, boolean acknowledged) {
        Response next = null;
        if (recentControlNotification == response) {
            next = nextControlNotification;
            if (next != null) {
                // next may be null
                recentControlNotification = next;
                nextControlNotification = null;
                onSend(next);
            } else if (acknowledged) {
                // next may be null
                recentControlNotification = null;
                nextControlNotification = null;
            }
        }
        return next;
    }

    /**
     * Send response for this relation.
     *
     * If the response is no notification, {@link #cancel()} the relation
     * internally without completing the exchange.
     *
     * @param response response to sent.
     * @since 3.0
     * @deprecated use {@link #onSend(Response)} instead
     */
    @Deprecated
    public void send(Response response) {
        onSend(response);
    }

    /**
     * On send response for this relation.
     *
     * If the response is no notification, {@link #cancel()} the relation
     * internally without completing the exchange.
     *
     * @param response response to sent.
     * @since 3.6
     */
    public void onSend(Response response) {
        if (!response.isNotification()) {
            cancel(false);
        }
    }

    /**
     * Get key-token, identifying this observer relation.
     *
     * Combination of source-address and token.
     *
     * @return identifying key
     * @since 3.6
     */
    public KeyToken getKeyToken() {
        return this.key;
    }

    /**
     * Get key, identifying this observer relation.
     *
     * Combination of source-address and token.
     *
     * @return identifying key
     * @deprecated use {@link #getKeyToken()} instead
     */
    @Deprecated
    public String getKey() {
        return this.key.toString();
    }

    /**
     * Cancel this observe relation.
     *
     * This methods invokes the cancel methods of the resource and the endpoint.
     *
     * Note: calling this method outside the execution of the related
     * {@link #exchange} may naturally cause indeterministic behavior.
     *
     * @param complete {@code true}, to complete the exchange, {@code false}, to
     *            not complete it.
     *
     * @since 3.0
     */
    private void cancel(boolean complete) {
        boolean cancel = false;
        boolean established = false;

        synchronized (this) {
            if (!canceled) {
                canceled = true;
                established = this.established;
                this.established = false;
                cancel = true;
            }
        }
        if (cancel) {
            LOGGER.debug("Canceling observe relation {} with {} ({})", getKey(), resource.getURI(), exchange);
            if (established) {
                resource.removeObserveRelation(this);
            }
            if (manager != null) {
                manager.removeObserveRelation(this);
            } else {
                remoteEndpoint.removeObserveRelation(this);
            }
            if (complete) {
                exchange.executeComplete();
            }
        }
    }

    /**
     * Process response using this observe relation.
     *
     * The first response will {@link #setEstablished()} the relation and
     * {@link ObservableResource#addObserveRelation(ObserveRelation)}. And the
     * {@link ObservableResource#getNotificationSequenceNumber()} will be set to
     * the options of all responses.
     *
     * @param relation the observe relation, or {@code null}, if not available
     * @param response response
     * @return current relation state.
     * @see #onResponse(Response)
     * @since 3.6
     */
    public static State onResponse(ObserveRelation relation, Response response) {
        State result = State.NONE;
        if (relation != null) {
            result = relation.onResponse(response);
        }
        boolean noNotification = result == State.NONE || result == State.CANCELED;
        if (response.isNotification() && (!response.isSuccess() || noNotification)) {
            LOGGER.warn("Application notification, not longer observing, remove observe-option {}", response);
            response.getOptions().removeObserve();
        }
        return result;
    }

    /**
     * Get key token from exchange.
     *
     * @param exchange observe- or cancel-request exchange
     * @return key token with remote endpoint and request token.
     * @since 3.6
     */
    public static KeyToken getKeyToken(Exchange exchange) {
        Request request = exchange.getRequest();
        return new KeyToken(request.getToken(), request.getSourceContext().getPeerAddress());
    }

    /**
     * Returns {@code true}, if the specified response is still in transit. A
     * response is in transit, if it has not yet been acknowledged, rejected or
     * its current transmission has not yet timed out.
     *
     * @param response notification to check.
     * @return {@code true}, if notification is in transit, {@code false},
     *         otherwise.
     * @since 3.0
     */
    private static boolean isInTransit(final Response response) {
        if (response == null || !response.isConfirmable()) {
            return false;
        }
        return !response.isAcknowledged() && !response.isTimedOut() && !response.isRejected();
    }

}
