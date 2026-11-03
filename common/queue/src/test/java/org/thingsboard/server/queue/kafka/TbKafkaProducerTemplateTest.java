// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
