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
package org.thingsboard.server.common.util;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.kv.AggTsKvEntry;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KvProtoUtilTest {

    @Test
    void protoDataTypeSerialization() {
        for (DataType dataType : DataType.values()) {
            assertThat(KvProtoUtil.fromProto(KvProtoUtil.toProto(dataType))).as(dataType.name()).isEqualTo(dataType);
        }
    }

    @Test
    void protoKeyValueProtoSerialization() {
        String key = "key";
        KvEntry kvEntry = new BooleanDataEntry(key, true);
        assertThat(KvProtoUtil.fromProto(KvProtoUtil.toProto(kvEntry))).as("deserialized").isEqualTo(kvEntry);

        kvEntry = new LongDataEntry(key, 23L);
        assertThat(KvProtoUtil.fromProto(KvProtoUtil.toProto(kvEntry))).as("deserialized").isEqualTo(kvEntry);

        kvEntry = new DoubleDataEntry(key, 23.0);
        assertThat(KvProtoUtil.fromProto(KvProtoUtil.toProto(kvEntry))).as("deserialized").isEqualTo(kvEntry);

        kvEntry = new StringDataEntry(key, "stringValue");
        assertThat(KvProtoUtil.fromProto(KvProtoUtil.toProto(kvEntry))).as("deserialized").isEqualTo(kvEntry);

        kvEntry = new JsonDataEntry(key, "jsonValue");
        assertThat(KvProtoUtil.fromProto(KvProtoUtil.toProto(kvEntry))).as("deserialized").isEqualTo(kvEntry);
    }

    @Test
    void protoTsKvEntrySerialization() {
        String key = "key";
        long ts = System.currentTimeMillis();
        KvEntry kvEntry = new BasicTsKvEntry(ts, new BooleanDataEntry(key, true));
        assertThat(KvProtoUtil.fromProto(KvProtoUtil.toProto(ts, kvEntry))).as("deserialized").isEqualTo(kvEntry);

        kvEntry = new BasicTsKvEntry(ts, new LongDataEntry(key, 23L));
        assertThat(KvProtoUtil.fromProto(KvProtoUtil.toProto(ts, kvEntry))).as("deserialized").isEqualTo(kvEntry);

        kvEntry = new BasicTsKvEntry(ts, new DoubleDataEntry(key, 23.0));
        assertThat(KvProtoUtil.fromProto(KvProtoUtil.toProto(ts, kvEntry))).as("deserialized").isEqualTo(kvEntry);

        kvEntry = new BasicTsKvEntry(ts, new StringDataEntry(key, "stringValue"));
        assertThat(KvProtoUtil.fromProto(KvProtoUtil.toProto(ts, kvEntry))).as("deserialized").isEqualTo(kvEntry);

        kvEntry = new BasicTsKvEntry(ts, new JsonDataEntry(key, "jsonValue"));
        assertThat(KvProtoUtil.fromProto(KvProtoUtil.toProto(ts, kvEntry))).as("deserialized").isEqualTo(kvEntry);
    }

    @Test
    void protoListTsKvEntrySerialization() {
        String key = "key";
        long ts = System.currentTimeMillis();
        KvEntry booleanDataEntry = new BooleanDataEntry(key, true);
        KvEntry longDataEntry = new LongDataEntry(key, 23L);
        KvEntry doubleDataEntry = new DoubleDataEntry(key, 23.0);
        KvEntry stringDataEntry = new StringDataEntry(key, "stringValue");
        KvEntry jsonDataEntry = new JsonDataEntry(key, "jsonValue");
        List<TsKvEntry> protoList = List.of(
                new BasicTsKvEntry(ts, booleanDataEntry),
                new BasicTsKvEntry(ts, longDataEntry),
                new BasicTsKvEntry(ts, doubleDataEntry),
                new BasicTsKvEntry(ts, stringDataEntry),
                new BasicTsKvEntry(ts, jsonDataEntry)
        );
        assertThat(KvProtoUtil.fromProtoList(KvProtoUtil.toProtoList(protoList))).as("deserialized").isEqualTo(protoList);

        protoList = List.of(
                new AggTsKvEntry(ts, booleanDataEntry, 3),
                new AggTsKvEntry(ts, longDataEntry, 5),
                new AggTsKvEntry(ts, doubleDataEntry, 2),
                new AggTsKvEntry(ts, stringDataEntry, 1),
                new AggTsKvEntry(ts, jsonDataEntry, 0)
        );
        assertThat(KvProtoUtil.fromProtoList(KvProtoUtil.toProtoList(protoList))).as("deserialized").isEqualTo(protoList);
    }

    @Test
    void protoListAttributeKvSerialization() {
        String key = "key";
        long ts = System.currentTimeMillis();
        KvEntry booleanDataEntry = new BooleanDataEntry(key, true);
        KvEntry longDataEntry = new LongDataEntry(key, 23L);
        KvEntry doubleDataEntry = new DoubleDataEntry(key, 23.0);
        KvEntry stringDataEntry = new StringDataEntry(key, "stringValue");
        KvEntry jsonDataEntry = new JsonDataEntry(key, "jsonValue");
        List<AttributeKvEntry> protoList = List.of(
                new BaseAttributeKvEntry(ts, booleanDataEntry),
                new BaseAttributeKvEntry(ts, longDataEntry),
                new BaseAttributeKvEntry(ts, doubleDataEntry),
                new BaseAttributeKvEntry(ts, stringDataEntry),
                new BaseAttributeKvEntry(ts, jsonDataEntry)
        );
        assertThat(KvProtoUtil.toAttributeKvList(KvProtoUtil.attrToTsKvProtos(protoList))).as("deserialized").isEqualTo(protoList);
    }

}
