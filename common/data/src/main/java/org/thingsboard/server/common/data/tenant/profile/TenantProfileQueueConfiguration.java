// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.tenant.profile;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategy;

import java.io.Serializable;

@Data
public class TenantProfileQueueConfiguration implements Serializable {

    private static final long serialVersionUID = -546600745123197362L;

    private String name;
    private String topic;
    private int pollInterval;
    private int partitions;
    private boolean consumerPerPartition;
    private long packProcessingTimeout;
    private SubmitStrategy submitStrategy;
    private ProcessingStrategy processingStrategy;
    private JsonNode additionalInfo;
}
