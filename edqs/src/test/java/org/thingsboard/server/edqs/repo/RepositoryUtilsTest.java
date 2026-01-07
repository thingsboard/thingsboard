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
package org.thingsboard.server.edqs.repo;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edqs.fields.DeviceFields;
import org.thingsboard.server.common.data.edqs.fields.DeviceProfileFields;
import org.thingsboard.server.common.data.query.BooleanFilterPredicate;
import org.thingsboard.server.common.data.query.BooleanFilterPredicate.BooleanOperation;
import org.thingsboard.server.common.data.query.ComplexFilterPredicate;
import org.thingsboard.server.common.data.query.ComplexFilterPredicate.ComplexOperation;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.data.query.NumericFilterPredicate.NumericOperation;
import org.thingsboard.server.common.data.query.StringFilterPredicate;
import org.thingsboard.server.common.data.query.StringFilterPredicate.StringOperation;
import org.thingsboard.server.edqs.data.DeviceData;
import org.thingsboard.server.edqs.data.EntityProfileData;
import org.thingsboard.server.edqs.data.dp.BoolDataPoint;
import org.thingsboard.server.edqs.data.dp.DoubleDataPoint;
import org.thingsboard.server.edqs.data.dp.LongDataPoint;
import org.thingsboard.server.edqs.data.dp.StringDataPoint;
import org.thingsboard.server.edqs.query.DataKey;
import org.thingsboard.server.edqs.query.EdqsFilter;
import org.thingsboard.server.edqs.util.RepositoryUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;


public class RepositoryUtilsTest {

    private static Stream<Arguments> deviceNameFilters() {
        return Stream.of(Arguments.of(null, getNameFilter(StringOperation.STARTS_WITH, "lora"), true),
                Arguments.of("loranet device 123", getNameFilter(StringOperation.STARTS_WITH, "lora"), true),
                Arguments.of("loranet 123", getNameFilter(StringOperation.STARTS_WITH, "ra"), false),
                Arguments.of("loranet 123", getNameFilter(StringOperation.ENDS_WITH, "123"), true),
                Arguments.of("loranet 123", getNameFilter(StringOperation.ENDS_WITH, "device"), false),
                Arguments.of("loranet 123", getNameFilter(StringOperation.EQUAL, "loranet 123"), true),
                Arguments.of("loranet 123", getNameFilter(StringOperation.EQUAL, "loranet "), false),
                Arguments.of("loranet 123", getNameFilter(StringOperation.NOT_EQUAL, "loranet"), true),
                Arguments.of("loranet 123", getNameFilter(StringOperation.NOT_EQUAL, "loranet 123"), false),
                Arguments.of("loranet 123", getNameFilter(StringOperation.CONTAINS, "loranet"), true),
                Arguments.of("loranet 123", getNameFilter(StringOperation.CONTAINS, "loranet123"), false),
                Arguments.of("loranet 123", getNameFilter(StringOperation.NOT_CONTAINS, "loranet123"), true),
                Arguments.of("loranet 123", getNameFilter(StringOperation.NOT_CONTAINS, "loranet"), false),
                Arguments.of("loranet 123", getNameFilter(StringOperation.IN, "loranet 123, loranet 124"), true),
                Arguments.of("loranet 123", getNameFilter(StringOperation.IN, "loranet 125, loranet 126"), false),
                Arguments.of("loranet 123", getNameFilter(StringOperation.NOT_IN, "loranet 125, loranet 126"), true),
                Arguments.of("loranet 123", getNameFilter(StringOperation.NOT_IN, "loranet 123, loranet 126"), false),

                // Basic CONTAINS
                Arguments.of("loranet 123", getNameFilter(StringOperation.CONTAINS, "%loranet"), false),
                Arguments.of("loranet 123", getNameFilter(StringOperation.CONTAINS, "loranet%"), true),
                Arguments.of("loranet 123", getNameFilter(StringOperation.CONTAINS, "%ranet%"), true),
                Arguments.of("loranet 123", getNameFilter(StringOperation.CONTAINS, "%123"), true),
                Arguments.of("loranet 123", getNameFilter(StringOperation.CONTAINS, "%loranx%"), false),

                // Basic STARTS_WITH
                Arguments.of("loranet 123", getNameFilter(StringOperation.STARTS_WITH, "loranet%"), true),
                Arguments.of("loranet 123", getNameFilter(StringOperation.STARTS_WITH, "lora%"), true),
                Arguments.of("loranet 123", getNameFilter(StringOperation.STARTS_WITH, "lorax%"), false),

                // Basic ENDS_WITH
                Arguments.of("loranet 123", getNameFilter(StringOperation.ENDS_WITH, "%123"), true),
                Arguments.of("loranet 123", getNameFilter(StringOperation.ENDS_WITH, "%23"), true),
                Arguments.of("loranet 123", getNameFilter(StringOperation.ENDS_WITH, "%124"), false),

                // CONTAINS with _
                Arguments.of("loranet 123", getNameFilter(StringOperation.CONTAINS, "loranet_123"), true), // '_' = ' '
                Arguments.of("loranet 123", getNameFilter(StringOperation.CONTAINS, "loranet_12_"), true),
                Arguments.of("loranet 123", getNameFilter(StringOperation.CONTAINS, "loran_t%"), true),

                // STARTS_WITH with _
                Arguments.of("loranet 123", getNameFilter(StringOperation.STARTS_WITH, "loranet_"), true),
                Arguments.of("loranet 123", getNameFilter(StringOperation.STARTS_WITH, "lora__t%"), true),
                Arguments.of("loranet 123", getNameFilter(StringOperation.STARTS_WITH, "lor_net%"), true),

                // ENDS_WITH with _
                Arguments.of("loranet 123", getNameFilter(StringOperation.ENDS_WITH, "_23"), true),
                Arguments.of("loranet 123", getNameFilter(StringOperation.ENDS_WITH, "_2_"), true),
                Arguments.of("loranet 123", getNameFilter(StringOperation.ENDS_WITH, "_3"), true),

                // Mixed patterns
                Arguments.of("loranet 123", getNameFilter(StringOperation.CONTAINS, "lora__t 1%"), true),
                Arguments.of("loranet 123", getNameFilter(StringOperation.CONTAINS, "lora%net%3"), true),
                Arguments.of("loranet 123", getNameFilter(StringOperation.CONTAINS, "%o_anet%2_3"), false),
                Arguments.of("loranet 123", getNameFilter(StringOperation.CONTAINS, "lora___ ___"), true)
        );
    }

