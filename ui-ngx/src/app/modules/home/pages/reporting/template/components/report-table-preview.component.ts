// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Directive } from '@angular/core';
import { AbstractReportComponentPreview } from '@home/pages/reporting/template/components/report-component.component';
import { TableReportColumnSettings, TableReportComponentConfig } from '@shared/models/report-component.models';
import { ComponentStyle, Font, textStyle } from '@shared/models/widget-settings.models';
import { deepClone } from '@core/utils';
import { DataKey } from '@shared/models/widget.models';
import { Direction } from '@shared/models/page/sort-order';

@Directive()
export abstract class AbstractReportTablePreviewComponent<C extends TableReportComponentConfig> extends AbstractReportComponentPreview<C> {

  showTableHeading: boolean;
  headingText: string;
  headingHeight: string;
  headingStyle: ComponentStyle;

  onComponentUpdated() {
    if (this.reportComponent.showTableHeading && this.reportComponent.tableHeading) {
        this.showTableHeading = true;
        const tableHeading = this.reportComponent.tableHeading;
        if (tableHeading.text && tableHeading.text.trim().length) {
          this.headingText = tableHeading.text;
        } else {
          this.headingText = '&nbsp;';
        }
        if (!this.isPlainFormat) {
          const font: Font = deepClone(tableHeading.font || {size: 20, sizeUnit: 'pt'} as Font);
          if (!font.size) {
            font.size = 20;
          }
          if (font.sizeUnit !== 'pt') {
            font.sizeUnit = 'pt';
          }
          this.headingStyle = textStyle(font);
          if (!this.headingStyle.fontWeight) {
            this.headingStyle.fontWeight = 'normal';
          }
          this.headingStyle.color = tableHeading.color || '#000';
          if (tableHeading.textAlignment) {
            this.headingStyle.textAlign = tableHeading.textAlignment;
          }
          if (tableHeading.verticalAlignment) {
            this.headingStyle.verticalAlign = tableHeading.verticalAlignment;
          }
          if (tableHeading.height) {
            this.headingHeight = tableHeading.height + 'pt';
          } else {
            this.headingHeight = '100%';
          }
        }
    } else {
      this.showTableHeading = false;
    }
  }


  headerStyle(column: DataKey): ComponentStyle {
    return this.styleFromColumnSettings(column, true);
  }

  cellStyle(column: DataKey): ComponentStyle {
    return this.styleFromColumnSettings(column);
  }

  columnWidth(column: DataKey): string {
    if (!this.isPlainFormat && column?.settings) {
      const columnSettings: TableReportColumnSettings =  column.settings;
      if (columnSettings?.columnWidth) {
        return columnSettings?.columnWidth;
      }
    }
    return null;
  }

  hasSortOrder(column: DataKey): boolean {
    return this.reportComponent.tableSortOrder?.column === column.label;
  }

  ascSortOrder(): boolean {
    return this.reportComponent.tableSortOrder?.direction !== Direction.DESC;
  }

  cellContent(column: DataKey): string {
    return '${' + column.label + '}';
  }

  protected styleFromColumnSettings(column: DataKey, header = false): ComponentStyle {
    let style: ComponentStyle = {};
    if (!this.isPlainFormat && column?.settings) {
      const columnSettings: TableReportColumnSettings =  column.settings;
      if (columnSettings) {
        const cellSettings = header ? columnSettings.header : columnSettings.cell;
        if (cellSettings) {
          if (cellSettings.font && cellSettings.font.sizeUnit !== 'pt') {
            cellSettings.font.sizeUnit = 'pt';
          }
          style = textStyle(cellSettings.font);
          style.textAlign = cellSettings.textAlignment;
          style.verticalAlign = cellSettings.verticalAlignment;
          style.color = cellSettings.color;
          style.backgroundColor = cellSettings.backgroundColor;
        }
      }
    }
    return style;
  }
}
