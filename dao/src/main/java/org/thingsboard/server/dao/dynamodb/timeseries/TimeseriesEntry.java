/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
package org.thingsboard.server.dao.dynamodb.timeseries;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.HashMap;
import java.util.Optional;

import static org.thingsboard.server.dao.dynamodb.timeseries.DynamoDBUtils.firstPresent;

@DynamoDBTable(tableName = "Timeseries")
public class TimeseriesEntry implements TsKvEntry {

    public static final String HASH_KEY_NAME = "partition_id";
    public static final String RANGE_KEY_NAME = "ts";
    // dynamo hash key / partition id
    private String entityType;
    private String entityId;
    private String key;

    // range key / id inside partition
    private Long ts;

    private Boolean booleanVal;
    private String stringVal;
    private Long longVal;
    private Double doubleVal;

    @Override
    @DynamoDBIgnore
    public DataType getDataType() {
        HashMap<Object, DataType> fields = Maps.newHashMap();
        fields.put(booleanVal, DataType.BOOLEAN);
        fields.put(stringVal, DataType.STRING);
        fields.put(longVal, DataType.LONG);
        fields.put(doubleVal, DataType.DOUBLE);
        fields.remove(null);
        return fields.values().iterator().next();
    }

    @Override
    @DynamoDBIgnore
    public Optional<String> getStrValue() {
        return Optional.of(stringVal);
    }

    @Override
    @DynamoDBIgnore
    public Optional<Long> getLongValue() {
        return Optional.of(longVal);
    }

    @Override
    @DynamoDBIgnore
    public Optional<Boolean> getBooleanValue() {
        return Optional.of(booleanVal);
    }

    @Override
    @DynamoDBIgnore
    public Optional<Double> getDoubleValue() {
        return Optional.of(doubleVal);
    }


    @Override
    @DynamoDBIgnore
    public String getValueAsString() {
        return String.valueOf(getValue());
    }


    @Override
    @DynamoDBIgnore
    public Object getValue() {
        return firstPresent(booleanVal, stringVal, longVal, doubleVal);
    }

    @DynamoDBHashKey(attributeName = "partition_id")
    public String getPartitionId() {
        return String.format("%s:%s:%s", entityType, entityId, key);
    }

    @Override
    @DynamoDBRangeKey(attributeName = "ts")
    public long getTs() {
        return ts;
    }

    @DynamoDBIgnore
    public String getEntityType() {
        return entityType;
    }

    @DynamoDBIgnore
    public String getEntityId() {
        return entityId;
    }

    @Override
    @DynamoDBIgnore
    public String getKey() {
        return key;
    }

    @DynamoDBAttribute(attributeName = "bool_v")
    public Boolean getBooleanVal() {
        return booleanVal;
    }

    @DynamoDBAttribute(attributeName = "str_v")
    public String getStringVal() {
        return stringVal;
    }

    @DynamoDBAttribute(attributeName = "long_v")
    public Long getLongVal() {
        return longVal;
    }

    @DynamoDBAttribute(attributeName = "dbl_v")
    public Double getDoubleVal() {
        return doubleVal;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("partition_id", getPartitionId())
            .add("ts", ts)
            .add("value", getValueAsString())
            .toString();
    }

    public static TimeseriesEntry newTimeseries() {
        return new TimeseriesEntry();
    }

    public TimeseriesEntry setPartitionId(String partition_id) {
        String[] parts = partition_id.split(":");
        this.entityType = parts[0];
        this.entityId = parts[1];
        this.key = parts[2];
        return this;
    }


    public TimeseriesEntry setTs(long ts) {
        this.ts = ts;
        return this;
    }

    public TimeseriesEntry setEntityType(String entityType) {
        this.entityType = entityType;
        return this;
    }

    public TimeseriesEntry setEntityId(String entityId) {
        this.entityId = entityId;
        return this;
    }

    public TimeseriesEntry setKey(String key) {
        this.key = key;
        return this;
    }

    public TimeseriesEntry setBooleanVal(Boolean booleanVal) {
        this.booleanVal = booleanVal;
        return this;
    }

    public TimeseriesEntry setStringVal(String stringVal) {
        this.stringVal = stringVal;
        return this;
    }

    public TimeseriesEntry setLongVal(Long longVal) {
        this.longVal = longVal;
        return this;
    }

    public TimeseriesEntry setDoubleVal(Double doubleVal) {
        this.doubleVal = doubleVal;
        return this;
    }

    public TimeseriesEntry setValue(DataType dataType, Object value) {
        if (dataType == DataType.BOOLEAN) {
            setBooleanVal((Boolean)value);
        } else if (dataType == DataType.DOUBLE) {
            setDoubleVal((Double) value);
        } else if(dataType == DataType.LONG) {
            setLongVal((Long)value);
        } else if (dataType == DataType.STRING) {
            setStringVal((String)value);
        }
        return this;
    }
}
