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

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.*;

import java.util.List;

import static com.google.common.collect.ImmutableMap.of;
import static com.google.common.collect.Lists.newArrayList;
import static org.thingsboard.server.dao.dynamodb.timeseries.DynamoDBUtils.*;
import static org.thingsboard.server.dao.dynamodb.timeseries.TimeseriesEntry.newTimeseries;

@Component
@Slf4j
public class DynamoTimeseriesDao {

    private final static String TIMESERIES_TABLE_NAME = "Timeseries";
    private final static String LATEST_TABLE_NAME = "Latest";

    private final DynamoDBMapper mapper;
    private final AmazonDynamoDB dynamoDbClient;
    private final Long readCapacity;
    private final Long writeCapacity;

    @Autowired
    public DynamoTimeseriesDao(
            AmazonDynamoDB amazonDynamoDB,
            @Value("${spring.dynamodb.throughput.capacity.reed}") Long readCapacity,
            @Value("${spring.dynamodb.throughput.capacity.write}") Long writeCapacity) {
        this.dynamoDbClient = amazonDynamoDB;
        this.readCapacity = readCapacity;
        this.writeCapacity = writeCapacity;
        this.mapper = new DynamoDBMapper(dynamoDbClient);
    }

    public List<TimeseriesEntry> findBySingleQuery(EntityId entityId, ReadTsKvQuery query) {
        if (query.getAggregation() != Aggregation.NONE) {
            throw new UnsupportedOperationException("Aggregation other than NONE is not supported.");
        }
        return fetchTimeseries(queryTimeseriesInTheRangeWithLimit(entityId, query));
    }

    public LatestValues findLatest(EntityId entityId, String key) {
        LatestValues latest = LatestValues.newLatest()
                .setEntityId(entityId.getId().toString())
                .setEntityType(entityId.getEntityType().name())
                .setKey(key);
        return mapper.load(LatestValues.class, latest.getId(), latest.getKey());
    }

    public List<LatestValues> findAllLatest(EntityId entityId) {
        LatestValues latest = LatestValues.newLatest()
                .setEntityId(entityId.getId().toString())
                .setEntityType(entityId.getEntityType().name());

        DynamoDBQueryExpression<LatestValues> expression = queryExpression();
        expression.withKeyConditionExpression("#id = :id")
                .addExpressionAttributeNamesEntry("#id", "id")
                .addExpressionAttributeValuesEntry(":id", attrValue(latest.getId()));

        return mapper.query(LatestValues.class, expression);
    }

    public void save(EntityId entityId, TsKvEntry tsKvEntry) {
        TimeseriesEntry timeseries = newTimeseries()
                .setEntityId(entityId.getId().toString())
                .setEntityType(entityId.getEntityType().name())
                .setKey(tsKvEntry.getKey())
                .setTs(tsKvEntry.getTs())
                .setValue(tsKvEntry.getDataType(), tsKvEntry.getValue());
        mapper.save(timeseries);
    }

    public void saveLatest(EntityId entityId, TsKvEntry tsKvEntry) {
        LatestValues latest = LatestValues.newLatest()
                .setEntityId(entityId.getId().toString())
                .setEntityType(entityId.getEntityType().name())
                .setKey(tsKvEntry.getKey())
                .setTs(tsKvEntry.getTs())
                .setValue(tsKvEntry.getDataType(), tsKvEntry.getValue());
        saveLatest(latest);
    }

    public void remove(EntityId entityId, DeleteTsKvQuery query) {
        DynamoDBQueryExpression<TimeseriesEntry> expression = queryTimeseriesInTheRange(entityId, query);
        PaginatedQueryList<TimeseriesEntry> items = mapper.query(TimeseriesEntry.class, expression);
        mapper.batchDelete(items);
    }



    public void removeLatest(EntityId entityId, DeleteTsKvQuery query) {
        TimeseriesEntry timeseriesHashKey = timeseriesHashKey(entityId, query.getKey());
        TimeseriesEntry timeEntry = findLastTimeseries(timeseriesHashKey);

        if (latestInTheRemoveRange(timeEntry, query)) {
            removeLatestValue(entityId, query);
            timeEntry = findTimeseriesUpTo(timeseriesHashKey, query.getStartTs());
            saveLatest(getNewLatestValueBasedOn(timeEntry));
        } else {
            log.info("Won't be deleted latest value for [{}]", timeEntry.getPartitionId());
        }
    }

    public void createTimeseriesTable() {
        List<AttributeDefinition> attributes = newArrayList(
                attributeDefinition(TimeseriesEntry.HASH_KEY_NAME, "S"),
                attributeDefinition(TimeseriesEntry.RANGE_KEY_NAME, "N"));

        List<KeySchemaElement> keySchema = newArrayList(
                keySchemaElement(TimeseriesEntry.HASH_KEY_NAME, KeyType.HASH),
                keySchemaElement(TimeseriesEntry.RANGE_KEY_NAME, KeyType.RANGE));

        CreateTableRequest createTableReq = new CreateTableRequest()
                .withTableName(TIMESERIES_TABLE_NAME)
                .withAttributeDefinitions(attributes)
                .withKeySchema(keySchema)
                .withProvisionedThroughput(new ProvisionedThroughput()
                        .withReadCapacityUnits(readCapacity)
                        .withWriteCapacityUnits(writeCapacity));
        dynamoDbClient.createTable(createTableReq);
    }

