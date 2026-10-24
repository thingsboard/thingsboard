// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.aws.kinesis;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;
import com.amazonaws.services.kinesis.producer.UserRecordResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.AbstractIntegration;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.integration.api.data.DownlinkData;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.integration.api.data.IntegrationMetaData;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.msg.TbMsg;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.kinesis.common.ConfigsBuilder;
import software.amazon.kinesis.common.InitialPositionInStream;
import software.amazon.kinesis.common.InitialPositionInStreamExtended;
import software.amazon.kinesis.common.KinesisClientUtil;
import software.amazon.kinesis.coordinator.Scheduler;
import software.amazon.kinesis.exceptions.InvalidStateException;
import software.amazon.kinesis.exceptions.ShutdownException;
import software.amazon.kinesis.leases.LeaseManagementConfig;
import software.amazon.kinesis.lifecycle.events.InitializationInput;
import software.amazon.kinesis.lifecycle.events.LeaseLostInput;
import software.amazon.kinesis.lifecycle.events.ProcessRecordsInput;
import software.amazon.kinesis.lifecycle.events.ShardEndedInput;
import software.amazon.kinesis.lifecycle.events.ShutdownRequestedInput;
import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.processor.ShardRecordProcessorFactory;
import software.amazon.kinesis.retrieval.RetrievalConfig;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Slf4j
public class AwsKinesisIntegration extends AbstractIntegration<KinesisIntegrationMsg> {

    private static final String DEFAULT_DOWNLINK_STREAM_NAME_PATTERN = "${streamName}";
    private static final String DEFAULT_DOWNLINK_PARTITION_KEY_PATTERN = "${partitionKey}";

    protected KinesisClientConfiguration kinesisClientConfiguration;
    protected IntegrationContext ctx;

    private Scheduler scheduler;
    private Thread schedulerThread;

    private KinesisProducer kinesisProducer;

