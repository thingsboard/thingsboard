// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.adaptor;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class TypeCastDisabledKeysTest {

    @Test
    public void testPutAndGetRoundTripKeysWithCommas() {
        TbMsgMetaData metaData = new TbMsgMetaData();
        TypeCastDisabledKeys.put(metaData, List.of("a,b", "c\"d"));
        assertThat(TypeCastDisabledKeys.get(metaData)).containsExactlyInAnyOrder("a,b", "c\"d");
    }

    @Test
    public void testPutSkipsEmptySet() {
        TbMsgMetaData metaData = new TbMsgMetaData();
        TypeCastDisabledKeys.put(metaData, Set.of());
        TypeCastDisabledKeys.put(metaData, null);
        assertThat(metaData.getValue(TypeCastDisabledKeys.METADATA_KEY)).isNull();
        assertThat(TypeCastDisabledKeys.get(metaData)).isEmpty();
    }

    @Test
    public void testGetReturnsEmptySetForInvalidValue() {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue(TypeCastDisabledKeys.METADATA_KEY, "joinEui,devAddr");
        assertThat(TypeCastDisabledKeys.get(metaData)).isEmpty();
        metaData.putValue(TypeCastDisabledKeys.METADATA_KEY, "{\"a\":1}");
        assertThat(TypeCastDisabledKeys.get(metaData)).isEmpty();
        assertThat(TypeCastDisabledKeys.get(new TbMsgMetaData())).isEqualTo(Set.of());
    }

}