    @ParameterizedTest
    @MethodSource("deviceNameFilters")
    public void testFilterByDeviceName(String deviceName, EdqsFilter keyFilter, boolean result) {
        DeviceData deviceData = new DeviceData(UUID.randomUUID());
        deviceData.setCustomerId(UUID.randomUUID());
        deviceData.setFields(DeviceFields.builder().name(deviceName).build());
        assertThat(RepositoryUtils.checkKeyFilters(deviceData, List.of(keyFilter))).isEqualTo(result);
    }

    private static Stream<Arguments> createdTimeFilters() {
        return Stream.of(Arguments.of(1000, getCreatedTimeFilter(NumericOperation.EQUAL, 1000), true),
                Arguments.of(1000, getCreatedTimeFilter(NumericOperation.EQUAL, 1001), false),
                Arguments.of(1000, getCreatedTimeFilter(NumericOperation.NOT_EQUAL, 1000), false),
                Arguments.of(1000, getCreatedTimeFilter(NumericOperation.NOT_EQUAL, 1001), true),
                Arguments.of(1000, getCreatedTimeFilter(NumericOperation.GREATER, 999), true),
                Arguments.of(1000, getCreatedTimeFilter(NumericOperation.GREATER, 1000), false),
                Arguments.of(1000, getCreatedTimeFilter(NumericOperation.GREATER_OR_EQUAL, 1000), true),
                Arguments.of(1000, getCreatedTimeFilter(NumericOperation.GREATER_OR_EQUAL, 1001), false),
                Arguments.of(1000, getCreatedTimeFilter(NumericOperation.LESS, 1001), true),
                Arguments.of(1000, getCreatedTimeFilter(NumericOperation.LESS, 1000), false),
                Arguments.of(1000, getCreatedTimeFilter(NumericOperation.LESS_OR_EQUAL, 1000), true),
                Arguments.of(1000, getCreatedTimeFilter(NumericOperation.LESS_OR_EQUAL, 999), false)
        );
    }

