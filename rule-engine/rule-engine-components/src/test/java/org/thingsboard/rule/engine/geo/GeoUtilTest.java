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
package org.thingsboard.rule.engine.geo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.geo.Coordinates;
import org.thingsboard.common.util.geo.GeoUtil;

@ExtendWith(MockitoExtension.class)
public class GeoUtilTest {

    public static final String SIMPLE_RECT = "[[51.903762928405555,23.642220786948297],[44.669801219635644,41.83345155830211]]";
    public static final String SIMPLE_RECT_WITH_HOLE_IN_CENTER = "[[[44.66980121963565,23.642220786948297],[44.66980121963565,41.83345155830211],[51.903762928405555,41.83345155830211],[51.903762928405555,23.642220786948297]],[[46.10464044504632,26.234282119122227],[50.8755868028522,26.25625220459488],[51.04164771375101,38.5595000692786],[45.99790855491869,38.75723083853248]]]";
    public static final String SAND_CLOCK = "[[47.45865912532852,25.531200822337155],[49.76760268310416,29.353995694578202],[51.42691963936519,25.355440138555966],[51.413219087617655,39.41629484105169],[49.78179072754938,33.37452133607305],[47.81395478511215,38.867042704235466]]";
    public static final String SAND_CLOCK_WITH_HOLE_IN_CENTER = "[[[51.426919639365195,25.355440138555966],[49.76760268310416,29.353995694578202],[47.45865912532852,25.531200822337155],[47.81395478511215,38.867042704235466],[49.78179072754938,33.37452133607305],[51.413219087617655,39.41629484105169]],[[49.8243299406579,30.210829028011513],[50.34591034217041,31.04569227597222],[49.56853374046142,32.53965808811239],[49.02409241675058,31.57297432731579]]]";
    public static final String SELF_INTERSECTING = "[[47.42893833699058,27.178954662008522],[51.71367987390804,37.46095466320854],[51.659197648757306,27.947907653551276],[47.41407351681856,37.46095466320854]]";
    public static final String SELF_INTERSECTING_WITH_HOLES = "[[[[47.42893833699058,27.17895466200852],[47.41407351681856,37.46095466320853],[49.63741575793274,32.47858934221699]],[[47.84342032696093,29.20045703244998],[49.124803688667576,32.38611942598416],[47.858163521459254,34.714948486085035]]],[[[51.659197648757306,27.947907653551276],[49.63741575793274,32.47858934221699],[51.71367987390804,37.46095466320853]],[[51.20718653775245,30.738363015535448],[51.317169097567344,34.58312797324915],[50.1351109330126,32.51793993882006]]]]";

    public static final Coordinates POINT_INSIDE_SIMPLE_RECT_CENTER = new Coordinates(48.37082198780869, 32.673342414527355);
    public static final Coordinates POINT_INSIDE_SIMPLE_RECT_NEAR_BORDER = new Coordinates(48.42916753187315, 40.956064637716224);
    public static final Coordinates POINT_OUTSIDE_SIMPLE_RECT = new Coordinates(52.94806646045028, 32.91501335472649);
    public static final Coordinates POINT_INSIDE_SAND_CLOCK_CENTER = new Coordinates(49.993588800145105, 31.289062500000004);
    public static final Coordinates POINT_INSIDE_SAND_CLOCK_NEAR_BORDER = new Coordinates(47.798651123976306, 26.895045405470082);
    public static final Coordinates POINT_OUTSIDE_SAND_CLOCK_1 = new Coordinates(49.553754212665936, 28.03748985004787);
    public static final Coordinates POINT_OUTSIDE_SAND_CLOCK_2 = new Coordinates(46.9802466961145, 32.321728498980335);
    public static final Coordinates POINT_INSIDE_SELF_INTERSECTING_UPPER_CENTER = new Coordinates(50.750366308834884, 32.51952867922265);
    public static final Coordinates POINT_INSIDE_SELF_INTERSECTING_LOWER_CENTER = new Coordinates(48.1371117277312, 32.40967825185941);
    public static final Coordinates POINT_INSIDE_SELF_INTERSECTING_NEAR_BORDER = new Coordinates(51.16552151942722, 35.66125090181154);
    public static final Coordinates POINT_OUTSIDE_SELF_INTERSECTING_1 = new Coordinates(49.66777277299077, 33.26651158529272);
    public static final Coordinates POINT_OUTSIDE_SELF_INTERSECTING_2 = new Coordinates(47.10052840114779, 32.16800731166027);
    public static final Coordinates POINT_OUTSIDE_SELF_INTERSECTING_3 = new Coordinates(78.76578380252519, 15.646485040786361);


