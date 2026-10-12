// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.integration;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.id.IntegrationId;

import java.io.Serial;

@Schema
@EqualsAndHashCode(callSuper = true)
public class IntegrationInfo extends AbstractIntegration {

    @Serial
    private static final long serialVersionUID = 4934987577236873728L;

    @Getter @Setter
    private transient ObjectNode status;
    @Getter @Setter
    private transient ArrayNode stats;

    public IntegrationInfo() {
        super();
    }

    public IntegrationInfo(IntegrationId id) {
        super(id);
    }

}
