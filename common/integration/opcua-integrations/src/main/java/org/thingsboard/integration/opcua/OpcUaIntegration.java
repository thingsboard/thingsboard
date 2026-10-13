// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.opcua;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.sdk.core.nodes.VariableNode;
import org.eclipse.milo.opcua.stack.client.DiscoveryClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodResult;
import org.eclipse.milo.opcua.stack.core.types.structured.CallResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteValue;
import org.eclipse.milo.opcua.stack.core.util.ConversionUtil;
import org.thingsboard.common.util.DonAsynchron;
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
import org.thingsboard.integration.api.util.ExceptionUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.thingsboard.integration.opcua.OpcUaIntegrationTask.CONNECT;
import static org.thingsboard.integration.opcua.OpcUaIntegrationTask.DISCONNECT;

@Slf4j
public class OpcUaIntegration extends AbstractIntegration<OpcUaIntegrationMsg> {

    private static final int MIN_DELAY_BETWEEN_RECONNECTS_IN_SEC = 10;
    private static final int MAX_DELAY_BETWEEN_RECONNECTS_IN_SEC = 600;
    private final AtomicLong clientHandles = new AtomicLong(1L);
    private OpcUaServerConfiguration opcUaServerConfiguration;
    private volatile OpcUaClient client;
    private UaSubscription subscription;
    private final Map<NodeId, OpcUaDevice> devices = new ConcurrentHashMap<>();
    private final Map<NodeId, List<OpcUaDevice>> devicesByTags = new ConcurrentHashMap<>();
    private volatile Map<Pattern, DeviceMapping> mappings;
    // This variable describes the state of integration, whether it has to run or shut down, not the state of the opc-ua client
    private volatile boolean connected = false;
    private volatile ScheduledFuture<?> nextPollFuture;

