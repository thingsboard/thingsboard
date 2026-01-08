/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
