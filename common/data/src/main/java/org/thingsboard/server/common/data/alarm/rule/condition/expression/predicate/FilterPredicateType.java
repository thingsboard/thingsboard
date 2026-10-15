// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AlarmRuleFilterPredicateType")
public enum FilterPredicateType {
    STRING,
    NUMERIC,
    BOOLEAN,
    NO_DATA,
    COMPLEX
}
