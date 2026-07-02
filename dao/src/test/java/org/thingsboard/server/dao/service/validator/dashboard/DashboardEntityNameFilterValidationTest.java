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
package org.thingsboard.server.dao.service.validator.dashboard;

import org.thingsboard.server.dao.service.validator.AbstractDashboardDataValidatorTest;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.dao.exception.DataValidationException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DashboardEntityNameFilterValidationTest extends AbstractDashboardDataValidatorTest {

    @Test
    void shouldAcceptValidEntityNameFilter() {
        Dashboard dashboard = filterAlias("Sensors", """
                {"type": "entityName", "entityType": "DEVICE", "entityNameFilter": "sensor"}""");

        assertThatCode(() -> validate(dashboard)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectEntityNameFilterWithoutEntityType() {
        Dashboard dashboard = filterAlias("Sensors", """
                {"type": "entityName", "entityNameFilter": "sensor"}""");

        assertThatThrownBy(() -> validate(dashboard))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Dashboard validation error: alias 'Sensors' field 'entityType' must not be null");
    }

    @Test
    void shouldRejectEntityNameFilterWithBlankEntityNameFilter() {
        Dashboard dashboard = filterAlias("Sensors", """
                {"type": "entityName", "entityType": "DEVICE", "entityNameFilter": "   "}""");

        assertThatThrownBy(() -> validate(dashboard))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Dashboard validation error: alias 'Sensors' field 'entityNameFilter' must not be blank");
    }

}
