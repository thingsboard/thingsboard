/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.gen.js.JsInvokeProtos;
import org.thingsboard.server.gen.js.JsInvokeProtos.JsInvokeErrorCode;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoJsQueueMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.usagestats.TbApiUsageClient;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@ConditionalOnExpression("'${js.evaluator:null}'=='remote' && ('${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core' || '${service.type:null}'=='tb-rule-engine')")
@Service
public class RemoteJsInvokeService extends AbstractJsInvokeService {

    @Value("${queue.js.max_eval_requests_timeout}")
    private long maxEvalRequestsTimeout;

    @Value("${queue.js.max_requests_timeout}")
    private long maxRequestsTimeout;

    @Value("${queue.js.max_exec_requests_timeout:2000}")
    private long maxExecRequestsTimeout;

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
    private final ExecutorService callbackExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(), ThingsBoardThreadFactory.forName("js-executor-remote-callback"));

    public RemoteJsInvokeService(TbApiUsageStateService apiUsageStateService, TbApiUsageClient apiUsageClient) {
        super(apiUsageStateService, apiUsageClient);
    }

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
    protected TbQueueRequestTemplate<TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>, TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> requestTemplate;

    protected final Map<String, String> scriptHashToBodysMap = new ConcurrentHashMap<>();
    private final Lock scriptsLock = new ReentrantLock();

    @PostConstruct
    public void init() {
        super.init(maxRequestsTimeout);
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
    protected ListenableFuture<UUID> doEval(UUID scriptId, String scriptHash, String functionName, String scriptBody) {
        JsInvokeProtos.JsCompileRequest jsRequest = JsInvokeProtos.JsCompileRequest.newBuilder()
                .setScriptHash(scriptHash)
                .setFunctionName(functionName)
                .setScriptBody(scriptBody).build();

        JsInvokeProtos.RemoteJsRequest jsRequestWrapper = JsInvokeProtos.RemoteJsRequest.newBuilder()
                .setCompileRequest(jsRequest)
                .build();

        log.trace("Post compile request for scriptId [{}] (hash: {})", scriptId, scriptHash);
        ListenableFuture<TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> future = sendJsRequest(UUID.randomUUID(), jsRequestWrapper, maxEvalRequestsTimeout, queueEvalMsgs);
        return Futures.transform(future, response -> {
            JsInvokeProtos.JsCompileResponse compilationResult = response.getValue().getCompileResponse();
            if (compilationResult.getSuccess()) {
                scriptsLock.lock();
                try {
                    scriptIdToNameAndHashMap.put(scriptId, Pair.of(functionName, scriptHash));
                    scriptHashToBodysMap.put(scriptHash, scriptBody);
                } finally {
                    scriptsLock.unlock();
                }
                return scriptId;
            } else {
                log.debug("[{}] (hash: {}) Failed to compile script due to [{}]: {}", scriptId, compilationResult.getScriptHash(),
                        compilationResult.getErrorCode().name(), compilationResult.getErrorDetails());
                throw new RuntimeException(compilationResult.getErrorDetails());
            }
        }, callbackExecutor);
    }

    @Override
    protected String constructFunctionName(UUID scriptId, String scriptHash) {
        return "invokeInternal_" + scriptHash;
    }

    @Override
    protected ListenableFuture<Object> doInvokeFunction(UUID scriptId, String scriptHash, String functionName, Object[] args) {
        log.trace("doInvokeFunction js-request for uuid {} with timeout {}ms", scriptHash, maxRequestsTimeout);
        String scriptBody = scriptHashToBodysMap.get(scriptHash);
        if (scriptBody == null) {
            return Futures.immediateFailedFuture(new RuntimeException("No script body found for script hash [" + scriptHash + "] (script id: [" + scriptId + "])"));
        }
        JsInvokeProtos.RemoteJsRequest jsRequestWrapper = buildJsInvokeRequest(scriptHash, functionName, args, false, null);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        UUID requestKey = UUID.randomUUID();
        ListenableFuture<TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> future = sendJsRequest(requestKey, jsRequestWrapper, maxRequestsTimeout, queueInvokeMsgs);
        return Futures.transformAsync(future, response -> {
            stopWatch.stop();
            log.trace("doInvokeFunction js-response took {}ms for uuid {}", stopWatch.getTotalTimeMillis(), response.getKey());
            JsInvokeProtos.JsInvokeResponse invokeResult = response.getValue().getInvokeResponse();
            if (invokeResult.getSuccess()) {
                return Futures.immediateFuture(invokeResult.getResult());
            } else {
                return handleInvokeError(requestKey, scriptId, scriptHash, invokeResult.getErrorCode(),
                        invokeResult.getErrorDetails(), functionName, args, scriptBody);
            }
        }, callbackExecutor);
    }

    private ListenableFuture<Object> handleInvokeError(UUID requestKey, UUID scriptId, String scriptHash,
                                                       JsInvokeErrorCode errorCode, String errorDetails,
                                                       String functionName, Object[] args, String scriptBody) {
        log.debug("[{}] Failed to invoke function due to [{}]: {}", scriptId, errorCode.name(), errorDetails);
        RuntimeException e = new RuntimeException(errorDetails);
        if (JsInvokeErrorCode.TIMEOUT_ERROR.equals(errorCode)) {
            onScriptExecutionError(scriptId, e, scriptBody);
            queueTimeoutMsgs.incrementAndGet();
        } else if (JsInvokeErrorCode.COMPILATION_ERROR.equals(errorCode)) {
            onScriptExecutionError(scriptId, e, scriptBody);
        } else if (JsInvokeErrorCode.NOT_FOUND_ERROR.equals(errorCode)) {
            log.debug("[{}] Remote JS executor couldn't find the script", scriptId);
            if (scriptBody != null) {
                JsInvokeProtos.RemoteJsRequest invokeRequestWithScriptBody = buildJsInvokeRequest(scriptHash, functionName, args, true, scriptBody);
                log.debug("[{}] Sending invoke request again with script body", scriptId);
                return Futures.transformAsync(sendJsRequest(requestKey, invokeRequestWithScriptBody, maxRequestsTimeout, queueInvokeMsgs), r -> {
                    JsInvokeProtos.JsInvokeResponse result = r.getValue().getInvokeResponse();
                    if (result.getSuccess()) {
                        return Futures.immediateFuture(result.getResult());
                    } else {
                        return handleInvokeError(requestKey, scriptId, scriptHash, result.getErrorCode(),
                                result.getErrorDetails(), functionName, args, null);
                    }
                }, MoreExecutors.directExecutor());
            }
        }
        queueFailedMsgs.incrementAndGet();
        return Futures.immediateFailedFuture(e);
    }

    private ListenableFuture<TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> sendJsRequest(UUID requestKey, JsInvokeProtos.RemoteJsRequest jsRequestWrapper,
                                                                                             long maxRequestsTimeout, AtomicInteger msgsCounter) {
        ListenableFuture<TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> future = requestTemplate.send(new TbProtoJsQueueMsg<>(requestKey, jsRequestWrapper));
        if (maxRequestsTimeout > 0) {
            future = Futures.withTimeout(future, maxRequestsTimeout, TimeUnit.MILLISECONDS, timeoutExecutorService);
        }
        queuePushedMsgs.incrementAndGet();
        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse> result) {
                msgsCounter.incrementAndGet();
            }

            @Override
            public void onFailure(Throwable t) {
                if (t instanceof TimeoutException || (t.getCause() != null && t.getCause() instanceof TimeoutException)) {
                    queueTimeoutMsgs.incrementAndGet();
                }
                queueFailedMsgs.incrementAndGet();
            }
        }, callbackExecutor);
        return future;
    }

    private JsInvokeProtos.RemoteJsRequest buildJsInvokeRequest(String scriptHash, String functionName, Object[] args, boolean includeScriptBody, String scriptBody) {
        JsInvokeProtos.JsInvokeRequest.Builder jsRequestBuilder = JsInvokeProtos.JsInvokeRequest.newBuilder()
                .setScriptHash(scriptHash)
                .setFunctionName(functionName)
                .setTimeout((int) maxExecRequestsTimeout);
        if (includeScriptBody) jsRequestBuilder.setScriptBody(scriptBody);
        for (Object arg : args) {
            jsRequestBuilder.addArgs(arg.toString());
        }

        JsInvokeProtos.RemoteJsRequest jsRequestWrapper = JsInvokeProtos.RemoteJsRequest.newBuilder()
                .setInvokeRequest(jsRequestBuilder.build())
                .build();
        return jsRequestWrapper;
    }

    @Override
    protected void doRelease(UUID scriptId, String scriptHash, String functionName) throws Exception {
        if (scriptIdToNameAndHashMap.values().stream().map(Pair::getSecond).anyMatch(hash -> hash.equals(scriptHash))) {
            return;
        }
        JsInvokeProtos.JsReleaseRequest jsRequest = JsInvokeProtos.JsReleaseRequest.newBuilder()
                .setScriptHash(scriptHash)
                .setFunctionName(functionName).build();

        JsInvokeProtos.RemoteJsRequest jsRequestWrapper = JsInvokeProtos.RemoteJsRequest.newBuilder()
                .setReleaseRequest(jsRequest)
                .build();

        ListenableFuture<TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> future = requestTemplate.send(new TbProtoJsQueueMsg<>(UUID.randomUUID(), jsRequestWrapper));
        if (maxRequestsTimeout > 0) {
            future = Futures.withTimeout(future, maxRequestsTimeout, TimeUnit.MILLISECONDS, timeoutExecutorService);
        }
        JsInvokeProtos.RemoteJsResponse response = future.get().getValue();

        JsInvokeProtos.JsReleaseResponse releaseResponse = response.getReleaseResponse();
        if (releaseResponse.getSuccess()) {
            scriptsLock.lock();
            try {
                if (scriptIdToNameAndHashMap.values().stream().map(Pair::getSecond).noneMatch(h -> h.equals(scriptHash))) {
                    scriptHashToBodysMap.remove(scriptHash);
                }
            } finally {
                scriptsLock.unlock();
            }
        } else {
            log.debug("[{}] Failed to release script", scriptHash);
        }
    }

    @Override
    protected long getMaxBlacklistDuration() {
        return TimeUnit.SECONDS.toMillis(maxBlackListDurationSec);
    }

}
