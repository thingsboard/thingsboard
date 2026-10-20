// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.report.configuration.CellSettings;
import org.thingsboard.server.common.data.report.configuration.ColumnSettings;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataKeySettings;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.TableSortOrder;
import org.thingsboard.server.common.data.report.configuration.components.TableWithLayoutReportComponent;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;
import org.thingsboard.server.common.data.report.configuration.style.Heading;
import org.thingsboard.server.common.data.report.configuration.style.TextAlignment;
import org.thingsboard.server.common.data.report.configuration.style.VerticalAlignment;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.util.ColorUtils;
import org.thingsboard.server.report.util.ThymeleafUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.thingsboard.server.report.util.ReportUtils.ENTITY_TIME_FIELDS;
import static org.thingsboard.server.report.util.ReportUtils.getSingleDataSource;
import static org.thingsboard.server.report.util.ReportUtils.sortRowsByTableSortOrder;

@Slf4j
public abstract class TableWithLayoutComponentRenderer<C extends TableWithLayoutReportComponent> extends ReportComponentWithLayoutRenderer<C> {

    protected String dataSourceName() {
        return "data source";
    }

    protected String noDataMessage() {
        return "Table content is empty";
    }

    @Override
    protected String renderContent(C component, ComponentData componentData) {
        Optional<DataSource> dataSource = getSingleDataSource(component);
        if (dataSource.isEmpty()) {
            return renderError("No " + dataSourceName() + " is configured for " + getType() + " component. Please check the " + dataSourceName() + " configuration.");
        }
        List<DataKey> columns = getColumns(component, dataSource.get());
        if (columns.isEmpty()) {
            return renderError("No columns are configured for " + getType() + " component. Please check the " + dataSourceName() + " configuration.");
        }

        HashMap<String, CellVariables> headers = buildCellVariables(columns, true);
        List<LinkedHashMap<String, CellVariables>> rows = buildDataRows(columns, component.getTableSortOrder(), componentData);

        HashMap<String, Object> componentVars = new HashMap<>();
        componentVars.put("columns", headers);
        componentVars.put("rows", rows);
        componentVars.put("noDataMessage", noDataMessage());

        if (component.isShowTableHeading() && component.getTableHeading() != null) {
            componentVars.put("showTableHeading", true);
            populateHeadingVariables(component, componentData, componentVars);
        } else {
            componentVars.put("showTableHeading", false);
        }
        return ThymeleafUtil.renderFromHtmlTemplate("html/components/table-template", componentVars);
    }

    private String renderError(String errorMessage) {
        return ThymeleafUtil.renderFromHtmlTemplate("html/components/error-template", Map.of("errorMessage", errorMessage));
    }

    protected List<DataKey> getColumns(C component, DataSource dataSource) {
        List<DataKey> dataKeys = new LinkedList<>();
        Optional.ofNullable(dataSource.getDataKeys())
                .ifPresent(dataKeys::addAll);
        Optional.ofNullable(dataSource.getLatestDataKeys())
                .ifPresent(dataKeys::addAll);
        return dataKeys;
    }

    private List<LinkedHashMap<String, CellVariables>> buildDataRows(List<DataKey> columns, TableSortOrder tableSortOrder, ComponentData reportDataSource) {
        List<LinkedHashMap<String, CellVariables>> rows = new ArrayList<>();
        HashMap<String, CellVariables> cellDefaults = buildCellVariables(columns, false);

        List<Map<String, String>> entityDatas = reportDataSource.getEntityDatas();
        sortRowsByTableSortOrder(entityDatas, tableSortOrder);
        for (Map<String, String> entityData : entityDatas) {
            LinkedHashMap<String, CellVariables> row = new LinkedHashMap<>();
            for (DataKey dataKey : columns) {
                String label = dataKey.getLabel();
                String key = dataKey.getName();
                CellVariables base = cellDefaults.get(label);
                String value = entityData.get(label);

                CellVariables variables = base.toBuilder()
                        .fontSize(formatFontSize(key, base.getFontSize()))
                        .fontWeight(formatFontWeight(key, base.getFontWeight()))
                        .color(formatColor(key, value, base.getColor()))
                        .value(formatValue(key, value, dataKey)).build();
                row.put(label, variables);
            }
            rows.add(row);
        }
        return rows;
    }

