///
/// Copyright © 2016-2025 The Thingsboard Authors
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
  selector: 'tb-update-image-attribute-widget-settings',
  templateUrl: './update-image-attribute-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class UpdateImageAttributeWidgetSettingsComponent extends WidgetSettingsComponent {

  updateImageAttributeWidgetSettingsForm: UntypedFormGroup;
  
  predefinedValues = widgetTitleAutocompleteValues;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.updateImageAttributeWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      widgetTitle: '',
      showResultMessage: true,
      displayPreview: true,
      displayClearButton: false,
      displayApplyButton: true,
      displayDiscardButton: true
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.updateImageAttributeWidgetSettingsForm = this.fb.group({
      widgetTitle: [settings.widgetTitle, []],
      showResultMessage: [settings.showResultMessage, []],
      displayPreview: [settings.displayPreview, []],
      displayClearButton: [settings.displayClearButton, []],
      displayApplyButton: [settings.displayApplyButton, []],
      displayDiscardButton: [settings.displayDiscardButton, []]
    });
  }
}
