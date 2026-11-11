// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.license;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.license.shared.TbInstanceStatistics;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.service.license.DefaultTbLicenseStatisticsService.CUSTOM_RULE_NODE_TYPE;
import static org.thingsboard.server.service.license.DefaultTbLicenseStatisticsService.peakHourlyPerCounter;
import static org.thingsboard.server.service.license.DefaultTbLicenseStatisticsService.prepareRuleNodeType;
import static org.thingsboard.server.service.license.DefaultTbLicenseStatisticsService.prepareRuleNodeTypes;
import static org.thingsboard.server.service.license.DefaultTbLicenseStatisticsService.totalPerCounter;

/**
 * Guards what the usage snapshot may contain. The privacy commitments made about this payload - aggregate
 * counters and enumerated labels only, no customer-authored text - are otherwise just a promise in a
 * document, and nothing in the build would notice a new field breaking them.
 * <p>
 * A failure here is not necessarily a defect: adding a counter is normal. It means the new field has to be
 * read against those commitments and then named in the list below, deliberately.
 */
public class TbInstanceStatisticsPayloadTest {

    /**
     * Every field this product actually fills in. Extending this list is the review gate, and the fixture has
     * to populate each of them. The binding half is the reference-typed fields: one left unset serialises as
     * null and would be waved through every check below without ever being read against the payload
     * commitments. The primitives among them cannot be null, so for those the fixture is only saying what a
     * populated payload looks like.
     */
    private static final Set<String> SENT_FIELDS = Set.of(
            "entitiesCounts",
            "ruleNodeTypes",
            "integrationsCountsPerType",
            "devicesCountsPerTransportType",
            "solutionTemplatesCountPerName",
            "apiUsageTotalInLastDay",
            "apiUsagePeakHourlyInLastDay",
            "secretsPerType",
            "jobsByTypeAndStatusLastMonth",
            "reportsCountsPerFormatType",
            "reportTemplatesByFormatAndType",
            "genericConverters",
            "typedConverters",
            "dedicatedConverters",
            "jsConvertersCount",
            "tbelConvertersCount",
            "scadaDashboardsCount",
            "postgresDbSize",
            "qrCodeUsage",
            "tbVersion",
            "cassandra",
            "timescale",
            "platform");

    /**
     * Fields the client library declares and this product deliberately never fills in: the full 24-point
     * hourly curve is reduced to a total and a peak by {@code DefaultTbLicenseStatisticsService}, and nothing
     * calls its setter. Asserted to serialise as null, so quietly starting to send one is a failure here.
     */
    private static final Set<String> NEVER_SENT_FIELDS = Set.of("apiUsageInLastDay");

    /** Every field the snapshot may carry at all, sent or not. */
    private static final Set<String> ALLOWED_FIELDS =
            Stream.concat(SENT_FIELDS.stream(), NEVER_SENT_FIELDS.stream()).collect(Collectors.toSet());

    /**
     * The only fields whose value is a string rather than a number or a boolean. Both are ThingsBoard-defined:
     * the build's own version, and the packaging type read from the {@code platform} system property.
     */
    private static final Set<String> ALLOWED_STRING_VALUE_FIELDS = Set.of("tbVersion", "platform");

