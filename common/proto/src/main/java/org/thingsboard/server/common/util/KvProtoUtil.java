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
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class KvProtoUtil {

    private static final DataType[] dataTypeByProtoNumber;

    static {
        int arraySize = Arrays.stream(DataType.values()).mapToInt(DataType::getProtoNumber).max().orElse(0);
        dataTypeByProtoNumber = new DataType[arraySize + 1];
        Arrays.stream(DataType.values()).forEach(dataType -> dataTypeByProtoNumber[dataType.getProtoNumber()] = dataType);
    }

    public static List<AttributeKvEntry> toAttributeKvList(List<TransportProtos.TsKvProto> dataList) {
        List<AttributeKvEntry> result = new ArrayList<>(dataList.size());
        dataList.forEach(proto -> result.add(new BaseAttributeKvEntry(fromTsKvProto(proto.getKv()), proto.getTs())));
        return result;
    }

    public static List<TransportProtos.TsKvProto> attrToTsKvProtos(List<AttributeKvEntry> result) {
        List<TransportProtos.TsKvProto> clientAttributes;
        if (result == null || result.isEmpty()) {
            clientAttributes = Collections.emptyList();
        } else {
            clientAttributes = new ArrayList<>(result.size());
            for (AttributeKvEntry attrEntry : result) {
                clientAttributes.add(toTsKvProto(attrEntry.getLastUpdateTs(), attrEntry));
            }
        }
        return clientAttributes;
    }

    public static List<TransportProtos.TsKvProto> toTsKvProtoList(List<TsKvEntry> result) {
        List<TransportProtos.TsKvProto> ts;
        if (result == null || result.isEmpty()) {
            ts = Collections.emptyList();
        } else {
            ts = new ArrayList<>(result.size());
            for (TsKvEntry attrEntry : result) {
                ts.add(toTsKvProto(attrEntry.getTs(), attrEntry));
            }
        }
        return ts;
    }

    public static List<TsKvEntry> fromTsKvProtoList(List<TransportProtos.TsKvProto> dataList) {
        List<TsKvEntry> result = new ArrayList<>(dataList.size());
        dataList.forEach(proto -> result.add(new BasicTsKvEntry(proto.getTs(), fromTsKvProto(proto.getKv()))));
        return result;
    }

    public static TransportProtos.TsKvProto toTsKvProto(long ts, KvEntry kvEntry) {
        return TransportProtos.TsKvProto.newBuilder().setTs(ts)
                .setKv(KvProtoUtil.toKeyValueTypeProto(kvEntry)).build();
    }

    public static TransportProtos.TsKvProto toTsKvProto(long ts, KvEntry kvEntry, Long version) {
        var builder = TransportProtos.TsKvProto.newBuilder()
                .setTs(ts)
                .setKv(KvProtoUtil.toKeyValueTypeProto(kvEntry));

        if (version != null) {
            builder.setVersion(version);
        }

        return builder.build();
    }

    public static TsKvEntry fromTsKvProto(TransportProtos.TsKvProto proto) {
        return new BasicTsKvEntry(proto.getTs(), fromTsKvProto(proto.getKv()), proto.hasVersion() ? proto.getVersion() : null);
    }

    public static TransportProtos.KeyValueProto toKeyValueTypeProto(KvEntry kvEntry) {
        TransportProtos.KeyValueProto.Builder builder = TransportProtos.KeyValueProto.newBuilder();
        builder.setKey(kvEntry.getKey());
        builder.setType(toKeyValueTypeProto(kvEntry.getDataType()));
        switch (kvEntry.getDataType()) {
            case BOOLEAN -> kvEntry.getBooleanValue().ifPresent(builder::setBoolV);
            case LONG -> kvEntry.getLongValue().ifPresent(builder::setLongV);
            case DOUBLE -> kvEntry.getDoubleValue().ifPresent(builder::setDoubleV);
            case JSON -> kvEntry.getJsonValue().ifPresent(builder::setJsonV);
            case STRING -> kvEntry.getStrValue().ifPresent(builder::setStringV);
        }
        return builder.build();
    }

    public static KvEntry fromTsKvProto(TransportProtos.KeyValueProto proto) {
        return switch (fromKeyValueTypeProto(proto.getType())) {
            case BOOLEAN -> new BooleanDataEntry(proto.getKey(), proto.getBoolV());
            case LONG -> new LongDataEntry(proto.getKey(), proto.getLongV());
            case DOUBLE -> new DoubleDataEntry(proto.getKey(), proto.getDoubleV());
            case STRING -> new StringDataEntry(proto.getKey(), proto.getStringV());
            case JSON -> new JsonDataEntry(proto.getKey(), proto.getJsonV());
        };
    }

    public static TransportProtos.TsKvProto.Builder toTsKvProtoBuilder(long ts, KvEntry kvEntry) {
        return TransportProtos.TsKvProto.newBuilder().setTs(ts).setKv(KvProtoUtil.toKeyValueTypeProto(kvEntry));
    }

    public static List<TsKvEntry> fromTsValueProtoList(String key, List<TransportProtos.TsValueProto> dataList) {
        List<TsKvEntry> result = new ArrayList<>(dataList.size());
        dataList.forEach(proto -> result.add(new BasicTsKvEntry(proto.getTs(), fromTsValueProto(key, proto))));
        return result;
    }

    public static TransportProtos.TsValueProto toTsValueProto(long ts, KvEntry attr) {
        TransportProtos.TsValueProto.Builder dataBuilder = TransportProtos.TsValueProto.newBuilder();
        dataBuilder.setTs(ts);
        dataBuilder.setType(toKeyValueTypeProto(attr.getDataType()));
        switch (attr.getDataType()) {
            case BOOLEAN -> attr.getBooleanValue().ifPresent(dataBuilder::setBoolV);
            case LONG -> attr.getLongValue().ifPresent(dataBuilder::setLongV);
            case DOUBLE -> attr.getDoubleValue().ifPresent(dataBuilder::setDoubleV);
            case JSON -> attr.getJsonValue().ifPresent(dataBuilder::setJsonV);
            case STRING -> attr.getStrValue().ifPresent(dataBuilder::setStringV);
        }
        return dataBuilder.build();
    }

    public static KvEntry fromTsValueProto(String key, TransportProtos.TsValueProto proto) {
        return switch (fromKeyValueTypeProto(proto.getType())) {
            case BOOLEAN -> new BooleanDataEntry(key, proto.getBoolV());
            case LONG -> new LongDataEntry(key, proto.getLongV());
            case DOUBLE -> new DoubleDataEntry(key, proto.getDoubleV());
            case STRING -> new StringDataEntry(key, proto.getStringV());
            case JSON -> new JsonDataEntry(key, proto.getJsonV());
        };
    }

    public static TransportProtos.KeyValueType toKeyValueTypeProto(DataType dataType) {
        return TransportProtos.KeyValueType.forNumber(dataType.getProtoNumber());
    }

    public static DataType fromKeyValueTypeProto(TransportProtos.KeyValueType keyValueType) {
        return dataTypeByProtoNumber[keyValueType.getNumber()];
    }

}
