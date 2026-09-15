// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.query;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AlarmRuleComplexOperation")
public enum ComplexOperation {
    AND,
    OR
}
