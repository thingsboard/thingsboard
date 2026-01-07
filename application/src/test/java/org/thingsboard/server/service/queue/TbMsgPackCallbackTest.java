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
package org.thingsboard.server.service.queue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.RuleNodeException;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;

import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class TbMsgPackCallbackTest {

    TenantId tenantId;
    UUID msgId;
    TbMsgPackProcessingContext ctx;
    TbMsgPackCallback callback;

    @BeforeEach
    void setUp() {
        tenantId = TenantId.fromUUID(UUID.randomUUID());
        msgId = UUID.randomUUID();
        ctx = mock(TbMsgPackProcessingContext.class);
        callback = spy(new TbMsgPackCallback(msgId, tenantId, ctx));
    }

    private static Stream<Arguments> testOnFailure_NotRateLimitException() {
        return Stream.of(
                Arguments.of(new RuleEngineException("rule engine no cause")),
                Arguments.of(new RuleEngineException("rule engine caused 1 lvl", new RuntimeException())),
                Arguments.of(new RuleEngineException("rule engine caused 2 lvl", new RuntimeException(new Exception()))),
                Arguments.of(new RuleEngineException("rule engine caused 2 lvl Throwable", new RuntimeException(new Throwable()))),
                Arguments.of(new RuleNodeException("rule node no cause", "RuleChain", new RuleNode()))
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOnFailure_NotRateLimitException(RuleEngineException ree) {
        callback.onFailure(ree);

        verify(callback, never()).onRateLimit(any());
        verify(callback, never()).onSuccess();
        verify(ctx, never()).onSuccess(any());
    }

    private static Stream<Arguments> testOnFailure_RateLimitException() {
        return Stream.of(
                Arguments.of(new RuleEngineException("caused lvl 1", new TbRateLimitsException(EntityType.ASSET))),
                Arguments.of(new RuleEngineException("caused lvl 2", new RuntimeException(new TbRateLimitsException(EntityType.ASSET)))),
                Arguments.of(
                        new RuleEngineException("caused lvl 3",
                                new RuntimeException(
                                        new Exception(
                                                new TbRateLimitsException(EntityType.ASSET)))))
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOnFailure_RateLimitException(RuleEngineException ree) {
        callback.onFailure(ree);

        verify(callback).onRateLimit(any());
        verify(callback).onFailure(any());
        verify(callback, never()).onSuccess();
        verify(ctx).onSuccess(msgId);
        verify(ctx).onSuccess(any());
        verify(ctx, never()).onFailure(any(), any(), any());
    }

}
