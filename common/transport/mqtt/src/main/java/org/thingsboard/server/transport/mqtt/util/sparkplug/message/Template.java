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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A class representing a template associated with a metric
 */
@JsonInclude(Include.NON_NULL)
public class Template {

	/**
	 * The Template version.
	 */
	@JsonProperty("version")
	private String version;
	
	/**
	 * The template reference
	 */
	@JsonProperty("reference")
	private String templateRef;
	
	/**
	 * True if the template is a definition, false otherwise.
	 */
	@JsonProperty("isDefinition")
	private boolean isDefinition;
	
	/**
	 * List of metrics.
	 */
	@JsonProperty("metrics")
	private List<Metric> metrics;
	
	/**
	 * List of parameters.
	 */
	@JsonProperty("parameters")
	@JsonInclude(Include.NON_EMPTY)
	private List<Parameter> parameters;
	
	public Template() {}
	
	public Template(String version, String templateRef, boolean isDefinition, List<Metric> metrics,
                    List<Parameter> parameters) {
		this.version = version;
		this.templateRef = templateRef;
		this.isDefinition = isDefinition;
		this.metrics = metrics;
		this.parameters = parameters;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getTemplateRef() {
		return templateRef;
	}

	public void setTemplateRef(String templateRef) {
		this.templateRef = templateRef;
	}

	@JsonGetter("isDefinition")
	public boolean isDefinition() {
		return isDefinition;
	}

	@JsonSetter("isDefinition")
	public void setDefinition(boolean isDefinition) {
		this.isDefinition = isDefinition;
	}

	public List<Metric> getMetrics() {
		return metrics;
	}

	public void setMetrics(List<Metric> metrics) {
		this.metrics = metrics;
	}
	
	public void addMetric(Metric metric) {
		this.metrics.add(metric);
	}

	public List<Parameter> getParameters() {
		return parameters;
	}

	public void setParameters(List<Parameter> parameters) {
		this.parameters = parameters;
	}
	
	public void addParameter(Parameter parameter) {
		this.parameters.add(parameter);
	}
	
	@Override
	public String toString() {
		return "Template [version=" + version + ", templateRef=" + templateRef + ", isDefinition=" + isDefinition
				+ ", metrics=" + metrics + ", parameters=" + parameters + "]";
	}

	/**
	 * A builder for creating a {@link Template} instance.
	 */
	public static class TemplateBuilder {

		private String version;
		private String templateRef;
		private boolean isDefinition;
		private List<Metric> metrics;
		private List<Parameter> parameters;
		
		public TemplateBuilder() {
			super();
			this.metrics = new ArrayList<Metric>();
			this.parameters = new ArrayList<Parameter>();
		}
		
		public TemplateBuilder(Template template) throws Exception {
			this.version = template.getVersion();
			this.templateRef = template.getTemplateRef();
			this.isDefinition = template.isDefinition();
			this.metrics = new ArrayList<Metric>(template.getMetrics().size());
			for (Metric metric : template.getMetrics()) {
				this.metrics.add(new Metric.MetricBuilder(metric).createMetric());
			}
			this.parameters = new ArrayList<Parameter>(template.getParameters().size());
			for (Parameter parameter : template.getParameters()) {
				this.parameters.add(new Parameter(parameter.getName(), parameter.getType(), parameter.getValue()));
			}
		}

		public TemplateBuilder version(String version) {
			this.version = version;
			return this;
		}

		public TemplateBuilder templateRef(String templateRef) {
			this.templateRef = templateRef;
			return this;
		}

		public TemplateBuilder definition(boolean isDefinition) {
			this.isDefinition = isDefinition;
			return this;
		}

		public TemplateBuilder addMetric(Metric metric) {
			this.metrics.add(metric);
			return this;
		}

		public TemplateBuilder addMetrics(Collection<Metric> metrics) {
			this.metrics.addAll(metrics);
			return this;
		}

		public TemplateBuilder addParameter(Parameter parameter) {
			this.parameters.add(parameter);
			return this;
		}

		public TemplateBuilder addParameters(Collection<Parameter> parameters) {
			this.parameters.addAll(parameters);
			return this;
		}
		
		public Template createTemplate() {
			return new Template(version, templateRef, isDefinition, metrics, parameters);
		}
	}
}
