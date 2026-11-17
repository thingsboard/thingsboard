// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.adaptor;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class NoCastKeysTest {

    @Test
    public void testPutAndGetRoundTripKeysWithCommas() {
        TbMsgMetaData metaData = new TbMsgMetaData();
        NoCastKeys.put(metaData, List.of("a,b", "c\"d"));
        assertThat(NoCastKeys.get(metaData)).containsExactlyInAnyOrder("a,b", "c\"d");
    }

    @Test
    public void testPutSkipsEmptySet() {
        TbMsgMetaData metaData = new TbMsgMetaData();
        NoCastKeys.put(metaData, Set.of());
        NoCastKeys.put(metaData, null);
        assertThat(metaData.getValue(NoCastKeys.METADATA_KEY)).isNull();
        assertThat(NoCastKeys.get(metaData)).isEmpty();
    }

    @Test
    public void testGetReturnsEmptySetForInvalidValue() {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue(NoCastKeys.METADATA_KEY, "joinEui,devAddr");
        assertThat(NoCastKeys.get(metaData)).isEmpty();
        metaData.putValue(NoCastKeys.METADATA_KEY, "{\"a\":1}");
        assertThat(NoCastKeys.get(metaData)).isEmpty();
        assertThat(NoCastKeys.get(new TbMsgMetaData())).isEqualTo(Set.of());
    }

}