    @ParameterizedTest
    @MethodSource("createdTimeFilters")
    public void testFilterDevicesByCreatedTime(long createdTime, EdqsFilter keyFilter, boolean result) {
        DeviceData deviceData = new DeviceData(UUID.randomUUID());
        deviceData.setCustomerId(UUID.randomUUID());
        deviceData.setFields(DeviceFields.builder().createdTime(createdTime).build());

        assertThat(RepositoryUtils.checkKeyFilters(deviceData, List.of(keyFilter))).isEqualTo(result);
    }

    private static Stream<Arguments> deviceNameAndTypeFilter() {
        return Stream.of(
                Arguments.of("loranet 123", "thermostat", List.of(getNameFilter(StringOperation.STARTS_WITH, "lo"), getTypeFilter(StringOperation.EQUAL, "thermostat")), true),
                Arguments.of("loranet 123", "thermostat", List.of(getNameFilter(StringOperation.STARTS_WITH, "net"), getTypeFilter(StringOperation.EQUAL, "thermostat")), false),
                Arguments.of("loranet 123", "thermostat", List.of(getNameFilter(StringOperation.STARTS_WITH, "lo"), getTypeFilter(StringOperation.EQUAL, "sensor1")), false),
                Arguments.of("loranet 123", "thermostat", List.of(getNameFilter(StringOperation.STARTS_WITH, "net"), getTypeFilter(StringOperation.EQUAL, "sensor1")), false));
    }

    @ParameterizedTest
    @MethodSource("deviceNameAndTypeFilter")
    public void testFilterByDeviceNameAndDeviceType(String deviceName, String deviceType, List<EdqsFilter> keyFilters, boolean result) {
        UUID deviceProfileId = UUID.randomUUID();
        EntityProfileData deviceProfile = new EntityProfileData(deviceProfileId, EntityType.DEVICE_PROFILE);
        deviceProfile.setFields(DeviceProfileFields.builder().name(deviceType).build());

        DeviceData deviceData = new DeviceData(UUID.randomUUID());
        deviceData.setCustomerId(UUID.randomUUID());
        deviceData.setFields(DeviceFields.builder().name(deviceName).deviceProfileId(deviceProfileId).type(deviceType).build());

        assertThat(RepositoryUtils.checkKeyFilters(deviceData, keyFilters)).isEqualTo(result);
    }

    private static Stream<Arguments> deviceNameComplexFilters() {
        return Stream.of(Arguments.of(null, List.of(getComplexComplexDeviceNameFilter(StringOperation.STARTS_WITH, "lo", ComplexOperation.AND, StringOperation.ENDS_WITH, "123")), true),
                Arguments.of("loranet 123", List.of(getComplexComplexDeviceNameFilter(StringOperation.STARTS_WITH, "lo", ComplexOperation.AND, StringOperation.ENDS_WITH, "123")), true),
                Arguments.of("loranet 123", List.of(getComplexComplexDeviceNameFilter(StringOperation.STARTS_WITH, "lo", ComplexOperation.AND, StringOperation.ENDS_WITH, "124")), false),
                Arguments.of("loranet 123", List.of(getComplexComplexDeviceNameFilter(StringOperation.STARTS_WITH, "lo", ComplexOperation.OR, StringOperation.STARTS_WITH, "net")), true),
                Arguments.of("loranet 123", List.of(getComplexComplexDeviceNameFilter(StringOperation.STARTS_WITH, "net", ComplexOperation.OR, StringOperation.STARTS_WITH, "the")), false),
                Arguments.of("loranet123", List.of(getComplexComplexDeviceNameFilter(StringOperation.STARTS_WITH, "lo", ComplexOperation.OR, StringOperation.STARTS_WITH, "the",
                        ComplexOperation.AND, StringOperation.ENDS_WITH, "123")), true),
                Arguments.of("loranet 123", List.of(getComplexComplexDeviceNameFilter(StringOperation.STARTS_WITH, "net", ComplexOperation.OR, StringOperation.STARTS_WITH, "the",
                        ComplexOperation.OR, StringOperation.ENDS_WITH, "123")), true),
                Arguments.of("loranet 123", List.of(getComplexComplexDeviceNameFilter(StringOperation.STARTS_WITH, "net", ComplexOperation.OR, StringOperation.STARTS_WITH, "the",
                        ComplexOperation.AND, StringOperation.ENDS_WITH, "123")), false),
                Arguments.of("loranet 123", List.of(getComplexComplexDeviceNameFilter(StringOperation.STARTS_WITH, "lo", ComplexOperation.OR, StringOperation.STARTS_WITH, "the",
                        ComplexOperation.AND, StringOperation.ENDS_WITH, "124")), false)
        );
    }