    public void createLatestTable() {
        List<AttributeDefinition> attributes = newArrayList(
                attributeDefinition(LatestValues.HASH_KEY_NAME, "S"),
                attributeDefinition(LatestValues.RANGE_KEY_NAME, "S"));

        List<KeySchemaElement> keySchema = newArrayList(
                keySchemaElement(LatestValues.HASH_KEY_NAME, KeyType.HASH),
                keySchemaElement(LatestValues.RANGE_KEY_NAME, KeyType.RANGE));

        CreateTableRequest createTableReq = new CreateTableRequest()
                .withTableName(LATEST_TABLE_NAME)
                .withAttributeDefinitions(attributes)
                .withKeySchema(keySchema)
                .withProvisionedThroughput(new ProvisionedThroughput()
                        .withReadCapacityUnits(readCapacity)
                        .withWriteCapacityUnits(writeCapacity));
        dynamoDbClient.createTable(createTableReq);
    }

    public void removeTimeseriesTable() {
        dynamoDbClient.deleteTable(TIMESERIES_TABLE_NAME);
    }

    public void removeLatestTable() {
        dynamoDbClient.deleteTable(LATEST_TABLE_NAME);
    }

    private DynamoDBQueryExpression<TimeseriesEntry> queryTimeseriesInTheRangeWithLimit(
            EntityId entityId, ReadTsKvQuery query) {
        return queryTimeseriesInTheRange(entityId, query)
                .withLimit(query.getLimit())
                .withScanIndexForward(isAscOrder(query));
    }

    private DynamoDBQueryExpression<TimeseriesEntry> queryTimeseriesInTheRange(EntityId entityId, TsKvQuery query) {
        DynamoDBQueryExpression<TimeseriesEntry> expression = queryExpression();
        return expression.withKeyConditionExpression("#partition_id = :partition_id AND #ts BETWEEN :start AND :end")
                .withExpressionAttributeNames(of("#ts", "ts", "#partition_id", "partition_id"))
                .withExpressionAttributeValues(
                        of(":start", attrValue(query.getStartTs()),
                                ":end", attrValue(query.getEndTs()),
                                ":partition_id", attrValue(hashKey(entityId, query))));
    }

    private boolean isAscOrder(ReadTsKvQuery query) {
        return "ASC".equals(query.getOrderBy().toUpperCase());
    }

    private String hashKey(EntityId entityId, TsKvQuery readTsKvQuery) {
        return timeseriesHashKey(entityId, readTsKvQuery.getKey())
                .getPartitionId();
    }

    private LatestValues getNewLatestValueBasedOn(TimeseriesEntry timeEntry) {
        return LatestValues.newLatest()
                .setEntityId(timeEntry.getEntityId())
                .setEntityType(timeEntry.getEntityType())
                .setKey(timeEntry.getKey())
                .setTs(timeEntry.getTs());
    }

    private void saveLatest(LatestValues latestValues) {
        mapper.save(latestValues);
    }

    private TimeseriesEntry findTimeseriesUpTo(TimeseriesEntry timeseriesHashKey, long time) {
        DynamoDBQueryExpression<TimeseriesEntry> expression = queryExpression();
        expression.withKeyConditionExpression("#partition_id = :partition_id AND #ts < :time")
                .withExpressionAttributeNames(of("#ts", "ts", "#partition_id", "partition_id"))
                .withLimit(1)
                .withScanIndexForward(false)
                .withExpressionAttributeValues(
                        of(":time", attrValue(time),
                                ":partition_id", attrValue(timeseriesHashKey.getPartitionId())));


        List<TimeseriesEntry> results = fetchTimeseries(expression);
        return results.get(0);
    }

    private List<TimeseriesEntry> fetchTimeseries(DynamoDBQueryExpression<TimeseriesEntry> expression) {
        return mapper.queryPage(TimeseriesEntry.class, expression).getResults();
    }

    private void removeLatestValue(EntityId entityId, DeleteTsKvQuery query) {
        LatestValues latest = LatestValues.newLatest()
                .setEntityId(entityId.getId().toString())
                .setEntityType(entityId.getEntityType().name())
                .setKey(query.getKey());
        mapper.delete(latest);
    }

    private boolean latestInTheRemoveRange(TimeseriesEntry timeEntry, DeleteTsKvQuery query) {
        long ts = timeEntry.getTs();
        return query.getStartTs() <= ts && ts <= query.getEndTs();
    }

    private TimeseriesEntry findLastTimeseries(TimeseriesEntry key) {
        DynamoDBQueryExpression<TimeseriesEntry> expression = queryExpression();
        expression.withKeyConditionExpression("#partition_id = :partition_id")
                .withExpressionAttributeNames(of("#partition_id", "partition_id"))
                .withLimit(1)
                .withScanIndexForward(false)
                .withExpressionAttributeValues(of(":partition_id", attrValue(key.getPartitionId())));

        List<TimeseriesEntry> results = fetchTimeseries(expression);
        return results.get(0);
    }

    private TimeseriesEntry timeseriesHashKey(EntityId entityId, String key) {
        return TimeseriesEntry.newTimeseries()
                .setEntityType(entityId.getEntityType().name())
                .setEntityId(entityId.getId().toString())
                .setKey(key);
    }
}
