// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer;

import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.TableSortOrder;
import org.thingsboard.server.common.data.report.configuration.components.TableReportComponent;
import org.thingsboard.server.common.data.report.configuration.style.Heading;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.util.ThymeleafUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.report.util.ReportUtils.getSingleDataSource;
import static org.thingsboard.server.report.util.ReportUtils.sortRowsByTableSortOrder;

public abstract class AbstractCsvComponentRenderer<C extends TableReportComponent> implements CsvReportComponentRenderer<C> {

    public List<List<String>> render(C component, ComponentData componentData) {
        Optional<DataSource> dataSourceOpt = getSingleDataSource(component);
        if (dataSourceOpt.isEmpty()) {
            return List.of(List.of("Data source is not configured for " + component.getType().name() + " component"));
        }

        DataSource dataSource = dataSourceOpt.get();
        List<DataKey> columns = getColumns(component, dataSource);

        List<List<String>> csvContent = new ArrayList<>();
        addHeading(component, componentData, csvContent);
        addHeaderRow(columns, csvContent);
        addDataRows(columns, component.getTableSortOrder(), componentData, csvContent);
        return csvContent;
    }

    private void addHeading(TableReportComponent component, ComponentData componentData, List<List<String>> content) {
        if (component.isShowTableHeading() && component.getTableHeading() != null) {
            Heading tableHeading = component.getTableHeading();
            String headingText = ThymeleafUtil.renderFromTextString(tableHeading.getText(), componentData.getVariables());
            content.add(List.of(headingText));
        }
    }

    private void addHeaderRow(List<DataKey> columns, List<List<String>> content) {
        List<String> headers = columns.stream()
                .map(DataKey::getLabel)
                .collect(Collectors.toList());
        content.add(headers);
    }

    private void addDataRows(List<DataKey> columns, TableSortOrder tableSortOrder, ComponentData componentData, List<List<String>> content) {
        List<Map<String, String>> entityDatas = componentData.getEntityDatas();
        sortRowsByTableSortOrder(entityDatas, tableSortOrder);

        for (Map<String, String> row : entityDatas) {
            List<String> formattedValues = columns.stream()
                    .map(col -> row.get(col.getLabel()))
                    .collect(Collectors.toList());
            content.add(formattedValues);
        }
    }

    public List<DataKey> getColumns(C component, DataSource dataSource) {
        return dataSource.getDataKeys();
    }

}
