/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt.util.sparkplug;

/**
 * Created by nickAS21 on 13.12.22
 */

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.transport.mqtt.util.sparkplug.message.DataSet;
import org.thingsboard.server.transport.mqtt.util.sparkplug.message.DataSetDataType;
import org.thingsboard.server.transport.mqtt.util.sparkplug.message.File;
import org.thingsboard.server.transport.mqtt.util.sparkplug.message.MetaData;
import org.thingsboard.server.transport.mqtt.util.sparkplug.message.Metric;
import org.thingsboard.server.transport.mqtt.util.sparkplug.message.MetricDataType;
import org.thingsboard.server.transport.mqtt.util.sparkplug.message.Parameter;
import org.thingsboard.server.transport.mqtt.util.sparkplug.message.ParameterDataType;
import org.thingsboard.server.transport.mqtt.util.sparkplug.message.PropertyDataType;
import org.thingsboard.server.transport.mqtt.util.sparkplug.message.PropertySet;
import org.thingsboard.server.transport.mqtt.util.sparkplug.message.PropertyValue;
import org.thingsboard.server.transport.mqtt.util.sparkplug.message.Row;
import org.thingsboard.server.transport.mqtt.util.sparkplug.message.Template;
import org.thingsboard.server.transport.mqtt.util.sparkplug.message.SparkplugValue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link SparkplugPayloadDecoder} implementation for decoding Sparkplug B payloads.
 */
@Slf4j
public class SparkplugBPayloadDecoder implements SparkplugPayloadDecoder<SparkplugBPayload> {


    public SparkplugBPayloadDecoder() {
        super();
    }

    public SparkplugBPayload buildFromByteArray(byte[] bytes) throws Exception {
        SparkplugBProto.Payload protoPayload = SparkplugBProto.Payload.parseFrom(bytes);
        SparkplugBPayload.SparkplugBPayloadBuilder builder = new SparkplugBPayload.SparkplugBPayloadBuilder(protoPayload.getSeq());

        // Set the timestamp
        if (protoPayload.hasTimestamp()) {
            builder.setTimestamp(new Date(protoPayload.getTimestamp()));
        }

        // Set the sequence number
        if (protoPayload.hasSeq()) {
            builder.setSeq(protoPayload.getSeq());
        }

        // Set the Metrics
        for (SparkplugBProto.Payload.Metric protoMetric : protoPayload.getMetricsList()) {
            builder.addMetric(convertMetric(protoMetric));
        }

        // Set the body
        if (protoPayload.hasBody()) {
            builder.setBody(protoPayload.getBody().toByteArray());
        }

        // Set the body
        if (protoPayload.hasUuid()) {
            builder.setUuid(protoPayload.getUuid());
        }

        return builder.createPayload();
    }

    private Metric convertMetric(SparkplugBProto.Payload.Metric protoMetric) throws Exception {
        // Convert the dataType
        MetricDataType dataType = MetricDataType.fromInteger((protoMetric.getDatatype()));

        // Build and return the Metric
        return new Metric.MetricBuilder(protoMetric.getName(), dataType, getMetricValue(protoMetric))
                .isHistorical(
                        protoMetric.hasIsHistorical() ? protoMetric.getIsHistorical() : null)
                .isTransient(protoMetric
                        .hasIsTransient() ? protoMetric.getIsTransient() : null)
                .timestamp(protoMetric.hasTimestamp() ? new Date(protoMetric.getTimestamp()) : null)
                .alias(protoMetric.hasAlias() ? protoMetric.getAlias() : null)
                .metaData(protoMetric.hasMetadata()
                        ? new MetaData.MetaDataBuilder().contentType(protoMetric.getMetadata().getContentType())
                        .size(protoMetric.getMetadata().getSize()).seq(protoMetric.getMetadata().getSeq())
                        .fileName(protoMetric.getMetadata().getFileName())
                        .fileType(protoMetric.getMetadata().getFileType())
                        .md5(protoMetric.getMetadata().getMd5())
                        .description(protoMetric.getMetadata().getDescription()).createMetaData()
                        : null)
                .properties(protoMetric.hasProperties()
                        ? new PropertySet.PropertySetBuilder().addProperties(convertProperties(protoMetric.getProperties()))
                        .createPropertySet()
                        : null)
                .createMetric();
    }

    private Map<String, PropertyValue> convertProperties(SparkplugBProto.Payload.PropertySet decodedPropSet)
            throws Exception {
        Map<String, PropertyValue> map = new HashMap<String, PropertyValue>();
        List<String> keys = decodedPropSet.getKeysList();
        List<SparkplugBProto.Payload.PropertyValue> values = decodedPropSet.getValuesList();
        for (int i = 0; i < keys.size(); i++) {
            SparkplugBProto.Payload.PropertyValue value = values.get(i);
            map.put(keys.get(i),
                    new PropertyValue(PropertyDataType.fromInteger(value.getType()), getPropertyValue(value)));
        }
        return map;
    }

