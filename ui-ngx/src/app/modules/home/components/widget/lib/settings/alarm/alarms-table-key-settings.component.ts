// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
    selector: 'tb-alarms-table-key-settings',
    templateUrl: './alarms-table-key-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class AlarmsTableKeySettingsComponent extends WidgetSettingsComponent {

  alarmsTableKeySettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.alarmsTableKeySettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      customTitle: '',
      columnWidth: '0px',
      useCellStyleFunction: false,
      cellStyleFunction: '',
      useCellContentFunction: false,
      useCellContentFunctionOnExport: true,
      cellContentFunction: '',
      defaultColumnVisibility: 'visible',
      columnSelectionToDisplay: 'enabled',
      columnExportOption: 'onlyVisible',
      disableSorting: false
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.alarmsTableKeySettingsForm = this.fb.group({
      customTitle: [settings.customTitle, []],
      columnWidth: [settings.columnWidth, []],
      useCellStyleFunction: [settings.useCellStyleFunction, []],
      cellStyleFunction: [settings.cellStyleFunction, [Validators.required]],
      useCellContentFunction: [settings.useCellContentFunction, []],
      useCellContentFunctionOnExport: [settings.useCellContentFunctionOnExport, []],
      cellContentFunction: [settings.cellContentFunction, [Validators.required]],
      defaultColumnVisibility: [settings.defaultColumnVisibility, []],
      columnSelectionToDisplay: [settings.columnSelectionToDisplay, []],
      columnExportOption: [settings.columnExportOption, []],
      disableSorting: [settings.disableSorting, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['useCellStyleFunction', 'useCellContentFunction'];
  }

  protected updateValidators(emitEvent: boolean) {
    const useCellStyleFunction: boolean = this.alarmsTableKeySettingsForm.get('useCellStyleFunction').value;
    const useCellContentFunction: boolean = this.alarmsTableKeySettingsForm.get('useCellContentFunction').value;
    if (useCellStyleFunction) {
      this.alarmsTableKeySettingsForm.get('cellStyleFunction').enable();
    } else {
      this.alarmsTableKeySettingsForm.get('cellStyleFunction').disable();
    }
    if (useCellContentFunction) {
      this.alarmsTableKeySettingsForm.get('cellContentFunction').enable();
      this.alarmsTableKeySettingsForm.get('useCellContentFunctionOnExport').enable();
    } else {
      this.alarmsTableKeySettingsForm.get('cellContentFunction').disable();
      this.alarmsTableKeySettingsForm.get('useCellContentFunctionOnExport').disable();
    }
    this.alarmsTableKeySettingsForm.get('cellStyleFunction').updateValueAndValidity({emitEvent});
    this.alarmsTableKeySettingsForm.get('cellContentFunction').updateValueAndValidity({emitEvent});
  }

}
