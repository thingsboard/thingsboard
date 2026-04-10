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
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
    selector: 'tb-flot-pie-widget-settings',
    templateUrl: './flot-pie-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class FlotPieWidgetSettingsComponent extends WidgetSettingsComponent {

  flotPieWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.flotPieWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      radius: 1,
      innerRadius: 0,
      tilt: 1,
      stroke: {
        color: '',
        width: 0
      },
      showLabels: false,
      showTooltip: true,
      animatedPie: false
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {

    this.flotPieWidgetSettingsForm = this.fb.group({

      // Common pie settings

      radius: [settings.radius, [Validators.min(0), Validators.max(1)]],
      innerRadius: [settings.innerRadius, [Validators.min(0), Validators.max(1)]],
      tilt: [settings.tilt, [Validators.min(0), Validators.max(1)]],

      // Stroke settings

      stroke: this.fb.group({
        color: [settings.stroke?.color, []],
        width: [settings.stroke?.width, [Validators.min(0)]]
      }),

      showLabels: [settings.showLabels, []],
      showTooltip: [settings.showTooltip, []],

      animatedPie: [settings.animatedPie, []],
    });
  }
}
