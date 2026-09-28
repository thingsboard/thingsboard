// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.integration;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.IntegrationId;

@Schema
@EqualsAndHashCode(callSuper = true)
public class IntegrationInfo extends AbstractIntegration {

    private static final long serialVersionUID = 4934987577236873728L;

    private transient ObjectNode status;
    private transient ArrayNode stats;

    public IntegrationInfo() {
        super();
    }

    public IntegrationInfo(IntegrationId id) {
        super(id);
    }

    public ObjectNode getStatus() {
        return status;
    }

    public void setStatus(ObjectNode status) {
        this.status = status;
    }

    public ArrayNode getStats() {
        return stats;
    }

    public void setStats(ArrayNode stats) {
        this.stats = stats;
    }

}
