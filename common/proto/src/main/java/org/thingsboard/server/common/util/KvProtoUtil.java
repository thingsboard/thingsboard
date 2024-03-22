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
        dataList.forEach(proto -> result.add(new BaseAttributeKvEntry(fromProto(proto.getKv()), proto.getTs())));
        return result;
    }

    public static List<TransportProtos.TsKvProto> attrToTsKvProtos(List<AttributeKvEntry> result) {
        List<TransportProtos.TsKvProto> clientAttributes;
        if (result == null || result.isEmpty()) {
            clientAttributes = Collections.emptyList();
        } else {
            clientAttributes = new ArrayList<>(result.size());
            for (AttributeKvEntry attrEntry : result) {
                clientAttributes.add(toProto(attrEntry.getLastUpdateTs(), attrEntry));
            }
        }
        return clientAttributes;
    }

    public static List<TransportProtos.TsKvProto> toProtoList(List<TsKvEntry> result) {
        List<TransportProtos.TsKvProto> ts;
        if (result == null || result.isEmpty()) {
            ts = Collections.emptyList();
        } else {
            ts = new ArrayList<>(result.size());
            for (TsKvEntry attrEntry : result) {
                ts.add(toProto(attrEntry.getTs(), attrEntry));
            }
        }
        return ts;
    }

    public static List<TsKvEntry> fromProtoList(List<TransportProtos.TsKvProto> dataList) {
        List<TsKvEntry> result = new ArrayList<>(dataList.size());
        dataList.forEach(proto -> result.add(new BasicTsKvEntry(proto.getTs(), fromProto(proto.getKv()))));
        return result;
    }

    public static TransportProtos.TsKvProto toProto(long ts, KvEntry kvEntry) {
        return TransportProtos.TsKvProto.newBuilder().setTs(ts)
                .setKv(KvProtoUtil.toProto(kvEntry)).build();
    }

    public static TsKvEntry fromProto(TransportProtos.TsKvProto proto) {
        return new BasicTsKvEntry(proto.getTs(), fromProto(proto.getKv()));
    }

    public static TransportProtos.KeyValueProto toProto(KvEntry kvEntry) {
        TransportProtos.KeyValueProto.Builder builder = TransportProtos.KeyValueProto.newBuilder();
        builder.setKey(kvEntry.getKey());
        builder.setType(toProto(kvEntry.getDataType()));
        switch (kvEntry.getDataType()) {
            case BOOLEAN -> kvEntry.getBooleanValue().ifPresent(builder::setBoolV);
            case LONG -> kvEntry.getLongValue().ifPresent(builder::setLongV);
            case DOUBLE -> kvEntry.getDoubleValue().ifPresent(builder::setDoubleV);
            case JSON -> kvEntry.getJsonValue().ifPresent(builder::setJsonV);
            case STRING -> kvEntry.getStrValue().ifPresent(builder::setStringV);
        }
        return builder.build();
    }

    public static KvEntry fromProto(TransportProtos.KeyValueProto proto) {
        return switch (fromProto(proto.getType())) {
            case BOOLEAN -> new BooleanDataEntry(proto.getKey(), proto.getBoolV());
            case LONG -> new LongDataEntry(proto.getKey(), proto.getLongV());
            case DOUBLE -> new DoubleDataEntry(proto.getKey(), proto.getDoubleV());
            case STRING -> new StringDataEntry(proto.getKey(), proto.getStringV());
            case JSON -> new JsonDataEntry(proto.getKey(), proto.getJsonV());
        };
    }

    public static TransportProtos.KeyValueType toProto(DataType dataType) {
        return TransportProtos.KeyValueType.forNumber(dataType.getProtoNumber());
    }

    public static DataType fromProto(TransportProtos.KeyValueType keyValueType) {
        return dataTypeByProtoNumber[keyValueType.getNumber()];
    }

}
