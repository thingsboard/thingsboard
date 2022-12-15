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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.thingsboard.server.transport.mqtt.util.sparkplug.message.DataSet.DataSetBuilder;
import org.thingsboard.server.transport.mqtt.util.sparkplug.message.MetaData.MetaDataBuilder;

import java.util.Date;

;

/**
 * A metric of a Sparkplug Payload.
 */
@JsonIgnoreProperties(value = { "isNull" })
@JsonInclude(Include.NON_NULL)
public class Metric {
	
	@JsonProperty("name")
	private String name;
	
	@JsonProperty("alias")
	private Long alias;
	
	@JsonProperty("timestamp")
	private Date timestamp;
	
	@JsonProperty("dataType")
	private MetricDataType dataType;
	
	@JsonProperty("isHistorical")
	private Boolean isHistorical;
	
	@JsonProperty("isTransient")
	private Boolean isTransient;
	
	@JsonProperty("metaData")
	private MetaData metaData;
	
	@JsonProperty("properties")
	@JsonInclude(Include.NON_EMPTY)
	private PropertySet properties;
	
	@JsonProperty("value")
	private Object value;
	
	private Boolean isNull = null;
	
	public Metric() {};

	/**
	 * @param name
	 * @param alias
	 * @param timestamp
	 * @param dataType
	 * @param isHistorical
	 * @param isTransient
	 * @param metaData
	 * @param properties
	 * @param value
	 * @throws Exception
	 */
	public Metric(String name, Long alias, Date timestamp, MetricDataType dataType, Boolean isHistorical,
                  Boolean isTransient, MetaData metaData, PropertySet properties, Object value)
					throws Exception {
		super();
		this.name = name;
		this.alias = alias;
		this.timestamp = timestamp;
		this.dataType = dataType;
		this.isHistorical = isHistorical;
		this.isTransient = isTransient;
		isNull = (value == null) ? true : false;
		this.metaData = metaData;
		this.properties = properties;
		this.value = value;
		this.dataType.checkType(value);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean hasName() {
		return !(name == null);
	}
	
	public boolean hasAlias() {
		return !(alias == null);
	}

	public Long getAlias() {
		return alias;
	}

	public void setAlias(long alias) {
		this.alias = alias;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public MetricDataType getDataType() {
		return dataType;
	}

	public void setDataType(MetricDataType dataType) {
		this.dataType = dataType;
	}

	@JsonGetter("metaData")
	public MetaData getMetaData() {
		return metaData;
	}

	@JsonSetter("metaData")
	public void setMetaData(MetaData metaData) {
		this.metaData = metaData;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
		isNull = (value == null);
	}

	public PropertySet getProperties() {
		return this.properties;
	}

	public void setProperties(PropertySet properties) {
		this.properties = properties;
	}

	@JsonIgnore
	public Boolean isHistorical() {
		return isHistorical == null ? false : isHistorical;
	}

	@JsonGetter("isHistorical")
	public Boolean getIsHistorical() {
		return isHistorical;
	}

	@JsonSetter("isHistorical")
	public void setHistorical(Boolean isHistorical) {
		this.isHistorical = isHistorical;
	}

	@JsonIgnore
	public Boolean isTransient() {
		return isTransient == null ? false : isTransient;
	}

	@JsonGetter("isTransient")
	public Boolean getIsTransient() {
		return isTransient;
	}

	@JsonSetter("isTransient")
	public void setTransient(Boolean isTransient) {
		this.isTransient = isTransient;
	}

	@JsonIgnore
	public Boolean isNull() {
		return isNull == null ? false : isNull;
	}

	@JsonIgnore
	public Boolean getIsNull() {
		return isNull;
	}
	
	@Override
	public String toString() {
		return "Metric [name=" + name + ", alias=" + alias + ", timestamp=" + timestamp + ", dataType=" + dataType
				+ ", isHistorical=" + isHistorical + ", isTransient=" + isTransient + ", isNull=" + isNull
				+ ", metaData=" + metaData + ", propertySet=" + properties + ", value=" + value + "]";
	}	
	
	/**
	 * A builder for creating a {@link Metric} instance.
	 */
	public static class MetricBuilder {

		private String name;
		private Long alias;
		private Date timestamp;
		private MetricDataType dataType;
		private Boolean isHistorical;
		private Boolean isTransient;
		private MetaData metaData = null;
		private PropertySet properties = null;
		private Object value;
		
		public MetricBuilder(String name, MetricDataType dataType, Object value) {
			this.name = name;
			this.timestamp = new Date();
			this.dataType = dataType;
			this.value = value;
		}
		
		public MetricBuilder(Long alias, MetricDataType dataType, Object value) {
			this.alias = alias;
			this.timestamp = new Date();
			this.dataType = dataType;
			this.value = value;
		}
		
		public MetricBuilder(Metric metric) throws Exception {
			this.name = metric.getName();
			this.alias = metric.getAlias();
			this.timestamp = metric.getTimestamp();
			this.dataType = metric.getDataType();
			this.isHistorical = metric.isHistorical();
			this.isTransient = metric.isTransient();
			this.metaData = metric.getMetaData() != null 
					? new MetaDataBuilder(metric.getMetaData()).createMetaData() : null;
			this.properties = metric.getMetaData() != null 
					? new PropertySet.PropertySetBuilder(metric.getProperties()).createPropertySet() : null;
			switch (dataType) {
				case DataSet:
					this.value = metric.getValue() != null 
							? new DataSetBuilder((DataSet) metric.getValue()).createDataSet()
							: null;
					break;
				case Template:
					this.value = metric.getValue() != null 
							? new Template.TemplateBuilder((Template) metric.getValue()).createTemplate()
							: null;
					break;
				default:
					this.value = metric.getValue();
			}
		}

		public MetricBuilder name(String name) {
			this.name = name;
			return this;
		}

		public MetricBuilder alias(Long alias) {
			this.alias = alias;
			return this;
		}

		public MetricBuilder timestamp(Date timestamp) {
			this.timestamp = timestamp;
			return this;
		}

		public MetricBuilder dataType(MetricDataType dataType) {
			this.dataType = dataType;
			return this;
		}

		public MetricBuilder isHistorical(Boolean isHistorical) {
			this.isHistorical = isHistorical;
			return this;
		}

		public MetricBuilder isTransient(Boolean isTransient) {
			this.isTransient = isTransient;
			return this;
		}

		public MetricBuilder metaData(MetaData metaData) {
			this.metaData = metaData;
			return this;
		}

		public MetricBuilder properties(PropertySet properties) {
			this.properties = properties;
			return this;
		}

		public MetricBuilder value(Object value) {
			this.value = value;
			return this;
		}
		
		public Metric createMetric() throws Exception {
			return new Metric(name, alias, timestamp, dataType, isHistorical, isTransient, metaData, 
					properties, value);
		}
	}
}
