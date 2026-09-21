// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.lang.Nullable;
import org.thingsboard.server.common.data.id.EntityId;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Argument {

    @Nullable
    private EntityId refEntityId;
    private ReferencedEntityKey refEntityKey;
    private String defaultValue;

    private Integer limit;
    private Long timeWindow;

}
