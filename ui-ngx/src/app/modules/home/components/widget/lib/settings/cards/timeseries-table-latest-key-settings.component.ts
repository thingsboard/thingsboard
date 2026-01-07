///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
  selector: 'tb-timeseries-table-latest-key-settings',
  templateUrl: './timeseries-table-latest-key-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class TimeseriesTableLatestKeySettingsComponent extends WidgetSettingsComponent {

  timeseriesTableLatestKeySettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.timeseriesTableLatestKeySettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      show: true,
      useCellStyleFunction: false,
      cellStyleFunction: '',
      useCellContentFunction: false,
      cellContentFunction: '',
      defaultColumnVisibility: 'visible',
      columnSelectionToDisplay: 'enabled',
      disableSorting: false
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.timeseriesTableLatestKeySettingsForm = this.fb.group({
      show: [settings.show, []],
      order: [settings.order, []],
      useCellStyleFunction: [settings.useCellStyleFunction, []],
      cellStyleFunction: [settings.cellStyleFunction, [Validators.required]],
      useCellContentFunction: [settings.useCellContentFunction, []],
      cellContentFunction: [settings.cellContentFunction, [Validators.required]],
      defaultColumnVisibility: [settings.defaultColumnVisibility, []],
      columnSelectionToDisplay: [settings.columnSelectionToDisplay, []],
      disableSorting: [settings.disableSorting, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['show', 'useCellStyleFunction', 'useCellContentFunction'];
  }

  protected updateValidators(emitEvent: boolean) {
    const show: boolean = this.timeseriesTableLatestKeySettingsForm.get('show').value;
    if (show) {
      this.timeseriesTableLatestKeySettingsForm.get('order').enable();
      this.timeseriesTableLatestKeySettingsForm.get('useCellStyleFunction').enable({emitEvent: false});
      this.timeseriesTableLatestKeySettingsForm.get('useCellContentFunction').enable({emitEvent: false});
      const useCellStyleFunction: boolean = this.timeseriesTableLatestKeySettingsForm.get('useCellStyleFunction').value;
      const useCellContentFunction: boolean = this.timeseriesTableLatestKeySettingsForm.get('useCellContentFunction').value;
      if (useCellStyleFunction) {
        this.timeseriesTableLatestKeySettingsForm.get('cellStyleFunction').enable();
      } else {
        this.timeseriesTableLatestKeySettingsForm.get('cellStyleFunction').disable();
      }
      if (useCellContentFunction) {
        this.timeseriesTableLatestKeySettingsForm.get('cellContentFunction').enable();
      } else {
        this.timeseriesTableLatestKeySettingsForm.get('cellContentFunction').disable();
      }
    } else {
      this.timeseriesTableLatestKeySettingsForm.get('order').disable();
      this.timeseriesTableLatestKeySettingsForm.get('useCellStyleFunction').disable({emitEvent: false});
      this.timeseriesTableLatestKeySettingsForm.get('cellStyleFunction').disable();
      this.timeseriesTableLatestKeySettingsForm.get('useCellContentFunction').disable({emitEvent: false});
      this.timeseriesTableLatestKeySettingsForm.get('cellContentFunction').disable();
    }
    this.timeseriesTableLatestKeySettingsForm.get('order').updateValueAndValidity({emitEvent});
    this.timeseriesTableLatestKeySettingsForm.get('cellStyleFunction').updateValueAndValidity({emitEvent});
    this.timeseriesTableLatestKeySettingsForm.get('cellContentFunction').updateValueAndValidity({emitEvent});
  }

}
