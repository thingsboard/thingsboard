// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.calculatedField;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;

import java.util.UUID;

@Getter
@Builder
public class CalculatedFieldException extends Exception {

    private final CalculatedFieldCtx ctx;
    private final EntityId eventEntity;
    private final UUID msgId;
    private final String msgType;
    private JsonNode arguments;
    private String errorMessage;
    private Exception cause;

}
