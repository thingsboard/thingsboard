/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import static org.thingsboard.server.common.data.UUIDConverter.fromString;
import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.QUEUE_COLUMN_FAMILY_NAME)
public class QueueEntity extends BaseSqlEntity<Queue> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Column(name = ModelConstants.QUEUE_TENANT_ID_PROPERTY)
    private String tenantId;

    @Column(name = ModelConstants.QUEUE_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.QUEUE_TOPIC_PROPERTY, unique = true)
    private String topic;

    @Column(name = ModelConstants.QUEUE_POLL_INTERVAL_PROPERTY)
    private int pollInterval;

    @Column(name = ModelConstants.QUEUE_PARTITIONS_PROPERTY)
    private int partitions;

    @Column(name = ModelConstants.QUEUE_PACK_PROCESSING_TIMEOUT_PROPERTY)
    private long packProcessingTimeout;

    @Type(type = "json")
    @Column(name = ModelConstants.QUEUE_SUBMIT_STRATEGY_PROPERTY)
    private JsonNode submitStrategy;

    @Type(type = "json")
    @Column(name = ModelConstants.QUEUE_PROCESSING_STRATEGY_PROPERTY)
    private JsonNode processingStrategy;

    public QueueEntity() {
    }

    public QueueEntity(Queue queue) {
        if (queue.getId() != null) {
            this.setUuid(queue.getId().getId());
        }
        if (queue.getTenantId() != null) {
            this.tenantId = fromTimeUUID(queue.getTenantId().getId());
        }
        this.name = queue.getName();
        this.topic = queue.getTopic();
        this.pollInterval = queue.getPollInterval();
        this.partitions = queue.getPartitions();
        this.packProcessingTimeout = queue.getPackProcessingTimeout();
        this.submitStrategy = mapper.valueToTree(queue.getSubmitStrategy());
        this.processingStrategy = mapper.valueToTree(queue.getProcessingStrategy());
    }

    @Override
    public Queue toData() {
        Queue queue = new Queue(new QueueId(getUuid()));
        if (tenantId != null) {
            queue.setTenantId(new TenantId(fromString(tenantId)));
        }
        queue.setName(name);
        queue.setTopic(topic);
        queue.setPollInterval(pollInterval);
        queue.setPartitions(partitions);
        queue.setPackProcessingTimeout(packProcessingTimeout);
        queue.setSubmitStrategy(mapper.convertValue(this.submitStrategy, SubmitStrategy.class));
        queue.setProcessingStrategy(mapper.convertValue(this.processingStrategy, ProcessingStrategy.class));
        return queue;
    }
}