    private Object getPropertyValue(SparkplugBProto.Payload.PropertyValue value) throws Exception {
        PropertyDataType type = PropertyDataType.fromInteger(value.getType());
        if (value.getIsNull()) {
            return null;
        }
        switch (type) {
            case Boolean:
                return value.getBooleanValue();
            case DateTime:
                return new Date(value.getLongValue());
            case Float:
                return value.getFloatValue();
            case Double:
                return value.getDoubleValue();
            case Int8:
                return (byte) value.getIntValue();
            case Int16:
            case UInt8:
                return (short) value.getIntValue();
            case Int32:
            case UInt16:
                return value.getIntValue();
            case UInt32:
            case Int64:
                return value.getLongValue();
            case UInt64:
                return BigInteger.valueOf(value.getLongValue());
            case String:
            case Text:
                return value.getStringValue();
            case PropertySet:
                return new PropertySet.PropertySetBuilder().addProperties(convertProperties(value.getPropertysetValue()))
                        .createPropertySet();
            case PropertySetList:
                List<PropertySet> propertySetList = new ArrayList<PropertySet>();
                List<SparkplugBProto.Payload.PropertySet> list = value.getPropertysetsValue().getPropertysetList();
                for (SparkplugBProto.Payload.PropertySet decodedPropSet : list) {
                    propertySetList.add(new PropertySet.PropertySetBuilder().addProperties(convertProperties(decodedPropSet))
                            .createPropertySet());
                }
                return propertySetList;
            case Unknown:
            default:
                throw new Exception("Failed to decode: Unknown Property Data Type " + type);
        }
    }

    private Object getMetricValue(SparkplugBProto.Payload.Metric protoMetric) throws Exception {
        // Check if the null flag has been set indicating that the value is null
        if (protoMetric.getIsNull()) {
            return null;
        }
        // Otherwise convert the value based on the type
        int metricType = protoMetric.getDatatype();
        switch (MetricDataType.fromInteger(metricType)) {
            case Boolean:
                return protoMetric.getBooleanValue();
            case DateTime:
                return new Date(protoMetric.getLongValue());
            case File:
                String filename = protoMetric.getMetadata().getFileName();
                byte[] fileBytes = protoMetric.getBytesValue().toByteArray();
                return new File(filename, fileBytes);
            case Float:
                return protoMetric.getFloatValue();
            case Double:
                return protoMetric.getDoubleValue();
            case Int8:
                return (byte) protoMetric.getIntValue();
            case Int16:
            case UInt8:
                return (short) protoMetric.getIntValue();
            case Int32:
            case UInt16:
                return protoMetric.getIntValue();
            case UInt32:
            case Int64:
                return protoMetric.getLongValue();
            case UInt64:
                return BigInteger.valueOf(protoMetric.getLongValue());
            case String:
            case Text:
            case UUID:
                return protoMetric.getStringValue();
            case Bytes:
                return protoMetric.getBytesValue().toByteArray();
            case DataSet:
                SparkplugBProto.Payload.DataSet protoDataSet = protoMetric.getDatasetValue();
                // Build the and create the DataSet
                return new DataSet.DataSetBuilder(protoDataSet.getNumOfColumns()).addColumnNames(protoDataSet.getColumnsList())
                        .addTypes(convertDataSetDataTypes(protoDataSet.getTypesList()))
                        .addRows(convertDataSetRows(protoDataSet.getRowsList(), protoDataSet.getTypesList()))
                        .createDataSet();
            case Template:
                SparkplugBProto.Payload.Template protoTemplate = protoMetric.getTemplateValue();
                List<Metric> metrics = new ArrayList<Metric>();
                List<Parameter> parameters = new ArrayList<Parameter>();

                for (SparkplugBProto.Payload.Template.Parameter protoParameter : protoTemplate.getParametersList()) {
                    String name = protoParameter.getName();
                    ParameterDataType type = ParameterDataType.fromInteger(protoParameter.getType());
                    Object value = getParameterValue(protoParameter);
                    if (log.isTraceEnabled()) {
                        log.trace("Setting template parameter name: " + name + ", type: " + type + ", value: "
                                + value + ", valueType" + value.getClass());
                    }

                    parameters.add(new Parameter(name, type, value));
                }

                for (SparkplugBProto.Payload.Metric protoTemplateMetric : protoTemplate.getMetricsList()) {
                    Metric templateMetric = convertMetric(protoTemplateMetric);
                    if (log.isTraceEnabled()) {
                        log.trace("Setting template parameter name: " + templateMetric.getName() + ", type: "
                                + templateMetric.getDataType() + ", value: " + templateMetric.getValue());
                    }
                    metrics.add(templateMetric);
                }

                Template template = new Template.TemplateBuilder().version(protoTemplate.getVersion())
                        .templateRef(protoTemplate.getTemplateRef()).definition(protoTemplate.getIsDefinition())
                        .addMetrics(metrics).addParameters(parameters).createTemplate();

                if (log.isTraceEnabled()) {
                    log.trace(
                            "Setting template - name: " + protoMetric.getName() + ", version: " + template.getVersion()
                                    + ", ref: " + template.getTemplateRef() + ", isDef: " + template.isDefinition()
                                    + ", metrics: " + metrics.size() + ", params: " + parameters.size());
                }

                return template;
            case Unknown:
            default:
                throw new Exception("Failed to decode: Unknown Metric DataType " + metricType);

        }
    }

