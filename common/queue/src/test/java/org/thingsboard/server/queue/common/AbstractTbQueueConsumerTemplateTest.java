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
package org.thingsboard.server.queue.common;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueMsg;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.spy;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class AbstractTbQueueConsumerTemplateTest {

    private static final long POLL_DURATION_MS = 100L;
    private static final long SLEEP_TOLERANCE_MS = 20L;

    @Test
    public void givenEmptyPartitionsAndLongPollingSupported_whenPoll_thenSleepsAndDoesNotCallDoPoll() {
        // Regression: with empty partitions AND isLongPollingSupported()==true (e.g. Kafka),
        // poll() previously returned instantly with no sleep, causing the consumer loop to busy-spin.
        TestConsumer consumer = spy(new TestConsumer("test-topic", true));
        consumer.subscribe(Collections.emptySet());

        long startNs = System.nanoTime();
        List<TbQueueMsg> result = consumer.poll(POLL_DURATION_MS);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

        assertThat(result, is(empty()));
        verify(consumer, never()).doPoll(anyLong());
        assertThat("poll() must sleep ~durationInMillis when partitions are empty (no busy-wait)",
                elapsedMs, greaterThanOrEqualTo(POLL_DURATION_MS - SLEEP_TOLERANCE_MS));
    }

    @Test
    public void givenEmptyPartitionsAndNoLongPolling_whenPoll_thenSleepsAndDoesNotCallDoPoll() {
        TestConsumer consumer = spy(new TestConsumer("test-topic", false));
        consumer.subscribe(Collections.emptySet());

        long startNs = System.nanoTime();
        List<TbQueueMsg> result = consumer.poll(POLL_DURATION_MS);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

        assertThat(result, is(empty()));
        verify(consumer, never()).doPoll(anyLong());
        assertThat(elapsedMs, greaterThanOrEqualTo(POLL_DURATION_MS - SLEEP_TOLERANCE_MS));
    }

    @Test
    public void givenNonEmptyPartitions_whenPoll_thenCallsDoPoll() {
        TestConsumer consumer = spy(new TestConsumer("test-topic", true));
        consumer.subscribe(Collections.singleton(new TopicPartitionInfo("test-topic", null, 0, true)));

        List<TbQueueMsg> result = consumer.poll(POLL_DURATION_MS);

        assertThat(result, is(empty()));
        verify(consumer, times(1)).doPoll(POLL_DURATION_MS);
    }

    @Test
    public void givenPartitionsBecomeEmptyAfterRebalance_whenPollAgain_thenStopsCallingDoPoll() {
        // Reproduces the observed trigger: a rebalance leaves the consumer with an empty
        // partition assignment. Subsequent poll() calls must not busy-spin or call doPoll().
        TestConsumer consumer = spy(new TestConsumer("test-topic", true));
        consumer.subscribe(Collections.singleton(new TopicPartitionInfo("test-topic", null, 0, true)));
        consumer.poll(POLL_DURATION_MS);
        verify(consumer, times(1)).doPoll(POLL_DURATION_MS);

        consumer.subscribe(Collections.emptySet());

        long startNs = System.nanoTime();
        List<TbQueueMsg> result = consumer.poll(POLL_DURATION_MS);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

        assertThat(result, is(empty()));
        verify(consumer, times(1)).doPoll(anyLong());
        assertThat(elapsedMs, greaterThanOrEqualTo(POLL_DURATION_MS - SLEEP_TOLERANCE_MS));
    }

    static class TestConsumer extends AbstractTbQueueConsumerTemplate<Object, TbQueueMsg> {

        private final boolean longPollingSupported;

        TestConsumer(String topic, boolean longPollingSupported) {
            super(topic);
            this.longPollingSupported = longPollingSupported;
        }

        @Override
        protected List<Object> doPoll(long durationInMillis) {
            return Collections.emptyList();
        }

        @Override
        protected TbQueueMsg decode(Object record) {
            return null;
        }

        @Override
        protected void doSubscribe(Set<TopicPartitionInfo> partitions) {
        }

        @Override
        protected void doCommit() {
        }

        @Override
        protected void doUnsubscribe() {
        }

        @Override
        protected boolean isLongPollingSupported() {
            return longPollingSupported;
        }
    }

}
