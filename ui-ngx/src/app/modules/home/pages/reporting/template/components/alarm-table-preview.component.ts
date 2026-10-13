// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, inject, ViewEncapsulation } from '@angular/core';
import { AlarmTableReportComponentConfig } from '@shared/models/report-component.models';
import { DataKey } from '@shared/models/widget.models';
import { ComponentStyle, dateFormatPreview } from '@shared/models/widget-settings.models';
import {
  AbstractReportTablePreviewComponent
} from '@home/pages/reporting/template/components/report-table-preview.component';
import { ReportTemplatePageComponent } from '@home/pages/reporting/template/report-template-page.component';
import { DatePipe } from '@angular/common';
import { alarmFields } from '@shared/models/alarm.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-alarm-table-preview',
    templateUrl: './report-table-preview.component.html',
    styleUrls: ['./report-table-preview.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class AlarmTablePreviewComponent extends AbstractReportTablePreviewComponent<AlarmTableReportComponentConfig> {

  private date = inject(DatePipe);
  private destroyRef = inject(DestroyRef);
  private templatePage = inject(ReportTemplatePageComponent);
  private timestampPreview: string;

  ngOnInit() {
    super.ngOnInit();
    this.templatePage.timeDataPattern$.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateTimestampPreview();
    });
  }

  onComponentUpdated() {
    super.onComponentUpdated();
    this.updateTimestampPreview();
  }

  private updateTimestampPreview() {
    this.timestampPreview = dateFormatPreview(this.date, this.templatePage.timeDataPattern, this.reportComponent.timewindow?.timezone);
  }

  get columns(): DataKey[] {
    return this.reportComponent.alarmSource.dataKeys;
  }

  cellContent(column: DataKey): string {
    const alarmField = alarmFields[column.name];
    if (alarmField?.time && !column.usePostProcessing) {
      return this.timestampPreview;
    } else {
      return super.cellContent(column);
    }
  }

  protected styleFromColumnSettings(column: DataKey, header = false): ComponentStyle {
    const style = super.styleFromColumnSettings(column, header);
    if (!this.isPlainFormat && !header) {
      const alarmField = alarmFields[column.name];
      if (alarmField?.time) {
        style.fontSize = style.fontSize || '9pt';
      }
      if ('severity' === column.name) {
        style.fontWeight = style.fontWeight || 'bold';
      }
    }
    return style;
  }

}
