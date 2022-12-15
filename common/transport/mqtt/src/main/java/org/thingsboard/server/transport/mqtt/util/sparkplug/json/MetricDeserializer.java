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
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.thingsboard.server.transport.mqtt.util.sparkplug.message.File;
import org.thingsboard.server.transport.mqtt.util.sparkplug.message.MetaData;
import org.thingsboard.server.transport.mqtt.util.sparkplug.message.Metric;
import org.thingsboard.server.transport.mqtt.util.sparkplug.message.MetricDataType;

import java.io.IOException;
import java.util.Base64;

/**
 * A custom JSON deserializer for {@link Metric} instances.
 */
public class MetricDeserializer extends StdDeserializer<Metric> implements ResolvableDeserializer {

	private final JsonDeserializer<?> defaultDeserializer;

	/**
	 * Constructor.
	 */
	protected MetricDeserializer(JsonDeserializer<?> defaultDeserializer) {
		super(Metric.class);
		this.defaultDeserializer = defaultDeserializer;
	}

	@Override
	public Metric deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		
		Metric metric = (Metric) defaultDeserializer.deserialize(parser, ctxt);
		System.out.println(metric);
		
		// Check if the data type is a File
		if (metric.getDataType().equals(MetricDataType.File)) {
			// Perform the custom logic for File types by building up the File object.
			MetaData metaData = metric.getMetaData();
			String fileName = metaData == null ? null : metaData.getFileName();
			File file = new File(fileName, Base64.getDecoder().decode((String)metric.getValue()));
			metric.setValue(file);
		}
		return metric;
	}

	@Override
	public void resolve(DeserializationContext ctxt) throws JsonMappingException {
		((ResolvableDeserializer) defaultDeserializer).resolve(ctxt);
	}

}