    @ParameterizedTest
    @MethodSource("deviceNameComplexFilters")
    public void testFilterByDeviceNameComplexFilters(String deviceName, List<EdqsFilter> keyFilters, boolean result) {
        DeviceData deviceData = new DeviceData(UUID.randomUUID());
        deviceData.setCustomerId(UUID.randomUUID());
        deviceData.setFields(DeviceFields.builder().name(deviceName).build());

        assertThat(RepositoryUtils.checkKeyFilters(deviceData, keyFilters)).isEqualTo(result);
    }

    private static Stream<Arguments> deviceTemperatureFilters() {
        return Stream.of(Arguments.of(22.8, getTemperatureFilter(NumericOperation.EQUAL, 22.8), true),
                Arguments.of(22.8, getTemperatureFilter(NumericOperation.EQUAL, 22.9), false),
                Arguments.of(22.8, getTemperatureFilter(NumericOperation.NOT_EQUAL, 22.8), false),
                Arguments.of(22.8, getTemperatureFilter(NumericOperation.NOT_EQUAL, 22.9), true),
                Arguments.of(22.8, getTemperatureFilter(NumericOperation.GREATER, 22.0), true),
                Arguments.of(22.8, getTemperatureFilter(NumericOperation.GREATER, 23.0), false),
                Arguments.of(22.8, getTemperatureFilter(NumericOperation.GREATER_OR_EQUAL, 22.8), true),
                Arguments.of(22.8, getTemperatureFilter(NumericOperation.GREATER_OR_EQUAL, 23.0), false),
                Arguments.of(22.8, getTemperatureFilter(NumericOperation.LESS, 23.0), true),
                Arguments.of(22.8, getTemperatureFilter(NumericOperation.LESS, 22.0), false),
                Arguments.of(22.8, getTemperatureFilter(NumericOperation.LESS_OR_EQUAL, 22.0), false),
                Arguments.of(22.8, getTemperatureFilter(NumericOperation.LESS_OR_EQUAL, 22.8), true)
        );
    }

    @ParameterizedTest
    @MethodSource("deviceTemperatureFilters")
    public void testFilterByDeviceTemperature(double tempValue, EdqsFilter keyFilter, boolean result) {
        DeviceData deviceData = new DeviceData(UUID.randomUUID());
        deviceData.setCustomerId(UUID.randomUUID());
        deviceData.setFields(DeviceFields.builder().name(StringUtils.randomAlphabetic(10)).build());
        deviceData.putTs(5, new DoubleDataPoint(System.currentTimeMillis(), tempValue));

        assertThat(RepositoryUtils.checkKeyFilters(deviceData, List.of(keyFilter))).isEqualTo(result);
    }

    private static Stream<Arguments> deviceTemperatureComplexFilters() {
        return Stream.of(Arguments.of(22.8, getComplexTemperatureFilter(NumericOperation.GREATER_OR_EQUAL, 22.8, ComplexOperation.AND, NumericOperation.LESS_OR_EQUAL, 30), true),
                Arguments.of(22.8, getComplexTemperatureFilter(NumericOperation.GREATER, 23.5, ComplexOperation.AND, NumericOperation.LESS_OR_EQUAL, 30), false),
                Arguments.of(22.8, getComplexComplexTemperatureFilter(NumericOperation.GREATER, 22.0, ComplexOperation.AND, NumericOperation.LESS_OR_EQUAL, 30, ComplexOperation.OR, NumericOperation.GREATER, 35), true),
                Arguments.of(22.8, getComplexComplexTemperatureFilter(NumericOperation.GREATER, 22.0, ComplexOperation.AND, NumericOperation.LESS_OR_EQUAL, 30, ComplexOperation.AND, NumericOperation.EQUAL, 22.8), true)
        );
    }

