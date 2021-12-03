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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        JsonArray polygonArray = JSON_PARSER.parse(polygonInString).getAsJsonArray();
        JsonArray arrayWithCoords = extractAllPolygonsCoordsToNewJson(polygonArray);
        List<Polygon> polygons = extractPolygonsFrom(arrayWithCoords);
        Map<Polygon, List<Polygon>> holes = getHolesForPolygons(polygons);
        excludeHolesFromPolygonsList(polygons, holes);

        return contains(polygons, holes, coordinates);
    }

    private static boolean contains(List<Polygon> polygons, Map<Polygon, List<Polygon>> holes, Coordinates coordinates) {
        for (Polygon polygon : polygons) {
            if (contains(polygon, coordinates)) {
                for (Polygon hole : holes.get(polygon)) {
                    if (contains(hole, coordinates)) {
                        return false;
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

    private static void extractAllPolygonsCoordsFromJson(JsonArray polygonArray, JsonArray polygons) {
        for (JsonElement element : polygonArray) {
            if (containsArrayWithPrimitives(element.getAsJsonArray())) {
                polygons.add(element);
            }
            else {
                extractAllPolygonsCoordsFromJson(element.getAsJsonArray(), polygons);
            }
        }
    }

    private static JsonArray extractAllPolygonsCoordsToNewJson(JsonArray polygonArray) {
        JsonArray result = new JsonArray();
        extractAllPolygonsCoordsFromJson(polygonArray, result);
        return result;
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

    private static List<Polygon> extractPolygonsFrom(JsonArray arrayWithCoords) {
        List<Polygon> polygons = new ArrayList<>();

        for (JsonElement coordinates : arrayWithCoords) {
            List<Coordinate> coordinatesList = parseCoordinates(coordinates.getAsJsonArray());
            Polygon polygon = buildPolygonFromCoordsList(coordinatesList);
            polygons.add(polygon);
        }
        return polygons;
    }

    private static Map<Polygon, List<Polygon>> getHolesForPolygons(List<Polygon> polygons) {
        Map<Polygon, List<Polygon>> holesForPolygons = new HashMap<>();

        for (Polygon polygon : polygons) {
            holesForPolygons.put(polygon, new ArrayList<>());
        }

        for (Polygon current : polygons) {
            List<Polygon> withoutCurrent = polygons.stream()
                    .filter(p -> !p.equalsExact(current))
                    .collect(Collectors.toList());

            for (Polygon another : withoutCurrent) {
                JtsGeometry currentGeo = jtsCtx.getShapeFactory().makeShape(current);
                JtsGeometry anotherGeo = jtsCtx.getShapeFactory().makeShape(another);

                boolean currentContainsAnother = currentGeo
                        .relate(anotherGeo)
                        .equals(SpatialRelation.CONTAINS);

                boolean anotherWithinCurrent = anotherGeo
                        .relate(currentGeo)
                        .equals(SpatialRelation.WITHIN);

                if (currentContainsAnother && anotherWithinCurrent) {
                    holesForPolygons.get(current).add(another);
                }
            }
        }

        for (Polygon polygon : polygons) {
            List<Polygon> holes = holesForPolygons.get(polygon);
            if (holes.isEmpty()) {
                holesForPolygons.remove(polygon);
            }
        }

        return holesForPolygons;
    }

    private static void excludeHolesFromPolygonsList(List<Polygon> polygons, Map<Polygon, List<Polygon>> holesForPolygons) {
        List<Polygon> polygonsToExclude = new ArrayList<>();

        for (Polygon polygon : polygons) {
            if (!holesForPolygons.containsKey(polygon)) {
                polygonsToExclude.add(polygon);
            }
        }

        polygons.removeAll(polygonsToExclude);
    }
}