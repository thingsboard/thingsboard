// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.adaptor;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.gen.transport.TransportProtos.KeyValueProto;
import org.thingsboard.server.gen.transport.TransportProtos.KeyValueType;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class NoCastKeysTest {

    @Test
    public void testFromKvListTakesOnlyNumericLookingStrings() {
        List<KeyValueProto> kvList = List.of(
                stringKv("joinEui", "0000000000000000"),
                stringKv("hardwareVersion", "1.0"),
                stringKv("devEui", "70b3d57ed0000001"),
                KeyValueProto.newBuilder().setKey("fCnt").setType(KeyValueType.LONG_V).setLongV(42).build());
        assertThat(NoCastKeys.fromKvList(kvList)).containsExactly("joinEui", "hardwareVersion");
    }

    @Test
    public void testPutAndGetRoundTripKeysWithCommas() {
        TbMsgMetaData metaData = new TbMsgMetaData();
        NoCastKeys.put(metaData, List.of(stringKv("a,b", "1"), stringKv("c\"d", "2")));
        assertThat(NoCastKeys.get(metaData)).containsExactlyInAnyOrder("a,b", "c\"d");
    }

    @Test
    public void testPutSkipsEmptySet() {
        TbMsgMetaData metaData = new TbMsgMetaData();
        NoCastKeys.put(metaData, List.of(stringKv("devEui", "70b3d57ed0000001")));
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

    private static KeyValueProto stringKv(String key, String value) {
        return KeyValueProto.newBuilder().setKey(key).setType(KeyValueType.STRING_V).setStringV(value).build();
    }

}
