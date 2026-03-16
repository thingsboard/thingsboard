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
import { ContentType } from '@shared/models/constants';

@Component({
    selector: 'tb-update-device-attribute-widget-settings',
    templateUrl: './update-device-attribute-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class UpdateDeviceAttributeWidgetSettingsComponent extends WidgetSettingsComponent {

  updateDeviceAttributeWidgetSettingsForm: UntypedFormGroup;

  contentTypes = ContentType;

  predefinedValues = widgetTitleAutocompleteValues;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.updateDeviceAttributeWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      title: '',
      buttonText: 'Update device attribute',
      entityAttributeType: 'SERVER_SCOPE',
      entityParameters: '{}',
      styleButton: {
        isRaised: true,
        isPrimary: false,
        bgColor: null,
        textColor: null
      }
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.updateDeviceAttributeWidgetSettingsForm = this.fb.group({
      title: [settings.title, []],
      buttonText: [settings.buttonText, []],
      entityAttributeType: [settings.entityAttributeType, []],
      entityParameters: [settings.entityParameters, []],
      styleButton: [settings.styleButton, []]
    });
  }
}
