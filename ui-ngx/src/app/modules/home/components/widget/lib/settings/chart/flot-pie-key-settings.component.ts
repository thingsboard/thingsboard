// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
    selector: 'tb-flot-pie-key-settings',
    templateUrl: './flot-pie-key-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class FlotPieKeySettingsComponent extends WidgetSettingsComponent {

  flotPieKeySettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.flotPieKeySettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      hideDataByDefault: false,
      disableDataHiding: false,
      removeFromLegend: false
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {

    this.flotPieKeySettingsForm = this.fb.group({

      // Common settings

      hideDataByDefault: [settings.hideDataByDefault, []],
      disableDataHiding: [settings.disableDataHiding, []],
      removeFromLegend: [settings.removeFromLegend, []]
    });
  }
}
