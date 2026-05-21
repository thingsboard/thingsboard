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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DashboardConfigurationStructureTest extends AbstractDashboardDataValidatorTest {

    @Nested
    class Title {

        @Test
        void shouldAcceptValidTitle() {
            Dashboard dashboard = new Dashboard();
            dashboard.setTitle("flight control");
            dashboard.setTenantId(tenantId);

            assertThatCode(() -> validate(dashboard)).doesNotThrowAnyException();
        }

        @Test
        void shouldRejectBlankTitle() {
            Dashboard dashboard = new Dashboard();
            dashboard.setTitle("   ");
            dashboard.setTenantId(tenantId);

            assertThatThrownBy(() -> validate(dashboard))
                    .isInstanceOf(DataValidationException.class)
                    .hasMessage("Dashboard title should be specified!");
        }

    }

    @Nested
    class Tenant {

        @Test
        void shouldRejectDashboardReferencingNonExistentTenant() {
            TenantId unknownTenantId = TenantId.fromUUID(UUID.fromString("11111111-1111-1111-1111-111111111111"));
            Dashboard dashboard = new Dashboard();
            dashboard.setTitle("flight control");
            dashboard.setTenantId(unknownTenantId);

            assertThatThrownBy(() -> validate(dashboard))
                    .isInstanceOf(DataValidationException.class)
                    .hasMessage("Dashboard is referencing to non-existent tenant!");
        }

    }

    @Nested
    class ConfigurationShape {

        @Test
        void shouldAcceptDashboardWithoutConfiguration() {
            Dashboard dashboard = dashboardWith(null);

            assertThatCode(() -> validate(dashboard)).doesNotThrowAnyException();
        }

        @Test
        void shouldAcceptNonObjectConfiguration() {
            Dashboard dashboard = dashboardWith(JacksonUtil.toJsonNode("\"just a string\""));

            assertThatCode(() -> validate(dashboard)).doesNotThrowAnyException();
        }

        @Test
        void shouldAcceptConfigurationWithoutEntityAliases() {
            Dashboard dashboard = dashboardWith(JacksonUtil.toJsonNode("{\"widgets\": {}}"));

            assertThatCode(() -> validate(dashboard)).doesNotThrowAnyException();
        }

        @Test
        void shouldAcceptEmptyEntityAliasesMap() {
            Dashboard dashboard = dashboardWith(JacksonUtil.toJsonNode("{\"entityAliases\": {}}"));

            assertThatCode(() -> validate(dashboard)).doesNotThrowAnyException();
        }

        @Test
        void shouldAcceptNullEntityAliases() {
            Dashboard dashboard = dashboardWith(JacksonUtil.toJsonNode("{\"entityAliases\": null}"));

            assertThatCode(() -> validate(dashboard)).doesNotThrowAnyException();
        }

        @Test
        void shouldRejectEntityAliasesAsArray() {
            Dashboard dashboard = dashboardWith(JacksonUtil.toJsonNode("{\"entityAliases\": []}"));

            assertThatThrownBy(() -> validate(dashboard))
                    .isInstanceOf(DataValidationException.class)
                    .hasMessageStartingWith("Dashboard configuration has invalid structure:");
        }

    }

    @Nested
    class EntityAliasShape {

        @Test
        void shouldAcceptWellFormedEntityAlias() {
            Dashboard dashboard = filterAlias("Devices", """
                    {"type": "entityType", "entityType": "DEVICE"}""");

            assertThatCode(() -> validate(dashboard)).doesNotThrowAnyException();
        }

        @Test
        void shouldRejectBlankAliasDisplayName() {
            String json = """
                    {
                      "entityAliases": {
                        "%s": {
                          "id": "%s",
                          "alias": "",
                          "filter": {"type": "entityType", "entityType": "DEVICE"}
                        }
                      }
                    }""".formatted(ALIAS_UUID, ALIAS_UUID);
            Dashboard dashboard = dashboardWith(JacksonUtil.toJsonNode(json));

            assertThatThrownBy(() -> validate(dashboard))
                    .isInstanceOf(DataValidationException.class)
                    .hasMessage("Dashboard validation error: alias '" + ALIAS_UUID + "' field 'alias' must not be blank");
        }

        @Test
        void shouldRejectKeyThatIsNotUuid() {
            String json = """
                    {"entityAliases": {"not-a-uuid": {"id": "%s", "alias": "X", "filter": {"type": "entityType", "entityType": "DEVICE"}}}}""".formatted(ALIAS_UUID);
            Dashboard dashboard = dashboardWith(JacksonUtil.toJsonNode(json));

            assertThatThrownBy(() -> validate(dashboard))
                    .isInstanceOf(DataValidationException.class)
                    .hasMessageStartingWith("Dashboard configuration has invalid structure:");
        }

        @Test
        void shouldRejectAliasValueThatIsNotObject() {
            String json = """
                    {"entityAliases": {"%s": "not an object"}}""".formatted(ALIAS_UUID);
            Dashboard dashboard = dashboardWith(JacksonUtil.toJsonNode(json));

            assertThatThrownBy(() -> validate(dashboard))
                    .isInstanceOf(DataValidationException.class)
                    .hasMessageStartingWith("Dashboard configuration has invalid structure:");
        }

        @Test
        void shouldRejectMissingIdField() {
            String json = """
                    {
                      "entityAliases": {
                        "%s": {"alias": "Devices", "filter": {"type": "entityType", "entityType": "DEVICE"}}
                      }
                    }""".formatted(ALIAS_UUID);
            Dashboard dashboard = dashboardWith(JacksonUtil.toJsonNode(json));

            assertThatThrownBy(() -> validate(dashboard))
                    .isInstanceOf(DataValidationException.class)
                    .hasMessage("Dashboard validation error: alias 'Devices' field 'id' must not be null");
        }

        @Test
        void shouldRejectNonTextualIdField() {
            String json = """
                    {
                      "entityAliases": {
                        "%s": {"id": 42, "alias": "Devices", "filter": {"type": "entityType", "entityType": "DEVICE"}}
                      }
                    }""".formatted(ALIAS_UUID);
            Dashboard dashboard = dashboardWith(JacksonUtil.toJsonNode(json));

            assertThatThrownBy(() -> validate(dashboard))
                    .isInstanceOf(DataValidationException.class)
                    .hasMessageStartingWith("Dashboard configuration has invalid structure:");
        }

        @Test
        void shouldRejectIdThatIsNotUuid() {
            String json = """
                    {
                      "entityAliases": {
                        "%s": {"id": "not-a-uuid", "alias": "Devices", "filter": {"type": "entityType", "entityType": "DEVICE"}}
                      }
                    }""".formatted(ALIAS_UUID);
            Dashboard dashboard = dashboardWith(JacksonUtil.toJsonNode(json));

            assertThatThrownBy(() -> validate(dashboard))
                    .isInstanceOf(DataValidationException.class)
                    .hasMessageStartingWith("Dashboard configuration has invalid structure:");
        }

        @Test
        void shouldRejectIdNotMatchingKey() {
            String json = """
                    {
                      "entityAliases": {
                        "%s": {"id": "11111111-1111-1111-1111-111111111111", "alias": "Devices", "filter": {"type": "entityType", "entityType": "DEVICE"}}
                      }
                    }""".formatted(ALIAS_UUID);
            Dashboard dashboard = dashboardWith(JacksonUtil.toJsonNode(json));

            assertThatThrownBy(() -> validate(dashboard))
                    .isInstanceOf(DataValidationException.class)
                    .hasMessage("Dashboard validation error: alias 'Devices' has 'id' that does not match its key!");
        }

        @Test
        void shouldRejectMissingAliasField() {
            String json = """
                    {
                      "entityAliases": {
                        "%s": {"id": "%s", "filter": {"type": "entityType", "entityType": "DEVICE"}}
                      }
                    }""".formatted(ALIAS_UUID, ALIAS_UUID);
            Dashboard dashboard = dashboardWith(JacksonUtil.toJsonNode(json));

            assertThatThrownBy(() -> validate(dashboard))
                    .isInstanceOf(DataValidationException.class)
                    .hasMessage("Dashboard validation error: alias '" + ALIAS_UUID + "' field 'alias' must not be blank");
        }

        @Test
        void shouldRejectMissingFilterField() {
            String json = """
                    {
                      "entityAliases": {
                        "%s": {"id": "%s", "alias": "Devices"}
                      }
                    }""".formatted(ALIAS_UUID, ALIAS_UUID);
            Dashboard dashboard = dashboardWith(JacksonUtil.toJsonNode(json));

            assertThatThrownBy(() -> validate(dashboard))
                    .isInstanceOf(DataValidationException.class)
                    .hasMessage("Dashboard validation error: alias 'Devices' field 'filter' must not be null");
        }

        @Test
        void shouldRejectFilterWithoutType() {
            String json = """
                    {
                      "entityAliases": {
                        "%s": {"id": "%s", "alias": "Devices", "filter": {}}
                      }
                    }""".formatted(ALIAS_UUID, ALIAS_UUID);
            Dashboard dashboard = dashboardWith(JacksonUtil.toJsonNode(json));

            assertThatThrownBy(() -> validate(dashboard))
                    .isInstanceOf(DataValidationException.class)
                    .hasMessageStartingWith("Dashboard configuration has invalid structure:");
        }

        @Test
        void shouldRejectFilterWithUnknownType() {
            String json = """
                    {
                      "entityAliases": {
                        "%s": {"id": "%s", "alias": "Devices", "filter": {"type": "bogus"}}
                      }
                    }""".formatted(ALIAS_UUID, ALIAS_UUID);
            Dashboard dashboard = dashboardWith(JacksonUtil.toJsonNode(json));

            assertThatThrownBy(() -> validate(dashboard))
                    .isInstanceOf(DataValidationException.class)
                    .hasMessageStartingWith("Dashboard configuration has invalid structure:");
        }

        @Test
        void shouldRejectNonObjectFilterField() {
            String json = """
                    {
                      "entityAliases": {
                        "%s": {"id": "%s", "alias": "Devices", "filter": "not an object"}
                      }
                    }""".formatted(ALIAS_UUID, ALIAS_UUID);
            Dashboard dashboard = dashboardWith(JacksonUtil.toJsonNode(json));

            assertThatThrownBy(() -> validate(dashboard))
                    .isInstanceOf(DataValidationException.class)
                    .hasMessageStartingWith("Dashboard configuration has invalid structure:");
        }

    }

}