    @ParameterizedTest
    @MethodSource("deviceTemperatureComplexFilters")
    public void testComplexFilterByDeviceTemperature(double tempValue, EdqsFilter keyFilter, boolean result) {
        DeviceData deviceData = new DeviceData(UUID.randomUUID());
        deviceData.setCustomerId(UUID.randomUUID());
        deviceData.setFields(DeviceFields.builder().name(StringUtils.randomAlphabetic(10)).build());
        deviceData.putTs(5, new DoubleDataPoint(System.currentTimeMillis(), tempValue));

        assertThat(RepositoryUtils.checkKeyFilters(deviceData, List.of(keyFilter))).isEqualTo(result);
    }

    private static Stream<Arguments> deviceHumidityFilters() {
        return Stream.of(Arguments.of(60, getHumidityFilter(NumericOperation.EQUAL, 60), true),
                Arguments.of(60, getHumidityFilter(NumericOperation.EQUAL, 61), false),
                Arguments.of(60, getHumidityFilter(NumericOperation.NOT_EQUAL, 60), false),
                Arguments.of(60, getHumidityFilter(NumericOperation.NOT_EQUAL, 61), true),
                Arguments.of(60, getHumidityFilter(NumericOperation.GREATER, 59), true),
                Arguments.of(60, getHumidityFilter(NumericOperation.GREATER, 60), false),
                Arguments.of(60, getHumidityFilter(NumericOperation.GREATER_OR_EQUAL, 60), true),
                Arguments.of(60, getHumidityFilter(NumericOperation.GREATER_OR_EQUAL, 61), false),
                Arguments.of(60, getHumidityFilter(NumericOperation.LESS, 61), true),
                Arguments.of(60, getHumidityFilter(NumericOperation.LESS, 60), false),
                Arguments.of(60, getHumidityFilter(NumericOperation.LESS_OR_EQUAL, 59), false),
                Arguments.of(60, getHumidityFilter(NumericOperation.LESS_OR_EQUAL, 60), true)
        );
    }

    @ParameterizedTest
    @MethodSource("deviceHumidityFilters")
    public void testFilterByDeviceHumidity(long humidityValue, EdqsFilter keyFilter, boolean result) {
        DeviceData deviceData = new DeviceData(UUID.randomUUID());
        deviceData.setCustomerId(UUID.randomUUID());
        deviceData.setFields(DeviceFields.builder().name(StringUtils.randomAlphabetic(10)).build());
        deviceData.putTs(6, new LongDataPoint(System.currentTimeMillis(), humidityValue));

        assertThat(RepositoryUtils.checkKeyFilters(deviceData, List.of(keyFilter))).isEqualTo(result);
    }

    private static Stream<Arguments> deviceTemperatureAndHumidityFilters() {
        return Stream.of(Arguments.of(22.8, 60, List.of(getTemperatureFilter(NumericOperation.EQUAL, 22.8), getHumidityFilter(NumericOperation.EQUAL, 60)), true),
                Arguments.of(22.8, 60, List.of(getTemperatureFilter(NumericOperation.EQUAL, 22.8), getHumidityFilter(NumericOperation.GREATER_OR_EQUAL, 61)), false),
                Arguments.of(22.8, 60, List.of(getTemperatureFilter(NumericOperation.GREATER, 23), getHumidityFilter(NumericOperation.GREATER_OR_EQUAL, 60)), false),
                Arguments.of(22.8, 60, List.of(getTemperatureFilter(NumericOperation.GREATER_OR_EQUAL, 22.9), getHumidityFilter(NumericOperation.GREATER_OR_EQUAL, 61)), false)
        );
    }

