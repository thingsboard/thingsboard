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
package org.thingsboard.server.common.data.msg;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.msg.TbMsgType.ALARM;
import static org.thingsboard.server.common.data.msg.TbMsgType.ALARM_DELETE;
import static org.thingsboard.server.common.data.msg.TbMsgType.DEDUPLICATION_TIMEOUT_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.DELAY_TIMEOUT_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.DEVICE_PROFILE_PERIODIC_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.DEVICE_PROFILE_UPDATE_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.DEVICE_UPDATE_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.ENTITY_ASSIGNED_TO_EDGE;
import static org.thingsboard.server.common.data.msg.TbMsgType.ENTITY_UNASSIGNED_FROM_EDGE;
import static org.thingsboard.server.common.data.msg.TbMsgType.GENERATOR_NODE_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.MSG_COUNT_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.NA;
import static org.thingsboard.server.common.data.msg.TbMsgType.PROVISION_FAILURE;
import static org.thingsboard.server.common.data.msg.TbMsgType.PROVISION_SUCCESS;
import static org.thingsboard.server.common.data.msg.TbMsgType.SEND_EMAIL;

class TbMsgTypeTest {

    private static final List<TbMsgType> typesWithNullRuleNodeConnection = List.of(
            ALARM,
            ALARM_DELETE,
            ENTITY_ASSIGNED_TO_EDGE,
            ENTITY_UNASSIGNED_FROM_EDGE,
            PROVISION_FAILURE,
            PROVISION_SUCCESS,
            SEND_EMAIL,
            GENERATOR_NODE_SELF_MSG,
            DEVICE_PROFILE_PERIODIC_SELF_MSG,
            DEVICE_PROFILE_UPDATE_SELF_MSG,
            DEVICE_UPDATE_SELF_MSG,
            DEDUPLICATION_TIMEOUT_SELF_MSG,
            DELAY_TIMEOUT_SELF_MSG,
            MSG_COUNT_SELF_MSG,
            NA
    );

    // backward-compatibility tests

    @Test
    void getRuleNodeConnectionsTest() {
        var tbMsgTypes = TbMsgType.values();
        for (var type : tbMsgTypes) {
            if (typesWithNullRuleNodeConnection.contains(type)) {
                assertThat(type.getRuleNodeConnection()).isEqualTo(TbNodeConnectionType.OTHER);
            } else {
                assertThat(type.getRuleNodeConnection()).isNotEqualTo(TbNodeConnectionType.OTHER);
            }
        }
    }

    @Test
    void getRuleNodeConnectionOrElseOtherTest() {
        var tbMsgTypes = TbMsgType.values();
        for (var type : tbMsgTypes) {
            if (typesWithNullRuleNodeConnection.contains(type)) {
                assertThat(type.getRuleNodeConnection())
                        .isEqualTo(TbNodeConnectionType.OTHER);
            } else {
                assertThat(type.getRuleNodeConnection()).isNotNull()
                        .isNotEqualTo(TbNodeConnectionType.OTHER);
            }
        }
    }

}
