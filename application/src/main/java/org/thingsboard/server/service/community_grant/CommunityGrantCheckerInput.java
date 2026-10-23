// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.common.util.JacksonUtil;

import java.util.Optional;

/**
 * The portal-decided input for the instance checker, passed through to its {@code TB_IC_INPUT} unread.
 */
public record CommunityGrantCheckerInput(String json) {

    /** Empty unless {@code input} is a non-empty JSON object. */
    public static Optional<CommunityGrantCheckerInput> from(JsonNode input) {
        if (input == null || !input.isObject() || input.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CommunityGrantCheckerInput(JacksonUtil.toString(input)));
    }

}
