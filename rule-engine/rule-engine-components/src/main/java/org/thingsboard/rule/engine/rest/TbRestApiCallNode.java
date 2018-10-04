/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import javax.net.ssl.SSLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "rest api call",
        configClazz = TbRestApiCallNodeConfiguration.class,
        nodeDescription = "Invoke REST API calls to external REST server",
        nodeDetails = "Will invoke REST API call <code>GET | POST | PUT | DELETE</code> to external REST server. " +
                "Message payload added into Request body. Configured attributes can be added into Headers from Message Metadata." +
                " Outbound message will contain response fields " +
                "(<code>status</code>, <code>statusCode</code>, <code>statusReason</code> and response <code>headers</code>) in the Message Metadata." +
                " Response body saved in outbound Message payload. " +
                "For example <b>statusCode</b> field can be accessed with <code>metadata.statusCode</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeRestApiCallConfig",
        iconUrl = "data:image/svg+xml;base64,PHN2ZyBzdHlsZT0iZW5hYmxlLWJhY2tncm91bmQ6bmV3IDAgMCA1MTIgNTEyIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHhtbDpzcGFjZT0icHJlc2VydmUiIHZpZXdCb3g9IjAgMCA1MTIgNTEyIiB2ZXJzaW9uPSIxLjEiIHk9IjBweCIgeD0iMHB4Ij48ZyB0cmFuc2Zvcm09Im1hdHJpeCguOTQ5NzUgMCAwIC45NDk3NSAxNy4xMiAyNi40OTIpIj48cGF0aCBkPSJtMTY5LjExIDEwOC41NGMtOS45MDY2IDAuMDczNC0xOS4wMTQgNi41NzI0LTIyLjAxNCAxNi40NjlsLTY5Ljk5MyAyMzEuMDhjLTMuNjkwNCAxMi4xODEgMy4yODkyIDI1LjIyIDE1LjQ2OSAyOC45MSAyLjIyNTkgMC42NzQ4MSA0LjQ5NjkgMSA2LjcyODUgMSA5Ljk3MjEgMCAxOS4xNjUtNi41MTUzIDIyLjE4Mi0xNi40NjdhNi41MjI0IDYuNTIyNCAwIDAgMCAwLjAwMiAtMC4wMDJsNjkuOTktMjMxLjA3YTYuNTIyNCA2LjUyMjQgMCAwIDAgMCAtMC4wMDJjMy42ODU1LTEyLjE4MS0zLjI4Ny0yNS4yMjUtMTUuNDcxLTI4LjkxMi0yLjI4MjUtMC42OTE0NS00LjYxMTYtMS4wMTY5LTYuODk4NC0xem04NC45ODggMGMtOS45MDQ4IDAuMDczNC0xOS4wMTggNi41Njc1LTIyLjAxOCAxNi40NjlsLTY5Ljk4NiAyMzEuMDhjLTMuNjg5OCAxMi4xNzkgMy4yODUzIDI1LjIxNyAxNS40NjUgMjguOTA4IDIuMjI5NyAwLjY3NjQ3IDQuNTAwOCAxLjAwMiA2LjczMjQgMS4wMDIgOS45NzIxIDAgMTkuMTY1LTYuNTE1MyAyMi4xODItMTYuNDY3YTYuNTIyNCA2LjUyMjQgMCAwIDAgMC4wMDIgLTAuMDAybDY5Ljk4OC0yMzEuMDdjMy42OTA4LTEyLjE4MS0zLjI4NTItMjUuMjIzLTE1LjQ2Ny0yOC45MTItMi4yODE0LTAuNjkyMzEtNC42MTA4LTEuMDE4OS02Ljg5ODQtMS4wMDJ6bS0yMTcuMjkgNDIuMjNjLTEyLjcyOS0wLjAwMDg3LTIzLjE4OCAxMC40NTYtMjMuMTg4IDIzLjE4NiAwLjAwMSAxMi43MjggMTAuNDU5IDIzLjE4NiAyMy4xODggMjMuMTg2IDEyLjcyNy0wLjAwMSAyMy4xODMtMTAuNDU5IDIzLjE4NC0yMy4xODYgMC4wMDA4NzYtMTIuNzI4LTEwLjQ1Ni0yMy4xODUtMjMuMTg0LTIzLjE4NnptMCAxNDYuNjRjLTEyLjcyNy0wLjAwMDg3LTIzLjE4NiAxMC40NTUtMjMuMTg4IDIzLjE4NC0wLjAwMDg3MyAxMi43MjkgMTAuNDU4IDIzLjE4OCAyMy4xODggMjMuMTg4IDEyLjcyOC0wLjAwMSAyMy4xODQtMTAuNDYgMjMuMTg0LTIzLjE4OC0wLjAwMS0xMi43MjYtMTAuNDU3LTIzLjE4My0yMy4xODQtMjMuMTg0em0yNzAuNzkgNDIuMjExYy0xMi43MjcgMC0yMy4xODQgMTAuNDU3LTIzLjE4NCAyMy4xODRzMTAuNDU1IDIzLjE4OCAyMy4xODQgMjMuMTg4aDE1NC45OGMxMi43MjkgMCAyMy4xODYtMTAuNDYgMjMuMTg2LTIzLjE4OCAwLjAwMS0xMi43MjgtMTAuNDU4LTIzLjE4NC0yMy4xODYtMjMuMTg0eiIgdHJhbnNmb3JtPSJtYXRyaXgoMS4wMzc2IDAgMCAxLjAzNzYgLTcuNTY3NiAtMTQuOTI1KSIgc3Ryb2tlLXdpZHRoPSIxLjI2OTMiLz48L2c+PC9zdmc+"
)
public class TbRestApiCallNode implements TbNode {

