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
package org.thingsboard.server.common.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class KvProtoUtilTest {

    private static final long TS = System.currentTimeMillis();

    private static Stream<KvEntry> kvEntryData() {
        String key = "key";
        return Stream.of(
                new BooleanDataEntry(key, true),
                new LongDataEntry(key, 23L),
                new DoubleDataEntry(key, 23.0),
                new StringDataEntry(key, "stringValue"),
                new JsonDataEntry(key, "jsonValue")
        );
    }

    private static Stream<KvEntry> basicTsKvEntryData() {
        return kvEntryData().map(kvEntry -> new BasicTsKvEntry(TS, kvEntry));
    }

    private static Stream<List<BaseAttributeKvEntry>> attributeKvEntryData() {
        return Stream.of(kvEntryData().map(kvEntry -> new BaseAttributeKvEntry(TS, kvEntry)).toList());
    }

    private static List<TsKvEntry> createTsKvEntryList(boolean withAggregation) {
        return kvEntryData().map(kvEntry -> {
                    if (withAggregation) {
                        return new AggTsKvEntry(TS, kvEntry, 0);
                    } else {
                        return new BasicTsKvEntry(TS, kvEntry);
                    }
                }).collect(Collectors.toList());
    }

    @ParameterizedTest
    @EnumSource(DataType.class)
    void protoDataTypeSerialization(DataType dataType) {
        assertThat(KvProtoUtil.fromKeyValueTypeProto(KvProtoUtil.toKeyValueTypeProto(dataType)))
                .as(dataType.name()).isEqualTo(dataType);
    }

    @ParameterizedTest
    @MethodSource("kvEntryData")
    void protoKeyValueProtoSerialization(KvEntry kvEntry) {
        assertThat(KvProtoUtil.fromTsKvProto(KvProtoUtil.toKeyValueTypeProto(kvEntry)))
                .as("deserialized").isEqualTo(kvEntry);
    }

    @ParameterizedTest
    @MethodSource("basicTsKvEntryData")
    void protoTsKvEntrySerialization(KvEntry kvEntry) {
        assertThat(KvProtoUtil.fromTsKvProto(KvProtoUtil.toTsKvProto(TS, kvEntry)))
                .as("deserialized").isEqualTo(kvEntry);
    }

    @ParameterizedTest
    @MethodSource("kvEntryData")
    void protoTsValueSerialization(KvEntry kvEntry) {
        assertThat(KvProtoUtil.fromTsValueProto(kvEntry.getKey(), KvProtoUtil.toTsValueProto(TS, kvEntry)))
                .as("deserialized").isEqualTo(kvEntry);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void protoListTsKvEntrySerialization(boolean withAggregation) {
        List<TsKvEntry> tsKvEntries = createTsKvEntryList(withAggregation);
        assertThat(KvProtoUtil.fromTsKvProtoList(KvProtoUtil.toTsKvProtoList(tsKvEntries)))
                .as("deserialized").isEqualTo(tsKvEntries);
    }

    @ParameterizedTest
    @MethodSource("attributeKvEntryData")
    void protoListAttributeKvSerialization(List<AttributeKvEntry> attributeKvEntries) {
        assertThat(KvProtoUtil.toAttributeKvList(KvProtoUtil.attrToTsKvProtos(attributeKvEntries)))
                .as("deserialized")
                .isEqualTo(attributeKvEntries);
    }

}
