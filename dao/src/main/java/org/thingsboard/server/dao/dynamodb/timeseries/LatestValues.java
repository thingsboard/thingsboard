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
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.Optional;

import static org.thingsboard.server.dao.dynamodb.timeseries.DynamoDBUtils.firstPresent;

@DynamoDBTable(tableName = "Latest")
public class LatestValues implements TsKvEntry {

    public static final String HASH_KEY_NAME = "id";
    public static final String RANGE_KEY_NAME = "key";
    // dynamo hash key / partition id
    private String entityType;
    private String entityId;
    private String key;

    private Long ts;
    private Boolean booleanVal;
    private String stringVal;
    private Long longVal;
    private Double doubleVal;

    @Override
    @DynamoDBIgnore
    public DataType getDataType() {
        if(booleanVal != null) return DataType.BOOLEAN;
        if(stringVal != null) return DataType.STRING;
        if(longVal != null) return DataType.LONG;
        if(doubleVal != null) return DataType.DOUBLE;
        return null;
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
        return (String) getValue();
    }


    @Override
    @DynamoDBIgnore
    public Object getValue() {
        return firstPresent(booleanVal, stringVal, longVal, doubleVal);
    }

    @DynamoDBHashKey(attributeName = "id")
    public String getId() {
        return String.format("%s:%s", entityType, entityId);
    }

    @Override
    @DynamoDBAttribute(attributeName = "ts")
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
    @DynamoDBRangeKey(attributeName = "key")
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
            .add("id", getId())
            .add("key", key)
            .add("ts", ts)
            .add("value", getValueAsString())
            .toString();
    }

    public static LatestValues newLatest() {
        return new LatestValues();
    }

    public LatestValues setId(String id) {
        String[] parts = id.split(":");
        this.entityType = parts[0];
        this.entityId = parts[1];
        return this;
    }


    public LatestValues setTs(long ts) {
        this.ts = ts;
        return this;
    }

    public LatestValues setEntityType(String entityType) {
        this.entityType = entityType;
        return this;
    }

    public LatestValues setEntityId(String entityId) {
        this.entityId = entityId;
        return this;
    }

    public LatestValues setKey(String key) {
        this.key = key;
        return this;
    }

    public LatestValues setBooleanVal(Boolean booleanVal) {
        this.booleanVal = booleanVal;
        return this;
    }

    public LatestValues setStringVal(String stringVal) {
        this.stringVal = stringVal;
        return this;
    }

    public LatestValues setLongVal(Long longVal) {
        this.longVal = longVal;
        return this;
    }

    public LatestValues setDoubleVal(Double doubleVal) {
        this.doubleVal = doubleVal;
        return this;
    }

    public LatestValues setValue(DataType dataType, Object value) {
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
