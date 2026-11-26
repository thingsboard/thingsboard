// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.config;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.edge.EdgeInfo;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.security.model.mfa.account.TwoFaAccountConfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class SwaggerConfigurationTest {

    @Test
    public void testNotRequiredPropertiesAreRemovedFromRequired() {
        SwaggerConfiguration config = new SwaggerConfiguration();
        // Local instance: the converter is not registered in the global ModelConverters singleton.
        ModelConverters converters = new ModelConverters(true);
        converters.addConverter(config.mapAwareConverter());

        Map<String, Schema> schemas = new HashMap<>();
        // Resolving the polymorphic parent produces the subtypes as allOf schemas.
        for (Class<?> cls : new Class<?>[]{TwoFaAccountConfig.class, RuleChainMetaData.class, EdgeInfo.class, Report.class}) {
            schemas.putAll(converters.readAll(new AnnotatedType(cls)));
        }

        config.removeNotRequiredProperties(schemas);

        // @NotBlank / @NotEmpty + @Schema(requiredMode = NOT_REQUIRED)
        assertThat(requiredOf(schemas.get("SmsTwoFaAccountConfig"))).doesNotContain("phoneNumber");
        assertThat(requiredOf(schemas.get("BackupCodeTwoFaAccountConfig"))).doesNotContain("codes");
        // @NotNull + @Schema(requiredMode = NOT_REQUIRED)
        assertThat(requiredOf(schemas.get("Report"))).doesNotContain("templateId").contains("name");
        assertThat(requiredOf(schemas.get("RuleChainMetaData")))
                .doesNotContain("firstNodeIndex", "connections", "ruleChainConnections")
                .contains("nodes");
        assertThat(requiredOf(schemas.get("EdgeInfo"))).doesNotContain("edgeLicenseKey").contains("name");
    }

    private static Set<String> requiredOf(Schema<?> schema) {
        assertThat(schema).isNotNull();
        Set<String> required = new HashSet<>();
        if (schema.getRequired() != null) {
            required.addAll(schema.getRequired());
        }
        if (schema.getAllOf() != null) {
            for (Schema<?> part : schema.getAllOf()) {
                if (part.getRequired() != null) {
                    required.addAll(part.getRequired());
                }
            }
        }
        return required;
    }

}
