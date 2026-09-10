// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { deepClone } from '@core/utils';
import {
  updateAttributeGeneralDefaultSettings
} from '@home/components/widget/lib/settings/input/update-attribute-general-settings.component';

@Component({
    selector: 'tb-update-date-attribute-widget-settings',
    templateUrl: './update-date-attribute-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class UpdateDateAttributeWidgetSettingsComponent extends WidgetSettingsComponent {

  updateDateAttributeWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.updateDateAttributeWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      ...updateAttributeGeneralDefaultSettings(false),
      showTimeInput: true
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.updateDateAttributeWidgetSettingsForm = this.fb.group({
      updateAttributeGeneralSettings: [settings.updateAttributeGeneralSettings, []],
      showTimeInput: [settings.showTimeInput, []]
    });
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    const updateAttributeGeneralSettings = deepClone(settings, ['showTimeInput']);
    return {
      updateAttributeGeneralSettings,
      showTimeInput: settings.showTimeInput
    };
  }

  protected prepareOutputSettings(settings: any): WidgetSettings {
    return {
      ...settings.updateAttributeGeneralSettings,
      showTimeInput: settings.showTimeInput
    };
  }
}
