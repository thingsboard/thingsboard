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
import { WidgetSettings, WidgetSettingsComponent, widgetTitleAutocompleteValues } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
  selector: 'tb-update-location-attribute-widget-settings',
  templateUrl: './update-location-attribute-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class UpdateLocationAttributeWidgetSettingsComponent extends WidgetSettingsComponent {

  updateLocationAttributeWidgetSettingsForm: UntypedFormGroup;
  
  predefinedValues = widgetTitleAutocompleteValues;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.updateLocationAttributeWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      widgetTitle: '',
      showResultMessage: true,
      latKeyName: 'latitude',
      lngKeyName: 'longitude',
      showGetLocation: true,
      enableHighAccuracy: false,

      showLabel: true,
      latLabel: '',
      lngLabel: '',
      inputFieldsAlignment: 'column',
      isLatRequired: true,
      isLngRequired: true,
      requiredErrorMessage: ''
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.updateLocationAttributeWidgetSettingsForm = this.fb.group({

      // General settings

      widgetTitle: [settings.widgetTitle, []],
      showResultMessage: [settings.showResultMessage, []],
      latKeyName: [settings.latKeyName, []],
      lngKeyName: [settings.lngKeyName, []],
      showGetLocation: [settings.showGetLocation, []],
      enableHighAccuracy: [settings.enableHighAccuracy, []],

      // Location fields settings

      showLabel: [settings.showLabel, []],
      latLabel: [settings.latLabel, []],
      lngLabel: [settings.lngLabel, []],
      inputFieldsAlignment: [settings.inputFieldsAlignment, []],
      isLatRequired: [settings.isLatRequired, []],
      isLngRequired: [settings.isLngRequired, []],
      requiredErrorMessage: [settings.requiredErrorMessage, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['showLabel', 'isLatRequired', 'isLngRequired'];
  }

  protected updateValidators(emitEvent: boolean) {
    const showLabel: boolean = this.updateLocationAttributeWidgetSettingsForm.get('showLabel').value;
    const isLatRequired: boolean = this.updateLocationAttributeWidgetSettingsForm.get('isLatRequired').value;
    const isLngRequired: boolean = this.updateLocationAttributeWidgetSettingsForm.get('isLngRequired').value;

    if (showLabel) {
      this.updateLocationAttributeWidgetSettingsForm.get('latLabel').enable();
      this.updateLocationAttributeWidgetSettingsForm.get('lngLabel').enable();
    } else {
      this.updateLocationAttributeWidgetSettingsForm.get('latLabel').disable();
      this.updateLocationAttributeWidgetSettingsForm.get('lngLabel').disable();
    }
    if (isLatRequired || isLngRequired) {
      this.updateLocationAttributeWidgetSettingsForm.get('requiredErrorMessage').enable();
    } else {
      this.updateLocationAttributeWidgetSettingsForm.get('requiredErrorMessage').disable();
    }
    this.updateLocationAttributeWidgetSettingsForm.get('latLabel').updateValueAndValidity({emitEvent});
    this.updateLocationAttributeWidgetSettingsForm.get('lngLabel').updateValueAndValidity({emitEvent});
    this.updateLocationAttributeWidgetSettingsForm.get('requiredErrorMessage').updateValueAndValidity({emitEvent});
  }
}
