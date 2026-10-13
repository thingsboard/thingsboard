// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.agent.imitator;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.thingsboard.server.controller.AbstractWebTest;
import org.thingsboard.server.gen.agent.v1.AckStatus;
import org.thingsboard.server.gen.agent.v1.AgentLogChunk;
import org.thingsboard.server.gen.agent.v1.AgentMetricsSync;
import org.thingsboard.server.gen.agent.v1.AgentRpcServiceGrpc;
import org.thingsboard.server.gen.agent.v1.AgentToServer;
import org.thingsboard.server.gen.agent.v1.AppCommand;
import org.thingsboard.server.gen.agent.v1.CommandAck;
import org.thingsboard.server.gen.agent.v1.CommandId;
import org.thingsboard.server.gen.agent.v1.CommandProgress;
import org.thingsboard.server.gen.agent.v1.CommandResult;
import org.thingsboard.server.gen.agent.v1.FinalizeClaim;
import org.thingsboard.server.gen.agent.v1.FinalizeClaimResult;
import org.thingsboard.server.gen.agent.v1.Hello;
import org.thingsboard.server.gen.agent.v1.HelloAck;
import org.thingsboard.server.gen.agent.v1.InitialSyncComplete;
import org.thingsboard.server.gen.agent.v1.ProjectStateSync;
import org.thingsboard.server.gen.agent.v1.ProvisionRequest;
import org.thingsboard.server.gen.agent.v1.ProvisionResponse;
import org.thingsboard.server.gen.agent.v1.ServerToAgent;
import org.thingsboard.server.gen.agent.v1.StartLogStream;
import org.thingsboard.server.gen.agent.v1.StepId;
import org.thingsboard.server.gen.agent.v1.StopLogStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Slf4j
public class AgentImitator {

    private final String host;
    private final int port;
    private String routingKey;
    private String routingSecret;

    private ManagedChannel channel;
    private StreamObserver<AgentToServer> requestObserver;

    private final Lock lock = new ReentrantLock();
    private CountDownLatch messagesLatch;
    private int consumedCommands;

    @Getter
    private volatile HelloAck helloAck;
    @Getter
    private volatile ProvisionResponse provisionResponse;
    @Getter
    private final List<ServerToAgent> downlinkMsgs = new ArrayList<>();

    @Getter
    private volatile Throwable streamError;

    // What this instance reports about itself at hello. The self-upgrade protocol
    // dispatches on containerId, so a test can run two imitators of one agent.
    private String agentVersion = "";
    private String containerId = "";
    private final CountDownLatch helloAckLatch = new CountDownLatch(1);
    private final CountDownLatch provisionLatch = new CountDownLatch(1);

    public AgentImitator(String host, int port, String routingKey, String routingSecret) {
        this.host = host;
        this.port = port;
        this.routingKey = routingKey;
        this.routingSecret = routingSecret;
        this.messagesLatch = new CountDownLatch(0);
    }

    public AgentImitator withIdentity(String agentVersion, String containerId) {
        this.agentVersion = agentVersion;
        this.containerId = containerId;
        return this;
    }

    public void connect() throws InterruptedException {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdownNow();
            channel.awaitTermination(5, TimeUnit.SECONDS);
        }
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .keepAliveTime(300, TimeUnit.SECONDS)
                .keepAliveTimeout(5, TimeUnit.SECONDS)
                .build();

        AgentRpcServiceGrpc.AgentRpcServiceStub stub = AgentRpcServiceGrpc.newStub(channel);