    @Test
    public void testPointsInSimplePolygons() {
        Assertions.assertTrue(GeoUtil.contains(SIMPLE_RECT, POINT_INSIDE_SIMPLE_RECT_CENTER),
                "Polygon " + SIMPLE_RECT + " must contain the dot " + POINT_INSIDE_SIMPLE_RECT_CENTER
                );
        Assertions.assertTrue(GeoUtil.contains(SIMPLE_RECT, POINT_INSIDE_SIMPLE_RECT_NEAR_BORDER),
                "Polygon " + SIMPLE_RECT + " must contain the dot " + POINT_INSIDE_SIMPLE_RECT_NEAR_BORDER
                );
        Assertions.assertTrue(GeoUtil.contains(SIMPLE_RECT_WITH_HOLE_IN_CENTER, POINT_INSIDE_SIMPLE_RECT_NEAR_BORDER),
                "Polygon " + SIMPLE_RECT_WITH_HOLE_IN_CENTER + " must contain the dot "
                                + POINT_INSIDE_SIMPLE_RECT_NEAR_BORDER
                );

        Assertions.assertFalse(GeoUtil.contains(SIMPLE_RECT, POINT_OUTSIDE_SIMPLE_RECT),
                "Polygon " + SIMPLE_RECT + " must not contain the dot "
                                + POINT_OUTSIDE_SIMPLE_RECT
                );
        Assertions.assertFalse(GeoUtil.contains(SIMPLE_RECT_WITH_HOLE_IN_CENTER, POINT_OUTSIDE_SIMPLE_RECT),
                "Polygon " + SIMPLE_RECT_WITH_HOLE_IN_CENTER + " must not contain the dot "
                                + POINT_OUTSIDE_SIMPLE_RECT
                );
        Assertions.assertFalse(GeoUtil.contains(SIMPLE_RECT_WITH_HOLE_IN_CENTER, POINT_INSIDE_SIMPLE_RECT_CENTER),
                "Polygon " + SIMPLE_RECT_WITH_HOLE_IN_CENTER + " must not contain the dot "
                                + POINT_INSIDE_SIMPLE_RECT_CENTER
                );
    }

    @Test
    public void testPointsInComplexPolygons() {
        Assertions.assertTrue(GeoUtil.contains(SAND_CLOCK, POINT_INSIDE_SAND_CLOCK_CENTER),
                "Polygon " + SAND_CLOCK + " must contain the dot " + POINT_INSIDE_SAND_CLOCK_CENTER
                );
        Assertions.assertTrue(GeoUtil.contains(SAND_CLOCK, POINT_INSIDE_SAND_CLOCK_NEAR_BORDER),
                "Polygon " + SAND_CLOCK + " must contain the dot " + POINT_INSIDE_SAND_CLOCK_NEAR_BORDER
                );
        Assertions.assertTrue(GeoUtil.contains(SAND_CLOCK_WITH_HOLE_IN_CENTER, POINT_INSIDE_SAND_CLOCK_NEAR_BORDER),
                "Polygon " + SAND_CLOCK_WITH_HOLE_IN_CENTER + " must contain the dot "
                                + POINT_INSIDE_SAND_CLOCK_NEAR_BORDER
                );

        Assertions.assertFalse(GeoUtil.contains(SAND_CLOCK, POINT_OUTSIDE_SAND_CLOCK_1),
                "Polygon " + SAND_CLOCK + " must not contain the dot "
                                + POINT_OUTSIDE_SAND_CLOCK_1
                );
        Assertions.assertFalse(GeoUtil.contains(SAND_CLOCK, POINT_OUTSIDE_SAND_CLOCK_2),
                "Polygon " + SAND_CLOCK + " must not contain the dot "
                                + POINT_OUTSIDE_SAND_CLOCK_2
                );
        Assertions.assertFalse(GeoUtil.contains(SAND_CLOCK_WITH_HOLE_IN_CENTER, POINT_INSIDE_SAND_CLOCK_CENTER),
                "Polygon " + SAND_CLOCK_WITH_HOLE_IN_CENTER + " must not contain the dot "
                                + POINT_INSIDE_SAND_CLOCK_CENTER
                );
        Assertions.assertFalse(GeoUtil.contains(SAND_CLOCK_WITH_HOLE_IN_CENTER, POINT_OUTSIDE_SAND_CLOCK_1),
                "Polygon " + SAND_CLOCK_WITH_HOLE_IN_CENTER + " must not contain the dot "
                                + POINT_OUTSIDE_SAND_CLOCK_1
                );
        Assertions.assertFalse(GeoUtil.contains(SAND_CLOCK_WITH_HOLE_IN_CENTER, POINT_OUTSIDE_SAND_CLOCK_2),
                "Polygon " + SAND_CLOCK_WITH_HOLE_IN_CENTER + " must not contain the dot "
                                + POINT_OUTSIDE_SAND_CLOCK_2
                );
    }

