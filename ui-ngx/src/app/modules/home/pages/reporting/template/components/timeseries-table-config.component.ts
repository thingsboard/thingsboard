// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, inject, ViewEncapsulation } from '@angular/core';
import { FormGroup } from '@angular/forms';
import {
  AbstractReportComponentConfig
} from '@home/pages/reporting/template/components/report-component-config.component';
import { deepClone } from '@core/utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import {
  DynamicFormDialogComponent,
  DynamicFormDialogData
} from '@home/components/widget/lib/settings/common/dynamic-form/dynamic-form-dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { merge, pairwise, startWith } from 'rxjs';
import {
  ReportDataKeySettingsType,
  TableReportColumnSettings,
  TableReportColumnSettingsForm,
  TimeColumnSettingsForm,
  TimeseriesTableReportComponentConfig
} from '@shared/models/report-component.models';
import { DataKey, Datasource, WidgetConfigMode } from '@shared/models/widget.models';
import {
  DataKeySettingsFormFunction
} from '@home/components/widget/lib/settings/common/key/data-keys.component.models';
import { FormProperty } from '@shared/models/dynamic-form.models';

@Component({
    selector: 'tb-timeseries-table-config',
    templateUrl: './timeseries-table-config.component.html',
    styleUrls: ['./report-component-config.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class TimeseriesTableConfigComponent extends AbstractReportComponentConfig<TimeseriesTableReportComponentConfig> {

  get columnLabels(): string[] {
    const result: string[] = [];
    if (this.reportConfigForm.get('showTimestamp').value) {
      result.push(this.reportConfigForm.get('timestampLabel').value);
    }
    const columns: DataKey[] = this.reportConfigForm.get('columns').value;
    return [...result, ...(columns || []).map(key => key.label)];
  }

  columnNameChanged: [string, string];

  settingsTab: 'data' | 'layout' = 'data';

  basicMode = WidgetConfigMode.basic;

  dataKeySettingsFormFunction: DataKeySettingsFormFunction = this.getDataKeySettingsForm.bind(this);

  private dialog =  inject(MatDialog);
  private translate = inject(TranslateService);

  private getDataKeySettingsForm(key: DataKey): FormProperty[] {
    if (['ts', 'createdTime'].includes(key.name)) {
      return TimeColumnSettingsForm;
    }
    return TableReportColumnSettingsForm;
  }

  protected buildForm(reportComponentConfig: TimeseriesTableReportComponentConfig): FormGroup {
    const form = this.fb.group({
      timewindow: [reportComponentConfig.timewindow, []],
      dataSources: [reportComponentConfig.dataSources, []],
      showTableHeading: [reportComponentConfig.showTableHeading, []],
      tableHeading: [reportComponentConfig.tableHeading, []],
      showTimestamp: [reportComponentConfig.showTimestamp, []],
      timestampLabel: [reportComponentConfig.timestampLabel, []],
      timestampPattern: [reportComponentConfig.timestampPattern, []],
      timestampColumnSettings: [reportComponentConfig.timestampColumnSettings, []],
      tableSortOrder: [reportComponentConfig.tableSortOrder, []],
      columns: [this.getColumns(reportComponentConfig.dataSources), []],
    });
    merge(form.get('showTimestamp').valueChanges, form.get('showTableHeading').valueChanges).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(form);
    });
    form.get('columns').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef),
      startWith(this.getColumns(reportComponentConfig.dataSources)),
      pairwise()
    ).subscribe(([prev, current]) => {
      const tableSortOrder = form.get("tableSortOrder").value;
      if (tableSortOrder && tableSortOrder.column) {
        const oldColumn = prev.find(c => c.label === tableSortOrder.column);
        if (oldColumn) {
          const newColumn = current.find(c => c.name === oldColumn.name);
          if (newColumn && newColumn.label !== tableSortOrder.column) {
            this.columnNameChanged = [oldColumn.label, newColumn.label];
          }
        }
      }
    });
    form.get('timestampLabel').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef),
      startWith(reportComponentConfig.timestampLabel),
      pairwise()
    ).subscribe(([prevLabel, currentLabel]) => {
      const tableSortOrder = form.get("tableSortOrder").value;
      if (tableSortOrder && tableSortOrder.column === prevLabel) {
        this.columnNameChanged = [prevLabel, currentLabel];
      }
    });
    this.updateValidators(form);
    return form;
  }

  protected prepareOutputConfig(config: any): any {
    this.setColumns(config.columns, config.dataSources);
    delete config.columns;
    return config;
  }

  editTimestampColumnSettings() {
    const timestampColumnSettings: TableReportColumnSettings = this.reportConfigForm.get('timestampColumnSettings').value;
    this.dialog.open<DynamicFormDialogComponent<TableReportColumnSettings>,
      DynamicFormDialogData<TableReportColumnSettings>, TableReportColumnSettings>(DynamicFormDialogComponent<TableReportColumnSettings>, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        title: this.translate.instant('report-template.component.timeseries-table.timestamp-column-settings'),
        properties: TimeColumnSettingsForm,
        value: timestampColumnSettings
      }
    }).afterClosed().subscribe(
      (res) => {
        if (res) {
          this.reportConfigForm.get('timestampColumnSettings').patchValue(res);
          this.reportConfigForm.markAsDirty();
        }
      }
    );
  }

  private getColumns(datasources?: Datasource[]): DataKey[] {
    if (datasources && datasources.length) {
      const dataKeys = deepClone(datasources[0].dataKeys) || [];
      dataKeys.forEach(k => {
        (k as any).latest = false;
      });
      const latestDataKeys = deepClone(datasources[0].latestDataKeys) || [];
      latestDataKeys.forEach(k => {
        (k as any).latest = true;
      });
      return dataKeys.concat(latestDataKeys);
    }
    return [];
  }

  private setColumns(columns: DataKey[], datasources?: Datasource[]) {
    if (datasources && datasources.length) {
      columns.forEach(key => {
        if (key?.settings) {
          key.settings.type = ReportDataKeySettingsType.COLUMN;
        }
      });
      const dataKeys = deepClone(columns.filter(c => !(c as any).latest));
      dataKeys.forEach(k => delete (k as any).latest);
      const latestDataKeys = deepClone(columns.filter(c => (c as any).latest));
      latestDataKeys.forEach(k => delete (k as any).latest);
      datasources[0].dataKeys = dataKeys;
      datasources[0].latestDataKeys = latestDataKeys;
    }
  }

  private updateValidators(form: FormGroup) {
    const showTimestamp: boolean = form.get('showTimestamp').value;
    const showTableHeading: boolean = form.get('showTableHeading').value;
    if (showTimestamp) {
      form.get('timestampLabel').enable({emitEvent: false});
      form.get('timestampPattern').enable({emitEvent: false});
    } else {
      form.get('timestampLabel').disable({emitEvent: false});
      form.get('timestampPattern').disable({emitEvent: false});
    }
    if (showTableHeading) {
      form.get('tableHeading').enable({emitEvent: false});
    } else {
      form.get('tableHeading').disable({emitEvent: false});
    }
  }

}
