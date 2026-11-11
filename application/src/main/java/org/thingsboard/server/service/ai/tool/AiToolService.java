// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.tool;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.service.security.model.SecurityUser;

public interface AiToolService {

    JsonNode resolveToolApproval(JsonNode decision, SecurityUser user);

}
