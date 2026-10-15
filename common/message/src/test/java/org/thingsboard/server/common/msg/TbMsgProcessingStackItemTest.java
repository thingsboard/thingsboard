// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.JavaSerDesUtil;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TbMsgProcessingStackItemTest {

    @Test
    void testSerialization() {
        TbMsgProcessingStackItem item = new TbMsgProcessingStackItem(new RuleChainId(UUID.randomUUID()), new RuleNodeId(UUID.randomUUID()));
        byte[] bytes = JavaSerDesUtil.encode(item);
        TbMsgProcessingStackItem itemDecoded = JavaSerDesUtil.decode(bytes);
        assertThat(item).isEqualTo(itemDecoded);
    }

}
