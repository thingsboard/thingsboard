// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ViewEncapsulation } from '@angular/core';
import { FormGroup } from '@angular/forms';
import {
  AbstractReportComponentConfig
} from '@home/pages/reporting/template/components/report-component-config.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  EntityTableReportComponentConfig,
  ReportDataKeySettingsType,
  TableReportColumnSettingsForm,
  TimeColumnSettingsForm
} from '@shared/models/report-component.models';
import { DataKey, Datasource, WidgetConfigMode } from '@shared/models/widget.models';
import {
  DataKeySettingsFormFunction
} from '@home/components/widget/lib/settings/common/key/data-keys.component.models';
import { FormProperty } from '@shared/models/dynamic-form.models';
import { pairwise, startWith } from 'rxjs';

@Component({
    selector: 'tb-entity-table-config',
    templateUrl: './entity-table-config.component.html',
    styleUrls: ['./report-component-config.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class EntityTableConfigComponent extends AbstractReportComponentConfig<EntityTableReportComponentConfig> {

  get columnLabels(): string[] {
    const columns: DataKey[] = this.reportConfigForm.get('columns').value;
    return (columns || []).map(key => key.label);
  }

  columnNameChanged: [string, string];

  settingsTab: 'data' | 'layout' = 'data';

  basicMode = WidgetConfigMode.basic;

  dataKeySettingsFormFunction: DataKeySettingsFormFunction = this.getDataKeySettingsForm.bind(this);

  private getDataKeySettingsForm(key: DataKey): FormProperty[] {
    if (key.name === 'createdTime') {
      return TimeColumnSettingsForm;
    }
    return TableReportColumnSettingsForm;
  }

  protected buildForm(reportComponentConfig: EntityTableReportComponentConfig): FormGroup {
    const form = this.fb.group({
      showTableHeading: [reportComponentConfig.showTableHeading, []],
      tableHeading: [reportComponentConfig.tableHeading, []],
      tableSortOrder: [reportComponentConfig.tableSortOrder, []],
      dataSources: [reportComponentConfig.dataSources, []],
      columns: [this.getColumns(reportComponentConfig.dataSources), []],
    });
    form.get('showTableHeading').valueChanges.pipe(
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
    this.updateValidators(form);
    return form;
  }

  protected prepareOutputConfig(config: any): any {
    this.setColumns(config.columns, config.dataSources);
    delete config.columns;
    return config;
  }

  private getColumns(datasources?: Datasource[]): DataKey[] {
    if (datasources && datasources.length) {
      return datasources[0].dataKeys || [];
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
      datasources[0].dataKeys = columns;
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
