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

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;

/**
 * The value of a property in a {@link PropertySet}.
 */
public class PropertyValue {
	
	private PropertyDataType type;
	private Object value;
	private Boolean isNull = null;
	
	public PropertyValue() {}
	
	/**
	 * A constructor.
	 * 
	 * @param type the property type
	 * @param value the property value
	 * @throws Exception
	 */
	public PropertyValue(PropertyDataType type, Object value) throws Exception {
		this.type = type;
		this.value = value;
		isNull = (value == null) ? true : false;
		type.checkType(value);
	}

	public PropertyDataType getType() {
		return type;
	}

	public void setType(PropertyDataType type) {
		this.type = type;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
		isNull = (value == null) ? true : false;
	}
	
	@JsonIgnore
	public Boolean isNull() {
		return isNull;
	}
	
	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (object == null || this.getClass() != object.getClass()) {
			return false;
		}
		PropertyValue propValue = (PropertyValue) object;
		return Objects.equals(type, propValue.getType())
				&& Objects.equals(value, propValue.getValue());
	}

	@Override
	public String toString() {
		return "PropertyValue [type=" + type + ", value=" + value + ", isNull=" + isNull + "]";
	}
}
