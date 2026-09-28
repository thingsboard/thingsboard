// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.tcpip;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.integration.api.AbstractIntegration;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.DownlinkData;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.integration.api.data.IntegrationMetaData;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.server.common.msg.TbMsg;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.thingsboard.integration.api.util.ConvertUtil.toDebugMessage;

@Slf4j
public abstract class AbstractIpIntegration extends AbstractIntegration<IpIntegrationMsg> {

    public static final String TEXT_PAYLOAD = "TEXT";
    public static final String BINARY_PAYLOAD = "BINARY";
    public static final String JSON_PAYLOAD = "JSON";
    public static final String HEX_PAYLOAD = "HEX";

    protected Map<String, List<DownlinkData>> devicesDownlinkData = new ConcurrentHashMap<>();
    protected Map<String, ChannelHandlerContext> connectedDevicesContexts = new ConcurrentHashMap<>();

    protected Cache<String, SocketAddress> deviceSenderAddress;

    protected IntegrationContext ctx;
    protected Channel serverChannel;
    protected EventLoopGroup bossGroup;
    protected EventLoopGroup workerGroup;

    private final ScheduledExecutorService scheduler = ThingsBoardExecutors.newSingleThreadScheduledExecutor("ip-integration-scheduled");
    protected ScheduledFuture bindFuture = null;

