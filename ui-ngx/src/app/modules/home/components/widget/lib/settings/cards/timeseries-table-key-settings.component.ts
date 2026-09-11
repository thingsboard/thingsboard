// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
