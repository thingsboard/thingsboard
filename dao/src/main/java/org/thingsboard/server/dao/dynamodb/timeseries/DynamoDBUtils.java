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

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;

public final class DynamoDBUtils {
    private DynamoDBUtils() {
    }

    public static Object firstPresent(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public static AttributeValue attrValue(String str) {
        return new AttributeValue().withS(str);
    }

    public static AttributeValue attrValue(long number) {
        return new AttributeValue().withN(Long.toString(number));
    }

    public static AttributeDefinition attributeDefinition(String hashKeyName, String type) {
        return new AttributeDefinition().withAttributeName(hashKeyName).withAttributeType(type);
    }

    public static KeySchemaElement keySchemaElement(String hashKeyName, KeyType hash) {
        return new KeySchemaElement().withAttributeName(hashKeyName).withKeyType(hash);
    }

    public static <T> DynamoDBQueryExpression<T> queryExpression() {
        return new DynamoDBQueryExpression<T>();
    }
}
