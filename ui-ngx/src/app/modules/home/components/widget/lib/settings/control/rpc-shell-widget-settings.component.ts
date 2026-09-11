// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
    selector: 'tb-rpc-shell-widget-settings',
    templateUrl: './rpc-shell-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class RpcShellWidgetSettingsComponent extends WidgetSettingsComponent {

  rpcShellWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.rpcShellWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      requestTimeout: 500
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.rpcShellWidgetSettingsForm = this.fb.group({
      // RPC settings
      requestTimeout: [settings.requestTimeout, [Validators.min(0), Validators.required]]
    });
  }
}
