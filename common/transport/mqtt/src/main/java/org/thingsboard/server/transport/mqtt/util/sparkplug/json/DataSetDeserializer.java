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
package org.thingsboard.server.transport.mqtt.util.sparkplug.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.thingsboard.server.transport.mqtt.util.sparkplug.message.DataSet;
import org.thingsboard.server.transport.mqtt.util.sparkplug.message.DataSetDataType;
import org.thingsboard.server.transport.mqtt.util.sparkplug.message.Row;
import org.thingsboard.server.transport.mqtt.util.sparkplug.message.SparkplugValue;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A JSON deserializer for {@link DataSet} instances.
 */
public class DataSetDeserializer extends StdDeserializer<DataSet> {
	
	private static Logger logger = LogManager.getLogger(DataSetDeserializer.class.getName());
	
	private static final String FIELD_SIZE = "numberOfColumns";
	private static final String FIELD_TYPES = "types";
	private static final String FIELD_NAMES = "columnNames";
	private static final String FIELD_ROWS = "rows";

	/**
	 * Constructor.
	 * 
	 * @param clazz
	 */
	protected DataSetDeserializer(Class<DataSet> clazz) {
		super(clazz);
	}

	@Override
	public DataSet deserialize(JsonParser parser, DeserializationContext context) 
			throws IOException, JsonProcessingException {
		JsonNode node = parser.getCodec().readTree(parser);
		long size = (Long) node.get(FIELD_SIZE).numberValue();
		DataSet.DataSetBuilder builder = new DataSet.DataSetBuilder(size);
		JsonNode namesNode = node.get(FIELD_NAMES);
		if (namesNode.isArray()) {
			for (JsonNode nameNode : namesNode) {
				builder.addColumnName(nameNode.textValue());
			}
		}
		JsonNode typesNode = node.get(FIELD_TYPES);
		List<DataSetDataType> typesList = new ArrayList<DataSetDataType>();
		if (typesNode.isArray()) {
			for (JsonNode typeNode : typesNode) {
				typesList.add(DataSetDataType.valueOf(typeNode.textValue()));
			}
			builder.addTypes(typesList);
		}
		JsonNode rowsNode = node.get(FIELD_ROWS);
		if (rowsNode.isArray()) {
			for (JsonNode rowNode : rowsNode) {
				List<SparkplugValue<?>> values = new ArrayList<SparkplugValue<?>>();
				for (int i = 0; i < size; i++) {
					JsonNode value = rowNode.get(i);
					DataSetDataType type = typesList.get(i);
					values.add(getValueFromNode(value, type));
				}
				builder.addRow(new Row(values));
			}
		}
		try {
			return builder.createDataSet();
		} catch (Exception e) {
			logger.error("Error deserializing DataSet ", e);
		}
		return null;
	}
	
	/*
	 * Creates and returns a Value instance
	 */
	private SparkplugValue<?> getValueFromNode(JsonNode nodeValue, DataSetDataType type) {
		switch (type) {
			case Boolean:
				return new SparkplugValue<Boolean>(type, (boolean)nodeValue.asBoolean());
			case DateTime:
				return new SparkplugValue<Date>(type, new Date(nodeValue.asLong()));
			case Double:
				return new SparkplugValue<Double>(type, nodeValue.asDouble());
			case Float:
				return new SparkplugValue<Float>(type, (float)nodeValue.asDouble());
			case Int16:
			case UInt8:
				return new SparkplugValue<Byte>(type, (byte)nodeValue.asInt());
			case UInt16:
			case Int32:
				return new SparkplugValue<Integer>(type, nodeValue.asInt());
			case UInt32:
			case Int64:
				return new SparkplugValue<Long>(type, (long)nodeValue.asLong());
			case Text:
			case String:
				return new SparkplugValue<String>(type, nodeValue.asText());
			case UInt64:
				return new SparkplugValue<BigInteger>(type, BigInteger.valueOf(nodeValue.asLong()));
			case Unknown:
			default:
				return null;
		}
	}
}
