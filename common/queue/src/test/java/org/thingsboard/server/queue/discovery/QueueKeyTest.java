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
package org.thingsboard.server.queue.discovery;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class QueueKeyTest {

    @Test
    void testToStringSystemTenant() {
        QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, DataConstants.MAIN_QUEUE_NAME, TenantId.SYS_TENANT_ID);
        log.info("The queue key is {}",queueKey);
        assertThat(queueKey.toString()).isEqualTo("QK(Main,TB_RULE_ENGINE,system)");
    }

    @Test
    void testToStringCustomTenant() {
        TenantId tenantId = TenantId.fromUUID(UUID.fromString("3ebd39eb-43d4-4911-a818-cdbf8d508f88"));
        QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, DataConstants.MAIN_QUEUE_NAME, tenantId);
        log.info("The queue key is {}",queueKey);
        assertThat(queueKey.toString()).isEqualTo("QK(Main,TB_RULE_ENGINE,3ebd39eb-43d4-4911-a818-cdbf8d508f88)");
    }
}
