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
package org.thingsboard.server.queue.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.queue.TbQueueMsg;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;

@Slf4j
class TbKafkaProducerTemplateTest {

    TbKafkaProducerTemplate<TbQueueMsg> producerTemplate;

    @BeforeEach
    void setUp() {
        producerTemplate = mock(TbKafkaProducerTemplate.class);
        willCallRealMethod().given(producerTemplate).addAnalyticHeaders(any());
        willReturn("tb-core-to-core-notifications-tb-core-3").given(producerTemplate).getClientId();
    }

    @Test
    void testAddAnalyticHeaders() {
        List<Header> headers = new ArrayList<>();
        producerTemplate.addAnalyticHeaders(headers);
        assertThat(headers).isNotEmpty();
        headers.forEach(r -> log.info("RecordHeader key [{}] value [{}]", r.key(), new String(r.value(), StandardCharsets.UTF_8)));
    }

}
