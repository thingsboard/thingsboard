// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
    selector: 'tb-update-multiple-attributes-widget-settings',
    templateUrl: './update-multiple-attributes-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class UpdateMultipleAttributesWidgetSettingsComponent extends WidgetSettingsComponent {

  updateMultipleAttributesWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.updateMultipleAttributesWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      widgetTitle: '',
      showResultMessage: true,
      showActionButtons: true,
      updateAllValues: false,
      saveButtonLabel: '',
      resetButtonLabel: '',
      showGroupTitle: false,
      groupTitle: '',
      fieldsAlignment: 'row',
      fieldsInRow: 2,
      rowGap: 5,
      columnGap: 10
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.updateMultipleAttributesWidgetSettingsForm = this.fb.group({

      // General settings

      widgetTitle: [settings.widgetTitle, []],
      showResultMessage: [settings.showResultMessage, []],

      // Action button settings

      showActionButtons: [settings.showActionButtons, []],
      updateAllValues: [settings.updateAllValues, []],
      saveButtonLabel: [settings.saveButtonLabel, []],
      resetButtonLabel: [settings.resetButtonLabel, []],

      // Group settings

      showGroupTitle: [settings.showGroupTitle, []],
      groupTitle: [settings.groupTitle, []],

      // Fields alignment

      fieldsAlignment: [settings.fieldsAlignment, []],
      fieldsInRow: [settings.fieldsInRow, [Validators.min(1)]],

      // Layout gap

      rowGap: [settings.rowGap, [Validators.min(0)]],
      columnGap: [settings.columnGap, [Validators.min(0)]]
    });
  }

  protected validatorTriggers(): string[] {
    return ['showActionButtons', 'showGroupTitle', 'fieldsAlignment'];
  }

  protected updateValidators(emitEvent: boolean) {
    const showActionButtons: boolean = this.updateMultipleAttributesWidgetSettingsForm.get('showActionButtons').value;
    const showGroupTitle: boolean = this.updateMultipleAttributesWidgetSettingsForm.get('showGroupTitle').value;
    const fieldsAlignment: string = this.updateMultipleAttributesWidgetSettingsForm.get('fieldsAlignment').value;

    if (showActionButtons) {
      this.updateMultipleAttributesWidgetSettingsForm.get('updateAllValues').enable();
      this.updateMultipleAttributesWidgetSettingsForm.get('saveButtonLabel').enable();
      this.updateMultipleAttributesWidgetSettingsForm.get('resetButtonLabel').enable();
    } else {
      this.updateMultipleAttributesWidgetSettingsForm.get('updateAllValues').disable();
      this.updateMultipleAttributesWidgetSettingsForm.get('saveButtonLabel').disable();
      this.updateMultipleAttributesWidgetSettingsForm.get('resetButtonLabel').disable();
    }
    if (showGroupTitle) {
      this.updateMultipleAttributesWidgetSettingsForm.get('groupTitle').enable();
    } else {
      this.updateMultipleAttributesWidgetSettingsForm.get('groupTitle').disable();
    }
    if (fieldsAlignment === 'row') {
      this.updateMultipleAttributesWidgetSettingsForm.get('fieldsInRow').enable();
      this.updateMultipleAttributesWidgetSettingsForm.get('columnGap').enable();
    } else {
      this.updateMultipleAttributesWidgetSettingsForm.get('fieldsInRow').disable();
      this.updateMultipleAttributesWidgetSettingsForm.get('columnGap').disable();
    }
    this.updateMultipleAttributesWidgetSettingsForm.get('updateAllValues').updateValueAndValidity({emitEvent});
    this.updateMultipleAttributesWidgetSettingsForm.get('saveButtonLabel').updateValueAndValidity({emitEvent});
    this.updateMultipleAttributesWidgetSettingsForm.get('resetButtonLabel').updateValueAndValidity({emitEvent});
    this.updateMultipleAttributesWidgetSettingsForm.get('groupTitle').updateValueAndValidity({emitEvent});
    this.updateMultipleAttributesWidgetSettingsForm.get('fieldsInRow').updateValueAndValidity({emitEvent});
    this.updateMultipleAttributesWidgetSettingsForm.get('columnGap').updateValueAndValidity({emitEvent});
  }
}