    /**
     * Free text in this payload would arrive as a map key rather than a value: {@code ruleNodeTypes} is keyed by
     * rule node class name, {@code solutionTemplatesCountPerName} by template name. A key is therefore held to an
     * enumerated shape - a bare identifier-like label, or a class name under ThingsBoard's own package.
     */
    private static final Pattern ALLOWED_PROPERTY_NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_]*|org\\.thingsboard\\.[A-Za-z0-9_.]+");

    @Test
    public void theSnapshotCarriesNoFieldThatHasNotBeenReviewed() {
        Set<String> declared = new TreeSet<>();
        for (Field field : TbInstanceStatistics.class.getDeclaredFields()) {
            if (!field.isSynthetic() && !Modifier.isStatic(field.getModifiers())) {
                declared.add(field.getName());
            }
        }
        assertThat(declared)
                .as("a field was added to or removed from the usage snapshot - check it against the payload "
                        + "commitments (aggregate counters and enumerated labels only, never customer-authored "
                        + "text) and then update ALLOWED_FIELDS")
                .containsExactlyInAnyOrderElementsOf(ALLOWED_FIELDS);
    }

    @Test
    public void everyReportedValueIsANumberABooleanOrOneOfTwoThingsBoardDefinedStrings() {
        JsonNode payload = JacksonUtil.valueToTree(populatedStatistics());

        // Before the checks below, because a null leaf satisfies every one of them: a field the fixture forgot
        // would otherwise be reviewed by nothing at all.
        for (String field : SENT_FIELDS) {
            assertThat(payload.hasNonNull(field))
                    .as("'%s' is serialised as null, so nothing here reads its contents - populate it in "
                            + "populatedStatistics(), or move it to NEVER_SENT_FIELDS if this product never "
                            + "fills it in", field)
                    .isTrue();
        }
        for (String field : NEVER_SENT_FIELDS) {
            assertThat(payload.path(field).isNull())
                    .as("'%s' is now being sent - read it against the payload commitments and move it to "
                            + "SENT_FIELDS, with a fixture value", field)
                    .isTrue();
        }

        for (Map.Entry<String, JsonNode> field : payload.properties()) {
            if (ALLOWED_STRING_VALUE_FIELDS.contains(field.getKey())) {
                assertThat(field.getValue().isTextual()).isTrue();
            } else {
                assertLeavesAreScalar(field.getKey(), field.getValue());
            }
        }
    }

    @Test
    public void aThirdPartyRuleNodeIsReportedOnlyAsCustom() {
        assertThat(prepareRuleNodeType("com.acme.internal.BillingEnrichmentNode")).isEqualTo(CUSTOM_RULE_NODE_TYPE);
        assertThat(prepareRuleNodeType("BillingEnrichmentNode")).isEqualTo(CUSTOM_RULE_NODE_TYPE);
        // a package that merely starts with the same letters is not the platform's package space
        assertThat(prepareRuleNodeType("org.thingsboardfoo.Bar")).isEqualTo(CUSTOM_RULE_NODE_TYPE);
    }

    @Test
    public void aBuiltInRuleNodeKeepsItsType() {
        String builtIn = "org.thingsboard.rule.engine.filter.TbMsgTypeFilterNode";
        assertThat(prepareRuleNodeType(builtIn)).isEqualTo(builtIn);
    }

    /**
     * Collapsing every third-party class onto one label makes key collisions the normal case, and the
     * collector's caller turns any throw into an empty map at debug level. Summing rather than throwing is
     * what keeps a single custom node from erasing the whole breakdown.
     */
    @Test
    public void severalThirdPartyClassesSumIntoOneCustomEntryInsteadOfColliding() {
        Map<String, Long> counts = prepareRuleNodeTypes(Map.of(
                "com.acme.FirstNode", 3L,
                "com.acme.SecondNode", 4L,
                "org.thingsboard.rule.engine.filter.TbMsgTypeFilterNode", 5L));

        assertThat(counts).containsOnlyKeys(CUSTOM_RULE_NODE_TYPE,
                "org.thingsboard.rule.engine.filter.TbMsgTypeFilterNode");
        assertThat(counts.get(CUSTOM_RULE_NODE_TYPE)).isEqualTo(7L);
    }

    @Test
    public void theHourlyCurveIsReducedToADailyTotalAndItsPeakHour() {
        Map<String, Map<Long, Long>> hourly = Map.of(
                "transportMsgCount", Map.of(1L, 10L, 2L, 40L, 3L, 25L),
                "smsCount", Map.of(1L, 0L, 2L, 0L));

        assertThat(totalPerCounter(hourly)).containsExactlyInAnyOrderEntriesOf(
                Map.of("transportMsgCount", 75L, "smsCount", 0L));
        assertThat(peakHourlyPerCounter(hourly)).containsExactlyInAnyOrderEntriesOf(
                Map.of("transportMsgCount", 40L, "smsCount", 0L));
    }

    /**
     * The collector writes -1 for an hour the timeseries store could not supply. Summing that would report a
     * total quietly lower than the truth, which is worse than reporting nothing for the counter.
     */
    @Test
    public void anUnavailableHourIsDroppedRatherThanFoldedIntoTheTotal() {
        Map<String, Map<Long, Long>> hourly = Map.of(
                "transportMsgCount", Map.of(1L, 10L, 2L, -1L, 3L, 5L),
                "jsExecutionCount", Map.of(1L, -1L, 2L, -1L));

        assertThat(totalPerCounter(hourly)).containsExactly(Map.entry("transportMsgCount", 15L));
        assertThat(peakHourlyPerCounter(hourly)).containsExactly(Map.entry("transportMsgCount", 10L));
    }

    private static void assertLeavesAreScalar(String path, JsonNode node) {
        if (node.isObject()) {
            for (Map.Entry<String, JsonNode> entry : node.properties()) {
                assertThat(entry.getKey())
                        .as("%s.%s must be an enumerated label or a ThingsBoard class name - a customer-authored "
                                + "string reaches this payload as a property name, not as a value", path, entry.getKey())
                        .matches(ALLOWED_PROPERTY_NAME);
                assertLeavesAreScalar(path + "." + entry.getKey(), entry.getValue());
            }
            return;
        }
        assertThat(node.isNumber() || node.isBoolean() || node.isNull())
                .as("%s must be a number or a boolean - a string here would put free text on the wire", path)
                .isTrue();
    }

    private static TbInstanceStatistics populatedStatistics() {
        TbInstanceStatistics statistics = new TbInstanceStatistics();
        statistics.setEntitiesCounts(Map.of("DEVICE", 12L));
        statistics.setRuleNodeTypes(prepareRuleNodeTypes(Map.of("com.acme.FirstNode", 1L)));
        statistics.setIntegrationsCountsPerType(Map.of("MQTT", 2L));
        statistics.setDevicesCountsPerTransportType(Map.of("DEFAULT", 12L));
        statistics.setSolutionTemplatesCountPerName(Map.of("smart_office", 1L));
        statistics.setApiUsageTotalInLastDay(Map.of("transportMsgCount", 9L));
        statistics.setApiUsagePeakHourlyInLastDay(Map.of("transportMsgCount", 5L));
        statistics.setSecretsPerType(Map.of("TEXT", 1L));
        statistics.setJobsByTypeAndStatusLastMonth(Map.of("CF_REPROCESSING", Map.of("COMPLETED", 1L)));
        statistics.setReportsCountsPerFormatType(Map.of("PDF", 1L));
        statistics.setReportTemplatesByFormatAndType(Map.of("PDF", Map.of("DASHBOARD", 1L)));
        statistics.setPostgresDbSize(1.25);
        statistics.setTbVersion("4.4.0");
        statistics.setPlatform("docker");
        statistics.setCassandra(false);
        statistics.setTimescale(false);
        return statistics;
    }

}