    private RecordProcessorFactory shardRecordProcessorFactory = new RecordProcessorFactory();

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);

        this.ctx = params.getContext();

        kinesisClientConfiguration = getClientConfiguration(configuration, KinesisClientConfiguration.class);

        init(kinesisClientConfiguration);
    }

    private void init(KinesisClientConfiguration kinesisClientConfiguration) {
        initConsumer(kinesisClientConfiguration);
        initProducer(kinesisClientConfiguration);
    }

    private void initConsumer(KinesisClientConfiguration kinesisClientConfiguration) {
        AwsCredentialsProvider credentialsProvider = getConsumerCredentialsProvider(kinesisClientConfiguration);

        NettyNioAsyncHttpClient.Builder builder = NettyNioAsyncHttpClient.builder().maxConcurrency(Integer.MAX_VALUE);

        String streamName = kinesisClientConfiguration.getStreamName();
        Region region = Region.of(kinesisClientConfiguration.getRegion());
        String applicationName = StringUtils.isBlank(kinesisClientConfiguration.getApplicationName()) ? streamName : kinesisClientConfiguration.getApplicationName();
        KinesisAsyncClient kinesisClient = KinesisClientUtil.createKinesisAsyncClient(KinesisAsyncClient.builder().region(region).httpClientBuilder(builder).credentialsProvider(credentialsProvider));

        DynamoDbAsyncClient dynamoClient = DynamoDbAsyncClient.builder().region(region).httpClientBuilder(builder).credentialsProvider(credentialsProvider).build();
        CloudWatchAsyncClient cloudWatchClient = CloudWatchAsyncClient.builder().region(region).httpClientBuilder(builder).credentialsProvider(credentialsProvider).build();

        ConfigsBuilder configsBuilder = new ConfigsBuilder(streamName, applicationName, kinesisClient, dynamoClient, cloudWatchClient, UUID.randomUUID().toString(), shardRecordProcessorFactory);

        RetrievalConfig retrievalConfig;
        if (kinesisClientConfiguration.isUseConsumersWithEnhancedFanOut()) {
            retrievalConfig = configsBuilder.retrievalConfig();
        } else {
            KinesisPollingConfig config = new KinesisPollingConfig(streamName, kinesisClient);
            if (kinesisClientConfiguration.getMaxRecords() != null) {
                config.setMaxRecords(kinesisClientConfiguration.getMaxRecords());
            }
            if (kinesisClientConfiguration.getRequestTimeout() != null) {
                config.setKinesisRequestTimeout(Duration.ofSeconds(kinesisClientConfiguration.getRequestTimeout()));
            }
            retrievalConfig = configsBuilder.retrievalConfig().retrievalSpecificConfig(config);
        }

        scheduler = new Scheduler(
                configsBuilder.checkpointConfig(),
                configsBuilder.coordinatorConfig(),
                getLeaseManagementConfig(kinesisClientConfiguration, configsBuilder),
                configsBuilder.lifecycleConfig(),
                configsBuilder.metricsConfig(),
                configsBuilder.processorConfig(),
                retrievalConfig
        );

        schedulerThread = new Thread(scheduler);
        schedulerThread.setDaemon(true);
        schedulerThread.start();

    }

    private LeaseManagementConfig getLeaseManagementConfig(KinesisClientConfiguration kinesisClientConfiguration, ConfigsBuilder configsBuilder) {
        InitialPositionInStream initialPositionInStream;
        if (StringUtils.isNoneBlank(kinesisClientConfiguration.getInitialPositionInStream())) {
            initialPositionInStream = InitialPositionInStream.valueOf(kinesisClientConfiguration.getInitialPositionInStream());
        } else {
            initialPositionInStream = InitialPositionInStream.LATEST;
        }
        return configsBuilder
                .leaseManagementConfig()
                .initialPositionInStream(InitialPositionInStreamExtended.newInitialPosition(initialPositionInStream));
    }

    private AwsCredentialsProvider getConsumerCredentialsProvider(KinesisClientConfiguration kinesisClientConfiguration) {
        AwsCredentialsProvider credentialsProvider;
        if (kinesisClientConfiguration.isUseCredentialsFromInstanceMetadata()) {
            credentialsProvider = software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider.create();
        } else {
            credentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create(kinesisClientConfiguration.getAccessKeyId(), kinesisClientConfiguration.getSecretAccessKey()));
        }
        return credentialsProvider;
    }

    private void initProducer(KinesisClientConfiguration kinesisClientConfiguration) {
        KinesisProducerConfiguration config = new KinesisProducerConfiguration();
        config.setCredentialsProvider(getProducerCredentialsProvider(kinesisClientConfiguration));
        config.setRegion(kinesisClientConfiguration.getRegion());
        kinesisProducer = new KinesisProducer(config);
    }

    private AWSCredentialsProvider getProducerCredentialsProvider(KinesisClientConfiguration kinesisClientConfiguration) {
        AWSCredentialsProvider credentialsProvider;
        if (kinesisClientConfiguration.isUseCredentialsFromInstanceMetadata()) {
            credentialsProvider = InstanceProfileCredentialsProvider.getInstance();
        } else {
            BasicAWSCredentials awsCreds = new BasicAWSCredentials(kinesisClientConfiguration.getAccessKeyId(), kinesisClientConfiguration.getSecretAccessKey());
            credentialsProvider = new AWSStaticCredentialsProvider(awsCreds);
        }
        return credentialsProvider;
    }

    private class RecordProcessorFactory implements ShardRecordProcessorFactory {
        public ShardRecordProcessor shardRecordProcessor() {
            return new ShardRecordProcessor() {
                private String shardId;

                @Override
                public void initialize(InitializationInput initializationInput) {
                    shardId = initializationInput.shardId();
                    log.info("Initializing @ Sequence: {}", initializationInput.extendedSequenceNumber());
                }

                @Override
                public void processRecords(ProcessRecordsInput processRecordsInput) {
                    try {
                        log.debug("Processing {} record(s)", processRecordsInput.records().size());
                        processRecordsInput.records().forEach(r -> {
                            process(new KinesisIntegrationMsg(shardId, r.sequenceNumber(), r.data(), r.partitionKey()));
                        });
                    } catch (Throwable t) {
                        log.error("Caught throwable while processing records", t);
                    }
                }

                @Override
                public void leaseLost(LeaseLostInput leaseLostInput) {
                    log.debug("Lost lease, so terminating.");
                }

                @Override
                public void shardEnded(ShardEndedInput shardEndedInput) {
                    try {
                        log.debug("Reached shard end checkpointing.");
                        shardEndedInput.checkpointer().checkpoint();
                    } catch (ShutdownException | InvalidStateException e) {
                        log.error("Exception while checkpointing at shard end. Giving up.", e);
                    }
                }

                @Override
                public void shutdownRequested(ShutdownRequestedInput shutdownRequestedInput) {
                    try {
                        log.debug("Scheduler is shutting down, checkpointing.");
                        shutdownRequestedInput.checkpointer().checkpoint();
                    } catch (ShutdownException | InvalidStateException e) {
                        log.error("Exception while checkpointing at requested shutdown. Giving up.", e);
                    }
                }
            };
        }

    }

    @Override
    protected void doValidateConfiguration(JsonNode configuration, boolean allowLocalNetworkHosts) {
        try {
            getClientConfiguration(configuration.get("clientConfiguration"), KinesisClientConfiguration.class);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Kinesis Integration Configuration structure!");
        }
    }

    @Override
    public void update(TbIntegrationInitParams params) throws Exception {
        if (schedulerThread != null) {
            schedulerThread.interrupt();
        }

        if (scheduler != null && !scheduler.gracefuleShutdownStarted()) {
            Future<Boolean> gracefulShutdownFuture = scheduler.startGracefulShutdown();
            log.info("Waiting up to 20 seconds for shutdown to complete.");
            gracefulShutdownFuture.get(20, TimeUnit.SECONDS);
            log.info("Completed, shutting down now.");
        }

        init(params);
    }

    @Override
    public void destroy() {
        if (schedulerThread != null) {
            schedulerThread.interrupt();
        }

        if (scheduler != null) {
            Future<Boolean> gracefulShutdownFuture = scheduler.startGracefulShutdown();
            log.info("Waiting up to 20 seconds for shutdown to complete.");
            try {
                gracefulShutdownFuture.get(20, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.info("Interrupted while waiting for graceful shutdown. Continuing.");
            } catch (ExecutionException e) {
                log.error("Exception while executing graceful shutdown.", e);
            } catch (TimeoutException e) {
                log.error("Timeout while waiting for shutdown. Scheduler may not have exited.");
            }
            log.info("Completed, shutting down now.");
        }

        if (kinesisProducer != null) {
            kinesisProducer.destroy();
        }
    }

    @Override
    public void process(KinesisIntegrationMsg msg) {
        String status = "OK";
        Exception exception = null;
        try {
            doProcess(context, msg);
            integrationStatistics.incMessagesProcessed();
        } catch (Exception e) {
            log.warn("Failed to apply data converter function: {}", e.getMessage(), e);
            integrationStatistics.incErrorsOccurred();
            exception = e;
            status = "ERROR";
        }
        persistDebug(context, "Uplink", getDefaultUplinkContentType(), () -> JacksonUtil.toString(msg.toJson()), status, exception);
    }

    @Override
    public void onDownlinkMsg(IntegrationDownlinkMsg downlink) {
        TbMsg msg = downlink.getTbMsg();
        logDownlink(context, "Downlink: " + msg.getType(), msg);
        if (downlinkConverter != null) {
            processDownLinkMsg(context, msg);
        }
    }

    protected void processDownLinkMsg(IntegrationContext context, TbMsg msg) {
        String status = "OK";
        Exception exception = null;
        try {
            if (doProcessDownLinkMsg(context, msg)) {
                integrationStatistics.incMessagesProcessed();
            }
        } catch (Exception e) {
            log.warn("Failed to process downLink message", e);
            exception = e;
            status = "ERROR";
        }
        reportDownlinkError(context, msg, status, exception);
    }

    private void doProcess(IntegrationContext context, KinesisIntegrationMsg msg) throws Exception {
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        mdMap.put("partitionKey", msg.getPartitionKey());
        mdMap.put("sequenceNumber", msg.getSequenceNumber());
        List<UplinkData> uplinkDataList = convertToUplinkDataList(context, msg.getPayload(), new UplinkMetaData<>(getDefaultUplinkContentType(), mdMap));
        if (uplinkDataList != null) {
            for (UplinkData data : uplinkDataList) {
                processUplinkDataBlocking(context, data);
                log.trace("[{}] Processing uplink data: {}", configuration.getId(), data);
            }
        }
    }

    private boolean doProcessDownLinkMsg(IntegrationContext context, TbMsg msg) throws Exception {
        FutureCallback<UserRecordResult> producerCallback = new FutureCallback<>() {
            @Override
            public void onFailure(Throwable t) {
                reportDownlinkError(context, msg, "ERROR", new Exception(t));
            }

            @Override
            public void onSuccess(UserRecordResult result) {
            }

        };

        Map<KinesisProducerKey, List<DownlinkData>> producerKeyToDataMap = convertDownLinkMsg(context, msg);
        for (Map.Entry<KinesisProducerKey, List<DownlinkData>> producerKeyEntry : producerKeyToDataMap.entrySet()) {
            for (DownlinkData data : producerKeyEntry.getValue()) {
                KinesisProducerKey producerKey = producerKeyEntry.getKey();
                logKinesisDownlink(context, producerKey, data);
                ByteBuffer dataPayload = ByteBuffer.wrap(data.getData());
                ListenableFuture<UserRecordResult> f = kinesisProducer.addUserRecord(producerKey.getStreamName(), producerKey.getPartitionKey(), dataPayload);
                Futures.addCallback(f, producerCallback, MoreExecutors.directExecutor());
            }
        }
        return !producerKeyToDataMap.isEmpty();
    }

    private Map<KinesisProducerKey, List<DownlinkData>> convertDownLinkMsg(IntegrationContext context, TbMsg msg) throws Exception {
        Map<KinesisProducerKey, List<DownlinkData>> producerKeyToDataMap = new HashMap<>();
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        List<DownlinkData> result = downlinkConverter.convertDownLink(context.getDownlinkConverterContext(), Collections.singletonList(msg), new IntegrationMetaData(mdMap));
        for (DownlinkData data : result) {
            if (!data.isEmpty()) {
                String streamName = compileDownlinkStreamName(data.getMetadata());
                String partitionKey = compileDownlinkPartitionKey(data.getMetadata());
                KinesisProducerKey key = new KinesisProducerKey(streamName, partitionKey);
                producerKeyToDataMap.computeIfAbsent(key, k -> new ArrayList<>()).add(data);
            }
        }
        return producerKeyToDataMap;
    }

    private String compileDownlinkPartitionKey(Map<String, String> md) {
        return compileDownlinkByPattern(md, DEFAULT_DOWNLINK_PARTITION_KEY_PATTERN);
    }

    private String compileDownlinkStreamName(Map<String, String> md) {
        return compileDownlinkByPattern(md, DEFAULT_DOWNLINK_STREAM_NAME_PATTERN);
    }

    private String compileDownlinkByPattern(Map<String, String> md, String defaultPattern) {
        if (md != null) {
            String result = defaultPattern;
            for (Map.Entry<String, String> mdEntry : md.entrySet()) {
                String key = "${" + mdEntry.getKey() + "}";
                result = result.replace(key, mdEntry.getValue());
            }
            return result;
        }
        return defaultPattern;
    }

    private void logKinesisDownlink(IntegrationContext context, KinesisProducerKey producerKey, DownlinkData data) {
        String status = downlinkConverter != null ? "OK" : "FAILURE";
        Supplier<String> msgSupplier = () -> {
            ObjectNode json = JacksonUtil.newObjectNode();
            json.put("streamName", producerKey.getStreamName());
            json.put("partitionKey", producerKey.getPartitionKey());
            json.set("payload", getDownlinkPayloadJson(data));
            return JacksonUtil.toString(json);
        };
        persistDebug(context, "Downlink", ContentType.JSON, msgSupplier, status, null);
    }

}
