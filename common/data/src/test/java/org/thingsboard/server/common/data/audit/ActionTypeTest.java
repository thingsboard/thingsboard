/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.audit.ActionType.ACTIVATED;
import static org.thingsboard.server.common.data.audit.ActionType.ATTRIBUTES_READ;
import static org.thingsboard.server.common.data.audit.ActionType.CREDENTIALS_READ;
import static org.thingsboard.server.common.data.audit.ActionType.CREDENTIALS_UPDATED;
import static org.thingsboard.server.common.data.audit.ActionType.DELETED_COMMENT;
import static org.thingsboard.server.common.data.audit.ActionType.LOCKOUT;
import static org.thingsboard.server.common.data.audit.ActionType.LOGIN;
import static org.thingsboard.server.common.data.audit.ActionType.LOGOUT;
import static org.thingsboard.server.common.data.audit.ActionType.RPC_CALL;
import static org.thingsboard.server.common.data.audit.ActionType.SMS_SENT;
import static org.thingsboard.server.common.data.audit.ActionType.SUSPENDED;

class ActionTypeTest {

    private static final List<ActionType> typesWithNullRuleEngineMsgType = List.of(
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
            SMS_SENT
    );

    // backward-compatibility tests

    @Test
    void getRuleEngineMsgTypeTest() {
        var types = ActionType.values();
        for (var type : types) {
            if (typesWithNullRuleEngineMsgType.contains(type)) {
                assertThat(type.getRuleEngineMsgType()).isEmpty();
            }
        }
    }

}
