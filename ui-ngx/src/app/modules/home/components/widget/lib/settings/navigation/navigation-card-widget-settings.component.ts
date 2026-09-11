// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
    selector: 'tb-navigation-card-widget-settings',
    templateUrl: './navigation-card-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class NavigationCardWidgetSettingsComponent extends WidgetSettingsComponent {

  navigationCardWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.navigationCardWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      name: '{i18n:device.devices}',
      icon: 'devices_other',
      path: '/devices'
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.navigationCardWidgetSettingsForm = this.fb.group({
      name: [settings.name, [Validators.required]],
      icon: [settings.icon, [Validators.required]],
      path: [settings.path, [Validators.required]]
    });
  }
}
