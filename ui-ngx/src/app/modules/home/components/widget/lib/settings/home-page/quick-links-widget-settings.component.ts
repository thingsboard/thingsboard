// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
    selector: 'tb-quick-links-widget-settings',
    templateUrl: './quick-links-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class QuickLinksWidgetSettingsComponent extends WidgetSettingsComponent {

  quickLinksWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.quickLinksWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      columns: 3
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.quickLinksWidgetSettingsForm = this.fb.group({
      columns: [settings.columns, [Validators.required, Validators.min(1), Validators.max(20)]]
    });
  }
}
