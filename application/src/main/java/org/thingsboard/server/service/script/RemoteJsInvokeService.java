/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.service.script;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.gen.js.JsInvokeProtos;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoJsQueueMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@ConditionalOnExpression("'${js.evaluator:null}'=='remote' && ('${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core' || '${service.type:null}'=='tb-rule-engine')")
@Service
public class RemoteJsInvokeService extends AbstractJsInvokeService {

    @Value("${queue.js.max_eval_requests_timeout}")
    private long maxEvalRequestsTimeout;

    @Value("${queue.js.max_invoke_requests_timeout}")
    private long maxInvokeRequestsTimeout;

    @Getter
    @Value("${js.remote.max_errors}")
    private int maxErrors;

    @Value("${js.remote.max_black_list_duration_sec:60}")
    private int maxBlackListDurationSec;

    @Value("${js.remote.stats.enabled:false}")
    private boolean statsEnabled;

    private final AtomicInteger queuePushedMsgs = new AtomicInteger(0);
    private final AtomicInteger queueInvokeMsgs = new AtomicInteger(0);
    private final AtomicInteger queueEvalMsgs = new AtomicInteger(0);
    private final AtomicInteger queueFailedMsgs = new AtomicInteger(0);
    private final AtomicInteger queueTimeoutMsgs = new AtomicInteger(0);

    @Scheduled(fixedDelayString = "${js.remote.stats.print_interval_ms}")
    public void printStats() {
        if (statsEnabled) {
            int pushedMsgs = queuePushedMsgs.getAndSet(0);
            int invokeMsgs = queueInvokeMsgs.getAndSet(0);
            int evalMsgs = queueEvalMsgs.getAndSet(0);
            int failed = queueFailedMsgs.getAndSet(0);
            int timedOut = queueTimeoutMsgs.getAndSet(0);
            if (pushedMsgs > 0 || invokeMsgs > 0 || evalMsgs > 0 || failed > 0 || timedOut > 0) {
                log.info("Queue JS Invoke Stats: pushed [{}] received [{}] invoke [{}] eval [{}] failed [{}] timedOut [{}]",
                        pushedMsgs, invokeMsgs + evalMsgs, invokeMsgs, evalMsgs, failed, timedOut);
            }
        }
    }

    @Autowired
    private TbQueueRequestTemplate<TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>, TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> requestTemplate;