        requestObserver = stub.controlStream(new StreamObserver<>() {
            @Override
            public void onNext(ServerToAgent msg) {
                if (msg.hasHelloAck()) {
                    helloAck = msg.getHelloAck();
                    helloAckLatch.countDown();
                } else {
                    lock.lock();
                    try {
                        downlinkMsgs.add(msg);
                        messagesLatch.countDown();
                    } finally {
                        lock.unlock();
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                streamError = t;
                helloAckLatch.countDown();
                log.info("Agent stream error: {}", t.getMessage());
            }

            @Override
            public void onCompleted() {
                log.info("Agent stream completed");
            }
        });

        requestObserver.onNext(AgentToServer.newBuilder()
                .setHello(Hello.newBuilder()
                        .setRoutingKey(routingKey)
                        .setRoutingSecret(routingSecret)
                        .setAgentVersion(agentVersion)
                        .setContainerId(containerId)
                        .build())
                .build());

        Assert.assertTrue("Timed out waiting for HelloAck",
                helloAckLatch.await(AbstractWebTest.TIMEOUT, TimeUnit.SECONDS));
        // onError counts the same latch down, so without these a rejected hello (unknown routing key,
        // FAILED_PRECONDITION/TAKEOVER_DEMOTED from the takeover triage) would look like a successful
        // connect and only resurface much later as an opaque Awaitility timeout.
        Assert.assertNull("Agent stream failed while connecting: " + streamError, streamError);
        Assert.assertNotNull("No HelloAck received", helloAck);
        Assert.assertTrue("Hello was rejected by the server", helloAck.getSuccess());
    }

    public Status connectExpectingError() throws InterruptedException {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();

        AgentRpcServiceGrpc.AgentRpcServiceStub stub = AgentRpcServiceGrpc.newStub(channel);
        CountDownLatch errorLatch = new CountDownLatch(1);

        requestObserver = stub.controlStream(new StreamObserver<>() {
            @Override
            public void onNext(ServerToAgent msg) {
                if (msg.hasHelloAck()) {
                    helloAck = msg.getHelloAck();
                }
            }

            @Override
            public void onError(Throwable t) {
                streamError = t;
                errorLatch.countDown();
            }

            @Override
            public void onCompleted() {
                errorLatch.countDown();
            }
        });

        requestObserver.onNext(AgentToServer.newBuilder()
                .setHello(Hello.newBuilder()
                        .setRoutingKey(routingKey)
                        .setRoutingSecret(routingSecret)
                        .setAgentVersion(agentVersion)
                        .setContainerId(containerId)
                        .build())
                .build());

        Assert.assertTrue("Timed out waiting for error response",
                errorLatch.await(AbstractWebTest.TIMEOUT, TimeUnit.SECONDS));

        if (streamError instanceof StatusRuntimeException sre) {
            return sre.getStatus();
        }
        if (streamError != null) {
            return Status.fromThrowable(streamError);
        }
        return null;
    }

    public void disconnect() throws InterruptedException {
        if (requestObserver != null) {
            try {
                requestObserver.onCompleted();
            } catch (Exception ignored) {
            }
        }
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
            channel.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    /**
     * Opens a control stream and sends a {@link ProvisionRequest}. Blocks until the
     * {@link ProvisionResponse} arrives. The server closes the stream after responding,
     * so callers should treat this imitator as single-use after provisioning.
     */
    public void provision(String provisionKey, String provisionSecret) throws InterruptedException {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();

        AgentRpcServiceGrpc.AgentRpcServiceStub stub = AgentRpcServiceGrpc.newStub(channel);

        requestObserver = stub.controlStream(new StreamObserver<>() {
            @Override
            public void onNext(ServerToAgent msg) {
                if (msg.hasProvisionResponse()) {
                    provisionResponse = msg.getProvisionResponse();
                    routingSecret = provisionResponse.getRoutingSecret();
                    routingKey = provisionResponse.getRoutingKey();
                    provisionLatch.countDown();
                }
            }

            @Override
            public void onError(Throwable t) {
                streamError = t;
                provisionLatch.countDown();
                log.info("Provision stream error: {}", t.getMessage());
            }

            @Override
            public void onCompleted() {
                provisionLatch.countDown();
            }
        });

        requestObserver.onNext(AgentToServer.newBuilder()
                .setProvision(ProvisionRequest.newBuilder()
                        .setProvisionKey(provisionKey)
                        .setProvisionSecret(provisionSecret)
                        .build())
                .build());

        Assert.assertTrue("Timed out waiting for ProvisionResponse",
                provisionLatch.await(AbstractWebTest.TIMEOUT, TimeUnit.SECONDS));
    }

    // --- Sending messages ---

    public void sendProjectSync(ProjectStateSync projectSync) {
        requestObserver.onNext(AgentToServer.newBuilder()
                .setProjectSync(projectSync)
                .build());
    }

    public void sendInitialSyncComplete() {
        requestObserver.onNext(AgentToServer.newBuilder()
                .setInitialSyncComplete(InitialSyncComplete.newBuilder().build())
                .build());
    }

    public void sendCommandAck(CommandId commandId, AckStatus status) {
        requestObserver.onNext(AgentToServer.newBuilder()
                .setCommandAck(CommandAck.newBuilder()
                        .setCommandId(commandId)
                        .setStatus(status)
                        .build())
                .build());
    }

    public void sendCommandProgress(CommandId commandId, StepId stepId, String stage) {
        sendCommandProgress(commandId, stepId, stage, null);
    }

    public void sendCommandProgress(CommandId commandId, StepId stepId, String stage, String message) {
        CommandProgress.Builder progress = CommandProgress.newBuilder()
                .setCommandId(commandId)
                .setStep(stepId)
                .setStage(stage);
        if (message != null) {
            progress.setMessage(message);
        }
        requestObserver.onNext(AgentToServer.newBuilder()
                .setProgress(progress.build())
                .build());
    }

    public void sendCommandResult(CommandId commandId, StepId stepId, boolean success) {
        sendCommandResult(commandId, stepId, success, Map.of());
    }

    public void sendCommandResult(CommandId commandId, StepId stepId, boolean success, Map<String, String> metadata) {
        requestObserver.onNext(AgentToServer.newBuilder()
                .setResult(CommandResult.newBuilder()
                        .setCommandId(commandId)
                        .setStep(stepId)
                        .setSuccess(success)
                        .putAllMetadata(metadata)
                        .build())
                .build());
    }

    public void sendAgentMetricsSync(AgentMetricsSync metricsSync) {
        requestObserver.onNext(AgentToServer.newBuilder()
                .setAgentMetricsSync(metricsSync)
                .build());
    }

    public void sendLogChunk(AgentLogChunk logChunk) {
        requestObserver.onNext(AgentToServer.newBuilder()
                .setLogChunk(logChunk)
                .build());
    }

    public void sendFinalizeClaim(CommandId commandId, StepId stepId, String containerId) {
        requestObserver.onNext(AgentToServer.newBuilder()
                .setFinalizeClaim(FinalizeClaim.newBuilder()
                        .setCommandId(commandId)
                        .setStepId(stepId)
                        .setContainerId(containerId)
                        .build())
                .build());
    }

    // --- Receiving messages ---

    /**
     * Arms the latch and drops everything received so far, so a downlink that lands between the two would
     * otherwise count down the previous latch and leave this one waiting for the full timeout. Both halves
     * therefore happen under {@code lock}, the same lock the stream callback counts down under.
     */
    public void expectMessageAmount(int messageAmount) {
        lock.lock();
        try {
            downlinkMsgs.clear();
            consumedCommands = 0;
            messagesLatch = new CountDownLatch(messageAmount);
        } finally {
            lock.unlock();
        }
    }

    public boolean waitForMessages() throws InterruptedException {
        CountDownLatch latch;
        lock.lock();
        try {
            latch = messagesLatch;
        } finally {
            lock.unlock();
        }
        boolean success = latch.await(AbstractWebTest.TIMEOUT, TimeUnit.SECONDS);
        if (!success) {
            lock.lock();
            try {
                for (ServerToAgent msg : downlinkMsgs) {
                    log.error("Received: {}", msg);
                }
                log.error("Message count: {}", downlinkMsgs.size());
            } finally {
                lock.unlock();
            }
            Assert.fail("Await for messages was not successful!");
        }
        return true;
    }

    public Optional<FinalizeClaimResult> findClaimResult(String containerId) {
        lock.lock();
        try {
            return claimResults(containerId).reduce((first, second) -> second);
        } finally {
            lock.unlock();
        }
    }

    /**
     * How many claim verdicts for this container have been received so far. Claim results are never cleared,
     * so a test sending a second claim has to wait for the count to grow rather than for a result to exist -
     * otherwise it reads the previous verdict.
     */
    public long countClaimResults(String containerId) {
        lock.lock();
        try {
            return claimResults(containerId).count();
        } finally {
            lock.unlock();
        }
    }

    private Stream<FinalizeClaimResult> claimResults(String containerId) {
        return downlinkMsgs.stream()
                .filter(ServerToAgent::hasFinalizeClaimResult)
                .map(ServerToAgent::getFinalizeClaimResult)
                .filter(result -> containerId.equals(result.getContainerId()));
    }

    public List<StartLogStream> getStartLogStreamRequests() {
        lock.lock();
        try {
            return downlinkMsgs.stream()
                    .filter(ServerToAgent::hasStartLogStream)
                    .map(ServerToAgent::getStartLogStream)
                    .toList();
        } finally {
            lock.unlock();
        }
    }

    public List<StopLogStream> getStopLogStreamRequests() {
        lock.lock();
        try {
            return downlinkMsgs.stream()
                    .filter(ServerToAgent::hasStopLogStream)
                    .map(ServerToAgent::getStopLogStream)
                    .toList();
        } finally {
            lock.unlock();
        }
    }

    public List<AppCommand> getReceivedCommands() {
        lock.lock();
        try {
            return downlinkMsgs.stream()
                    .filter(ServerToAgent::hasAppCommand)
                    .map(ServerToAgent::getAppCommand)
                    .toList();
        } finally {
            lock.unlock();
        }
    }

    public AppCommand getLatestCommand() {
        lock.lock();
        try {
            List<AppCommand> commands = getReceivedCommands();
            Assert.assertFalse("No commands received", commands.isEmpty());
            return commands.get(commands.size() - 1);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Poll-based command consumption. Unlike the {@link #expectMessageAmount(int)} /
     * {@link #waitForMessages()} latch path, these methods never clear already-received
     * commands and do not depend on a latch being armed before the command arrives, so
     * they are immune to the race where a downlink lands before the test starts waiting.
     */
    public boolean hasUnconsumedCommand(Predicate<AppCommand> predicate) {
        lock.lock();
        try {
            List<AppCommand> commands = getReceivedCommands();
            for (int i = consumedCommands; i < commands.size(); i++) {
                if (predicate.test(commands.get(i))) {
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public AppCommand consumeNextCommand(Predicate<AppCommand> predicate) {
        lock.lock();
        try {
            List<AppCommand> commands = getReceivedCommands();
            for (int i = consumedCommands; i < commands.size(); i++) {
                if (predicate.test(commands.get(i))) {
                    consumedCommands = i + 1;
                    return commands.get(i);
                }
            }
            Assert.fail("No unconsumed command matching predicate");
            return null;
        } finally {
            lock.unlock();
        }
    }
}
