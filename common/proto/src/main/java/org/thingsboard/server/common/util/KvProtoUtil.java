/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import java.util.Collections;
import java.util.List;

public class KvProtoUtil {

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


    public static List<TransportProtos.TsKvProto> tsToTsKvProtos(List<TsKvEntry> result) {
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

    public static TransportProtos.TsKvProto toTsKvProto(long ts, KvEntry kvEntry) {
        return TransportProtos.TsKvProto.newBuilder().setTs(ts)
                .setKv(KvProtoUtil.toKeyValueProto(kvEntry)).build();
    }

    public static TransportProtos.KeyValueProto toKeyValueProto(KvEntry kvEntry) {
        TransportProtos.KeyValueProto.Builder builder = TransportProtos.KeyValueProto.newBuilder();
        builder.setKey(kvEntry.getKey());
        switch (kvEntry.getDataType()) {
            case BOOLEAN:
                builder.setType(TransportProtos.KeyValueType.BOOLEAN_V);
                builder.setBoolV(kvEntry.getBooleanValue().get());
                break;
            case DOUBLE:
                builder.setType(TransportProtos.KeyValueType.DOUBLE_V);
                builder.setDoubleV(kvEntry.getDoubleValue().get());
                break;
            case LONG:
                builder.setType(TransportProtos.KeyValueType.LONG_V);
                builder.setLongV(kvEntry.getLongValue().get());
                break;
            case STRING:
                builder.setType(TransportProtos.KeyValueType.STRING_V);
                builder.setStringV(kvEntry.getStrValue().get());
                break;
            case JSON:
                builder.setType(TransportProtos.KeyValueType.JSON_V);
                builder.setJsonV(kvEntry.getJsonValue().get());
                break;
        }
        return builder.build();
    }

    public static TransportProtos.TsKvProto.Builder toKeyValueProto(long ts, KvEntry attr) {
        TransportProtos.KeyValueProto.Builder dataBuilder = TransportProtos.KeyValueProto.newBuilder();
        dataBuilder.setKey(attr.getKey());
        dataBuilder.setType(TransportProtos.KeyValueType.forNumber(attr.getDataType().ordinal()));
        switch (attr.getDataType()) {
            case BOOLEAN:
                attr.getBooleanValue().ifPresent(dataBuilder::setBoolV);
                break;
            case LONG:
                attr.getLongValue().ifPresent(dataBuilder::setLongV);
                break;
            case DOUBLE:
                attr.getDoubleValue().ifPresent(dataBuilder::setDoubleV);
                break;
            case JSON:
                attr.getJsonValue().ifPresent(dataBuilder::setJsonV);
                break;
            case STRING:
                attr.getStrValue().ifPresent(dataBuilder::setStringV);
                break;
        }
        return TransportProtos.TsKvProto.newBuilder().setTs(ts).setKv(dataBuilder);
    }

    public static List<TsKvEntry> toTsKvEntityList(List<TransportProtos.TsKvProto> dataList) {
        List<TsKvEntry> result = new ArrayList<>(dataList.size());
        dataList.forEach(proto -> result.add(new BasicTsKvEntry(proto.getTs(), getKvEntry(proto.getKv()))));
        return result;
    }

    public static List<AttributeKvEntry> toAttributeKvList(List<TransportProtos.TsKvProto> dataList) {
        List<AttributeKvEntry> result = new ArrayList<>(dataList.size());
        dataList.forEach(proto -> result.add(new BaseAttributeKvEntry(getKvEntry(proto.getKv()), proto.getTs())));
        return result;
    }

    private static KvEntry getKvEntry(TransportProtos.KeyValueProto proto) {
        KvEntry entry = null;
        DataType type = DataType.values()[proto.getType().getNumber()];
        switch (type) {
            case BOOLEAN:
                entry = new BooleanDataEntry(proto.getKey(), proto.getBoolV());
                break;
            case LONG:
                entry = new LongDataEntry(proto.getKey(), proto.getLongV());
                break;
            case DOUBLE:
                entry = new DoubleDataEntry(proto.getKey(), proto.getDoubleV());
                break;
            case STRING:
                entry = new StringDataEntry(proto.getKey(), proto.getStringV());
                break;
            case JSON:
                entry = new JsonDataEntry(proto.getKey(), proto.getJsonV());
                break;
        }
        return entry;
    }
}