    private final AtomicInteger delayBetweenReconnects = new AtomicInteger(10);
    private final BlockingQueue<OpcUaIntegrationTask> taskQueue = new LinkedBlockingQueue<>();
    private final Lock taskLock = new ReentrantLock();
    private static final String OPC_TCP_SCHEME = "opc.tcp://";


    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        opcUaServerConfiguration = getClientConfiguration(configuration, OpcUaServerConfiguration.class);
        if (opcUaServerConfiguration.getMapping().isEmpty()) {
            throw new IllegalArgumentException("No mapping elements configured!");
        }
        opcUaServerConfiguration.getMapping().forEach(DeviceMapping::initMappingPatterns);
        this.mappings = opcUaServerConfiguration.getMapping().stream().collect(Collectors.toConcurrentMap(m -> Pattern.compile(m.getDeviceNodePattern()), Function.identity()));
        submit(CONNECT);
    }

    @Override
    protected void doValidateConfiguration(JsonNode configuration, boolean allowLocalNetworkHosts) {
        OpcUaServerConfiguration opcUaServerConfiguration;
        try {
            opcUaServerConfiguration = getClientConfiguration(configuration.get("clientConfiguration"), OpcUaServerConfiguration.class);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
            throw new IllegalArgumentException("Invalid OPC-UA Integration Configuration structure!");
        }
        if (!allowLocalNetworkHosts && isLocalNetworkHost(opcUaServerConfiguration.getHost())) {
            throw new IllegalArgumentException("Usage of local network host for OPC-UA server connection is not allowed!");
        }
    }

    @Override
    public void process(OpcUaIntegrationMsg msg) {
        String status = "OK";
        Exception exception = null;
        try {
            doProcess(context, msg);
            integrationStatistics.incMessagesProcessed();
        } catch (Exception e) {
            log.warn("[{}] Failed to apply data converter function", getConfigurationId(), e);
            exception = e;
            status = "ERROR";
        }
        if (!status.equals("OK")) {
            integrationStatistics.incErrorsOccurred();
        }
        persistDebug(context, "Uplink", getDefaultUplinkContentType(), () -> JacksonUtil.toString(msg.toJson()), status, exception);
    }

    private void doProcess(IntegrationContext context, OpcUaIntegrationMsg msg) throws Exception {
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        mdMap.putAll(msg.getDeviceMetadata());
        List<UplinkData> uplinkDataList = convertToUplinkDataList(context, msg.getPayload(), new UplinkMetaData<>(getDefaultUplinkContentType(), mdMap));
        if (uplinkDataList != null) {
            for (UplinkData data : uplinkDataList) {
                log.trace("[{}] Processing uplink data: {}", getConfigurationId(), data);
                processUplinkDataBlocking(context, data);
            }
        }
    }

    @Override
    public void onDownlinkMsg(IntegrationDownlinkMsg downlink) {
        TbMsg msg = downlink.getTbMsg();
        try {
            if (!connected) {
                persistDebug(context, "Downlink", "ERROR", null,
                        null, "FAILURE",
                        new OpcUaIntegrationException("Cannot process downlink message because of connection was lost.", new RuntimeException()));
                return;
            }

            logDownlink(context, "Downlink: " + msg.getType(), msg);
            if (downlinkConverter != null) {
                processDownLinkMsg(context, msg);
            }
        } catch (Exception ex) {
            reportDownlinkError(context, msg, "ERROR", ex);
        }
    }

    private void processDownLinkMsg(IntegrationContext context, TbMsg msg) throws Exception {
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        List<DownlinkData> result = downlinkConverter.convertDownLink(context.getDownlinkConverterContext(), Collections.singletonList(msg), new IntegrationMetaData(mdMap));
        List<WriteValue> writeValues = prepareWriteValues(result);
        List<CallMethodRequest> callMethods = prepareCallMethods(result);

        if (writeValues.isEmpty() && callMethods.isEmpty()) {
            return;
        }

        DonAsynchron.withCallback(doProcessDownLinkMsg(writeValues, callMethods), processResult -> {
            integrationStatistics.incMessagesProcessed();
            logOpcUaDownlink(context, writeValues, callMethods);
        }, ex -> reportDownlinkError(context, msg, "ERROR", ex), MoreExecutors.directExecutor());
    }

    private ListenableFuture<Void> doProcessDownLinkMsg(List<WriteValue> writeValues, List<CallMethodRequest> callMethods) {
        FluentFuture<WriteResponse> writeFuture = DonAsynchron.toFluentFuture(client.write(writeValues));
        FluentFuture<CallResponse> callFuture = DonAsynchron.toFluentFuture(client.call(callMethods));
        return Futures.whenAllComplete(writeFuture, callFuture).call(() -> {
            List<String> errs = new ArrayList<>();
            WriteResponse writeResponse = writeFuture.get();
            CallResponse callResponse = callFuture.get();

            StatusCode[] codes = writeResponse.getResults();
            for (int i = 0; i < codes.length; i++) {
                StatusCode sc = codes[i];
                if (!sc.isGood()) {
                    long code = sc.getValue();
                    String statusName = StatusCodes.lookup(code)
                            .map(arr -> arr.length > 0 ? arr[0] : null)
                            .orElse(String.format("0x%08X", code));
                    errs.add("Write " + writeValues.get(i).getNodeId() + ", status = " + statusName);
                }
            }

            CallMethodResult[] res = callResponse.getResults();
            for (int i = 0; i < res.length; i++) {
                CallMethodRequest req = callMethods.get(i);
                CallMethodResult r = res[i];
                if (!r.getStatusCode().isGood()) {
                    errs.add("Call" + req.getObjectId() + "#" + req.getMethodId() + ", " + r.getStatusCode());
                }
                StatusCode[] argCodes = r.getInputArgumentResults();
                if (argCodes != null) {
                    for (int a = 0; a < argCodes.length; a++) {
                        if (!argCodes[a].isGood()) {
                            errs.add("Call arg[" + a + "] -> " + argCodes[a]);
                        }
                    }
                }
            }

            if (!errs.isEmpty()) {
                throw new OpcUaIntegrationException(String.join("\n", errs));
            }

            return null;
        }, MoreExecutors.directExecutor());
    }

    private void submit(OpcUaIntegrationTask task) {
        submit(task, 0);
    }

    private void submit(OpcUaIntegrationTask task, int delayInSec) {
        if (delayInSec > 0) {
            log.debug("[{}] Adding task to queue: {} with delay {}", configuration.getId(), task, delayInSec);
        } else {
            log.debug("[{}] Adding task to queue: {}", configuration.getId(), task);
        }
        if (nextPollFuture != null) {
            nextPollFuture.cancel(true);
        }
        if (DISCONNECT.equals(task)) {
            taskQueue.removeIf(Objects::nonNull);
        }
        taskQueue.add(task);
        log.debug("[{}] queue size: {}", configuration.getId(), taskQueue.size());
        if (delayInSec > 0) {
            nextPollFuture = context.getScheduledExecutorService().schedule(this::submitPoll, delayInSec, TimeUnit.SECONDS);
        } else {
            submitPoll();
        }
    }

    private void submitPoll() {
        context.getExecutorService().execute(this::pollTask);
    }

    private void pollTask() {
        taskLock.lock();
        try {
            OpcUaIntegrationTask task = taskQueue.poll();
            if (task != null) {
                deduplicate(task);
                log.debug("[{}] Going to process task: {}", configuration.getId(), task);
                processTask(task);
            }
        } catch (Exception e) {
            log.warn("[{}] Unhandled error during task processing: ", configuration.getId(), e);
        } finally {
            taskLock.unlock();
        }
    }

    private void deduplicate(OpcUaIntegrationTask task) {
        while (true) {
            OpcUaIntegrationTask next = taskQueue.peek();
            if (task.equals(next)) {
                log.debug("[{}] Remove duplicated task from queue: {}", getConfigurationId(), next);
                taskQueue.poll();
            } else {
                break;
            }
        }
    }

    private void processTask(OpcUaIntegrationTask task) {
        switch (task) {
            case CONNECT:
                doConnect();
                break;
            case DISCONNECT:
                doDisconnect();
                break;
            case SCAN:
                scanForDevices();
                break;
        }
    }

    private void initClient(OpcUaServerConfiguration configuration) throws OpcUaIntegrationException {
        try {
            String effectiveEndpoint = buildEffectiveEndpoint(configuration);
            log.info("[{}] Initializing OPC-UA connection to [{}] (host={}, port={})", getConfigurationId(), effectiveEndpoint, configuration.getHost(), configuration.getPort());

            SecurityPolicy securityPolicy = SecurityPolicy.valueOf(configuration.getSecurity());
            IdentityProvider identityProvider = configuration.getIdentity().toProvider();

            List<EndpointDescription> endpoints;
            try {
                endpoints = DiscoveryClient.getEndpoints(effectiveEndpoint).get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("[{}] Failed to connect to provided endpoint! With error: [{}]", getConfigurationId(), e.getMessage());
                throw new OpcUaIntegrationException("Failed to connect to provided endpoint: " + effectiveEndpoint, e);
            }
            log.info("Endpoints processing finished. Processed endpoints count: {}", endpoints.size());

            EndpointDescription endpoint = endpoints.stream()
                    .filter(e -> e.getSecurityPolicyUri().equals(securityPolicy.getUri()))
                    .findFirst()
                    .orElseThrow(() -> new Exception("no desired endpoints returned"));

            if (!endpoint.getEndpointUrl().equals(effectiveEndpoint)) {
                endpoint = new EndpointDescription(
                        effectiveEndpoint,
                        endpoint.getServer(),
                        endpoint.getServerCertificate(),
                        endpoint.getSecurityMode(),
                        endpoint.getSecurityPolicyUri(),
                        endpoint.getUserIdentityTokens(),
                        endpoint.getTransportProfileUri(),
                        endpoint.getSecurityLevel());
            }

            OpcUaClientConfigBuilder configBuilder = OpcUaClientConfig.builder()
                    .setApplicationName(LocalizedText.english(configuration.getApplicationName()))
                    .setApplicationUri(configuration.getApplicationUri())
                    .setEndpoint(endpoint)
                    .setIdentityProvider(identityProvider)
                    .setRequestTimeout(Unsigned.uint(configuration.getTimeoutInMillis()));

            if (securityPolicy != SecurityPolicy.None) {
                CertificateInfo certificate = OpcUaConfigurationTools.loadCertificate(configuration.getKeystore());
                configBuilder.setCertificate(certificate.getCertificate())
                        .setKeyPair(certificate.getKeyPair());
            }

            OpcUaClientConfig config = configBuilder.build();

            log.info("Creating client...");

            client = OpcUaClient.create(config);
            client.connect().get(30, TimeUnit.SECONDS);
            log.info("[{}] OPC-UA Client connected successfully!", getConfigurationId());
            sendConnectionSucceededMessageToRuleEngine();
            subscription = client.getSubscriptionManager().createSubscription(1000.0).get();
            connected = true;
        } catch (TimeoutException e) {
            throw new OpcUaIntegrationException("Failed to connect to OPC-UA server - timeout.");
        } catch (Exception e) {
            log.debug("[{}] Failed to connect to OPC-UA server. Reason: {}", getConfigurationId(), e.getMessage(), e);
            throw new OpcUaIntegrationException("Failed to connect to OPC-UA server. Reason: " + e.getMessage(), e);
        }
    }

    private String buildEffectiveEndpoint(OpcUaServerConfiguration cfg) {
        String url = OPC_TCP_SCHEME + cfg.getHost() + ":" + cfg.getPort();
        if (StringUtils.isNotBlank(cfg.getEndpoint())) {
            url += "/" + StringUtils.removeStart(cfg.getEndpoint().trim(), "/");
        }
        return url;
    }

    private void sendConnectionSucceededMessageToRuleEngine() {
        log.info("[{}] Sending OPC-UA integration succeeded message to Rule Engine", getConfigurationId());
        TbMsg tbMsg = sendAlertToRuleEngine(TbMsgType.OPC_UA_INT_SUCCESS);
        persistDebug(context, "CONNECT", ContentType.JSON, tbMsg.getData(), "SUCCESS", null);
    }

    private void sendConnectionFailedMessageToRuleEngine(Exception e) {
        log.warn("[{}] Sending OPC-UA integration failed message to Rule Engine", getConfigurationId());
        TbMsg tbMsg = sendAlertToRuleEngine(TbMsgType.OPC_UA_INT_FAILURE);
        persistDebug(context, "CONNECT", ContentType.JSON, tbMsg.getData(), "FAILURE", e);
    }

    private TbMsg sendAlertToRuleEngine(TbMsgType messageType) {
        TbMsgMetaData tbMsgMetaData = new TbMsgMetaData();
        tbMsgMetaData.putValue("name", this.configuration.getName());
        tbMsgMetaData.putValue("id", this.configuration.getId().getId().toString());
        tbMsgMetaData.putValue("host", opcUaServerConfiguration.getHost());
        tbMsgMetaData.putValue("port", Integer.toString(opcUaServerConfiguration.getPort()));
        TbMsg tbMsg = TbMsg.newMsg()
                .type(messageType)
                .originator(this.configuration.getId())
                .copyMetaData(tbMsgMetaData)
                .dataType(TbMsgDataType.JSON)
                .data("{}")
                .build();

        if (context != null) {
            context.processCustomMsg(tbMsg, null);
        }
        return tbMsg;
    }

    @Override
    public void destroy() {
        submit(DISCONNECT);
    }

    private void doDisconnect() {
        if (connected) {
            try {
                connected = false;
                if (client != null) {
                    try {
                        if (subscription != null) {
                            client.deleteSubscriptions(Collections.singletonList(subscription.getSubscriptionId()));
                        }
                        client.disconnect().get(10, TimeUnit.SECONDS);
                    } finally {
                        client = null;
                        subscription = null;
                    }
                    log.info("[{}] OPC-UA client disconnected", this.configuration.getId());
                }
            } catch (Exception e) {
                log.warn("[{}] Failed to disconnect", this.configuration.getId(), e);
            }
        }
    }

    private void doConnect() {
        if (connected) {
            log.info("[{}] Ignore connect task because client is already connected", configuration.getId());
            return;
        }
        devices.clear();
        devicesByTags.clear();
        try {
            initClient(opcUaServerConfiguration);
            delayBetweenReconnects.set(MIN_DELAY_BETWEEN_RECONNECTS_IN_SEC);
            submit(OpcUaIntegrationTask.SCAN);
            log.info("[{}] OPC-UA client was connected successfully to server {}", configuration.getId(), opcUaServerConfiguration.getHost());
        } catch (OpcUaIntegrationException e) {
            sendConnectionFailedMessageToRuleEngine(e);
            int currentDelayBetweenReconnectAttempt = delayBetweenReconnects.get();
            if (currentDelayBetweenReconnectAttempt < MAX_DELAY_BETWEEN_RECONNECTS_IN_SEC) {
                delayBetweenReconnects.set(currentDelayBetweenReconnectAttempt + 10);
            }
            submit(CONNECT, currentDelayBetweenReconnectAttempt);
        }
    }

    private void scanForDevices() {
        if (!connected) {
            return;
        }
        boolean requiresReconnect = false;
        try {
            long startTs = System.currentTimeMillis();
            ScanExceptions exceptions = scanForDevices(new OpcUaNode(Identifiers.RootFolder, ""));
            log.debug("[{}] Device scan cycle completed in {} ms", this.configuration.getId(), (System.currentTimeMillis() - startTs));
            List<OpcUaDevice> deleted = devices.values().stream().filter(opcUaDevice -> opcUaDevice.getScanTs() < startTs).collect(Collectors.toList());
            if (!deleted.isEmpty()) {
                log.info("[{}] Devices {} are no longer available", this.configuration.getId(), deleted);
            }
            deleted.stream().map(OpcUaDevice::getNodeId).forEach(devices::remove);

            if (exceptions.getCritical() != null) {
                var e = exceptions.getCritical();
                UaException uaException = ExceptionUtil.lookupException(e.getCause(), UaException.class);
                if (uaException != null) {
                    e.getNode().ifPresent(node -> log.error(String.format("[%s] Browsing nodeId=%s failed: %s", this.configuration.getName(), node.getNodeId(), uaException.getMessage()), uaException));
                    sendConnectionFailedMessageToRuleEngine(e);
                    submit(DISCONNECT);
                    submit(CONNECT, MIN_DELAY_BETWEEN_RECONNECTS_IN_SEC);
                    requiresReconnect = true;
                }
            }
        } catch (Throwable e) {
            log.warn("[{}] Periodic device scan failed!", this.configuration.getId(), e);
        }
        if (!requiresReconnect) {
            log.debug("[{}] Scheduling next scan in {} seconds!", this.configuration.getId(), opcUaServerConfiguration.getScanPeriodInSeconds());
            submit(OpcUaIntegrationTask.SCAN, opcUaServerConfiguration.getScanPeriodInSeconds());
        }
    }

    private boolean scanForChildren(OpcUaNode opcUaNode, DeviceMapping deviceMapping) {
        if (opcUaNode.getFqn().isEmpty()) {
            return true;
        }
        if (deviceMapping.getMappingType() == DeviceMappingType.FQN) {
            String[] opcUaFqnLevels = opcUaNode.getFqn().split("\\.");
            for (int i = 0; i < opcUaFqnLevels.length; i++) {
                if (deviceMapping.getMappingPathPatterns().size() > i) {
                    Pattern pattern = deviceMapping.getMappingPathPatterns().get(i);
                    if (!pattern.matcher(opcUaFqnLevels[i]).matches()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean scanById(OpcUaNode node, Map.Entry<Pattern, DeviceMapping> mappingEntry) {
        if (mappingEntry.getValue().getNamespace() != null) {
            return node.getNodeId().getNamespaceIndex().intValue() == mappingEntry.getValue().getNamespace()
                    && mappingEntry.getKey().matcher(node.getNodeId().getIdentifier().toString()).matches();
        } else {
            return mappingEntry.getKey().matcher(node.getNodeId().getIdentifier().toString()).matches();
        }
    }

    private boolean scanByFqn(OpcUaNode node, Map.Entry<Pattern, DeviceMapping> mappingEntry) {
        return mappingEntry.getKey().matcher(node.getFqn()).matches();
    }

    private BiFunction<OpcUaNode, Map.Entry<Pattern, DeviceMapping>, Boolean> getMatchingFunction(DeviceMappingType mappingType) {
        return switch (mappingType) {
            case ID -> this::scanById;
            case FQN -> this::scanByFqn;
            default -> throw new IllegalArgumentException("unknown DeviceMappingType: " + mappingType);
        };
    }

    private ScanExceptions scanForDevices(OpcUaNode node) {
        var errors = new ScanExceptions();
        scanForDevices(node, errors);
        return errors;
    }

    private void scanForDevices(OpcUaNode node, ScanExceptions errors) {
        log.debug("[{}] Scanning node: {}", getConfigurationId(), node);
        List<DeviceMapping> matchedMappings = new ArrayList<>();
        boolean scanChildren = false;
        for (Map.Entry<Pattern, DeviceMapping> mappingEntry : mappings.entrySet()) {
            if (getMatchingFunction(mappingEntry.getValue().getMappingType()).apply(node, mappingEntry)) {
                matchedMappings.add(mappingEntry.getValue());
            } else {
                scanChildren = scanChildren || scanForChildren(node, mappingEntry.getValue());
            }
        }

        matchedMappings.forEach(m -> {
            if (errors.getCritical() != null) {
                return;
            }
            try {
                log.debug("[{}] Matched mapping: [{}]", getConfigurationId(), m);
                scanDevice(node, m, errors);
            } catch (Exception e) {
                log.error("[{}] Failed to scan device: {}", getConfigurationId(), node, e);
                errors.add(new OpcUaIntegrationException(node, "Failed to scan device", e));
            }
        });
        if (scanChildren) {
            try {
                BrowseResult browseResult = client.browse(getBrowseDescription(node.getNodeId())).get();
                List<ReferenceDescription> references = ConversionUtil.toList(browseResult.getReferences());
                for (ReferenceDescription rd : references) {
                    if (rd.getNodeId().isLocal()) {
                        rd.getNodeId().toNodeId(client.getNamespaceTable()).ifPresent(childNodeId ->
                                scanForDevices(new OpcUaNode(node, childNodeId, rd.getBrowseName().getName()), errors));
                    } else {
                        log.trace("[{}] Ignoring remote node: {}", getConfigurationId(), rd.getNodeId());
                    }
                }
            } catch (Exception e) {
                errors.add(new OpcUaIntegrationException(node, "Failed to browse node", e));
            }
        } else {
            log.debug("[{}] Skip scanning children for node: {}", getConfigurationId(), node);
        }
    }

    private String getConfigurationId() {
        return this.configuration.getName();
    }

    private void scanDevice(OpcUaNode node, DeviceMapping m, ScanExceptions errors) throws Exception {
        log.debug("[{}] Scanning device node: {}", getConfigurationId(), node);
        Set<String> tags = m.getAllTags();
        log.debug("[{}] Scanning node hierarchy for tags: {}", getConfigurationId(), tags);
        Map<String, NodeId> tagMap = lookupTags(node.getNodeId(), node.getName(), tags, errors);
        log.debug("[{}] Scanned {} tags out of {}", getConfigurationId(), tagMap.size(), tags.size());

        OpcUaDevice device;
        if (devices.containsKey(node.getNodeId())) {
            device = devices.get(node.getNodeId());
        } else {
            device = new OpcUaDevice(node, m);
            devices.put(node.getNodeId(), device);
            Map<String, NodeId> newTags = device.registerTags(tagMap);
            if (!newTags.isEmpty()) {
                for (NodeId tagId : newTags.values()) {
                    devicesByTags.computeIfAbsent(tagId, key -> new ArrayList<>()).add(device);
                    if (client != null && client.getAddressSpace() != null) {
                        VariableNode varNode = client.getAddressSpace().getVariableNode(tagId);
                        DataValue dataValue = varNode.getValue();
                        if (dataValue != null) {
                            device.updateTag(tagId, dataValue);
                        }
                    } else {
                        String msg = String.format("Error scan device: Client: [%s] Address Space: [%s]", client, client != null ? client.getAddressSpace() : null);
                        log.error(msg);
                    }
                }
                log.debug("[{}] Going to subscribe to tags: {}", getConfigurationId(), newTags);
                subscribeToTags(newTags);
            }
            onDeviceDataUpdate(device, null);
        }

        device.updateScanTs();

        Map<String, NodeId> newTags = device.registerTags(tagMap);
        if (!newTags.isEmpty()) {
            for (NodeId tagId : newTags.values()) {
                devicesByTags.computeIfAbsent(tagId, key -> new ArrayList<>()).add(device);
            }
            log.debug("[{}] Going to subscribe to tags: {}", getConfigurationId(), newTags);
            subscribeToTags(newTags);
        }
    }

    private void onDeviceDataUpdate(OpcUaDevice device, NodeId affectedTagId) {
        OpcUaIntegrationMsg message = device.prepareMsg(affectedTagId);
        if (context != null) {
            process(message);
        }
    }

    private void subscribeToTags(Map<String, NodeId> newTags) throws InterruptedException, ExecutionException, TimeoutException {
        List<MonitoredItemCreateRequest> requests = new ArrayList<>();
        for (Map.Entry<String, NodeId> kv : newTags.entrySet()) {
            // subscribe to the Value attribute of the server's CurrentTime node
            ReadValueId readValueId = new ReadValueId(
                    kv.getValue(),
                    AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE);
            // important: client handle must be unique per item
            UInteger clientHandle = Unsigned.uint(clientHandles.getAndIncrement());

            MonitoringParameters parameters = new MonitoringParameters(
                    clientHandle,
                    1000.0,     // sampling interval
                    null,       // filter, null means use default
                    Unsigned.uint(10),   // queue size
                    true        // discard oldest
            );

            requests.add(new MonitoredItemCreateRequest(
                    readValueId, MonitoringMode.Reporting, parameters));
        }

        List<UaMonitoredItem> items = subscription.createMonitoredItems(
                TimestampsToReturn.Both,
                requests,
                (item, i) -> item.setValueConsumer(OpcUaIntegration.this::onSubscriptionValue)
        ).get(5, TimeUnit.SECONDS);

        for (UaMonitoredItem item : items) {
            if (item.getStatusCode().isGood()) {
                log.trace("[{}] Monitoring Item created for nodeId={}", getConfigurationId(), item.getReadValueId().getNodeId());
            } else {
                log.warn("[{}] Failed to create item for nodeId={} (status={})", getConfigurationId(),
                        item.getReadValueId().getNodeId(), item.getStatusCode());
            }
        }
    }

    private void onSubscriptionValue(UaMonitoredItem item, DataValue dataValue) {
        try {
            if (context != null && !context.isClosed()) {
                log.debug("[{}] Subscription value received: item={}, value={}", getConfigurationId(),
                        item.getReadValueId().getNodeId(), dataValue.getValue());
                NodeId tagId = item.getReadValueId().getNodeId();
                devicesByTags.getOrDefault(tagId, Collections.emptyList()).forEach(
                        device -> {
                            device.updateTag(tagId, dataValue);
                            onDeviceDataUpdate(device, tagId);
                        }
                );
            }
        } catch (Exception e) {
            log.warn("[{}] Failed to process subscription value [{}][{}]", getConfigurationId(), item.getReadValueId().getNodeId(), item.getStatusCode());
        }
    }

    private Map<String, NodeId> lookupTags(NodeId nodeId, String deviceNodeName, Set<String> tags, ScanExceptions errors) {
        Map<String, NodeId> values = new HashMap<>();
        if (errors.getCritical() != null) {
            return values;
        }
        try {
            BrowseResult browseResult = client.browse(getBrowseDescription(nodeId)).get(5, TimeUnit.SECONDS);
            List<ReferenceDescription> references = ConversionUtil.toList(browseResult.getReferences());

            for (ReferenceDescription rd : references) {
                NodeId childId;
                if (rd.getNodeId().isLocal()) {
                    childId = rd.getNodeId().toNodeId(client.getNamespaceTable()).get();
                } else {
                    log.trace("[{}] Ignoring remote node: {}", getConfigurationId(), rd.getNodeId());
                    continue;
                }

                String name;
                String childIdStr = childId.getIdentifier().toString();
                if (childIdStr.contains(deviceNodeName)) {
                    name = childIdStr.substring(childIdStr.indexOf(deviceNodeName) + deviceNodeName.length() + 1);
                } else {
                    name = rd.getBrowseName().getName();
                }

                if (StringUtils.isEmpty(name) || name.endsWith("//")) {
                    log.trace("[{}] Ignoring self-referenced node: {}", getConfigurationId(), rd.getNodeId());
                    continue;
                }

                log.trace("[{}] Found tag: [{}].[{}]", getConfigurationId(), nodeId, name);
                if (tags.contains(name)) {
                    values.put(name, childId);
                }
                // recursively browse children
                values.putAll(lookupTags(childId, deviceNodeName, tags, errors));
            }
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            errors.add(new OpcUaIntegrationException(String.format("[%s] Browsing nodeId=%s failed: %s", getConfigurationId(), nodeId, e.getMessage()), e));
        }
        return values;
    }

    private BrowseDescription getBrowseDescription(NodeId nodeId) {
        return new BrowseDescription(
                nodeId,
                BrowseDirection.Forward,
                Identifiers.References,
                true,
                Unsigned.uint(NodeClass.Object.getValue() | NodeClass.Variable.getValue()),
                Unsigned.uint(BrowseResultMask.All.getValue())
        );
    }

    private List<WriteValue> prepareWriteValues(List<DownlinkData> dataList) throws OpcUaIntegrationException {
        List<WriteValue> writeValuesList = new ArrayList<>();
        for (DownlinkData data : dataList) {
            if (!data.isEmpty() && data.getContentType().equals("JSON")) {
                JsonNode payload = JacksonUtil.fromBytes(data.getData());
                if (payload.has("writeValues")) {
                    JsonNode writeValues = payload.get("writeValues");
                    if (writeValues.isArray()) {
                        for (JsonNode writeValueJson : writeValues) {
                            Optional<NodeId> nodeId = Optional.empty();
                            Optional<Variant> variantValue = Optional.empty();
                            if (writeValueJson.has("nodeId")) {
                                String nodeIdStr = writeValueJson.get("nodeId").asText();
                                try {
                                    nodeId = NodeId.parseSafe(nodeIdStr);
                                } catch (Exception e) {
                                    throw new OpcUaIntegrationException("Invalid OPC UA writeValues payload: failed to parse nodeId from value " + nodeIdStr + " Node: " + writeValueJson, e);
                                }
                            }
                            variantValue = toVariant(writeValueJson);
                            if (nodeId.isPresent() && variantValue.isPresent()) {
                                WriteValue writeValue = new WriteValue(
                                        nodeId.get(), AttributeId.Value.uid(), null, DataValue.valueOnly(variantValue.get()));
                                writeValuesList.add(writeValue);
                            }
                        }
                    }
                }
            }
        }
        return writeValuesList;
    }

    private List<CallMethodRequest> prepareCallMethods(List<DownlinkData> dataList) throws OpcUaIntegrationException {
        List<CallMethodRequest> callMethodRequests = new ArrayList<>();
        for (DownlinkData data : dataList) {
            if (!data.isEmpty() && data.getContentType().equals("JSON")) {
                JsonNode payload = JacksonUtil.fromBytes(data.getData());
                if (payload.has("callMethods")) {
                    JsonNode callMethods = payload.get("callMethods");
                    if (callMethods.isArray()) {
                        for (JsonNode callMethodJson : callMethods) {
                            Optional<NodeId> objectId = Optional.empty();
                            Optional<NodeId> methodId = Optional.empty();
                            Optional<Variant[]> arguments = Optional.empty();
                            if (callMethodJson.has("objectId")) {
                                String objectIdStr = callMethodJson.get("objectId").asText();
                                try {
                                    objectId = NodeId.parseSafe(objectIdStr);
                                } catch (Exception e) {
                                    throw new OpcUaIntegrationException("Invalid OPC UA callMethods payload: failed to parse objectId NodeId from value " + objectIdStr + " Node: " + callMethodJson, e);
                                }
                            }
                            if (callMethodJson.has("methodId")) {
                                String methodIdStr = callMethodJson.get("methodId").asText();
                                try {
                                    methodId = NodeId.parseSafe(methodIdStr);
                                } catch (Exception e) {
                                    throw new OpcUaIntegrationException("Invalid OPC UA callMethods payload: failed to parse methodId NodeId from value '" + methodIdStr + " Node: " + callMethodJson, e);
                                }
                            }
                            if (callMethodJson.has("args")) {
                                JsonNode argsJson = callMethodJson.get("args");
                                if (argsJson.isArray()) {
                                    List<Variant> argsList = new ArrayList<>();
                                    for (JsonNode argJson : argsJson) {
                                        if (argJson.isObject()) {
                                            Optional<Variant> argument = toVariant(argJson);
                                            argument.ifPresent(argsList::add);
                                        } else {
                                            Variant value = new Variant(extractValue(argJson));
                                            argsList.add(value);
                                        }
                                    }
                                    arguments = Optional.of(argsList.toArray(new Variant[]{}));
                                }
                            }
                            if (objectId.isPresent() && methodId.isPresent()) {
                                Variant[] args = arguments.orElseGet(() -> new Variant[]{});
                                CallMethodRequest callMethodRequest = new CallMethodRequest(objectId.get(), methodId.get(), args);
                                callMethodRequests.add(callMethodRequest);
                            }
                        }
                    }
                }
            }
        }
        return callMethodRequests;
    }

    private Optional<Variant> toVariant(JsonNode wrapper) throws OpcUaIntegrationException {
        if (!wrapper.has("value")) {
            return Optional.empty();
        }

        JsonNode valueJson = wrapper.get("value");
        Object value = extractValue(valueJson);
        if (wrapper.hasNonNull("dataType")) {
            String typeName = wrapper.get("dataType").asText();
            try {
                OpcUaType opcUaType = OpcUaType.fromOpcUaType(typeName);
                Object opcVal = opcUaType.convertValue(value);
                return Optional.of(new Variant(opcVal));
            } catch (Exception e) {
                throw new OpcUaIntegrationException("Invalid OPC UA payload: failed to convert value to OPC UA type " + typeName + " Node: " + wrapper, e);
            }
        } else {
            return Optional.of(new Variant(value));
        }
    }

    private Object extractValue(JsonNode valueJson) {
        Object val = null;
        if (valueJson.isValueNode()) {
            if (valueJson.isTextual()) {
                val = valueJson.asText();
            } else if (valueJson.isInt()) {
                val = valueJson.asInt();
            } else if (valueJson.isLong()) {
                val = valueJson.asLong();
            } else if (valueJson.isFloatingPointNumber()) {
                val = valueJson.asDouble();
            } else if (valueJson.isBoolean()) {
                val = valueJson.asBoolean();
            }
        }
        return val;
    }

    private void logOpcUaDownlink(IntegrationContext context, List<WriteValue> writeValues, List<CallMethodRequest> callMethods) {
        String status = downlinkConverter != null ? "OK" : "FAILURE";
        Supplier<String> msgSupplier = () -> {
            ObjectNode json = JacksonUtil.newObjectNode();
            if (!writeValues.isEmpty()) {
                json.set("writeValues", toJsonStringList(writeValues));
            }
            if (!callMethods.isEmpty()) {
                json.set("callMethods", toJsonStringList(callMethods));
            }
            return JacksonUtil.toString(json);
        };
        persistDebug(context, "Downlink", ContentType.JSON, msgSupplier, status, null);
    }

    private JsonNode toJsonStringList(List<?> list) {
        ArrayNode arrayNode = JacksonUtil.newArrayNode();
        for (Object item : list) {
            arrayNode.add(item.toString());
        }
        return arrayNode;
    }

}
