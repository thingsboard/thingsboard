// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.common.util.geo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = PerimeterDefinitionDeserializer.class)
@JsonSerialize(using = PerimeterDefinitionSerializer.class)
public interface PerimeterDefinition extends Serializable {

    @JsonIgnore
    PerimeterType getType();

    @JsonIgnore
    boolean checkMatches(Coordinates entityCoordinates);
}