    @ParameterizedTest
    @MethodSource("deviceTemperatureAndHumidityFilters")
    public void testFilterByDeviceTemperatureAndHumidity(double tempValue, long humidityValue, List<EdqsFilter> keyFilters, boolean result) {
        DeviceData deviceData = new DeviceData(UUID.randomUUID());
        deviceData.setCustomerId(UUID.randomUUID());
        deviceData.setFields(DeviceFields.builder().name(StringUtils.randomAlphabetic(10)).build());
        deviceData.putTs(5, new DoubleDataPoint(System.currentTimeMillis(), tempValue));
        deviceData.putTs(6, new LongDataPoint(System.currentTimeMillis(), humidityValue));

        assertThat(RepositoryUtils.checkKeyFilters(deviceData, keyFilters)).isEqualTo(result);
    }

    private static Stream<Arguments> deviceVersionAttributeFilters() {
        return Stream.of(Arguments.of(true, getActiveAttributeFilter(BooleanOperation.EQUAL, true), true),
                Arguments.of(true, getActiveAttributeFilter(BooleanOperation.EQUAL, false), false),
                Arguments.of(true, getActiveAttributeFilter(BooleanOperation.NOT_EQUAL, true), false),
                Arguments.of(true, getActiveAttributeFilter(BooleanOperation.NOT_EQUAL, false), true)
        );
    }

    @ParameterizedTest
    @MethodSource("deviceVersionAttributeFilters")
    public void testFilterByDeviceVersionAttribute(Boolean active, EdqsFilter keyFilter, boolean result) {
        DeviceData deviceData = new DeviceData(UUID.randomUUID());
        deviceData.setCustomerId(UUID.randomUUID());
        deviceData.setFields(DeviceFields.builder().name(StringUtils.randomAlphabetic(10)).build());
        deviceData.putAttr(2, AttributeScope.SERVER_SCOPE, new BoolDataPoint(System.currentTimeMillis(), active));

        assertThat(RepositoryUtils.checkKeyFilters(deviceData, List.of(keyFilter))).isEqualTo(result);
    }

    private static Stream<Arguments> deviceActiveAndVersionFilters() {
        return Stream.of(Arguments.of(true, "3.2.1", List.of(getActiveAttributeFilter(BooleanOperation.EQUAL, true), getVersionAttributeFilter(StringOperation.EQUAL, "3.2.1")), true),
                Arguments.of(true, "3.2.1", List.of(getActiveAttributeFilter(BooleanOperation.EQUAL, true), getVersionAttributeFilter(StringOperation.EQUAL, "3.2.2")), false),
                Arguments.of(true, "3.2.1", List.of(getActiveAttributeFilter(BooleanOperation.EQUAL, false), getVersionAttributeFilter(StringOperation.EQUAL, "3.2.1")), false),
                Arguments.of(true, "3.2.1", List.of(getActiveAttributeFilter(BooleanOperation.EQUAL, false), getVersionAttributeFilter(StringOperation.EQUAL, "3.2.2")), false)
        );
    }

    @ParameterizedTest
    @MethodSource("deviceActiveAndVersionFilters")
    public void testFilterByActiveAndVersionAttributes(Boolean active, String version, List<EdqsFilter> keyFilters, boolean result) {
        DeviceData deviceData = new DeviceData(UUID.randomUUID());
        deviceData.setCustomerId(UUID.randomUUID());
        deviceData.setFields(DeviceFields.builder().name(StringUtils.randomAlphabetic(10)).build());
        deviceData.putAttr(1, AttributeScope.CLIENT_SCOPE, new StringDataPoint(System.currentTimeMillis(), version));
        deviceData.putAttr(2, AttributeScope.SERVER_SCOPE, new BoolDataPoint(System.currentTimeMillis(), active));

        assertThat(RepositoryUtils.checkKeyFilters(deviceData, keyFilters)).isEqualTo(result);
    }

    private static EdqsFilter getVersionAttributeFilter(StringOperation operation, String predicateValue) {
        StringFilterPredicate filterPredicate = new StringFilterPredicate();
        filterPredicate.setOperation(operation);
        filterPredicate.setValue(FilterPredicateValue.fromString(predicateValue));

        DataKey key = new DataKey(EntityKeyType.CLIENT_ATTRIBUTE, "version", 1);
        return new EdqsFilter(key, EntityKeyValueType.STRING, filterPredicate);
    }


