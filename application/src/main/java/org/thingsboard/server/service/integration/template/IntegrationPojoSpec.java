// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.template;

/**
 * Describes one POJO walk for the export service: the root class to recursively walk
 * (visiting fields annotated with @TemplateField), and the JSON-path prefix that
 * walk's discovered paths should be nested under.
 *
 * <p>Multiple specs per IntegrationType (see {@link IntegrationConfigPojoRegistry})
 * let one type have annotations spread across separate POJOs (e.g., MqttClientConfiguration
 * under "configuration.clientConfiguration", a top-level config wrapper under "configuration",
 * and the universal Integration#name annotation at "" — combined in the same export run).
 */
public record IntegrationPojoSpec(Class<?> rootClass, String jsonPathPrefix) {
    public IntegrationPojoSpec {
        if (rootClass == null) {
            throw new IllegalArgumentException("rootClass must not be null");
        }
        if (jsonPathPrefix == null) {
            throw new IllegalArgumentException("jsonPathPrefix must not be null (use empty string for root)");
        }
    }
}
