// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.common.util.geo;

import lombok.Data;

@Data
public class CirclePerimeterDefinition implements PerimeterDefinition {

    private final Double latitude;
    private final Double longitude;
    private final Double radius;

    @Override
    public PerimeterType getType() {
        return PerimeterType.CIRCLE;
    }

    @Override
    public boolean checkMatches(Coordinates entityCoordinates) {
        Coordinates perimeterCoordinates = new Coordinates(latitude, longitude);
        return radius > GeoUtil.distance(entityCoordinates, perimeterCoordinates, RangeUnit.METER);
    }

}