    private static final String STATUS = "status";
    private static final String STATUS_CODE = "statusCode";
    private static final String STATUS_REASON = "statusReason";
    private static final String ERROR = "error";
    private static final String ERROR_BODY = "error_body";

    private TbRestApiCallNodeConfiguration config;

    private EventLoopGroup eventLoopGroup;
    private AsyncRestTemplate httpClient;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        try {
            this.config = TbNodeUtils.convert(configuration, TbRestApiCallNodeConfiguration.class);
            this.eventLoopGroup = new NioEventLoopGroup();
            Netty4ClientHttpRequestFactory nettyFactory = new Netty4ClientHttpRequestFactory(this.eventLoopGroup);
            nettyFactory.setSslContext(SslContextBuilder.forClient().build());
            httpClient = new AsyncRestTemplate(nettyFactory);
        } catch (SSLException e) {
            throw new TbNodeException(e);
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
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
                    ctx.tellNext(next, TbRelationTypes.SUCCESS);
                } else {
                    TbMsg next = processFailureResponse(ctx, msg, responseEntity);
                    ctx.tellNext(next, TbRelationTypes.FAILURE);
                }
            }
        });
    }

    @Override
    public void destroy() {
        if (this.eventLoopGroup != null) {
            this.eventLoopGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }
    }

    private TbMsg processResponse(TbContext ctx, TbMsg origMsg, ResponseEntity<String> response) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue(STATUS, response.getStatusCode().name());
        metaData.putValue(STATUS_CODE, response.getStatusCode().value()+"");
        metaData.putValue(STATUS_REASON, response.getStatusCode().getReasonPhrase());
        response.getHeaders().toSingleValueMap().forEach((k,v) -> metaData.putValue(k,v) );
        return ctx.transformMsg(origMsg, origMsg.getType(), origMsg.getOriginator(), metaData, response.getBody());
    }

    private TbMsg processFailureResponse(TbContext ctx, TbMsg origMsg, ResponseEntity<String> response) {
        TbMsgMetaData metaData = origMsg.getMetaData().copy();
        metaData.putValue(STATUS, response.getStatusCode().name());
        metaData.putValue(STATUS_CODE, response.getStatusCode().value()+"");
        metaData.putValue(STATUS_REASON, response.getStatusCode().getReasonPhrase());
        metaData.putValue(ERROR_BODY, response.getBody());
        return ctx.transformMsg(origMsg, origMsg.getType(), origMsg.getOriginator(), metaData, origMsg.getData());
    }

    private TbMsg processException(TbContext ctx, TbMsg origMsg, Throwable e) {
        TbMsgMetaData metaData = origMsg.getMetaData().copy();
        metaData.putValue(ERROR, e.getClass() + ": " + e.getMessage());
        if (e instanceof HttpClientErrorException) {
            HttpClientErrorException httpClientErrorException = (HttpClientErrorException)e;
            metaData.putValue(STATUS, httpClientErrorException.getStatusText());
            metaData.putValue(STATUS_CODE, httpClientErrorException.getRawStatusCode()+"");
            metaData.putValue(ERROR_BODY, httpClientErrorException.getResponseBodyAsString());
        }
        return ctx.transformMsg(origMsg, origMsg.getType(), origMsg.getOriginator(), metaData, origMsg.getData());
    }

    private HttpHeaders prepareHeaders(TbMsgMetaData metaData) {
        HttpHeaders headers = new HttpHeaders();
        config.getHeaders().forEach((k,v) -> {
            headers.add(TbNodeUtils.processPattern(k, metaData), TbNodeUtils.processPattern(v, metaData));
        });
        return headers;
    }

}
