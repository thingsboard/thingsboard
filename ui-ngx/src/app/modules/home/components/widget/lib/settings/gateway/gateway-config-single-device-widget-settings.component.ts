// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
    selector: 'tb-gateway-config-single-device-widget-settings',
    templateUrl: './gateway-config-single-device-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class GatewayConfigSingleDeviceWidgetSettingsComponent extends WidgetSettingsComponent {

  gatewayConfigSingleDeviceWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.gatewayConfigSingleDeviceWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      gatewayTitle: 'Gateway configuration (Single device)',
      readOnly: false
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.gatewayConfigSingleDeviceWidgetSettingsForm = this.fb.group({
      gatewayTitle: [settings.gatewayTitle, []],
      readOnly: [settings.readOnly, []]
    });
  }
}
