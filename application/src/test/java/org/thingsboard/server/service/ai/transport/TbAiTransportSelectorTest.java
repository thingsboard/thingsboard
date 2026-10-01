// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.service.security.model.SecurityUser;
import reactor.core.publisher.Flux;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class TbAiTransportSelectorTest {

    UUID chatId = UUID.randomUUID();
    JsonNode request = JacksonUtil.newObjectNode().put("message", "Hi");
    TbAiTurnContext context = new TbAiTurnContext(mock(SecurityUser.class), "Bearer jwt", null, () -> "token",
            new TbAiClientRequest("https://tb.example.com", Map.of()));
    SseTbAiTransport sse = mock(SseTbAiTransport.class);
    ChannelTbAiTransport channel = mock(ChannelTbAiTransport.class);
    ServerSentEvent<String> sseEvent = ServerSentEvent.<String>builder().event("assistantMessage").data("{\"message\":\"sse\"}").build();
    ServerSentEvent<String> channelEvent = ServerSentEvent.<String>builder().event("assistantMessage").data("{\"message\":\"channel\"}").build();

    @Test
    void shouldUseChannel_whenEnabledAndAvailable() {
        // GIVEN
        given(channel.sendChatMessage(chatId, request, context)).willReturn(Flux.just(channelEvent));

        // WHEN
        List<ServerSentEvent<String>> events = selector(true, Clock.systemUTC()).sendChatMessage(chatId, request, context).collectList().block();

        // THEN
        assertThat(events).containsExactly(channelEvent);
        verify(sse, never()).sendChatMessage(any(), any(), any());
    }

    @Test
    void shouldUseSse_whenChannelIsDisabled() {
        // GIVEN
        given(sse.sendChatMessage(chatId, request, context)).willReturn(Flux.just(sseEvent));

        // WHEN
        List<ServerSentEvent<String>> events = selector(false, Clock.systemUTC()).sendChatMessage(chatId, request, context).collectList().block();

        // THEN
        assertThat(events).containsExactly(sseEvent);
        verify(channel, never()).sendChatMessage(any(), any(), any());
    }

    @Test
    void shouldFallBackToSseAndBackOff_whenChannelIsUnavailable() {
        // GIVEN
        given(channel.sendChatMessage(chatId, request, context))
                .willReturn(Flux.error(new TbAiChannelUnavailableException("404", null)));
        given(sse.sendChatMessage(chatId, request, context)).willReturn(Flux.just(sseEvent));
        var selector = selector(true, Clock.fixed(Instant.parse("2026-10-01T10:00:00Z"), ZoneOffset.UTC));

        // WHEN
        List<ServerSentEvent<String>> first = selector.sendChatMessage(chatId, request, context).collectList().block();
        List<ServerSentEvent<String>> second = selector.sendChatMessage(chatId, request, context).collectList().block();

        // THEN
        assertThat(first).containsExactly(sseEvent);
        assertThat(second).containsExactly(sseEvent);
        verify(channel, times(1)).sendChatMessage(chatId, request, context);
    }

    @Test
    void shouldRetryChannel_whenBackoffHasPassed() {
        // GIVEN
        given(channel.sendChatMessage(chatId, request, context))
                .willReturn(Flux.error(new TbAiChannelUnavailableException("404", null)));
        given(sse.sendChatMessage(chatId, request, context)).willReturn(Flux.just(sseEvent));
        var clock = new MutableClock(Instant.parse("2026-10-01T10:00:00Z"));
        var selector = selector(true, clock);
        selector.sendChatMessage(chatId, request, context).collectList().block();

        // WHEN
        clock.advance(Duration.ofMinutes(11));
        selector.sendChatMessage(chatId, request, context).collectList().block();

        // THEN
        verify(channel, times(2)).sendChatMessage(chatId, request, context);
    }

    TbAiTransportSelector selector(boolean enabled, Clock clock) {
        return new TbAiTransportSelector(sse, channel, enabled, Duration.ofMinutes(10), clock);
    }

    static class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }

    }

}
