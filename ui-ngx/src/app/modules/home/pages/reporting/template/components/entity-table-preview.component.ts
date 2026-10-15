// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, inject, ViewEncapsulation } from '@angular/core';
import { EntityTableReportComponentConfig } from '@shared/models/report-component.models';
import { DataKey, Datasource } from '@shared/models/widget.models';
import {
  AbstractReportTablePreviewComponent
} from '@home/pages/reporting/template/components/report-table-preview.component';
import { DatePipe } from '@angular/common';
import { ReportTemplatePageComponent } from '@home/pages/reporting/template/report-template-page.component';
import { ComponentStyle, dateFormatPreview } from '@shared/models/widget-settings.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-entity-table-preview',
    templateUrl: './report-table-preview.component.html',
    styleUrls: ['./report-table-preview.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class EntityTablePreviewComponent extends AbstractReportTablePreviewComponent<EntityTableReportComponentConfig> {

  private date = inject(DatePipe);
  private destroyRef = inject(DestroyRef);
  private templatePage = inject(ReportTemplatePageComponent);
  private timestampPreview = dateFormatPreview(this.date, this.templatePage.timeDataPattern);

  ngOnInit() {
    super.ngOnInit();
    this.templatePage.timeDataPattern$.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.timestampPreview = dateFormatPreview(this.date, this.templatePage.timeDataPattern);;
    });
  }

  cellContent(column: DataKey): string {
    if (column.name === 'createdTime' && !column.usePostProcessing) {
      return this.timestampPreview;
    } else {
      return super.cellContent(column);
    }
  }

  protected styleFromColumnSettings(column: DataKey, header = false): ComponentStyle {
    const style = super.styleFromColumnSettings(column, header);
    if (!this.isPlainFormat && !header) {
      if (column.name === 'createdTime') {
        style.fontSize = style.fontSize || '9pt';
      }
    }
    return style;
  }

  get columns(): DataKey[] {
    const datasources: Datasource[] = this.reportComponent.dataSources;
    if (datasources && datasources.length) {
      return datasources[0].dataKeys || [];
    }
    return [];
  }

}
