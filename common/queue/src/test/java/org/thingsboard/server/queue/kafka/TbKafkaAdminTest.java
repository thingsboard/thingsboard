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
package org.thingsboard.server.queue.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;

class TbKafkaAdminTest {

    static final int MANY_PARTITIONS = 999;
    TbKafkaAdmin tbKafkaAdmin = mock(TbKafkaAdmin.class);

    @BeforeEach
    void setUp() {
        willReturn(MANY_PARTITIONS).given(tbKafkaAdmin).getNumPartitions();
        willCallRealMethod().given(tbKafkaAdmin).getPartitionsForTopic(anyString());
    }

    @Test
    void givenResponseTopic_whenGetPartitionsForTopicAndStartsWith_thenReturnOne() {
        assertThat(tbKafkaAdmin.getPartitionsForTopic("js_eval.responses.tb-rule-engine-99")).isEqualTo(1);
        assertThat(tbKafkaAdmin.getPartitionsForTopic("tb_transport.api.responses.tb-mqtt-transport-0")).isEqualTo(1);
    }

    @Test
    void givenRuleEngineCustomTopic_whenGetPartitionsForTopic_thenReturnMany() {
        assertThat(tbKafkaAdmin.getPartitionsForTopic("tb_rule_engine.js_eval.responses.99")).isEqualTo(MANY_PARTITIONS);
        assertThat(tbKafkaAdmin.getPartitionsForTopic("tb_rule_engine.tb_transport.api.responses.0")).isEqualTo(MANY_PARTITIONS);
    }

    @Test
    void givenResponseTopic_whenGetPartitionsForTopic_thenReturnMany() {
        assertThat(tbKafkaAdmin.getPartitionsForTopic("js_eval.requests")).isEqualTo(MANY_PARTITIONS);
        assertThat(tbKafkaAdmin.getPartitionsForTopic("tb_transport.api.requests")).isEqualTo(MANY_PARTITIONS);
        assertThat(tbKafkaAdmin.getPartitionsForTopic("tb_ota_package")).isEqualTo(MANY_PARTITIONS);
        assertThat(tbKafkaAdmin.getPartitionsForTopic("tb_core.0")).isEqualTo(MANY_PARTITIONS);
        assertThat(tbKafkaAdmin.getPartitionsForTopic("tb_rule_engine.main.11")).isEqualTo(MANY_PARTITIONS);
    }

}
