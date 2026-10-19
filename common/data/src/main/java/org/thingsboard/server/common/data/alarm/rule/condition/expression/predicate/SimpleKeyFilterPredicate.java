// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate;

import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionValue;

@Schema(name = "AlarmRuleSimpleKeyFilterPredicate")
public interface SimpleKeyFilterPredicate<T> extends KeyFilterPredicate {

    AlarmConditionValue<T> getValue();

}
