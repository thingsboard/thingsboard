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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A class that maintains a set of properties associated with a {@link Metric}.
 */
public class PropertySet implements Map<String, PropertyValue> {
	
	@JsonIgnore
	private Map<String, PropertyValue> map;
	
	public PropertySet() {
		this.map = new HashMap<String, PropertyValue>();
	}
	
	private PropertySet(Map<String, PropertyValue> propertyMap) {
		this.map = propertyMap;
	}
	
	@JsonIgnore
	public PropertyValue getPropertyValue(String name) {
		return this.map.get(name);
	}
	
	@JsonIgnore
	public void setProperty(String name, PropertyValue value) {
		this.map.put(name, value);
	}
	
	@JsonIgnore
	public void removeProperty(String name) {
		this.map.remove(name);
	}
	
	@JsonIgnore
	public void clear() {
		this.map.clear();
	}
	
	@JsonIgnore
	public Set<String> getNames() {
		return map.keySet();
	}
	
	@JsonIgnore
	public Collection<PropertyValue> getValues() {
		return map.values();
	}

	@JsonIgnore
	public Map<String, PropertyValue> getPropertyMap() {
		return map;
	}
	
	@Override
	public String toString() {
		return "PropertySet [propertyMap=" + map + "]";
	}
	
	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	@Override
	public PropertyValue get(Object key) {
		return map.get(key);
	}

	@Override
	public PropertyValue put(String key, PropertyValue value) {
		return map.put(key, value);
	}

	@Override
	public PropertyValue remove(Object key) {
		return map.remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ? extends PropertyValue> m) {
		map.putAll(m);
	}

	@Override
	public Set<String> keySet() {
		return map.keySet();
	}

	@Override
	public Collection<PropertyValue> values() {
		return map.values();
	}

	@Override
	public Set<Entry<String, PropertyValue>> entrySet() {
		return map.entrySet();
	}

	/**
	 * A builder for a PropertySet instance
	 */
	public static class PropertySetBuilder {
		
		private Map<String, PropertyValue> propertyMap;
		
		public PropertySetBuilder() {
			this.propertyMap = new HashMap<String, PropertyValue>();
		}
		
		public PropertySetBuilder(PropertySet propertySet) throws Exception {
			this.propertyMap = new HashMap<String, PropertyValue>();
			for (String name : propertySet.getNames()) {
				PropertyValue value = propertySet.getPropertyValue(name);
				propertyMap.put(name, new PropertyValue(value.getType(), value.getValue()));
			}
		}
		
		public PropertySetBuilder addProperty(String name, PropertyValue value) {
			this.propertyMap.put(name, value);
			return this;
		}
		
		public PropertySetBuilder addProperties(Map<String, PropertyValue> properties) {
			this.propertyMap.putAll(properties);
			return this;
		}
		
		public PropertySet createPropertySet() {
			return new PropertySet(propertyMap);
		}
	}
}