    private Collection<Row> convertDataSetRows(List<SparkplugBProto.Payload.DataSet.Row> protoRows,
                                               List<Integer> protoTypes) throws Exception {
        Collection<Row> rows = new ArrayList<Row>();
        if (protoRows != null) {
            for (SparkplugBProto.Payload.DataSet.Row protoRow : protoRows) {
                List<SparkplugBProto.Payload.DataSet.DataSetValue> protoValues = protoRow.getElementsList();
                List<SparkplugValue<?>> values = new ArrayList<SparkplugValue<?>>();
                for (int index = 0; index < protoRow.getElementsCount(); index++) {
                    values.add(convertDataSetValue(protoTypes.get(index), protoValues.get(index)));
                }
                // Add the values to the row and the row to the rows
                rows.add(new Row.RowBuilder().addValues(values).createRow());
            }
        }
        return rows;
    }

    private Collection<DataSetDataType> convertDataSetDataTypes(List<Integer> protoTypes) {
        List<DataSetDataType> types = new ArrayList<DataSetDataType>();
        // Build up a List of column types
        for (int type : protoTypes) {
            types.add(DataSetDataType.fromInteger(type));
        }
        return types;
    }

    private Object getParameterValue(SparkplugBProto.Payload.Template.Parameter protoParameter) throws Exception {
        // Otherwise convert the value based on the type
        int type = protoParameter.getType();
        switch (MetricDataType.fromInteger(type)) {
            case Boolean:
                return protoParameter.getBooleanValue();
            case DateTime:
                return new Date(protoParameter.getLongValue());
            case Float:
                return protoParameter.getFloatValue();
            case Double:
                return protoParameter.getDoubleValue();
            case Int8:
                return (byte) protoParameter.getIntValue();
            case Int16:
            case UInt8:
                return (short) protoParameter.getIntValue();
            case Int32:
            case UInt16:
                return protoParameter.getIntValue();
            case UInt32:
            case Int64:
                return protoParameter.getLongValue();
            case UInt64:
                return BigInteger.valueOf(protoParameter.getLongValue());
            case String:
            case Text:
                return protoParameter.getStringValue();
            case Unknown:
            default:
                throw new Exception("Failed to decode: Unknown Parameter Type " + type);
        }
    }

    private SparkplugValue<?> convertDataSetValue(int protoType, SparkplugBProto.Payload.DataSet.DataSetValue protoValue)
            throws Exception {

        DataSetDataType type = DataSetDataType.fromInteger(protoType);
        switch (type) {
            case Boolean:
                return new SparkplugValue<Boolean>(type, protoValue.getBooleanValue());
            case DateTime:
                // FIXME - remove after is_null is supported for dataset values
                if (protoValue.getLongValue() == -9223372036854775808L) {
                    return new SparkplugValue<Date>(type, null);
                } else {
                    return new SparkplugValue<Date>(type, new Date(protoValue.getLongValue()));
                }
            case Float:
                return new SparkplugValue<Float>(type, protoValue.getFloatValue());
            case Double:
                return new SparkplugValue<Double>(type, protoValue.getDoubleValue());
            case Int8:
                return new SparkplugValue<Byte>(type, (byte) protoValue.getIntValue());
            case UInt8:
            case Int16:
                return new SparkplugValue<Short>(type, (short) protoValue.getIntValue());
            case UInt16:
            case Int32:
                return new SparkplugValue<Integer>(type, protoValue.getIntValue());
            case UInt32:
            case Int64:
                return new SparkplugValue<Long>(type, protoValue.getLongValue());
            case UInt64:
                return new SparkplugValue<BigInteger>(type, BigInteger.valueOf(protoValue.getLongValue()));
            case String:
            case Text:
                if (protoValue.getStringValue().equals("null")) {
                    return new SparkplugValue<String>(type, null);
                } else {
                    return new SparkplugValue<String>(type, protoValue.getStringValue());
                }
            case Unknown:
            default:
                log.error("Unknown DataType: " + protoType);
                throw new Exception("Failed to decode");
        }
    }
}