    private Map<UUID, String> scriptIdToBodysMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        super.init(maxInvokeRequestsTimeout);
        requestTemplate.init();
    }

    @PreDestroy
    public void destroy() {
        super.stop();
        if (requestTemplate != null) {
            requestTemplate.stop();
        }
    }

    @Override
    protected ListenableFuture<UUID> doEval(UUID scriptId, String functionName, String scriptBody) {
        JsInvokeProtos.JsCompileRequest jsRequest = JsInvokeProtos.JsCompileRequest.newBuilder()
                .setScriptIdMSB(scriptId.getMostSignificantBits())
                .setScriptIdLSB(scriptId.getLeastSignificantBits())
                .setFunctionName(functionName)
                .setScriptBody(scriptBody).build();

        JsInvokeProtos.RemoteJsRequest jsRequestWrapper = JsInvokeProtos.RemoteJsRequest.newBuilder()
                .setCompileRequest(jsRequest)
                .build();

        log.trace("Post compile request for scriptId [{}]", scriptId);
        ListenableFuture<TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> future = requestTemplate.send(new TbProtoJsQueueMsg<>(UUID.randomUUID(), jsRequestWrapper));
        if (maxEvalRequestsTimeout > 0) {
            future = Futures.withTimeout(future, maxEvalRequestsTimeout, TimeUnit.MILLISECONDS, timeoutExecutorService);
        }
        queuePushedMsgs.incrementAndGet();
        Futures.addCallback(future, new FutureCallback<TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>>() {
            @Override
            public void onSuccess(@Nullable TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse> result) {
                queueEvalMsgs.incrementAndGet();
            }

            @Override
            public void onFailure(Throwable t) {
                if (t instanceof TimeoutException || (t.getCause() != null && t.getCause() instanceof TimeoutException)) {
                    queueTimeoutMsgs.incrementAndGet();
                }
                queueFailedMsgs.incrementAndGet();
            }
        }, MoreExecutors.directExecutor());
        return Futures.transform(future, response -> {
            JsInvokeProtos.JsCompileResponse compilationResult = response.getValue().getCompileResponse();
            UUID compiledScriptId = new UUID(compilationResult.getScriptIdMSB(), compilationResult.getScriptIdLSB());
            if (compilationResult.getSuccess()) {
                scriptIdToNameMap.put(scriptId, functionName);
                scriptIdToBodysMap.put(scriptId, scriptBody);
                return compiledScriptId;
            } else {
                log.debug("[{}] Failed to compile script due to [{}]: {}", compiledScriptId, compilationResult.getErrorCode().name(), compilationResult.getErrorDetails());
                throw new RuntimeException(compilationResult.getErrorDetails());
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    protected ListenableFuture<Object> doInvokeFunction(UUID scriptId, String functionName, Object[] args) {
        String scriptBody = scriptIdToBodysMap.get(scriptId);
        if (scriptBody == null) {
            return Futures.immediateFailedFuture(new RuntimeException("No script body found for scriptId: [" + scriptId + "]!"));
        }
        JsInvokeProtos.JsInvokeRequest.Builder jsRequestBuilder = JsInvokeProtos.JsInvokeRequest.newBuilder()
                .setScriptIdMSB(scriptId.getMostSignificantBits())
                .setScriptIdLSB(scriptId.getLeastSignificantBits())
                .setFunctionName(functionName)
                .setTimeout((int) maxInvokeRequestsTimeout)
                .setScriptBody(scriptIdToBodysMap.get(scriptId));

        for (Object arg : args) {
            jsRequestBuilder.addArgs(arg.toString());
        }

        JsInvokeProtos.RemoteJsRequest jsRequestWrapper = JsInvokeProtos.RemoteJsRequest.newBuilder()
                .setInvokeRequest(jsRequestBuilder.build())
                .build();

        ListenableFuture<TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> future = requestTemplate.send(new TbProtoJsQueueMsg<>(UUID.randomUUID(), jsRequestWrapper));
        if (maxInvokeRequestsTimeout > 0) {
            future = Futures.withTimeout(future, maxInvokeRequestsTimeout, TimeUnit.MILLISECONDS, timeoutExecutorService);
        }
        queuePushedMsgs.incrementAndGet();
        Futures.addCallback(future, new FutureCallback<TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>>() {
            @Override
            public void onSuccess(@Nullable TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse> result) {
                queueInvokeMsgs.incrementAndGet();
            }

            @Override
            public void onFailure(Throwable t) {
                onScriptExecutionError(scriptId);
                if (t instanceof TimeoutException || (t.getCause() != null && t.getCause() instanceof TimeoutException)) {
                    queueTimeoutMsgs.incrementAndGet();
                }
                queueFailedMsgs.incrementAndGet();
            }
        }, MoreExecutors.directExecutor());
        return Futures.transform(future, response -> {
            JsInvokeProtos.JsInvokeResponse invokeResult = response.getValue().getInvokeResponse();
            if (invokeResult.getSuccess()) {
                return invokeResult.getResult();
            } else {
                onScriptExecutionError(scriptId);
                log.debug("[{}] Failed to compile script due to [{}]: {}", scriptId, invokeResult.getErrorCode().name(), invokeResult.getErrorDetails());
                throw new RuntimeException(invokeResult.getErrorDetails());
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    protected void doRelease(UUID scriptId, String functionName) throws Exception {
        JsInvokeProtos.JsReleaseRequest jsRequest = JsInvokeProtos.JsReleaseRequest.newBuilder()
                .setScriptIdMSB(scriptId.getMostSignificantBits())
                .setScriptIdLSB(scriptId.getLeastSignificantBits())
                .setFunctionName(functionName).build();

        JsInvokeProtos.RemoteJsRequest jsRequestWrapper = JsInvokeProtos.RemoteJsRequest.newBuilder()
                .setReleaseRequest(jsRequest)
                .build();

        ListenableFuture<TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> future = requestTemplate.send(new TbProtoJsQueueMsg<>(UUID.randomUUID(), jsRequestWrapper));
        if (maxInvokeRequestsTimeout > 0) {
            future = Futures.withTimeout(future, maxInvokeRequestsTimeout, TimeUnit.MILLISECONDS, timeoutExecutorService);
        }
        JsInvokeProtos.RemoteJsResponse response = future.get().getValue();

        JsInvokeProtos.JsReleaseResponse compilationResult = response.getReleaseResponse();
        UUID compiledScriptId = new UUID(compilationResult.getScriptIdMSB(), compilationResult.getScriptIdLSB());
        if (compilationResult.getSuccess()) {
            scriptIdToBodysMap.remove(scriptId);
        } else {
            log.debug("[{}] Failed to release script due", compiledScriptId);
        }
    }

    @Override
    protected long getMaxBlacklistDuration() {
        return TimeUnit.SECONDS.toMillis(maxBlackListDurationSec);
    }

}
