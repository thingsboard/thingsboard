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
package org.thingsboard.server.transport.lwm2m.attributes;

import org.eclipse.leshan.core.link.LinkParseException;
import org.eclipse.leshan.core.link.attributes.InvalidAttributeException;
import org.eclipse.leshan.core.link.lwm2m.attributes.DefaultLwM2mAttributeParser;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttribute;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DefaultLwM2mAttributeParserTest {

    private final DefaultLwM2mAttributeParser parser = new DefaultLwM2mAttributeParser();

    private static Stream<Arguments> validAttributes() throws InvalidAttributeException {
        return Stream.of(//
                Arguments.of("pmin", LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD)), //
                Arguments.of("pmin=30", LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 30l)), //
                Arguments.of("pmax", LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD)), //
                Arguments.of("pmax=60", LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD, 60l)), //
                Arguments.of("dim=2", LwM2mAttributes.create(LwM2mAttributes.DIMENSION, 2l)), //
                Arguments.of("epmin", LwM2mAttributes.create(LwM2mAttributes.EVALUATE_MINIMUM_PERIOD)), //
                Arguments.of("epmin=30", LwM2mAttributes.create(LwM2mAttributes.EVALUATE_MINIMUM_PERIOD, 30l)), //
                Arguments.of("epmax", LwM2mAttributes.create(LwM2mAttributes.EVALUATE_MAXIMUM_PERIOD)), //
                Arguments.of("epmax=60", LwM2mAttributes.create(LwM2mAttributes.EVALUATE_MAXIMUM_PERIOD, 60l)), //
                Arguments.of("lt", LwM2mAttributes.create(LwM2mAttributes.LESSER_THAN)), //
                Arguments.of("lt=30", LwM2mAttributes.create(LwM2mAttributes.LESSER_THAN, 30d)), //
                Arguments.of("lt=-30", LwM2mAttributes.create(LwM2mAttributes.LESSER_THAN, -30d)), //
                Arguments.of("lt=30.55", LwM2mAttributes.create(LwM2mAttributes.LESSER_THAN, 30.55)), //
                Arguments.of("lt=-30.55", LwM2mAttributes.create(LwM2mAttributes.LESSER_THAN, -30.55)), //
                Arguments.of("gt", LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN)), //
                Arguments.of("gt=60", LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN, 60d)), //
                Arguments.of("gt=-60", LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN, -60d)), //
                Arguments.of("gt=60.55", LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN, 60.55)), //
                Arguments.of("gt=-60.55", LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN, -60.55)), //
                Arguments.of("st", LwM2mAttributes.create(LwM2mAttributes.STEP)), //
                Arguments.of("st=60", LwM2mAttributes.create(LwM2mAttributes.STEP, 60d)), //
                Arguments.of("st=60.55", LwM2mAttributes.create(LwM2mAttributes.STEP, 60.55)) //
        );
    }

    @ParameterizedTest(name = "Tests {index} : {0}")
    @MethodSource("validAttributes")
    public void check_one_valid_attribute(String attributeAsString, LwM2mAttribute<?> expectedValue)
            throws LinkParseException, InvalidAttributeException {
        LwM2mAttributeSet parsed = new LwM2mAttributeSet(parser.parseUriQuery(attributeAsString));
        LwM2mAttributeSet attResult = new LwM2mAttributeSet(expectedValue);
        assertEquals(parsed, attResult);
    }

    private static String[] invalidAttributes() {
        return new String[] { //
                // MINIMUM_PERIOD
                "pmin=", //
                "pmin=1.9", //
                "pmin=-1.8", //
                "pmin=a", //
                // MAXIMUM_PERIOD
                "pmax=", //
                "pmax=-2", //
                "pmax=2.7", //
                "pmax=-2.6", //
                "pmax=bc", //
                // DIMENSION
                "dim", //
                "dim=", //
                "dim=-3", //
                "dim=3.5", //
                "dim=-3.4", //
                "dim=def", //
                // EVALUATE_MINIMUM_PERIOD
                "epmin=", //
                "epmin=-4", //
                "epmin=4.3", //
                "epmin=-4.2", //
                "epmin=ghij", //
                // EVALUATE_MAXIMUM_PERIOD
                "epmax=", //
                "epmax=-5", //
                "epmax=5.1", //
                "epmax=-5.9", //
                "epmax=klmno", //
                // LESSER_THAN
                "lt=", //
                "lt=pqrts", //
                "lt=0abc", //
                // GREATER_THAN
                "gt=", //
                "gt=uvwxyz", //
                "gt=0.xyz", //
                // STEP
                "st=", //
                "st=-6", //
                "st=-6.8", //
                // Multi-attributes
                "&pmin&pmax=60&gt=2&", //
                "&pmin&pmax=60&gt=2", //
                "pmin&pmax=60&gt=2&", //
                "pmin&pmax=60&&gt=2", //
                "pmin&pmax=60&&gt=2&&&", //
                "&&&pmin&pmax=60&&gt=2", //
        };
    }

    @ParameterizedTest(name = "Test {index} : {0}")
    @MethodSource("invalidAttributes")
    public void check_invalid_attributes(String invalidAttributesAsString) {
        assertThrows(InvalidAttributeException.class,
                () -> new LwM2mAttributeSet(parser.parseUriQuery(invalidAttributesAsString)));

    }

    @Test
    public void check_multiple_valid_attributes() throws LinkParseException, InvalidAttributeException {

        LwM2mAttributeSet parsed = new LwM2mAttributeSet(parser.parseUriQuery("pmin&pmax=60&gt=2"));
        LwM2mAttributeSet attResult = new LwM2mAttributeSet( //
                LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD), //
                LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD, 60l), //
                LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN, 2d));

        assertEquals(attResult, parsed);
    }
}
