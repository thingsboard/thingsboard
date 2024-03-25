/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.data.alarm.rule.condition;

import java.util.List;

import static org.thingsboard.server.common.data.alarm.rule.condition.ArgumentOperation.CONTAINS;
import static org.thingsboard.server.common.data.alarm.rule.condition.ArgumentOperation.ENDS_WITH;
import static org.thingsboard.server.common.data.alarm.rule.condition.ArgumentOperation.EQUAL;
import static org.thingsboard.server.common.data.alarm.rule.condition.ArgumentOperation.GREATER;
import static org.thingsboard.server.common.data.alarm.rule.condition.ArgumentOperation.GREATER_OR_EQUAL;
import static org.thingsboard.server.common.data.alarm.rule.condition.ArgumentOperation.LESS;
import static org.thingsboard.server.common.data.alarm.rule.condition.ArgumentOperation.LESS_OR_EQUAL;
import static org.thingsboard.server.common.data.alarm.rule.condition.ArgumentOperation.NOT_CONTAINS;
import static org.thingsboard.server.common.data.alarm.rule.condition.ArgumentOperation.NOT_EQUAL;
import static org.thingsboard.server.common.data.alarm.rule.condition.ArgumentOperation.STARTS_WITH;

public enum ArgumentValueType {
    STRING(EQUAL, NOT_EQUAL, STARTS_WITH, ENDS_WITH, CONTAINS, NOT_CONTAINS),
    NUMERIC(EQUAL, NOT_EQUAL, GREATER, LESS, GREATER_OR_EQUAL, LESS_OR_EQUAL),
    DATE_TIME(EQUAL, NOT_EQUAL, GREATER, LESS, GREATER_OR_EQUAL, LESS_OR_EQUAL),
    BOOLEAN(EQUAL, NOT_EQUAL);

    private final List<ArgumentOperation> availableOperations;

    ArgumentValueType(ArgumentOperation... availableOperations) {
        this.availableOperations = List.of(availableOperations);
    }

    public boolean isAvailable(ArgumentOperation operation) {
        return this.availableOperations.contains(operation);
    }
}
