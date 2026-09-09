// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
    selector: 'tb-flot-latest-key-settings',
    templateUrl: './flot-latest-key-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class FlotLatestKeySettingsComponent extends WidgetSettingsComponent {

  flotLatestKeySettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.flotLatestKeySettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      useAsThreshold: false,
      thresholdLineWidth: null,
      thresholdColor: null
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.flotLatestKeySettingsForm = this.fb.group({
      useAsThreshold: [settings.useAsThreshold, []],
      thresholdLineWidth: [settings.thresholdLineWidth, [Validators.min(0)]],
      thresholdColor: [settings.thresholdColor, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['useAsThreshold'];
  }

  protected updateValidators(emitEvent: boolean) {
    const useAsThreshold: boolean = this.flotLatestKeySettingsForm.get('useAsThreshold').value;
    if (useAsThreshold) {
      this.flotLatestKeySettingsForm.get('thresholdLineWidth').enable();
      this.flotLatestKeySettingsForm.get('thresholdColor').enable();
    } else {
      this.flotLatestKeySettingsForm.get('thresholdLineWidth').disable();
      this.flotLatestKeySettingsForm.get('thresholdColor').disable();
    }
    this.flotLatestKeySettingsForm.get('thresholdLineWidth').updateValueAndValidity({emitEvent});
    this.flotLatestKeySettingsForm.get('thresholdColor').updateValueAndValidity({emitEvent});
  }

}
