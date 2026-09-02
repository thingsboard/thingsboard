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
package org.thingsboard.monitoring.notification.incident;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.monitoring.data.notification.AffectedService;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class IncidentManagerTest {

    private RecordingTransport transport;
    private IncidentManager manager;

    @BeforeEach
    void setUp() {
        transport = new RecordingTransport();
        manager = new IncidentManager(transport, 3600L, "tbqa", false);
    }

    @AfterEach
    void tearDown() {
        manager.shutdown();
    }

    @Test
    void formatDurationRendersMinutesAndHours() {
        assertThat(IncidentManager.formatDuration(Duration.ofSeconds(30))).isEqualTo("0m");
        assertThat(IncidentManager.formatDuration(Duration.ofMinutes(5))).isEqualTo("5m");
        assertThat(IncidentManager.formatDuration(Duration.ofMinutes(59))).isEqualTo("59m");
        assertThat(IncidentManager.formatDuration(Duration.ofMinutes(60))).isEqualTo("1h");
        assertThat(IncidentManager.formatDuration(Duration.ofMinutes(75))).isEqualTo("1h15m");
        assertThat(IncidentManager.formatDuration(Duration.ofMinutes(120))).isEqualTo("2h");
    }

    @Test
    void firstFailureOpensIncidentAndPostsHeaderAndReply() {
        manager.sendAlert("CoAP failure message",
                List.of(AffectedService.failing("CoAP", 1)));

        assertThat(transport.incidents).hasSize(1);
        assertThat(transport.incidents.get(0)).contains(":rotating_light:").contains(":red_circle: CoAP (1)");
        assertThat(transport.replies).hasSize(1);
        assertThat(transport.replies.get(0).text()).isEqualTo("CoAP failure message");
    }

    @Test
    void isolatedRecoveryWithoutActiveIncidentIsIgnored() {
        manager.sendAlert("Login is OK",
                List.of(AffectedService.recovered("Login")));

        assertThat(transport.incidents).isEmpty();
        assertThat(transport.replies).isEmpty();
        assertThat(transport.updates).isEmpty();
    }

    @Test
    void subsequentFailureUpdatesHeaderAndPostsReply() {
        manager.sendAlert("CoAP failure", List.of(AffectedService.failing("CoAP", 1)));
        manager.sendAlert("CoAP repeat", List.of(AffectedService.failing("CoAP", 3)));

        assertThat(transport.incidents).hasSize(1);
        assertThat(transport.replies).hasSize(2);
        assertThat(transport.updates).hasSize(1);
        assertThat(transport.updates.get(0).text()).contains(":red_circle: CoAP (3)");
    }

    @Test
    void recoveryAfterFailureMovesServiceToGreenAndKeepsFailureCount() {
        manager.sendAlert("CoAP failure", List.of(AffectedService.failing("CoAP", 4)));
        manager.sendAlert("CoAP is OK", List.of(AffectedService.recovered("CoAP")));

        assertThat(transport.updates).hasSize(1);
        String updated = transport.updates.get(0).text();
        assertThat(updated).contains(":large_green_circle: CoAP (4)").doesNotContain(":red_circle:");
    }

    @Test
    void highLatencyIsTrackedAsYellow() {
        manager.sendAlert("high latency",
                List.of(AffectedService.highLatency("logInLatency")));

        assertThat(transport.incidents.get(0)).contains(":large_yellow_circle: logInLatency");
    }

    @Test
    void repeatingSameFailureCountDoesNotTriggerRedundantUpdate() {
        manager.sendAlert("CoAP failure", List.of(AffectedService.failing("CoAP", 3)));
        manager.sendAlert("CoAP still failing", List.of(AffectedService.failing("CoAP", 3)));

        assertThat(transport.updates).isEmpty();
        assertThat(transport.replies).hasSize(2);
    }

    @Test
    void fullLifecycleStartFailRecoverResolve() {
        manager.sendAlert("Login failure", List.of(AffectedService.failing("Login", 1)));
        manager.sendAlert("WS failure", List.of(AffectedService.failing("WS Connect", 1)));
        manager.sendAlert("Login is OK", List.of(AffectedService.recovered("Login")));

        assertThat(transport.incidents).hasSize(1);

        manager.resolveIncident();

        assertThat(transport.updates).last()
                .extracting(RecordingTransport.Message::text)
                .asString()
                .contains(":white_check_mark:")
                .contains(":red_circle: WS Connect")
                .contains(":large_green_circle: Login (1)");
    }

    @Test
    void resolveWithoutActiveIncidentIsNoOp() {
        manager.resolveIncident();
        assertThat(transport.updates).isEmpty();
    }

    @Test
    void doesNotAutoResolveWhileServicesAreStillFailing() throws Exception {
        manager.shutdown();
        transport = new RecordingTransport();
        manager = new IncidentManager(transport, 1L, "tbqa", false);

        manager.sendAlert("CoAP failure", List.of(AffectedService.failing("CoAP", 1)));
        Thread.sleep(1500);

        assertThat(transport.updates)
                .extracting(RecordingTransport.Message::text)
                .noneMatch(t -> t.contains(":white_check_mark:"));

        manager.sendAlert("CoAP is OK", List.of(AffectedService.recovered("CoAP")));
        Thread.sleep(1500);

        assertThat(transport.updates)
                .extracting(RecordingTransport.Message::text)
                .anyMatch(t -> t.contains(":white_check_mark:"));
    }

    private static class RecordingTransport implements IncidentTransport {
        private final AtomicInteger threadCounter = new AtomicInteger();
        final java.util.List<String> incidents = new java.util.ArrayList<>();
        final java.util.List<Message> replies = new java.util.ArrayList<>();
        final java.util.List<Message> updates = new java.util.ArrayList<>();

        @Override
        public String postIncident(String text) {
            incidents.add(text);
            return "thread-" + threadCounter.incrementAndGet();
        }

        @Override
        public void postThreadReply(String threadId, String text) {
            replies.add(new Message(threadId, text));
        }

        @Override
        public void updateIncident(String threadId, String text) {
            updates.add(new Message(threadId, text));
        }

        record Message(String threadId, String text) {}
    }
}