    private static EdqsFilter getActiveAttributeFilter(BooleanOperation operation, boolean predicateValue) {
        BooleanFilterPredicate filterPredicate = new BooleanFilterPredicate();
        filterPredicate.setOperation(operation);
        filterPredicate.setValue(FilterPredicateValue.fromBoolean(predicateValue));

        DataKey key = new DataKey(EntityKeyType.SERVER_ATTRIBUTE, "active", 2);
        return new EdqsFilter(key, EntityKeyValueType.BOOLEAN, filterPredicate);
    }

    private static EdqsFilter getTemperatureFilter(NumericOperation operation, double predicateValue) {
        return getTimeseriesFilter("temperature", 5, operation, predicateValue);
    }

    private static EdqsFilter getHumidityFilter(NumericOperation operation, double predicateValue) {
        return getTimeseriesFilter("humidity", 6, operation, predicateValue);
    }

    private static EdqsFilter getTimeseriesFilter(String key, Integer keysId, NumericOperation operation, double predicateValue) {
        NumericFilterPredicate filterPredicate = new NumericFilterPredicate();
        filterPredicate.setOperation(operation);
        filterPredicate.setValue(FilterPredicateValue.fromDouble(predicateValue));

        DataKey newKey = new DataKey(EntityKeyType.TIME_SERIES, key, keysId);
        return new EdqsFilter(newKey, EntityKeyValueType.NUMERIC, filterPredicate);
    }

    private static EdqsFilter getNameFilter(StringOperation operation, String predicateValue) {
        return getStringEntityFieldFilter("name", operation, predicateValue);
    }

    private static EdqsFilter getTypeFilter(StringOperation operation, String predicateValue) {
        return getStringEntityFieldFilter("type", operation, predicateValue);
    }

    private static EdqsFilter getStringEntityFieldFilter(String fieldName, StringOperation operation, String predicateValue) {
        StringFilterPredicate filterPredicate = new StringFilterPredicate();
        filterPredicate.setOperation(operation);
        filterPredicate.setValue(FilterPredicateValue.fromString(predicateValue));

        DataKey key = new DataKey(EntityKeyType.ENTITY_FIELD, fieldName, 3);
        return new EdqsFilter(key, EntityKeyValueType.STRING, filterPredicate);
    }

    private static EdqsFilter getCreatedTimeFilter(NumericOperation operation, double predicateValue) {
        return getDatetimeEntityFieldFilter("createdTime", operation, predicateValue);
    }

    private static EdqsFilter getDatetimeEntityFieldFilter(String fieldName, NumericOperation operation, double predicateValue) {
        NumericFilterPredicate filterPredicate = new NumericFilterPredicate();
        filterPredicate.setOperation(operation);
        filterPredicate.setValue(FilterPredicateValue.fromDouble(predicateValue));

        DataKey key = new DataKey(EntityKeyType.ENTITY_FIELD, fieldName, 3);
        return new EdqsFilter(key, EntityKeyValueType.DATE_TIME, filterPredicate);
    }

    private static EdqsFilter getComplexTemperatureFilter(NumericOperation operation, double predicateValue, ComplexOperation complexOperation, NumericOperation operation2, double predicateValue2) {
        ComplexFilterPredicate complexFilterPredicate = getComplexNumericFilterPredicate(operation, predicateValue, complexOperation, operation2, predicateValue2);

        DataKey key = new DataKey(EntityKeyType.TIME_SERIES, "temperature", 5);
        return new EdqsFilter(key, EntityKeyValueType.NUMERIC, complexFilterPredicate);
    }

