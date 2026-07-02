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

class DashboardEntityViewSearchQueryFilterValidationTest extends AbstractDashboardDataValidatorTest {

    @Test
    void shouldAcceptValidEntityViewSearchQueryFilter() {
        Dashboard dashboard = filterAlias("Views", """
                {
                  "type": "entityViewSearchQuery",
                  "rootEntity": {"entityType": "DEVICE", "id": "%s"},
                  "direction": "FROM",
                  "maxLevel": 1,
                  "entityViewTypes": ["summary"]
                }""".formatted(ALIAS_UUID));

        assertThatCode(() -> validate(dashboard)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectEntityViewSearchQueryFilterWithEmptyEntityViewTypes() {
        Dashboard dashboard = filterAlias("Views", """
                {
                  "type": "entityViewSearchQuery",
                  "rootEntity": {"entityType": "DEVICE", "id": "%s"},
                  "direction": "FROM",
                  "maxLevel": 1,
                  "entityViewTypes": []
                }""".formatted(ALIAS_UUID));

        assertThatThrownBy(() -> validate(dashboard))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Dashboard validation error: alias 'Views' field 'entityViewTypes' must not be empty");
    }

    @Test
    void shouldRejectEntityViewSearchQueryFilterWithBlankEntityViewTypeElement() {
        Dashboard dashboard = filterAlias("Views", """
                {
                  "type": "entityViewSearchQuery",
                  "rootEntity": {"entityType": "DEVICE", "id": "%s"},
                  "direction": "FROM",
                  "maxLevel": 1,
                  "entityViewTypes": ["summary", "  "]
                }""".formatted(ALIAS_UUID));

        assertThatThrownBy(() -> validate(dashboard))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Dashboard validation error: alias 'Views' field 'entityViewTypes' element at index 1 must not be blank");
    }

}
