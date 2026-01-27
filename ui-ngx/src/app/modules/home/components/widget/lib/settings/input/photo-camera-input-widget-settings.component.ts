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
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { WidgetSettings, WidgetSettingsComponent, widgetTitleAutocompleteValues } from '@shared/models/widget.models';
import { AppState } from '@core/core.state';
import { Store } from '@ngrx/store';

@Component({
  selector: 'tb-photo-camera-input-widget-settings',
  templateUrl: './photo-camera-input-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class PhotoCameraInputWidgetSettingsComponent extends WidgetSettingsComponent {

  photoCameraInputWidgetSettingsForm: UntypedFormGroup;

  predefinedValues = widgetTitleAutocompleteValues;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.photoCameraInputWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      widgetTitle: '',

      saveToGallery: false,
      usePublicGalleryLink: true,
      imageFormat: 'image/png',
      imageQuality: 0.92,
      maxWidth: 640,
      maxHeight: 480
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.photoCameraInputWidgetSettingsForm = this.fb.group({

      // General settings

      widgetTitle: [settings.widgetTitle, []],

      // Image settings
      saveToGallery: [settings.saveToGallery],
      usePublicGalleryLink: [settings.usePublicGalleryLink],
      imageFormat: [settings.imageFormat, []],
      imageQuality: [settings.imageQuality, [Validators.min(0), Validators.max(100)]],
      maxWidth: [settings.maxWidth, [Validators.min(1)]],
      maxHeight: [settings.maxHeight, [Validators.min(1)]]
    });
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    return {
      ...settings,
      saveToGallery: settings.saveToGallery ?? false,
      usePublicGalleryLink: settings.usePublicGalleryLink ?? false,
      imageQuality: settings.imageQuality * 100
    }
  }

  protected prepareOutputSettings(settings: WidgetSettings): WidgetSettings {
    return {
      ...settings,
      imageQuality: settings.imageQuality / 100
    }
  }
}
