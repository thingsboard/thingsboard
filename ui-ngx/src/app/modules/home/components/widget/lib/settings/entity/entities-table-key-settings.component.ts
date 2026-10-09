// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
    selector: 'tb-entities-table-key-settings',
    templateUrl: './entities-table-key-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class EntitiesTableKeySettingsComponent extends WidgetSettingsComponent {

  entitiesTableKeySettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.entitiesTableKeySettingsForm;
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
    this.entitiesTableKeySettingsForm = this.fb.group({
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
    const useCellStyleFunction: boolean = this.entitiesTableKeySettingsForm.get('useCellStyleFunction').value;
    const useCellContentFunction: boolean = this.entitiesTableKeySettingsForm.get('useCellContentFunction').value;
    if (useCellStyleFunction) {
      this.entitiesTableKeySettingsForm.get('cellStyleFunction').enable();
    } else {
      this.entitiesTableKeySettingsForm.get('cellStyleFunction').disable();
    }
    if (useCellContentFunction) {
      this.entitiesTableKeySettingsForm.get('cellContentFunction').enable();
      this.entitiesTableKeySettingsForm.get('useCellContentFunctionOnExport').enable();
    } else {
      this.entitiesTableKeySettingsForm.get('cellContentFunction').disable();
      this.entitiesTableKeySettingsForm.get('useCellContentFunctionOnExport').disable();
    }
    this.entitiesTableKeySettingsForm.get('cellStyleFunction').updateValueAndValidity({emitEvent});
    this.entitiesTableKeySettingsForm.get('cellContentFunction').updateValueAndValidity({emitEvent});
  }

}
