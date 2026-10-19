// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public interface ArgumentsBasedCalculatedFieldConfiguration extends CalculatedFieldConfiguration {

    @Valid
    @NotEmpty
    Map<String, Argument> getArguments();

    default Set<EntityId> getReferencedEntities() {
        var args = getArguments();
        if (args == null) {
            return Collections.emptySet();
        }
        return args.values().stream()
                .map(Argument::getRefEntityId)
                .filter(Objects::nonNull)
                .collect(toSet());
    }

}
