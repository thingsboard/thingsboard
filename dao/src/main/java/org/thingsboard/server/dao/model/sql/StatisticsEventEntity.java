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
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.event.StatisticsEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;

import static org.thingsboard.server.dao.model.ModelConstants.EVENT_ERRORS_OCCURRED_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_MESSAGES_PROCESSED_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.STATS_EVENT_TABLE_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = STATS_EVENT_TABLE_NAME)
@NoArgsConstructor
public class StatisticsEventEntity extends EventEntity<StatisticsEvent> implements BaseEntity<StatisticsEvent> {

    @Column(name = EVENT_MESSAGES_PROCESSED_COLUMN_NAME)
    private long messagesProcessed;
    @Column(name = EVENT_ERRORS_OCCURRED_COLUMN_NAME)
    private long errorsOccurred;

    public StatisticsEventEntity(StatisticsEvent event) {
        super(event);
        this.messagesProcessed = event.getMessagesProcessed();
        this.errorsOccurred = event.getErrorsOccurred();
    }

    @Override
    public StatisticsEvent toData() {
        return StatisticsEvent.builder()
                .tenantId(TenantId.fromUUID(tenantId))
                .entityId(entityId)
                .serviceId(serviceId)
                .id(id)
                .ts(ts)
                .messagesProcessed(messagesProcessed)
                .errorsOccurred(errorsOccurred)
                .build();
    }

}
