// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, inject, ViewEncapsulation } from '@angular/core';
import { TimeseriesTableReportComponentConfig } from '@shared/models/report-component.models';
import { DataKey, Datasource } from '@shared/models/widget.models';
import { ComponentStyle, dateFormatPreview } from '@shared/models/widget-settings.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import {
  AbstractReportTablePreviewComponent
} from '@home/pages/reporting/template/components/report-table-preview.component';
import { DatePipe } from '@angular/common';
import { ReportTemplatePageComponent } from '@home/pages/reporting/template/report-template-page.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-timeseries-table-preview',
    templateUrl: './report-table-preview.component.html',
    styleUrls: ['./report-table-preview.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class TimeseriesTablePreviewComponent extends AbstractReportTablePreviewComponent<TimeseriesTableReportComponentConfig> {

  columns: DataKey[] = [];

  private date = inject(DatePipe);
  private destroyRef = inject(DestroyRef);
  private templatePage = inject(ReportTemplatePageComponent);
  private timestampColumn: DataKey = null;
  private timestampPreview: string;
  private createdTimePreview: string;

  ngOnInit() {
    super.ngOnInit();
    this.templatePage.timeDataPattern$.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateCreatedTimePreview();
    });
  }

  onComponentUpdated() {
    super.onComponentUpdated();
    this.columns = [];
    if (this.reportComponent.showTimestamp) {
      if (!this.timestampColumn) {
        this.timestampColumn = {
          name: 'ts',
          type: DataKeyType.timeseries
        };
      }
      this.timestampColumn.label = this.reportComponent.timestampLabel || 'Timestamp';
      this.timestampColumn.settings = this.reportComponent.timestampColumnSettings;
      this.timestampPreview = dateFormatPreview(this.date, this.reportComponent.timestampPattern, this.reportComponent.timewindow?.timezone);
      this.columns.push(
        this.timestampColumn
      );
    }
    this.updateCreatedTimePreview();
    const datasources: Datasource[] = this.reportComponent.dataSources;
    if (datasources && datasources.length) {
      const datasource = datasources[0];
      this.columns.push(...(datasource.dataKeys || []));
      this.columns.push(...(datasource.latestDataKeys || []));
    }
  }

  private updateCreatedTimePreview() {
    this.createdTimePreview = dateFormatPreview(this.date, this.templatePage.timeDataPattern, this.reportComponent.timewindow?.timezone);
  }

  cellContent(column: DataKey): string {
    if (column.name === 'ts') {
      return this.timestampPreview;
    } else if ('createdTime' === column.name && !column.usePostProcessing) {
      return this.createdTimePreview;
    } else {
      return super.cellContent(column);
    }
  }

  protected styleFromColumnSettings(column: DataKey, header = false): ComponentStyle {
    const style = super.styleFromColumnSettings(column, header);
    if (!this.isPlainFormat && !header) {
      if (['ts', 'createdTime'].includes(column.name)) {
        style.fontSize = style.fontSize || '9pt';
      }
    }
    return style;
  }

}