    private void populateHeadingVariables(C component, ComponentData componentData, Map<String, Object> vars) {
        Heading heading = component.getTableHeading();
        String headingText = ThymeleafUtil.renderFromTextString(heading.getText(), componentData.getVariables());
        Font font = getHeadingFont(heading);

        vars.put("headingText", headingText);
        vars.put("headingColor", ColorUtils.normalizeCssColorOrDefault(heading.getColor(), "#000"));
        vars.put("headingFontSize", Optional.ofNullable(font.getSize()).filter(s -> s > 0).orElse(10f));
        vars.put("headingFontWeight", Optional.ofNullable(font.getWeight()).orElse(FontWeight.NORMAL).getValue());
        vars.put("headingFontStyle", Optional.ofNullable(font.getStyle()).orElse(FontStyle.NORMAL).getValue());
        vars.put("headingFontFamily", StringUtils.defaultString(font.getFamily(), "Roboto"));
        vars.put("headingTextAlignment", Optional.ofNullable(heading.getTextAlignment()).orElse(TextAlignment.CENTER).getValue());
        vars.put("headingVerticalAlignment", Optional.ofNullable(heading.getVerticalAlignment()).orElse(VerticalAlignment.MIDDLE).getValue());
        vars.put("headingHeight", heading.getHeight() != null && heading.getHeight() > 0 ? heading.getHeight() + "pt" : "100%");
    }

    private Float formatFontSize(String key, Float fontSize) {
        if (fontSize != null) {
            return fontSize;
        }
        return defaultFontSize(key);
    }

    private String formatFontWeight(String key, String fontWeight) {
        if (fontWeight != null) {
            return fontWeight;
        }
        return defaultFontWeight(key);
    }

    private String formatColor(String key, String value, String defaultColor) {
        if (defaultColor != null) {
            return defaultColor;
        }
        return defaultColor(key, value);
    }

    private String formatValue(String key, String value, DataKey dataKey) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return defaultValue(key, value);
    }

    protected Float defaultFontSize(String key) {
        if (ENTITY_TIME_FIELDS.contains(key)) {
            return 9f;
        }
        return null;
    }

    protected String defaultFontWeight(String key) {
        return null;
    }

    protected String defaultColor(String key, String value) {
        return null;
    }

    protected String defaultValue(String key, String value) {
        return value;
    }

    protected HashMap<String, CellVariables> buildCellVariables(List<DataKey> columns, boolean isHeader) {
        HashMap<String, CellVariables> result = new LinkedHashMap<>();
        for (DataKey dataKey : columns) {
            result.put(dataKey.getLabel(), toCellVariables(dataKey, isHeader));
        }
        return result;
    }

    protected CellVariables toCellVariables(DataKey dataKey, boolean isHeader) {
        DataKeySettings settings = dataKey.getSettings();
        ColumnSettings columnSettings = null;
        if (settings instanceof ColumnSettings) {
            columnSettings = (ColumnSettings) settings;
        }
        return toCellVariables(dataKey.getName(), columnSettings, isHeader);
    }

    protected CellVariables toCellVariables(String key, ColumnSettings columnSettings, boolean isHeader) {
        if (columnSettings != null) {
            CellVariables cellVariables;
            CellSettings cellSettings = isHeader ? columnSettings.getHeader() : columnSettings.getCell();
            if (cellSettings != null) {
                Font font = cellSettings.getFont();
                cellVariables = CellVariables.builder()
                        .key(key)
                        .color(cellSettings.getColor() != null ? ColorUtils.normalizeCssColor(cellSettings.getColor()) : null)
                        .backgroundColor(cellSettings.getBackgroundColor() != null ? ColorUtils.normalizeCssColor(cellSettings.getBackgroundColor()) : null)
                        .fontSize(font != null && font.getSize() != null && font.getSize() > 0 ? font.getSize() : null)
                        .fontWeight(font != null && font.getWeight() != null ? font.getWeight().name() : null)
                        .fontStyle(font != null && font.getStyle() != null ? font.getStyle().name() : null)
                        .fontFamily(font != null && font.getFamily() != null && !font.getFamily().isEmpty() ? font.getFamily() : null)
                        .textAlignment(cellSettings.getTextAlignment() != null ? cellSettings.getTextAlignment().name() : null)
                        .verticalAlignment(cellSettings.getVerticalAlignment() != null ? cellSettings.getVerticalAlignment().name() : null)
                        .build();
            } else {
                cellVariables = new CellVariables(key);
            }
            if (isHeader && !StringUtils.isBlank(columnSettings.getColumnWidth())) {
                cellVariables.setWidth(columnSettings.getColumnWidth());
            }
            return cellVariables;
        }
        return new CellVariables(key);
    }

    private Font getHeadingFont(Heading tableHeading) {
        Font headingFont = tableHeading.getFont();
        if (headingFont == null) {
            headingFont = new Font();
            headingFont.setSize(20f);
            headingFont.setFamily("Roboto");
            headingFont.setStyle(FontStyle.NORMAL);
            headingFont.setWeight(FontWeight.NORMAL);
        }
        return headingFont;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder(toBuilder = true)
    static class CellVariables {
        private String key;
        private String value;
        private String width;
        private String color;
        private String backgroundColor;
        private Float fontSize;
        private String fontWeight;
        private String fontStyle;
        private String fontFamily;
        private String textAlignment;
        private String verticalAlignment;

        public CellVariables(String key) {
            this.key = key;
        }
    }

}