    @Test
    public void testPointsInSelfIntersectingPolygons() {
        Assertions.assertTrue(GeoUtil.contains(SELF_INTERSECTING, POINT_INSIDE_SELF_INTERSECTING_UPPER_CENTER),
                "Polygon " + SELF_INTERSECTING + " must contain the dot "
                                + POINT_INSIDE_SELF_INTERSECTING_UPPER_CENTER
                );
        Assertions.assertTrue(GeoUtil.contains(SELF_INTERSECTING, POINT_INSIDE_SELF_INTERSECTING_LOWER_CENTER),
                "Polygon " + SELF_INTERSECTING + " must contain the dot "
                                + POINT_INSIDE_SELF_INTERSECTING_LOWER_CENTER
                );
        Assertions.assertTrue(GeoUtil.contains(SELF_INTERSECTING, POINT_INSIDE_SELF_INTERSECTING_NEAR_BORDER),
                "Polygon " + SELF_INTERSECTING + " must contain the dot "
                                + POINT_INSIDE_SELF_INTERSECTING_NEAR_BORDER
                );
        Assertions.assertTrue(GeoUtil.contains(SELF_INTERSECTING_WITH_HOLES, POINT_INSIDE_SELF_INTERSECTING_NEAR_BORDER),
                "Polygon " + SELF_INTERSECTING_WITH_HOLES + " must contain the dot "
                                + POINT_INSIDE_SAND_CLOCK_NEAR_BORDER
                );

        Assertions.assertFalse(GeoUtil.contains(SELF_INTERSECTING, POINT_OUTSIDE_SELF_INTERSECTING_1),
                "Polygon " + SELF_INTERSECTING + " must not contain the dot "
                                + POINT_OUTSIDE_SELF_INTERSECTING_1
                );
        Assertions.assertFalse(GeoUtil.contains(SELF_INTERSECTING, POINT_OUTSIDE_SELF_INTERSECTING_2),
                "Polygon " + SELF_INTERSECTING + " must not contain the dot "
                                + POINT_OUTSIDE_SELF_INTERSECTING_2
                );
        Assertions.assertFalse(GeoUtil.contains(SELF_INTERSECTING, POINT_OUTSIDE_SELF_INTERSECTING_3),
                "Polygon " + SELF_INTERSECTING + " must not contain the dot "
                                + POINT_OUTSIDE_SELF_INTERSECTING_3
                );
        Assertions.assertFalse(GeoUtil.contains(SELF_INTERSECTING_WITH_HOLES, POINT_OUTSIDE_SELF_INTERSECTING_1),
                "Polygon " + SELF_INTERSECTING_WITH_HOLES + " must not contain the dot "
                                + POINT_OUTSIDE_SELF_INTERSECTING_1
                );
        Assertions.assertFalse(GeoUtil.contains(SELF_INTERSECTING_WITH_HOLES, POINT_OUTSIDE_SELF_INTERSECTING_2),
                "Polygon " + SELF_INTERSECTING_WITH_HOLES + " must not contain the dot "
                                + POINT_OUTSIDE_SELF_INTERSECTING_2
                );
        Assertions.assertFalse(GeoUtil.contains(SELF_INTERSECTING_WITH_HOLES, POINT_OUTSIDE_SELF_INTERSECTING_3),
                "Polygon " + SELF_INTERSECTING_WITH_HOLES + " must not contain the dot "
                                + POINT_OUTSIDE_SELF_INTERSECTING_3
                );
        Assertions.assertFalse(GeoUtil.contains(SELF_INTERSECTING_WITH_HOLES, POINT_INSIDE_SELF_INTERSECTING_UPPER_CENTER),
                "Polygon " + SELF_INTERSECTING_WITH_HOLES + " must not contain the dot "
                                + POINT_INSIDE_SELF_INTERSECTING_UPPER_CENTER
                );
        Assertions.assertFalse(GeoUtil.contains(SELF_INTERSECTING_WITH_HOLES, POINT_INSIDE_SELF_INTERSECTING_LOWER_CENTER),
                "Polygon " + SELF_INTERSECTING_WITH_HOLES + " must not contain the dot "
                                + POINT_INSIDE_SELF_INTERSECTING_LOWER_CENTER
                );
    }

}
