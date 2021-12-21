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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        JsonArray polygonsJson = normalizePolygonsJson(JSON_PARSER.parse(polygonInString).getAsJsonArray());
        List<Polygon> polygons = buildPolygonsFromJson(polygonsJson);

        Map<Polygon, List<Polygon>> polygonsHoles = getPolygonsHoles(polygons);

        Set<Polygon> allHoles = polygonsHoles.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        polygons.removeIf(allHoles::contains);

        return contains(polygons, polygonsHoles, coordinates);
    }

    private static boolean contains(List<Polygon> polygons, Map<Polygon, List<Polygon>> holes, Coordinates coordinates) {
        for (Polygon polygon : polygons) {
            if (contains(polygon, coordinates)) {
                if (!holes.isEmpty()) {
                    for (Polygon hole : holes.get(polygon)) {
                        if (contains(hole, coordinates)) {
                            return false;
                        }
                    }
                }

                return true;
            }
        }

        return false;
    }

    private static boolean contains(Polygon polygon, Coordinates coordinates) {
        JtsGeometry geometry = jtsCtx.getShapeFactory().makeShape(polygon);
        Point point = new PointImpl(coordinates.getLatitude(), coordinates.getLongitude(), jtsCtx);

        return geometry.relate(point).equals(SpatialRelation.CONTAINS);
    }

    private static JsonArray normalizePolygonsJson(JsonArray polygonsJsonArray) {
        JsonArray result = new JsonArray();
        normalizePolygonsJson(polygonsJsonArray, result);
        return result;
    }

    private static void normalizePolygonsJson(JsonArray polygonsJsonArray, JsonArray result) {
        if (containsArrayWithPrimitives(polygonsJsonArray)) {
            result.add(polygonsJsonArray);
        } else {
            for (JsonElement element : polygonsJsonArray) {
                if (containsArrayWithPrimitives(element.getAsJsonArray())) {
                    result.add(element);
                } else {
                    normalizePolygonsJson(element.getAsJsonArray(), result);
                }
            }
        }
    }

    private static Map<Polygon, List<Polygon>> getPolygonsHoles(List<Polygon> polygons) {
        Map<Polygon, List<Polygon>> polygonsHoles = new HashMap<>();

        for (Polygon polygon : polygons) {
            List<Polygon> holes = polygons.stream()
                    .filter(another -> !another.equalsExact(polygon))
                    .filter(another -> {
                        JtsGeometry currentGeo = jtsCtx.getShapeFactory().makeShape(polygon);
                        JtsGeometry anotherGeo = jtsCtx.getShapeFactory().makeShape(another);

                        boolean currentContainsAnother = currentGeo
                                .relate(anotherGeo)
                                .equals(SpatialRelation.CONTAINS);

                        boolean anotherWithinCurrent = anotherGeo
                                .relate(currentGeo)
                                .equals(SpatialRelation.WITHIN);

                        return currentContainsAnother && anotherWithinCurrent;
                    })
                    .collect(Collectors.toList());

            if (!holes.isEmpty()) {
                polygonsHoles.put(polygon, holes);
            }
        }

        return polygonsHoles;
    }

    private static List<Polygon> buildPolygonsFromJson(JsonArray polygonsJsonArray) {
        List<Polygon> polygons = new LinkedList<>();

        for (JsonElement polygonJsonArray : polygonsJsonArray) {
            Polygon polygon = buildPolygonFromCoordinates(parseCoordinates(polygonJsonArray.getAsJsonArray()));
            polygons.add(polygon);
        }

        return polygons;
    }

    private static Polygon buildPolygonFromCoordinates(List<Coordinate> coordinates) {
        if (coordinates.size() == 2) {
            Coordinate a = coordinates.get(0);
            Coordinate c = coordinates.get(1);
            coordinates.clear();

            Coordinate b = new Coordinate(a.x, c.y);
            Coordinate d = new Coordinate(c.x, a.y);
            coordinates.addAll(List.of(a, b, c, d, a));
        }

        CoordinateSequence coordinateSequence = jtsCtx
                .getShapeFactory()
                .getGeometryFactory()
                .getCoordinateSequenceFactory()
                .create(coordinates.toArray(new Coordinate[0]));

        return jtsCtx.getShapeFactory().getGeometryFactory().createPolygon(coordinateSequence);
    }

    private static List<Coordinate> parseCoordinates(JsonArray coordinatesJson) {
        List<Coordinate> result = new ArrayList<>();

        for (JsonElement coords : coordinatesJson) {
            double x = coords.getAsJsonArray().get(0).getAsDouble();
            double y = coords.getAsJsonArray().get(1).getAsDouble();
            result.add(new Coordinate(x, y));
        }

        if (result.size() >= 3) {
            result.add(result.get(0));
        }

        return result;
    }

    private static boolean containsPrimitives(JsonArray array) {
        for (JsonElement element : array) {
            return element.isJsonPrimitive();
        }

        return false;
    }

    private static boolean containsArrayWithPrimitives(JsonArray array) {
        for (JsonElement element : array) {
            if (!containsPrimitives(element.getAsJsonArray())) {
                return false;
            }
        }

        return true;
    }

}
