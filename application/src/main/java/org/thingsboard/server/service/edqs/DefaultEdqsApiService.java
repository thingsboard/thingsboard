/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.edqs;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.edqs.query.EdqsRequest;
import org.thingsboard.server.common.data.edqs.query.EdqsResponse;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.edqs.EdqsApiService;
import org.thingsboard.server.edqs.state.EdqsPartitionService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.FromEdqsMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdqsMsg;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.provider.EdqsClientQueueFactory;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnExpression("'${queue.edqs.api.supported:true}' == 'true' && ('${service.type:null}' == 'monolith' || '${service.type:null}' == 'tb-core')")
public class DefaultEdqsApiService implements EdqsApiService {

    private final EdqsPartitionService edqsPartitionService;
    private final EdqsClientQueueFactory queueFactory;
    private TbQueueRequestTemplate<TbProtoQueueMsg<ToEdqsMsg>, TbProtoQueueMsg<FromEdqsMsg>> requestTemplate;

    @PostConstruct
    private void init() {
        requestTemplate = queueFactory.createEdqsRequestTemplate();
        requestTemplate.init();
    }

    @Override
    public ListenableFuture<EdqsResponse> processRequest(TenantId tenantId, CustomerId customerId, EdqsRequest request) {
        var requestMsg = ToEdqsMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setTs(System.currentTimeMillis())
                .setRequestMsg(TransportProtos.EdqsRequestMsg.newBuilder()
                        .setValue(JacksonUtil.toString(request))
                        .build());
        if (customerId != null && !customerId.isNullUid()) {
            requestMsg.setCustomerIdMSB(customerId.getId().getMostSignificantBits());
            requestMsg.setCustomerIdLSB(customerId.getId().getLeastSignificantBits());
        }

        UUID key = UUID.randomUUID();
        Integer partition = edqsPartitionService.resolvePartition(tenantId, key);
        ListenableFuture<TbProtoQueueMsg<FromEdqsMsg>> resultFuture = requestTemplate.send(new TbProtoQueueMsg<>(key, requestMsg.build()), partition);
        return Futures.transform(resultFuture, msg -> {
            TransportProtos.EdqsResponseMsg responseMsg = msg.getValue().getResponseMsg();
            return JacksonUtil.fromString(responseMsg.getValue(), EdqsResponse.class);
        }, MoreExecutors.directExecutor());
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @PreDestroy
    private void stop() {
        requestTemplate.stop();
    }

}
