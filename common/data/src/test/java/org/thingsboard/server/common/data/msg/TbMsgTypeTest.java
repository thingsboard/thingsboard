// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.msg;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
import static org.thingsboard.server.common.data.msg.TbMsgType.OPC_UA_INT_FAILURE;
import static org.thingsboard.server.common.data.msg.TbMsgType.OPC_UA_INT_SUCCESS;
import static org.thingsboard.server.common.data.msg.TbMsgType.PROVISION_FAILURE;
import static org.thingsboard.server.common.data.msg.TbMsgType.PROVISION_SUCCESS;
import static org.thingsboard.server.common.data.msg.TbMsgType.SEND_EMAIL;
import static org.thingsboard.server.common.data.msg.TbMsgType.TB_AGG_LATEST_CLEAR_INACTIVE_ENTITIES_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.TB_AGG_LATEST_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.TB_ALARMS_COUNT_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.TB_SIMPLE_AGG_ENTITIES_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.TB_SIMPLE_AGG_PERSIST_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.TB_SIMPLE_AGG_REPORT_SELF_MSG;

class TbMsgTypeTest {

    private static final List<TbMsgType> typesWithNullRuleNodeConnection = List.of(
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
            TB_AGG_LATEST_SELF_MSG,
            TB_AGG_LATEST_CLEAR_INACTIVE_ENTITIES_SELF_MSG,
            TB_ALARMS_COUNT_SELF_MSG,
            TB_SIMPLE_AGG_REPORT_SELF_MSG,
            TB_SIMPLE_AGG_PERSIST_SELF_MSG,
            TB_SIMPLE_AGG_ENTITIES_SELF_MSG,
            OPC_UA_INT_SUCCESS,
            OPC_UA_INT_FAILURE,
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
