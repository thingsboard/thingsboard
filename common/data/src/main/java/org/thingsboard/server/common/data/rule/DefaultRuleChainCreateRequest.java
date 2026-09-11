// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.rule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

@Schema
@Data
@Slf4j
public class DefaultRuleChainCreateRequest implements Serializable {

    private static final long serialVersionUID = 5600333716030561537L;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Name of the new rule chain", example = "Root Rule Chain")
    private String name;

}
