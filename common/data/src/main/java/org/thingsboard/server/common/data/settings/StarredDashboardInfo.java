// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.settings;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Schema
@Data
public class StarredDashboardInfo extends AbstractUserDashboardInfo implements Serializable {

    private static final long serialVersionUID = -7830828696329673361L;
    @Schema(description = "Starred timestamp")
    private long starredAt;

}
