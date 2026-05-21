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

class DashboardRelationsQueryFilterValidationTest extends AbstractDashboardDataValidatorTest {

    @Test
    void shouldAcceptValidRelationsQueryFilter() {
        Dashboard dashboard = filterAlias("Related", """
                {
                  "type": "relationsQuery",
                  "rootEntity": {"entityType": "DEVICE", "id": "%s"},
                  "direction": "FROM",
                  "maxLevel": 1,
                  "filters": []
                }""".formatted(ALIAS_UUID));

        assertThatCode(() -> validate(dashboard)).doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptRelationsQueryFilterWithStateDrivenRoot() {
        Dashboard dashboard = filterAlias("Related", """
                {
                  "type": "relationsQuery",
                  "rootStateEntity": true,
                  "direction": "FROM",
                  "maxLevel": 1
                }""");

        assertThatCode(() -> validate(dashboard)).doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptRelationsQueryFilterWithZeroMaxLevel() {
        // maxLevel = 0 means "unlimited" in the UI
        Dashboard dashboard = filterAlias("Related", """
                {
                  "type": "relationsQuery",
                  "rootEntity": {"entityType": "DEVICE", "id": "%s"},
                  "direction": "FROM",
                  "maxLevel": 0
                }""".formatted(ALIAS_UUID));

        assertThatCode(() -> validate(dashboard)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectRelationsQueryFilterWithoutDirection() {
        Dashboard dashboard = filterAlias("Related", """
                {
                  "type": "relationsQuery",
                  "rootEntity": {"entityType": "DEVICE", "id": "%s"},
                  "maxLevel": 1
                }""".formatted(ALIAS_UUID));

        assertThatThrownBy(() -> validate(dashboard))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Dashboard validation error: alias 'Related' field 'direction' must not be null");
    }

    @Test
    void shouldRejectRelationsQueryFilterWithoutRootEntityWhenStateDrivenIsFalse() {
        Dashboard dashboard = filterAlias("Related", """
                {
                  "type": "relationsQuery",
                  "direction": "FROM",
                  "maxLevel": 1
                }""");

        assertThatThrownBy(() -> validate(dashboard))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Dashboard validation error: alias 'Related' field 'rootEntity' must not be null when 'rootStateEntity' is false");
    }

}
