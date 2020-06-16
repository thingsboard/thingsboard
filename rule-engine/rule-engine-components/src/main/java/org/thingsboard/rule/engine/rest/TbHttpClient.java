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
package org.thingsboard.rule.engine.rest;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbRelationTypes;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import javax.net.ssl.SSLException;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

@Data
@Slf4j
class TbHttpClient {

    private static final String STATUS = "status";
    private static final String STATUS_CODE = "statusCode";
    private static final String STATUS_REASON = "statusReason";
    private static final String ERROR = "error";
    private static final String ERROR_BODY = "error_body";

    private final TbRestApiCallNodeConfiguration config;

    private EventLoopGroup eventLoopGroup;
    private AsyncRestTemplate httpClient;
    private Deque<ListenableFuture<ResponseEntity<String>>> pendingFutures;

    TbHttpClient(TbRestApiCallNodeConfiguration config) throws TbNodeException {
        try {
            this.config = config;
            if (config.getMaxParallelRequestsCount() > 0) {
                pendingFutures = new ConcurrentLinkedDeque<>();
            }
            if (config.isUseSimpleClientHttpFactory()) {
                httpClient = new AsyncRestTemplate();
            } else {
                this.eventLoopGroup = new NioEventLoopGroup();
                Netty4ClientHttpRequestFactory nettyFactory = new Netty4ClientHttpRequestFactory(this.eventLoopGroup);
                nettyFactory.setSslContext(SslContextBuilder.forClient().build());
                nettyFactory.setReadTimeout(config.getReadTimeoutMs());
                httpClient = new AsyncRestTemplate(nettyFactory);
            }
        } catch (SSLException e) {
            throw new TbNodeException(e);
        }
    }

    void destroy() {
        if (this.eventLoopGroup != null) {
            this.eventLoopGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }
    }

    void processMessage(TbContext ctx, TbMsg msg) {
        String endpointUrl = TbNodeUtils.processPattern(config.getRestEndpointUrlPattern(), msg.getMetaData());
        HttpHeaders headers = prepareHeaders(msg.getMetaData());
        HttpMethod method = HttpMethod.valueOf(config.getRequestMethod());
        HttpEntity<String> entity = new HttpEntity<>(msg.getData(), headers);

        ListenableFuture<ResponseEntity<String>> future = httpClient.exchange(
                endpointUrl, method, entity, String.class);
        future.addCallback(new ListenableFutureCallback<ResponseEntity<String>>() {
            @Override
            public void onFailure(Throwable throwable) {
                TbMsg next = processException(ctx, msg, throwable);
                ctx.tellFailure(next, throwable);
            }

            @Override
            public void onSuccess(ResponseEntity<String> responseEntity) {
                if (responseEntity.getStatusCode().is2xxSuccessful()) {
                    TbMsg next = processResponse(ctx, msg, responseEntity);
                    ctx.tellSuccess(next);
                } else {
                    TbMsg next = processFailureResponse(ctx, msg, responseEntity);
                    ctx.tellNext(next, TbRelationTypes.FAILURE);
                }
            }
        });
        if (pendingFutures != null) {
            processParallelRequests(future);
        }
    }

    private TbMsg processResponse(TbContext ctx, TbMsg origMsg, ResponseEntity<String> response) {
        TbMsgMetaData metaData = origMsg.getMetaData();
        metaData.putValue(STATUS, response.getStatusCode().name());
        metaData.putValue(STATUS_CODE, response.getStatusCode().value() + "");
        metaData.putValue(STATUS_REASON, response.getStatusCode().getReasonPhrase());
        response.getHeaders().toSingleValueMap().forEach(metaData::putValue);
        return ctx.transformMsg(origMsg, origMsg.getType(), origMsg.getOriginator(), metaData, response.getBody());
    }

    private TbMsg processFailureResponse(TbContext ctx, TbMsg origMsg, ResponseEntity<String> response) {
        TbMsgMetaData metaData = origMsg.getMetaData();
        metaData.putValue(STATUS, response.getStatusCode().name());
        metaData.putValue(STATUS_CODE, response.getStatusCode().value() + "");
        metaData.putValue(STATUS_REASON, response.getStatusCode().getReasonPhrase());
        metaData.putValue(ERROR_BODY, response.getBody());
        return ctx.transformMsg(origMsg, origMsg.getType(), origMsg.getOriginator(), metaData, origMsg.getData());
    }

    private TbMsg processException(TbContext ctx, TbMsg origMsg, Throwable e) {
        TbMsgMetaData metaData = origMsg.getMetaData();
        metaData.putValue(ERROR, e.getClass() + ": " + e.getMessage());
        if (e instanceof HttpClientErrorException) {
            HttpClientErrorException httpClientErrorException = (HttpClientErrorException) e;
            metaData.putValue(STATUS, httpClientErrorException.getStatusText());
            metaData.putValue(STATUS_CODE, httpClientErrorException.getRawStatusCode() + "");
            metaData.putValue(ERROR_BODY, httpClientErrorException.getResponseBodyAsString());
        }
        return ctx.transformMsg(origMsg, origMsg.getType(), origMsg.getOriginator(), metaData, origMsg.getData());
    }

    private HttpHeaders prepareHeaders(TbMsgMetaData metaData) {
        HttpHeaders headers = new HttpHeaders();
        config.getHeaders().forEach((k, v) -> headers.add(TbNodeUtils.processPattern(k, metaData), TbNodeUtils.processPattern(v, metaData)));
        return headers;
    }

    private void processParallelRequests(ListenableFuture<ResponseEntity<String>> future) {
        pendingFutures.add(future);
        if (pendingFutures.size() > config.getMaxParallelRequestsCount()) {
            for (int i = 0; i < config.getMaxParallelRequestsCount(); i++) {
                try {
                    ListenableFuture<ResponseEntity<String>> pendingFuture = pendingFutures.removeFirst();
                    try {
                        pendingFuture.get(config.getReadTimeoutMs(), TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        log.warn("Timeout during waiting for reply!", e);
                        pendingFuture.cancel(true);
                    }
                } catch (Exception e) {
                    log.warn("Failure during waiting for reply!", e);
                }
            }
        }
    }

}