    protected ContentType uplinkContentType;

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        if (downlinkConverter != null) {
            initCache(params);
        }
        this.ctx = params.getContext();
    }

    private void initCache(TbIntegrationInitParams params) {
        int timeToLiveInMinutes = 1440;
        int cacheSize = 1000;


        JsonNode timeToLiveInMinutesJson = params.getConfiguration().getConfiguration().get("clientConfiguration").get("timeToLiveInMinutes");
        if (timeToLiveInMinutesJson != null) {
            timeToLiveInMinutes = timeToLiveInMinutesJson.asInt();
        }

        JsonNode cacheSizeJson = params.getConfiguration().getConfiguration().get("clientConfiguration").get("cacheSize");
        if (cacheSizeJson != null) {
            cacheSize = cacheSizeJson.asInt();
        }

        deviceSenderAddress = Caffeine.newBuilder()
                .expireAfterWrite(timeToLiveInMinutes, TimeUnit.MINUTES)
                .maximumSize(cacheSize)
                .build();
    }

    @Override
    public void onDownlinkMsg(IntegrationDownlinkMsg downlink) {
        TbMsg msg = downlink.getTbMsg();
        logDownlink(context, "Downlink: " + msg.getType(), msg);
        if (downlinkConverter != null) {
            Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
            String status;
            Exception exception;
            try {
                String entityName = downlink.getEntityName();
                if (entityName == null || entityName.length() == 0) {
                    throw new RuntimeException("EntityName for downlink is empty.");
                }
                List<DownlinkData> result = downlinkConverter.convertDownLink(
                        context.getDownlinkConverterContext(),
                        Collections.singletonList(msg),
                        new IntegrationMetaData(mdMap));

                if (devicesDownlinkData.get(entityName) == null || devicesDownlinkData.get(entityName).isEmpty()) {
                    devicesDownlinkData.put(entityName, new ArrayList<>());
                }
                for (DownlinkData downlinkData : result) {
                    devicesDownlinkData.get(entityName).add(downlinkData);
                }

                downlinkWriter(entityName);
            } catch (Exception e) {
                log.warn("Failed to process downLink message", e);
                exception = e;
                status = "ERROR";
                reportDownlinkError(context, msg, status, exception);
            }
        }
    }

    protected void downlinkWriter(String entityName) {
        log.trace("Write downlink [{}]", entityName);
        ChannelHandlerContext deviceCtx = connectedDevicesContexts.get(entityName);
        if (deviceCtx == null) {
            log.warn("Device context not found, downlink data will be send when uplink message will be arrived.");
            return;
        }
        sendDownlink(deviceCtx, entityName);
    }

    protected abstract void sendDownlink(ChannelHandlerContext ctx, String entityName);

    @Override
    public void process(IpIntegrationMsg msg) {

    }

    private void process(IpIntegrationMsg msg, ChannelHandlerContext ctx) {
        String status = "OK";
        Exception exception = null;
        try {
            List<UplinkData> upLinkDataList = getUplinkDataList(context, msg);

            processUplinkData(context, upLinkDataList);

            if (downlinkConverter != null) {
                for (UplinkData uplinkData : upLinkDataList) {
                    String entityName;
                    if (uplinkData.isAsset()) {
                        entityName = uplinkData.getAssetName();
                    } else {
                        entityName = uplinkData.getDeviceName();
                    }
                    connectedDevicesContexts.put(entityName, ctx);
                    deviceSenderAddress.put(entityName, msg.getAddress());

                    if (!devicesDownlinkData.isEmpty() && devicesDownlinkData.get(entityName) != null) {
                        log.trace("Already have downlink: [{}]", entityName);
                        downlinkWriter(entityName);
                    } else {
                        log.trace("No downlink data for [{}]", entityName);
                    }
                }
            }

            integrationStatistics.incMessagesProcessed();
        } catch (Exception e) {
            log.debug("Failed to apply data converter function: {}", e.getMessage(), e);
            exception = e;
            status = "ERROR";
        }
        if (!status.equals("OK")) {
            integrationStatistics.incErrorsOccurred();
        }
        persistDebug(context, "Uplink", uplinkContentType, () -> toDebugMessage(uplinkContentType.name(), msg.getPayload()), status, exception);
    }

    protected void startServer() {
        try {
            bind();
        } catch (Exception e) {
            log.warn("[{}] Integration wasn't able to bind to required port. Starting re-bind mechanism!", e);
            bindFuture = scheduler.scheduleAtFixedRate(() -> {
                try {
                    bind();
                } catch (Exception ex) {
                    log.warn("[{}] Integration wasn't able to bind to required port. Waiting for port to be release externally...", ex);
                }
            }, 0, 5, TimeUnit.SECONDS);
        }
    }

    protected abstract void bind() throws Exception;

    @Override
    public void destroy() {
        try {
            if (bindFuture != null) {
                bindFuture.cancel(true);
            }
            if (serverChannel != null) {
                ChannelFuture cf = serverChannel.close().sync();
                cf.awaitUninterruptibly();
            }
            log.info("[{}] Integration was successfully stopped", configuration.getName());
        } catch (Exception e) {
            log.error("Exception while closing of channel, integration [{}]", e, configuration.getName());
        } finally {
            if (bossGroup != null) {
                bossGroup.shutdownGracefully();
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
        }
        scheduler.shutdownNow();
    }

    public byte[] writeValueAsBytes(String msg) {
        try {
            return JacksonUtil.writeValueAsBytes(msg);
        } catch (IllegalArgumentException e) {
            log.error("{}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public boolean isEmptyFrame(ByteBuf frame) {
        return frame == null;
    }

    public boolean isEmptyByteArray(byte[] byteArray) {
        return byteArray == null || byteArray.length == 0;
    }

    public boolean isEmptyObjectNode(ObjectNode objectNode) {
        if (objectNode == null) {
            return true;
        }
        JsonNode jsonNode = objectNode.get("reports");
        return jsonNode == null || (jsonNode.isArray() && (jsonNode.size() == 0 || jsonNode.get(0).size() == 0));
    }

    public byte[] toByteArray(ByteBuf buffer) {
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        return bytes;
    }

    public ObjectNode getJsonHexReport(byte[] hexBytes) {
        String hexString = Hex.encodeHexString(hexBytes);
        ArrayNode reports = JacksonUtil.newArrayNode();
        reports.add(JacksonUtil.newObjectNode().put("value", hexString));
        ObjectNode payload = JacksonUtil.newObjectNode();
        payload.set("reports", reports);
        return payload;
    }

    private List<UplinkData> getUplinkDataList(IntegrationContext context, IpIntegrationMsg msg) throws Exception {
        Map<String, String> metadataMap = new HashMap<>(metadataTemplate.getKvMap());
        return convertToUplinkDataList(context, msg.getPayload(), new UplinkMetaData<>(uplinkContentType, metadataMap));
    }

    private void processUplinkData(IntegrationContext context, List<UplinkData> uplinkDataList) throws Exception {
        if (uplinkDataList != null && !uplinkDataList.isEmpty()) {
            for (UplinkData uplinkData : uplinkDataList) {
                processUplinkData(context, uplinkData);
                log.trace("Processed uplink data: [{}]", uplinkData);
            }
        } else {
            log.trace("UplinkData list is empty");
        }
    }

    @Data
    protected class RawIpIntegrationMsg<T> {
        @Getter
        private final SocketAddress address;
        @Getter
        private final T data;
    }

    protected abstract class AbstractChannelHandler<T> extends SimpleChannelInboundHandler<RawIpIntegrationMsg<T>> {

        private Function<T, byte[]> transformer;
        private Predicate<T> predicate;

        protected AbstractChannelHandler(Function<T, byte[]> transformer, Predicate<T> predicate) {
            this.transformer = transformer;
            this.predicate = predicate;
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, RawIpIntegrationMsg<T> msg) throws Exception {
            try {
                if (predicate.test(msg.getData())) {
                    log.debug("Message is ignored, because it's not supported by current integration. Message [{}]", msg);
                    return;
                }
                IpIntegrationMsg message = new IpIntegrationMsg(msg.getAddress(), transformer.apply(msg.getData()));
                process(message, ctx);
            } catch (Exception e) {
                log.error("[{}] Exception happened during read messages from channel!", e.getMessage(), e);
                throw new Exception(e);
            }
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            ctx.flush();
            log.debug("Channel Read Complete [{}]", ctx.name());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            log.error("Exception caught", cause);
        }
    }

}
