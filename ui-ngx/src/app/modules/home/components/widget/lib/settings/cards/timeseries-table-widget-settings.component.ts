///
/// Copyright © 2016-2023 The Thingsboard Authors
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
  selector: 'tb-timeseries-table-widget-settings',
  templateUrl: './timeseries-table-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class TimeseriesTableWidgetSettingsComponent extends WidgetSettingsComponent {

  timeseriesTableWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.timeseriesTableWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      enableSearch: true,
      enableSelectColumnDisplay: true,
      enableStickyHeader: true,
      enableStickyAction: true,
      reserveSpaceForHiddenAction: 'true',
      showTimestamp: true,
      showMilliseconds: false,
      displayPagination: true,
      useEntityLabel: false,
      defaultPageSize: 10,
      hideEmptyLines: false,
      disableStickyHeader: false,
      useRowStyleFunction: false,
      rowStyleFunction: ''
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.timeseriesTableWidgetSettingsForm = this.fb.group({
      enableSearch: [settings.enableSearch, []],
      enableSelectColumnDisplay: [settings.enableSelectColumnDisplay, []],
      enableStickyHeader: [settings.enableStickyHeader, []],
      enableStickyAction: [settings.enableStickyAction, []],
      reserveSpaceForHiddenAction: [settings.reserveSpaceForHiddenAction, []],
      showTimestamp: [settings.showTimestamp, []],
      showMilliseconds: [settings.showMilliseconds, []],
      displayPagination: [settings.displayPagination, []],
      useEntityLabel: [settings.useEntityLabel, []],
      defaultPageSize: [settings.defaultPageSize, [Validators.min(1)]],
      hideEmptyLines: [settings.hideEmptyLines, []],
      disableStickyHeader: [settings.disableStickyHeader, []],
      useRowStyleFunction: [settings.useRowStyleFunction, []],
      rowStyleFunction: [settings.rowStyleFunction, [Validators.required]]
    });
  }

  protected validatorTriggers(): string[] {
    return ['useRowStyleFunction', 'displayPagination'];
  }

  protected updateValidators(emitEvent: boolean) {
    const useRowStyleFunction: boolean = this.timeseriesTableWidgetSettingsForm.get('useRowStyleFunction').value;
    const displayPagination: boolean = this.timeseriesTableWidgetSettingsForm.get('displayPagination').value;
    if (useRowStyleFunction) {
      this.timeseriesTableWidgetSettingsForm.get('rowStyleFunction').enable();
    } else {
      this.timeseriesTableWidgetSettingsForm.get('rowStyleFunction').disable();
    }
    if (displayPagination) {
      this.timeseriesTableWidgetSettingsForm.get('defaultPageSize').enable();
    } else {
      this.timeseriesTableWidgetSettingsForm.get('defaultPageSize').disable();
    }
    this.timeseriesTableWidgetSettingsForm.get('rowStyleFunction').updateValueAndValidity({emitEvent});
    this.timeseriesTableWidgetSettingsForm.get('defaultPageSize').updateValueAndValidity({emitEvent});
  }

}
