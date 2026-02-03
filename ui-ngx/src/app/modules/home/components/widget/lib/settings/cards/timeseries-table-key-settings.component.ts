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
    selector: 'tb-timeseries-table-key-settings',
    templateUrl: './timeseries-table-key-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class TimeseriesTableKeySettingsComponent extends WidgetSettingsComponent {

  timeseriesTableKeySettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.timeseriesTableKeySettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
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
    this.timeseriesTableKeySettingsForm = this.fb.group({
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
    return ['useCellStyleFunction', 'useCellContentFunction'];
  }

  protected updateValidators(emitEvent: boolean) {
    const useCellStyleFunction: boolean = this.timeseriesTableKeySettingsForm.get('useCellStyleFunction').value;
    const useCellContentFunction: boolean = this.timeseriesTableKeySettingsForm.get('useCellContentFunction').value;
    if (useCellStyleFunction) {
      this.timeseriesTableKeySettingsForm.get('cellStyleFunction').enable();
    } else {
      this.timeseriesTableKeySettingsForm.get('cellStyleFunction').disable();
    }
    if (useCellContentFunction) {
      this.timeseriesTableKeySettingsForm.get('cellContentFunction').enable();
    } else {
      this.timeseriesTableKeySettingsForm.get('cellContentFunction').disable();
    }
    this.timeseriesTableKeySettingsForm.get('cellStyleFunction').updateValueAndValidity({emitEvent});
    this.timeseriesTableKeySettingsForm.get('cellContentFunction').updateValueAndValidity({emitEvent});
  }

}
