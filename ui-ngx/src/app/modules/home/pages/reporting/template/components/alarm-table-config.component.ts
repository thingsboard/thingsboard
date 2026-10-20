// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ViewEncapsulation } from '@angular/core';
import { FormGroup } from '@angular/forms';
import {
  AbstractReportComponentConfig
} from '@home/pages/reporting/template/components/report-component-config.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormProperty } from '@shared/models/dynamic-form.models';
import {
  DataKeySettingsFormFunction
} from '@home/components/widget/lib/settings/common/key/data-keys.component.models';
import {
  AlarmTableReportComponentConfig,
  ReportDataKeySettingsType,
  SeverityColumnSettingsForm,
  TableReportColumnSettingsForm,
  TimeColumnSettingsForm
} from '@shared/models/report-component.models';
import { DataKey, Datasource, WidgetConfigMode } from '@shared/models/widget.models';
import { alarmFields } from '@shared/models/alarm.models';
import { pairwise, startWith } from 'rxjs';

@Component({
    selector: 'tb-alarm-table-config',
    templateUrl: './alarm-table-config.component.html',
    styleUrls: ['./report-component-config.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class AlarmTableConfigComponent extends AbstractReportComponentConfig<AlarmTableReportComponentConfig> {

  get columnLabels(): string[] {
    const columns: DataKey[] = this.reportConfigForm.get('columns').value;
    return (columns || []).map(key => key.label);
  }

  columnNameChanged: [string, string];

  settingsTab: 'data' | 'layout' = 'data';

  basicMode = WidgetConfigMode.basic;

  dataKeySettingsFormFunction: DataKeySettingsFormFunction = this.getDataKeySettingsForm.bind(this);

  private getDataKeySettingsForm(key: DataKey): FormProperty[] {
    const alarmField = alarmFields[key.name];
    if (alarmField?.time) {
      return TimeColumnSettingsForm;
    } else if (key.name === 'severity') {
      return SeverityColumnSettingsForm;
    }
    return TableReportColumnSettingsForm;
  }

  protected buildForm(reportComponentConfig: AlarmTableReportComponentConfig): FormGroup {
    const form = this.fb.group({
      timewindow: [reportComponentConfig.timewindow, []],
      dataSources: [[reportComponentConfig.alarmSource], []],
      alarmFilterConfig: [reportComponentConfig.alarmSource.alarmFilterConfig, []],
      showTableHeading: [reportComponentConfig.showTableHeading, []],
      tableHeading: [reportComponentConfig.tableHeading, []],
      tableSortOrder: [reportComponentConfig.tableSortOrder, []],
      columns: [this.getColumns(reportComponentConfig.alarmSource), []],
    });
    form.get('showTableHeading').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(form);
    });
    form.get('columns').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef),
      startWith(this.getColumns(reportComponentConfig.alarmSource)),
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
    this.updateValidators(form);
    return form;
  }

  protected prepareOutputConfig(config: any): AlarmTableReportComponentConfig {
    config.alarmSource = config.dataSources[0];
    delete config.dataSources;
    config.alarmSource.alarmFilterConfig = config.alarmFilterConfig;
    delete config.alarmFilterConfig;
    this.setColumns(config.columns, config.alarmSource);
    delete config.columns;
    return config;
  }

  private getColumns(alarmSource?: Datasource): DataKey[] {
    if (alarmSource) {
      return alarmSource.dataKeys || [];
    }
    return [];
  }

  private setColumns(columns: DataKey[], alarmSource?: Datasource) {
    if (alarmSource) {
      columns.forEach(key => {
        if (key?.settings) {
          key.settings.type = ReportDataKeySettingsType.COLUMN;
        }
      });
      alarmSource.dataKeys = columns;
    }
  }

  private updateValidators(form: FormGroup) {
    const showTableHeading: boolean = form.get('showTableHeading').value;
    if (showTableHeading) {
      form.get('tableHeading').enable({emitEvent: false});
    } else {
      form.get('tableHeading').disable({emitEvent: false});
    }
  }
}
