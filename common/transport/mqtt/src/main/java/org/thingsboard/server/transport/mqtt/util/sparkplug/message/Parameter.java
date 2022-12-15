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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.Objects;

/**
 * A class to represent a parameter associated with a template.
 */
public class Parameter {
	
	/**
	 * The name of the parameter
	 */
	@JsonProperty("name")
	private String name;
	
	/**
	 * The data type of the parameter
	 */
	@JsonProperty("type")
	private ParameterDataType type;
	
	/**
	 * The value of the parameter
	 */
	@JsonProperty("value")
	private Object value;
	
	
	/**
	 * Constructs a Parameter instance.
	 * 
	 * @param name The name of the parameter.
	 * @param type The type of the parameter.
	 * @param value The value of the parameter.
	 * @throws Exception
	 */
	public Parameter(String name, ParameterDataType type, Object value) throws Exception {
		this.name = name;
		this.type = type;
		this.value = value;
		this.type.checkType(value);
	}

	@JsonGetter("name")
	public String getName() {
		return name;
	}

	@JsonSetter("name")
	public void setName(String name) {
		this.name = name;
	}

	public ParameterDataType getType() {
		return type;
	}

	public void setType(ParameterDataType type) {
		this.type = type;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}
	
	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (object == null || this.getClass() != object.getClass()) {
			return false;
		}
		Parameter param = (Parameter) object;
		return Objects.equals(name, param.getName())
				&& Objects.equals(type, param.getType())
				&& Objects.equals(value, param.getValue());
	}

	@Override
	public String toString() {
		return "Parameter [name=" + name + ", type=" + type + ", value=" + value + "]";
	}
}
