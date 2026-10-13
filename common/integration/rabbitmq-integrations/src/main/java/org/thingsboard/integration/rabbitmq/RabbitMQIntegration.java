// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.Recoverable;
import com.rabbitmq.client.RecoveryListener;
import com.rabbitmq.client.ShutdownSignalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.AbstractIntegration;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.DownlinkData;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.integration.api.data.IntegrationMetaData;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbMsg;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
public class RabbitMQIntegration extends AbstractIntegration<RabbitMQIntegrationMsg> {

    private static final long DEFAULT_POOL_PERIOD_IN_MS = 200;
    // reassigned on init, read on the downlink thread that does not hold rabbitMQLock
    private volatile RabbitMQConsumerConfiguration rabbitMQConsumerConfiguration;
    private Connection connection;
    private volatile Channel channel;
    private List<String> queues;
    private ScheduledFuture<?> taskFuture;
    private volatile boolean stopped;
    private final Lock rabbitMQLock = new ReentrantLock();
    private long poolPeriod;

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        stopped = false;
        rabbitMQConsumerConfiguration = getClientConfiguration(configuration, RabbitMQConsumerConfiguration.class);
        poolPeriod = Math.max(rabbitMQConsumerConfiguration.getPollPeriod(), DEFAULT_POOL_PERIOD_IN_MS);
        queues = new ArrayList<>(Arrays.asList(rabbitMQConsumerConfiguration.getQueues().trim().split(",")));
        createConnection();
        initQueues();
        schedulePoll();
    }

    @Override
    public void process(RabbitMQIntegrationMsg msg) {
        String status = "OK";
        Exception exception = null;
        try {
            doProcess(context, msg);
            integrationStatistics.incMessagesProcessed();
        } catch (Exception e) {
            log.debug("Failed to apply data converter function: {}", e.getMessage(), e);
            integrationStatistics.incErrorsOccurred();
            exception = e;
            status = "ERROR";
        }
        persistDebug(context, "Uplink", getDefaultUplinkContentType(), () -> JacksonUtil.toString(msg.getMsg()), status, exception);
    }

    private void doProcess(IntegrationContext context, RabbitMQIntegrationMsg msg) throws Exception {
        byte[] bytes = msg.getMsg().getBytes();
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        List<UplinkData> uplinkDataList = convertToUplinkDataList(context, bytes, new UplinkMetaData<>(getDefaultUplinkContentType(), mdMap));
        if (uplinkDataList != null) {
            for (UplinkData data : uplinkDataList) {
                processUplinkDataBlocking(context, data);
                log.trace("[{}] Processing uplink data: {}", configuration.getId(), data);
            }
        }
    }

    @Override
    public void onDownlinkMsg(IntegrationDownlinkMsg downlink) {
        TbMsg msg = downlink.getTbMsg();
        logDownlink(context, "Downlink: " + msg.getType(), msg);
        if (downlinkConverter != null) {
            processDownLinkMsg(context, msg);
        }
    }

    @Override
    protected void doCheckConnection(Integration integration, IntegrationContext ctx) throws ThingsboardException {
        this.configuration = integration;
        try {
            rabbitMQConsumerConfiguration = getClientConfiguration(configuration, RabbitMQConsumerConfiguration.class);
            createConnection();
        } catch (Exception e) {
            throw new ThingsboardException(e.getMessage(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    @Override
    public void destroy() {
        stopped = true;
        rabbitMQLock.lock();
        try {
            if (channel != null && channel.isOpen()) {
                try {
                    channel.close();
                } catch (Exception e) {
                    log.error("Failed to close Channel.", e);
                }
            }
            if (connection != null && connection.isOpen()) {
                try {
                    connection.close();
                } catch (Exception e) {
                    log.error("Failed to close Connection.", e);
                }
            }
            if (taskFuture != null) {
                taskFuture.cancel(true);
            }
        } finally {
            rabbitMQLock.unlock();
        }
    }

    private void schedulePoll() {
        taskFuture = this.context.getScheduledExecutorService().schedule(this::submitPoll, poolPeriod, TimeUnit.MILLISECONDS);
    }

    private void submitPoll() {
        context.getExecutorService().execute(this::pollMessages);
    }

    private void pollMessages() {
        rabbitMQLock.lock();
        try {
            if (stopped || !channel.isOpen()) {
                return;
            }
            List<GetResponse> requests = doPoll();
            if (!CollectionUtils.isEmpty(requests)) {
                requests.forEach(request -> process(new RabbitMQIntegrationMsg(new String(request.getBody(), StandardCharsets.UTF_8))));
                submitPoll();
            } else {
                schedulePoll();
            }
        } finally {
            rabbitMQLock.unlock();
        }
    }

    private List<GetResponse> doPoll() {
        return queues.stream()
                .map(queue -> {
                    try {
                        return channel.basicGet(queue, true);
                    } catch (IOException | ShutdownSignalException exception) {
                        log.error("[{}][{}] Channel was closed with the error: {}", this.configuration.getTenantId().getId(), this.configuration.getId().getId(), exception.getMessage());
                        persistDebug(context, "Uplink", getDefaultUplinkContentType(), "", "ERROR", exception);

                        // cooldown period
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return null;
                }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private void processDownLinkMsg(IntegrationContext context, TbMsg msg) {
        List<DownlinkData> downlinkDataList;
        try {
            downlinkDataList = convertDownLinkMsg(context, msg);
        } catch (Exception e) {
            log.warn("Failed to process downLink message", e);
            reportDownlinkError(context, msg, "ERROR", e);
            return;
        }
        // each converted message is published on its own, so a failed one does not discard the rest
        for (DownlinkData data : downlinkDataList) {
            try {
                channel.basicPublish(
                        rabbitMQConsumerConfiguration.getExchangeName(), rabbitMQConsumerConfiguration.getDownlinkTopic(),
                        MessageProperties.MINIMAL_BASIC, data.getData());
                reportDownlinkOk(context, data);
            } catch (Exception e) {
                log.warn("[{}][{}] Failed to publish downLink message", configuration.getTenantId().getId(), configuration.getId().getId(), e);
                reportDownlinkError(context, msg, "ERROR", e);
            }
        }
    }

    private List<DownlinkData> convertDownLinkMsg(IntegrationContext context, TbMsg msg) throws Exception {
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        List<DownlinkData> result = downlinkConverter.convertDownLink(
                context.getDownlinkConverterContext(), Collections.singletonList(msg), new IntegrationMetaData(mdMap));
        return result.stream().filter(data -> !data.isEmpty()).collect(Collectors.toList());
    }

    private void createConnection() {
        var connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(rabbitMQConsumerConfiguration.getHost());
        connectionFactory.setPort(rabbitMQConsumerConfiguration.getPort());
        if (!StringUtils.isEmpty(rabbitMQConsumerConfiguration.getVirtualHost())) {
            connectionFactory.setVirtualHost(rabbitMQConsumerConfiguration.getVirtualHost());
        }
        if (!StringUtils.isEmpty(rabbitMQConsumerConfiguration.getUsername())) {
            connectionFactory.setUsername(rabbitMQConsumerConfiguration.getUsername());
        }
        if (!StringUtils.isEmpty(rabbitMQConsumerConfiguration.getPassword())) {
            connectionFactory.setPassword(rabbitMQConsumerConfiguration.getPassword());
        }
        connectionFactory.setAutomaticRecoveryEnabled(true);
        connectionFactory.setConnectionTimeout(rabbitMQConsumerConfiguration.getConnectionTimeout());
        connectionFactory.setHandshakeTimeout(rabbitMQConsumerConfiguration.getHandshakeTimeout());

        try {
            connection = connectionFactory.newConnection();
        } catch (IOException | TimeoutException e) {
            log.error("Failed to create connection.", e);
            throw new RuntimeException("Failed to create connection.", e);
        }

        try {
            channel = connection.createChannel();
            channel.addShutdownListener(cause -> {
                log.debug("[{}][{}] Channel was closed with the error: [{}]", configuration.getTenantId(), configuration.getName(), cause.getMessage());
                if (cause.isHardError()) {
                    context.saveLifecycleEvent(ComponentLifecycleEvent.STOPPED, cause);
                }
            });
            ((Recoverable) connection).addRecoveryListener(new RecoveryListener() {
                @Override
                public void handleRecovery(Recoverable recoverable) {
                    log.debug("[{}][{}] Integration recovered successfully!", configuration.getTenantId(), configuration.getName());
                    context.saveLifecycleEvent(ComponentLifecycleEvent.STARTED, null);
                    schedulePoll();
                }

                @Override
                public void handleRecoveryStarted(Recoverable recoverable) {
                    log.debug("[{}][{}] Integration recovery started!", configuration.getTenantId(), configuration.getName());
                }
            });
        } catch (IOException e) {
            log.error("Failed to create channel.", e);
            throw new RuntimeException("Failed to create channel.", e);
        }
    }

    private void initQueues() {
        rabbitMQLock.lock();
        try {
            queues.forEach(this::createQueueIfNotExists);
        } finally {
            rabbitMQLock.unlock();
        }
    }

    private void createQueueIfNotExists(String queue) {
        try {
            channel.queueDeclare(queue,
                    rabbitMQConsumerConfiguration.getDurable(),
                    rabbitMQConsumerConfiguration.getExclusive(),
                    rabbitMQConsumerConfiguration.getAutoDelete(),
                    null);
        } catch (IOException | ShutdownSignalException e) {
            log.error("Failed to bind queue: [{}]", queue);
            throw new RuntimeException(String.format("Failed to bind queue: [%s]", queue), e);
        }
    }

}
