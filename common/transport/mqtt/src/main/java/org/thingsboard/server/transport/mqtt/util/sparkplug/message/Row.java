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
package org.thingsboard.server.transport.mqtt.util.sparkplug.message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A class for representing a row of a data set.
 */
public class Row {

	private List<SparkplugValue<?>> values;

	public Row(List<SparkplugValue<?>> values) {
		this.values = values;
	}

	public List<SparkplugValue<?>> getValues() {
		return values;
	}

	public void setValues(List<SparkplugValue<?>> values) {
		this.values = values;
	}

	public void addValue(SparkplugValue<?> value) {
		this.values.add(value);
	}

	@Override
	public String toString() {
		return "Row [values=" + values + "]";
	}
	
	/**
	 * Converts a {@link Row} instance to a {@link List} of Objects representing the values.
	 * 
	 * @param row a {@link Row} instance.
	 * @return a {@link List} of Objects.
	 */
	public static List<Object> toValues(Row row) {
		List<Object> list = new ArrayList<Object>(row.getValues().size());
		for (SparkplugValue<?> value : row.getValues()) {
			list.add(value.getValue());
		}
		return list;
	}
	
	/**
	 * A builder for creating a {@link Row} instance.
	 */
	public static class RowBuilder {
		
		private List<SparkplugValue<?>> values;
		
		public RowBuilder() {
			this.values = new ArrayList<SparkplugValue<?>>();
		}
		
		public RowBuilder(Row row) {
			this.values = new ArrayList<SparkplugValue<?>>(row.getValues());
		}

		public RowBuilder addValue(SparkplugValue<?> value) {
			this.values.add(value);
			return this;
		}

		public RowBuilder addValues(Collection<SparkplugValue<?>> values) {
			this.values.addAll(values);
			return this;
		}

		public Row createRow() {
			return new Row(values);
		}
	}
}
