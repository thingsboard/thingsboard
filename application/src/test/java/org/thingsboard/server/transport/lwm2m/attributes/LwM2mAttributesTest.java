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

import org.eclipse.leshan.core.link.attributes.InvalidAttributeException;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttribute;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeModel;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LwM2mAttributesTest {

    @ParameterizedTest(name = "Tests {index} : {0}")
    @MethodSource("supportNullAttributes")
    public void check_attribute_can_be_created_with_null_value(LwM2mAttributeModel<?> model)
            throws InvalidAttributeException {
        LwM2mAttribute<?> attribute = LwM2mAttributes.create(model);
        assertNotNull(attribute);
        assertFalse(attribute.hasValue());
        assertNull(attribute.getValue());
        attribute = LwM2mAttributes.create(model, null);
        assertNotNull(attribute);
        assertFalse(attribute.hasValue());
        assertNull(attribute.getValue());
    }

    @ParameterizedTest(name = "Tests {index} : {0}")
    @MethodSource("doesntSupportAttributesWithoutValue")
    public void check_attribute_can_not_be_created_without_value(LwM2mAttributeModel<?> model) {
        assertThrows(IllegalArgumentException.class, () -> LwM2mAttributes.create(model));
    }

    @ParameterizedTest(name = "Tests {index} : {0}")
    @MethodSource("doesntSupportAttributesWithValueNull")
    public void check_attribute_can_not_be_created_with_null(LwM2mAttributeModel<?> model) {
        assertThrows(IllegalArgumentException.class, () -> LwM2mAttributes.create(model, null));
    }

   private static Stream<Arguments> supportNullAttributes() throws InvalidAttributeException {
        return Stream.of(//
                Arguments.of(LwM2mAttributes.MINIMUM_PERIOD), //
                Arguments.of(LwM2mAttributes.MAXIMUM_PERIOD), //
                Arguments.of(LwM2mAttributes.EVALUATE_MINIMUM_PERIOD), //
                Arguments.of(LwM2mAttributes.EVALUATE_MAXIMUM_PERIOD), //
                Arguments.of(LwM2mAttributes.LESSER_THAN), //
                Arguments.of(LwM2mAttributes.GREATER_THAN), //
                Arguments.of(LwM2mAttributes.STEP) //
        );
    }

    private static Stream<Arguments> doesntSupportAttributesWithoutValue() throws InvalidAttributeException {
        return Stream.of(//
                Arguments.of(LwM2mAttributes.ENABLER_VERSION), //
                Arguments.of(LwM2mAttributes.OBJECT_VERSION)//
        );
    }

    private static Stream<Arguments> doesntSupportAttributesWithValueNull() throws InvalidAttributeException {
        return Stream.of(//
                Arguments.of(LwM2mAttributes.DIMENSION), //
                Arguments.of(LwM2mAttributes.SHORT_SERVER_ID) //
        );
    }
}

