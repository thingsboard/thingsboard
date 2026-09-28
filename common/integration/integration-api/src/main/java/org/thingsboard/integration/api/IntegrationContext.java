// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.channel.EventLoopGroup;
import org.thingsboard.integration.api.converter.ConverterContext;
import org.thingsboard.integration.api.data.DownLinkMsg;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.server.common.data.event.IntegrationDebugEvent;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.gen.integration.AssetUplinkDataProto;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;
import org.thingsboard.server.gen.integration.EntityViewDataProto;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

public interface IntegrationContext {

    /**
     * Returns current service id that is used mostly for logging.
     *
     * @return service id
     */
    String getServiceId();

    /**
     * Returns context of execution for uplink data converter
     *
     * @return
     */
    ConverterContext getUplinkConverterContext();

    /**
     * Returns context of execution for downlink data converter
     *
     * @return
     */
    ConverterContext getDownlinkConverterContext();

    /**
     * Processes the uplink data and executes callback with result. The uplink data is pushed to queue for delivery.
     * Callback is executed when uplink data is queued successfully.
     *
     * @return
     */
    void processUplinkData(DeviceUplinkDataProto uplinkData, IntegrationCallback<Void> callback);

    void processUplinkData(AssetUplinkDataProto uplinkData, IntegrationCallback<Void> callback);

    /**
     * Dispatch custom message to the rule engine.
     * Note that msg originator is verified to be either tenantId or integrationId or any device/asset that belongs to the corresponding tenant.
     *
     * @param msg - custom message to dispatch
     */
    void processCustomMsg(TbMsg msg, IntegrationCallback<Void> callback);

    /**
     * Saves event to ThingsBoard based on provided type and body on behalf of the integration
     */
    void saveEvent(IntegrationDebugEvent event, IntegrationCallback<Void> callback);

    void saveLifecycleEvent(ComponentLifecycleEvent lcEvent, Exception e);

    void saveRawDataEvent(String deviceName, String type, String uid, JsonNode body, IntegrationCallback<Void> callback);

    /**
     * Provides Netty Event loop group to be used by integrations in order to avoid creating separate threads per integration.
     *
     * @return event loop group
     */
    EventLoopGroup getEventLoopGroup();

    /**
     * Provides access to ScheduledExecutorService to schedule periodic tasks.
     * Allows using N threads per M integrations instead of using N threads per integration.
     *
     * @return scheduled executor
     */
    ScheduledExecutorService getScheduledExecutorService();

    /**
     * Provides access to ExecutorService to submit tasks.
     * Allows using N threads per M integrations instead of using N threads per integration.
     *
     * @return executor
     */
    ExecutorService getExecutorService();

    /**
     * Provides access to ExecutorService to process messages after JS executor responses.
     * Allows using N threads per M integrations instead of using N threads per integration.
     *
     * @return callback executor
     */
    ExecutorService getCallBackExecutorService();

    DownLinkMsg getDownlinkMsg(String deviceName);

    DownLinkMsg putDownlinkMsg(IntegrationDownlinkMsg msg);

    void removeDownlinkMsg(String deviceName);

    //TODO: Implement
    boolean isClosed();

    boolean isExceptionStackTraceEnabled();

    void onUplinkMessageProcessed(boolean success);

    void onDownlinkMessageProcessed(boolean success);

    Optional<IntegrationRateLimitService> getRateLimitService();

    int getIntegrationConnectTimeoutSec();

    // Configuration parameters for the MQTT client that is used in MQTT-based integrations

    int getMqttClientRetransmissionMaxAttempts();

    long getMqttClientRetransmissionInitialDelayMillis();

    double getMqttClientRetransmissionJitterFactor();

    int getBackPressureHighWatermark();

    int getBackPressureLowWatermark();

}
