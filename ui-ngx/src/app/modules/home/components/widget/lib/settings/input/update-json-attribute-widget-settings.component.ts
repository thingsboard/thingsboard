// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
    selector: 'tb-update-json-attribute-widget-settings',
    templateUrl: './update-json-attribute-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class UpdateJsonAttributeWidgetSettingsComponent extends WidgetSettingsComponent {

  updateJsonAttributeWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.updateJsonAttributeWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      widgetTitle: '',
      showLabel: true,
      labelValue: '',
      showResultMessage: true,

      widgetMode: 'ATTRIBUTE',
      attributeScope: 'SERVER_SCOPE',
      attributeRequired: true
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.updateJsonAttributeWidgetSettingsForm = this.fb.group({

      // General settings

      widgetTitle: [settings.widgetTitle, []],
      showLabel: [settings.showLabel, []],
      labelValue: [settings.labelValue, []],
      showResultMessage: [settings.showResultMessage, []],

      // Attribute settings

      widgetMode: [settings.widgetMode, []],
      attributeScope: [settings.attributeScope, []],
      attributeRequired: [settings.attributeRequired, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['showLabel', 'widgetMode'];
  }

  protected updateValidators(emitEvent: boolean) {
    const showLabel: boolean = this.updateJsonAttributeWidgetSettingsForm.get('showLabel').value;
    const widgetMode: string = this.updateJsonAttributeWidgetSettingsForm.get('widgetMode').value;

    if (showLabel) {
      this.updateJsonAttributeWidgetSettingsForm.get('labelValue').enable();
    } else {
      this.updateJsonAttributeWidgetSettingsForm.get('labelValue').disable();
    }
    if (widgetMode === 'ATTRIBUTE') {
      this.updateJsonAttributeWidgetSettingsForm.get('attributeScope').enable();
    } else {
      this.updateJsonAttributeWidgetSettingsForm.get('attributeScope').disable();
    }
    this.updateJsonAttributeWidgetSettingsForm.get('labelValue').updateValueAndValidity({emitEvent});
    this.updateJsonAttributeWidgetSettingsForm.get('attributeScope').updateValueAndValidity({emitEvent});
  }
}
