// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.common.util.geo;

import lombok.Data;

@Data
public class PolygonPerimeterDefinition implements PerimeterDefinition {

    private final String polygonDefinition;

    @Override
    public PerimeterType getType() {
        return PerimeterType.POLYGON;
    }

    @Override
    public boolean checkMatches(Coordinates entityCoordinates) {
        return GeoUtil.contains(polygonDefinition, entityCoordinates);
    }

}
