// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.relation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.thingsboard.server.common.data.StringUtils;

public record RelationPathLevel(@NotNull EntitySearchDirection direction, @NotBlank String relationType) {

    public void validate() {
        if (direction == null) {
            throw new IllegalArgumentException("Direction must be specified!");
        }
        if (StringUtils.isBlank(relationType)) {
            throw new IllegalArgumentException("Relation type must be specified!");
        }
    }
}
