// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
    selector: 'tb-simple-card-widget-settings',
    templateUrl: './simple-card-widget-settings.component.html',
    styleUrls: [],
    standalone: false
})
export class SimpleCardWidgetSettingsComponent extends WidgetSettingsComponent {

  simpleCardWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.simpleCardWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      labelPosition: 'left'
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.simpleCardWidgetSettingsForm = this.fb.group({
      labelPosition: [settings.labelPosition, []]
    });
  }
}
