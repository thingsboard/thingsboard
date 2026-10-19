// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.util;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.report.configuration.TableSortOrder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class ReportUtilsTest {

    private static final String COLUMN = "VALUE";

    @Test
    void testSortRowsByTableSortOrderWithMixedTypesAsc() {
        List<Map<String, String>> rows = buildRows("sssss", "1a", "", "5", "4", "sdgsdg", "");

        ReportUtils.sortRowsByTableSortOrder(rows, new TableSortOrder(COLUMN, TableSortOrder.Direction.ASC));

        // blanks first (like null), then numbers numerically as one block, then strings lexically
        assertThat(columnValues(rows)).containsExactly("", "", "4", "5", "1a", "sdgsdg", "sssss");
    }

    @Test
    void testSortRowsByTableSortOrderWithMixedTypesDesc() {
        List<Map<String, String>> rows = buildRows("sssss", "1a", "", "5", "4", "sdgsdg", "");

        ReportUtils.sortRowsByTableSortOrder(rows, new TableSortOrder(COLUMN, TableSortOrder.Direction.DESC));

        // reversed: strings, then numbers, then blanks sink to the end
        assertThat(columnValues(rows)).containsExactly("sssss", "sdgsdg", "1a", "5", "4", "", "");
    }

    @Test
    void testSortRowsByTableSortOrderDoesNotViolateContractForLargeMixedData() {
        // These values previously broke comparator transitivity. The violation is only detected by
        // TimSort's merge path, which kicks in for arrays of 32+ elements, so we need a large list.
        String[] sample = {"10", "9", "1a", "100", "2b", "5", "abc", "42", "9z", "0"};
        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            rows.add(row(sample[i % sample.length]));
        }

        // must not throw "Comparison method violates its general contract!"
        assertThatCode(() -> ReportUtils.sortRowsByTableSortOrder(rows, new TableSortOrder(COLUMN, TableSortOrder.Direction.ASC)))
                .doesNotThrowAnyException();

        // and the result must be consistently ordered by the comparator
        List<String> values = columnValues(rows);
        for (int i = 0; i < values.size() - 1; i++) {
            assertThat(ReportUtils.compareMixedValuesBlankFirst(values.get(i), values.get(i + 1)))
                    .as("values at index %d and %d should be non-decreasing", i, i + 1)
                    .isLessThanOrEqualTo(0);
        }
    }

    @Test
    void testCompareMixedValuesBlankFirst() {
        // nulls and blanks treated alike, sorted first
        assertThat(ReportUtils.compareMixedValuesBlankFirst(null, null)).isZero();
        assertThat(ReportUtils.compareMixedValuesBlankFirst(null, "1")).isNegative();
        assertThat(ReportUtils.compareMixedValuesBlankFirst("1", null)).isPositive();
        assertThat(ReportUtils.compareMixedValuesBlankFirst("", "  ")).isZero();
        assertThat(ReportUtils.compareMixedValuesBlankFirst(null, "")).isZero();
        assertThat(ReportUtils.compareMixedValuesBlankFirst("", "1")).isNegative();
        assertThat(ReportUtils.compareMixedValuesBlankFirst("1", "")).isPositive();

        // both numeric -> numeric comparison (not lexical: "9" < "10")
        assertThat(ReportUtils.compareMixedValuesBlankFirst("9", "10")).isNegative();

        // numeric always before string, regardless of lexical order
        assertThat(ReportUtils.compareMixedValuesBlankFirst("5", "1a")).isNegative();
        assertThat(ReportUtils.compareMixedValuesBlankFirst("1a", "5")).isPositive();

        // both strings -> case-insensitive lexical
        assertThat(ReportUtils.compareMixedValuesBlankFirst("abc", "ABD")).isNegative();
    }

    private static List<Map<String, String>> buildRows(String... values) {
        List<Map<String, String>> rows = new ArrayList<>();
        for (String value : values) {
            rows.add(row(value));
        }
        return rows;
    }

    private static Map<String, String> row(String value) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put(COLUMN, value);
        return row;
    }

    private static List<String> columnValues(List<Map<String, String>> rows) {
        return rows.stream().map(row -> row.get(COLUMN)).collect(Collectors.toList());
    }

}
