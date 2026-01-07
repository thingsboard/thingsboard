/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.common.data.audit;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.audit.ActionType.ACTIVATED;
import static org.thingsboard.server.common.data.audit.ActionType.ALARM_ACK;
import static org.thingsboard.server.common.data.audit.ActionType.ALARM_ASSIGNED;
import static org.thingsboard.server.common.data.audit.ActionType.ALARM_CLEAR;
import static org.thingsboard.server.common.data.audit.ActionType.ALARM_DELETE;
import static org.thingsboard.server.common.data.audit.ActionType.ALARM_UNASSIGNED;
import static org.thingsboard.server.common.data.audit.ActionType.ATTRIBUTES_READ;
import static org.thingsboard.server.common.data.audit.ActionType.CREDENTIALS_READ;
import static org.thingsboard.server.common.data.audit.ActionType.CREDENTIALS_UPDATED;
import static org.thingsboard.server.common.data.audit.ActionType.DELETED_COMMENT;
import static org.thingsboard.server.common.data.audit.ActionType.LOCKOUT;
import static org.thingsboard.server.common.data.audit.ActionType.LOGIN;
import static org.thingsboard.server.common.data.audit.ActionType.LOGOUT;
import static org.thingsboard.server.common.data.audit.ActionType.REST_API_RULE_ENGINE_CALL;
import static org.thingsboard.server.common.data.audit.ActionType.RPC_CALL;
import static org.thingsboard.server.common.data.audit.ActionType.SMS_SENT;
import static org.thingsboard.server.common.data.audit.ActionType.SUSPENDED;

class ActionTypeTest {

    private final Set<ActionType> typesWithNullRuleEngineMsgType = EnumSet.of(
            RPC_CALL,
            CREDENTIALS_UPDATED,
            ACTIVATED,
            SUSPENDED,
            CREDENTIALS_READ,
            ATTRIBUTES_READ,
            LOGIN,
            LOGOUT,
            LOCKOUT,
            DELETED_COMMENT,
            SMS_SENT,
            REST_API_RULE_ENGINE_CALL
    );

    private final Set<ActionType> alarmActionTypes = EnumSet.of(
            ALARM_ACK, ALARM_CLEAR, ALARM_DELETE, ALARM_ASSIGNED, ALARM_UNASSIGNED
    );

    private final Set<ActionType> readActionTypes = EnumSet.of(CREDENTIALS_READ, ATTRIBUTES_READ);

    // backward-compatibility tests

    @Test
    void getRuleEngineMsgTypeTest() {
        var types = ActionType.values();
        for (var type : types) {
            if (typesWithNullRuleEngineMsgType.contains(type)) {
                assertThat(type.getRuleEngineMsgType()).isEmpty();
            } else {
                assertThat(type.getRuleEngineMsgType()).isPresent();
            }
        }
    }

    @Test
    void isAlarmActionTest() {
        var types = ActionType.values();
        for (var type : types) {
            if (alarmActionTypes.contains(type)) {
                assertThat(type.isAlarmAction()).isTrue();
            } else {
                assertThat(type.isAlarmAction()).isFalse();
            }
        }
    }

    @Test
    void isReadActionTypeTest() {
        var types = ActionType.values();
        for (var type : types) {
            if (readActionTypes.contains(type)) {
                assertThat(type.isRead()).isTrue();
            } else {
                assertThat(type.isRead()).isFalse();
            }
        }
    }

}
