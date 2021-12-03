/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.rule.engine.geo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.context.jts.JtsSpatialContextFactory;
import org.locationtech.spatial4j.distance.DistanceUtils;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.SpatialRelation;
import org.locationtech.spatial4j.shape.impl.PointImpl;
import org.locationtech.spatial4j.shape.jts.JtsGeometry;

import java.util.ArrayList;
import java.util.List;

public class GeoUtil {

    private static final SpatialContext distCtx = SpatialContext.GEO;
    private static final JtsSpatialContext jtsCtx;

    private static final JsonParser JSON_PARSER = new JsonParser();

    static {
        JtsSpatialContextFactory factory = new JtsSpatialContextFactory();
        factory.normWrapLongitude = true;
        jtsCtx = factory.newSpatialContext();
    }

    public static synchronized double distance(Coordinates x, Coordinates y, RangeUnit unit) {
        Point xLL = distCtx.getShapeFactory().pointXY(x.getLongitude(), x.getLatitude());
        Point yLL = distCtx.getShapeFactory().pointXY(y.getLongitude(), y.getLatitude());
        return unit.fromKm(distCtx.getDistCalc().distance(xLL, yLL) * DistanceUtils.DEG_TO_KM);
    }

    public static synchronized boolean contains(String polygonInString, Coordinates coordinates) {
        JsonArray polygonArray = JSON_PARSER.parse(polygonInString).getAsJsonArray();

        JsonArray arrayWithCoords = polygonArray;
        JsonArray innerArray = polygonArray.get(0).getAsJsonArray();
        if (!containsPrimitives(innerArray)) {
            arrayWithCoords = innerArray;
        }

        List<Coordinate> coordinateList = parseCoordinates(arrayWithCoords);
        Polygon polygon = buildPolygonFromCoordsList(coordinateList);

        JtsGeometry geometry = jtsCtx.getShapeFactory().makeShape(polygon);
        Point point = new PointImpl(coordinates.getLatitude(), coordinates.getLongitude(), jtsCtx);

        return geometry.relate(point).equals(SpatialRelation.CONTAINS);
    }

    private static List<Coordinate> parseCoordinates(JsonArray coordinates) {
        List<Coordinate> allCoords = new ArrayList<>();

        for (JsonElement coords : coordinates) {
            double x = coords.getAsJsonArray().get(0).getAsDouble();
            double y = coords.getAsJsonArray().get(1).getAsDouble();

            allCoords.add(new Coordinate(x, y));
        }

        allCoords.add(allCoords.get(0));

        return allCoords;
    }

    private static Polygon buildPolygonFromCoordsList(List<Coordinate> coordinates) {
        CoordinateSequence coordinateSequence = jtsCtx
                .getShapeFactory()
                .getGeometryFactory()
                .getCoordinateSequenceFactory()
                .create(coordinates.toArray(new Coordinate[0]));

        return jtsCtx.getShapeFactory().getGeometryFactory().createPolygon(coordinateSequence);
    }

    private static boolean containsPrimitives(JsonArray array) {
        for (JsonElement element : array) {
            return element.isJsonPrimitive();
        }
        return false;
    }
}