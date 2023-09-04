/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.dao.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.thingsboard.server.dao.exception.DataValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

@Slf4j
public class DataValidatorTest {

    DataValidator<?> dataValidator;

    @BeforeEach
    void setUp() {
        dataValidator = spy(DataValidator.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "coffee", "1", "big box", "世界", "!", "--", "~!@#$%^&*()_+=-/|\\[]{};:'`\"?<>,.", "\uD83D\uDC0C", "\041",
            "Gdy Pomorze nie pomoże, to pomoże może morze, a gdy morze nie pomoże, to pomoże może Gdańsk",
    })
    void testDeviceName_thenOK(final String name) {
        dataValidator.validateName("Device", name);
        dataValidator.validateName("Asset", name);
        dataValidator.validateName("Customer", name);
        dataValidator.validateName("Tenant", name);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", " ", "  ", "\n", "\r\n", "\t", "\000", "\000\000", "\001", "\002", "\040", "\u0000", "\u0000\u0000",
            "F0929906\000\000\000\000\000\000\000\000\000", "\000\000\000F0929906",
            "\u0000F0929906", "F092\u00009906", "F0929906\u0000"
    })
    void testDeviceName_thenDataValidationException(final String name) {
        DataValidationException exception;
        exception = Assertions.assertThrows(DataValidationException.class, () -> dataValidator.validateName("Asset", name));
        log.warn("Exception message Asset: {}", exception.getMessage());
        assertThat(exception.getMessage()).as("message Asset").containsPattern("Asset .*name.*");

        exception = Assertions.assertThrows(DataValidationException.class, () -> dataValidator.validateName("Device", name));
        log.warn("Exception message Device: {}", exception.getMessage());
        assertThat(exception.getMessage()).as("message Device").containsPattern("Device .*name.*");
    }

    @Test
    public void validateEmail() {
        String email = "aZ1_!#$%&'*+/=?`{|}~^.-@mail.io";
        DataValidator.validateEmail(email);
    }

    @Test
    public void validateInvalidEmail1() {
        String email = "test:1@mail.io";
        Assertions.assertThrows(DataValidationException.class, () -> {
            DataValidator.validateEmail(email);
        });
    }
    @Test
    public void validateInvalidEmail2() {
        String email = "test()1@mail.io";
        Assertions.assertThrows(DataValidationException.class, () -> {
            DataValidator.validateEmail(email);
        });
    }

    @Test
    public void validateInvalidEmail3() {
        String email = "test[]1@mail.io";
        Assertions.assertThrows(DataValidationException.class, () -> {
            DataValidator.validateEmail(email);
        });
    }

    @Test
    public void validateInvalidEmail4() {
        String email = "test\\1@mail.io";
        Assertions.assertThrows(DataValidationException.class, () -> {
            DataValidator.validateEmail(email);
        });
    }

    @Test
    public void validateInvalidEmail5() {
        String email = "test\"1@mail.io";
        Assertions.assertThrows(DataValidationException.class, () -> {
            DataValidator.validateEmail(email);
        });
    }

    @Test
    public void validateInvalidEmail6() {
        String email = "test<>1@mail.io";
        Assertions.assertThrows(DataValidationException.class, () -> {
            DataValidator.validateEmail(email);
        });
    }
}
