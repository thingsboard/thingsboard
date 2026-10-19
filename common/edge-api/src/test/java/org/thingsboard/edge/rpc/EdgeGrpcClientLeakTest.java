// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.edge.rpc;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.buffer.PooledByteBufAllocator;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.gen.edge.v1.ConnectResponseCode;
import org.thingsboard.server.gen.edge.v1.ConnectResponseMsg;
import org.thingsboard.server.gen.edge.v1.EdgeRpcServiceGrpc;
import org.thingsboard.server.gen.edge.v1.RequestMsg;
import org.thingsboard.server.gen.edge.v1.RequestMsgType;
import org.thingsboard.server.gen.edge.v1.ResponseMsg;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.fail;

class EdgeGrpcClientLeakTest {

    // The window in which the default shared event loop group would be destroyed after the channel
    // terminates (SharedResourceHolder delays destruction by 1 second). If EdgeGrpcClient ever goes
    // back to the shared group, writes after this window hit a terminated executor and every buffer
    // committed to grpc-netty's WriteQueue is pinned forever (4112 bytes per message, silent after
    // the first RejectedExecutionException).
    private static final long SHARED_GROUP_DEATH_WINDOW_MS = 3000;
    private static final int MSG_COUNT = 50;
    private static final long AWAIT_TIMEOUT_MS = 15_000;

    private Server server;
    private EdgeGrpcClient client;

    @Test
    void uplinksSentAfterTransportDeathDoNotPinPooledBuffers() throws Exception {
        server = NettyServerBuilder.forPort(0)
                .addService(new EdgeRpcServiceGrpc.EdgeRpcServiceImplBase() {
                    @Override
                    public StreamObserver<RequestMsg> handleMsgs(StreamObserver<ResponseMsg> outputStream) {
                        return new StreamObserver<>() {
                            @Override
                            public void onNext(RequestMsg requestMsg) {
                                if (requestMsg.hasConnectRequestMsg()) {
                                    outputStream.onNext(ResponseMsg.newBuilder()
                                            .setConnectResponseMsg(ConnectResponseMsg.newBuilder()
                                                    .setResponseCode(ConnectResponseCode.ACCEPTED)
                                                    .build())
                                            .build());
                                }
                            }

                            @Override
                            public void onError(Throwable t) {
                            }

                            @Override
                            public void onCompleted() {
                            }
                        };
                    }
                })
                .build()
                .start();

        client = new EdgeGrpcClient();
        ReflectionTestUtils.setField(client, "rpcHost", "localhost");
        ReflectionTestUtils.setField(client, "rpcPort", server.getPort());
        ReflectionTestUtils.setField(client, "timeoutSecs", 1);
        ReflectionTestUtils.setField(client, "keepAliveTimeSec", 10);
        ReflectionTestUtils.setField(client, "keepAliveTimeoutSec", 5);
        ReflectionTestUtils.setField(client, "maxInboundMessageSize", 4194304);

        client.connect("leakTest", "leakTest", msg -> {}, cfg -> {}, msg -> {}, e -> {});
        await("client to connect", () -> client.isConnected());

        server.shutdownNow();
        server.awaitTermination(10, TimeUnit.SECONDS);
        await("client to observe the transport death", () -> !client.isConnected());
        Thread.sleep(SHARED_GROUP_DEATH_WINDOW_MS);

        long baseline = pinnedBytes();
        @SuppressWarnings("unchecked")
        StreamObserver<RequestMsg> inputStream = (StreamObserver<RequestMsg>) ReflectionTestUtils.getField(client, "inputStream");
        RequestMsg uplink = RequestMsg.newBuilder()
                .setMsgType(RequestMsgType.UPLINK_RPC_MESSAGE)
                .setUplinkMsg(UplinkMsg.newBuilder().setUplinkMsgId(1).build())
                .build();
        // Bypasses the connected gate on purpose: this models the check-then-act straggler (and the
        // pre-gate retry loop) writing to a stream whose transport is already gone. Exceptions are
        // swallowed the same way the production retry loop survives them.
        for (int i = 0; i < MSG_COUNT; i++) {
            try {
                inputStream.onNext(uplink);
            } catch (RuntimeException ignored) {
            }
        }

        long deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MS;
        while (pinnedBytes() > baseline) {
            if (System.currentTimeMillis() > deadline) {
                fail("Pinned pooled memory did not return to baseline: " + (pinnedBytes() - baseline)
                        + " bytes retained after " + MSG_COUNT + " uplinks to a dead stream");
            }
            Thread.sleep(50);
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (client != null) {
            client.disconnect(true);
            client.destroy();
        }
        if (server != null) {
            server.shutdownNow();
        }
    }

    private void await(String what, BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MS;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                fail("Timed out waiting for " + what);
            }
            Thread.sleep(50);
        }
    }

    // grpc-netty builds its own PooledByteBufAllocator instances instead of using
    // PooledByteBufAllocator.DEFAULT, and the factory that owns them is package private - so they
    // have to be pulled out reflectively. Both variants are checked so the assertion holds no matter
    // which one the transport picks on this platform/version.
    private static long pinnedBytes() {
        try {
            Class<?> utils = Class.forName("io.grpc.netty.shaded.io.grpc.netty.Utils");
            Method getByteBufAllocator = utils.getDeclaredMethod("getByteBufAllocator", boolean.class);
            getByteBufAllocator.setAccessible(true);
            long total = 0;
            for (boolean forceHeapBuffer : new boolean[]{false, true}) {
                PooledByteBufAllocator pooled = (PooledByteBufAllocator) getByteBufAllocator.invoke(null, forceHeapBuffer);
                total += pooled.pinnedDirectMemory() + pooled.pinnedHeapMemory();
            }
            return total;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read the gRPC allocator metrics", e);
        }
    }

}
