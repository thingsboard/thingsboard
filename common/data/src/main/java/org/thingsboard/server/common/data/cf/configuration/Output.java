/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.common.data.cf.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.server.common.data.AttributeScope;

import java.util.Objects;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TimeSeriesOutput.class, name = "TIME_SERIES"),
        @JsonSubTypes.Type(value = AttributesOutput.class, name = "ATTRIBUTES")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface Output {

    @JsonIgnore
    OutputType getType();

    String getName();

    OutputStrategy getStrategy();

    default AttributeScope getScope() {
        return null;
    }

    Integer getDecimalsByDefault();

    void setDecimalsByDefault(Integer decimalsByDefault);

    default boolean hasContextOnlyChanges(Output other) {
        if (!getType().equals(other.getType())) {
            return true;
        }
        if (!Objects.equals(getName(), other.getName())) {
            return true;
        }
        if (getScope() != (other.getScope())) {
            return true;
        }
        if (!Objects.equals(getDecimalsByDefault(), other.getDecimalsByDefault())) {
            return true;
        }
        if (getStrategy().hasContextOnlyChanges(other.getStrategy())) {
            return true;
        }
        return false;
    }

}