    private static EdqsFilter getComplexComplexTemperatureFilter(NumericOperation operation, double predicateValue, ComplexOperation complexOperation, NumericOperation operation2, double predicateValue2,
                                                                 ComplexOperation complexOperation2, NumericOperation operation3, double predicateValue3) {
        ComplexFilterPredicate complexFilterPredicate = getComplexNumericFilterPredicate(operation, predicateValue, complexOperation, operation2, predicateValue2);

        NumericFilterPredicate filterPredicate = new NumericFilterPredicate();
        filterPredicate.setOperation(operation);
        filterPredicate.setValue(FilterPredicateValue.fromDouble(predicateValue));

        ComplexFilterPredicate mainComplexFilterPredicate = new ComplexFilterPredicate();
        mainComplexFilterPredicate.setOperation(complexOperation2);
        mainComplexFilterPredicate.setPredicates(List.of(complexFilterPredicate, filterPredicate));

        DataKey key = new DataKey(EntityKeyType.TIME_SERIES, "temperature", 5);
        return new EdqsFilter(key, EntityKeyValueType.NUMERIC, mainComplexFilterPredicate);
    }

    private static ComplexFilterPredicate getComplexNumericFilterPredicate(NumericOperation operation, double predicateValue, ComplexOperation complexOperation, NumericOperation operation2, double predicateValue2) {
        NumericFilterPredicate filterPredicate = new NumericFilterPredicate();
        filterPredicate.setOperation(operation);
        filterPredicate.setValue(FilterPredicateValue.fromDouble(predicateValue));

        NumericFilterPredicate filterPredicate2 = new NumericFilterPredicate();
        filterPredicate2.setOperation(operation2);
        filterPredicate2.setValue(FilterPredicateValue.fromDouble(predicateValue2));

        ComplexFilterPredicate complexFilterPredicate = new ComplexFilterPredicate();
        complexFilterPredicate.setOperation(complexOperation);
        complexFilterPredicate.setPredicates(List.of(filterPredicate, filterPredicate2));
        return complexFilterPredicate;
    }

    private static EdqsFilter getComplexComplexDeviceNameFilter(StringOperation operation, String predicateValue, ComplexOperation complexOperation, StringOperation operation2, String predicateValue2) {
        ComplexFilterPredicate complexFilterPredicate = getComplexStringFilterPredicate(operation, predicateValue, complexOperation, operation2, predicateValue2);
        DataKey key = new DataKey(EntityKeyType.ENTITY_FIELD, "name", 3);
        return new EdqsFilter(key, EntityKeyValueType.STRING, complexFilterPredicate);
    }

    private static EdqsFilter getComplexComplexDeviceNameFilter(StringOperation operation, String predicateValue, ComplexOperation complexOperation, StringOperation operation2, String predicateValue2,
                                                                ComplexOperation complexOperation2, StringOperation operation3, String predicateValue3) {
        ComplexFilterPredicate complexFilterPredicate = getComplexStringFilterPredicate(operation, predicateValue, complexOperation, operation2, predicateValue2);

        StringFilterPredicate filterPredicate = new StringFilterPredicate();
        filterPredicate.setOperation(operation3);
        filterPredicate.setValue(FilterPredicateValue.fromString(predicateValue3));

        ComplexFilterPredicate mainComplexFilterPredicate = new ComplexFilterPredicate();
        mainComplexFilterPredicate.setOperation(complexOperation2);
        mainComplexFilterPredicate.setPredicates(List.of(complexFilterPredicate, filterPredicate));

        DataKey key = new DataKey(EntityKeyType.ENTITY_FIELD, "name", 3);
        return new EdqsFilter(key, EntityKeyValueType.STRING, mainComplexFilterPredicate);
    }

    private static ComplexFilterPredicate getComplexStringFilterPredicate(StringOperation operation, String predicateValue, ComplexOperation complexOperation, StringOperation operation2, String predicateValue2) {
        StringFilterPredicate filterPredicate = new StringFilterPredicate();
        filterPredicate.setOperation(operation);
        filterPredicate.setValue(FilterPredicateValue.fromString(predicateValue));

        StringFilterPredicate filterPredicate2 = new StringFilterPredicate();
        filterPredicate2.setOperation(operation2);
        filterPredicate2.setValue(FilterPredicateValue.fromString(predicateValue2));

        ComplexFilterPredicate complexFilterPredicate = new ComplexFilterPredicate();
        complexFilterPredicate.setOperation(complexOperation);
        complexFilterPredicate.setPredicates(List.of(filterPredicate, filterPredicate2));
        return complexFilterPredicate;
    }

}
