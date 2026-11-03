// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
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
