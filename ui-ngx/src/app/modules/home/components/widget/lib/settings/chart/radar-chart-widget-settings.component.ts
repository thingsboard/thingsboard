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

import { Component, TemplateRef, ViewChild } from '@angular/core';
import { WidgetSettings } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  radarChartWidgetDefaultSettings,
  RadarChartWidgetSettings
} from '@home/components/widget/lib/chart/radar-chart-widget.models';
import {
  LatestChartWidgetSettingsComponent
} from '@home/components/widget/lib/settings/chart/latest-chart-widget-settings.component';

@Component({
  selector: 'tb-radar-chart-widget-settings',
  templateUrl: './latest-chart-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class RadarChartWidgetSettingsComponent extends LatestChartWidgetSettingsComponent<RadarChartWidgetSettings> {

  @ViewChild('radarChart')
  radarChartConfigTemplate: TemplateRef<any>;

  constructor(protected store: Store<AppState>,
              protected fb: UntypedFormBuilder) {
    super(store, fb);
  }

  protected defaultLatestChartSettings() {
    return radarChartWidgetDefaultSettings;
  }

  public latestChartConfigTemplate(): TemplateRef<any> {
    return this.radarChartConfigTemplate;
  }

  protected setupLatestChartControls(latestChartWidgetSettingsForm: UntypedFormGroup, settings: WidgetSettings) {
    latestChartWidgetSettingsForm.addControl('shape', this.fb.control(settings.shape, []));
    latestChartWidgetSettingsForm.addControl('color', this.fb.control(settings.color, []));
    latestChartWidgetSettingsForm.addControl('showLine', this.fb.control(settings.showLine, []));
    latestChartWidgetSettingsForm.addControl('lineType', this.fb.control(settings.lineType, []));
    latestChartWidgetSettingsForm.addControl('lineWidth', this.fb.control(settings.lineWidth, [Validators.min(0)]));
    latestChartWidgetSettingsForm.addControl('showPoints', this.fb.control(settings.showPoints, []));
    latestChartWidgetSettingsForm.addControl('pointShape', this.fb.control(settings.pointShape, []));
    latestChartWidgetSettingsForm.addControl('pointSize', this.fb.control(settings.pointSize, [Validators.min(0)]));
    latestChartWidgetSettingsForm.addControl('showLabel', this.fb.control(settings.showLabel, []));
    latestChartWidgetSettingsForm.addControl('labelPosition', this.fb.control(settings.labelPosition, []));
    latestChartWidgetSettingsForm.addControl('labelFont', this.fb.control(settings.labelFont, []));
    latestChartWidgetSettingsForm.addControl('labelColor', this.fb.control(settings.labelColor, []));
    latestChartWidgetSettingsForm.addControl('fillAreaSettings', this.fb.control(settings.fillAreaSettings, []));

    latestChartWidgetSettingsForm.addControl('normalizeAxes', this.fb.control(settings.normalizeAxes, []));
    latestChartWidgetSettingsForm.addControl('axisShowLabel', this.fb.control(settings.axisShowLabel, []));
    latestChartWidgetSettingsForm.addControl('axisLabelFont', this.fb.control(settings.axisLabelFont, []));
    latestChartWidgetSettingsForm.addControl('axisShowTickLabels', this.fb.control(settings.axisShowTickLabels, []));
    latestChartWidgetSettingsForm.addControl('axisTickLabelFont', this.fb.control(settings.axisTickLabelFont, []));
    latestChartWidgetSettingsForm.addControl('axisTickLabelColor', this.fb.control(settings.axisTickLabelColor, []));
  }

  protected latestChartValidatorTriggers(): string[] {
    return ['showLine', 'showPoints', 'showLabel', 'axisShowLabel', 'axisShowTickLabels'];
  }

  protected updateLatestChartValidators(latestChartWidgetSettingsForm: UntypedFormGroup, emitEvent: boolean, trigger?: string) {
    const showLine: boolean = latestChartWidgetSettingsForm.get('showLine').value;
    const showPoints: boolean = latestChartWidgetSettingsForm.get('showPoints').value;
    const showLabel: boolean = latestChartWidgetSettingsForm.get('showLabel').value;
    const axisShowLabel: boolean = latestChartWidgetSettingsForm.get('axisShowLabel').value;
    const axisShowTickLabels: boolean = latestChartWidgetSettingsForm.get('axisShowTickLabels').value;

    if (showLine) {
      latestChartWidgetSettingsForm.get('lineType').enable();
      latestChartWidgetSettingsForm.get('lineWidth').enable();
    } else {
      latestChartWidgetSettingsForm.get('lineType').disable();
      latestChartWidgetSettingsForm.get('lineWidth').disable();
    }

    if (showPoints) {
      latestChartWidgetSettingsForm.get('pointShape').enable();
      latestChartWidgetSettingsForm.get('pointSize').enable();
    } else {
      latestChartWidgetSettingsForm.get('pointShape').disable();
      latestChartWidgetSettingsForm.get('pointSize').disable();
    }

    if (showLabel) {
      latestChartWidgetSettingsForm.get('labelPosition').enable();
      latestChartWidgetSettingsForm.get('labelFont').enable();
      latestChartWidgetSettingsForm.get('labelColor').enable();
    } else {
      latestChartWidgetSettingsForm.get('labelPosition').disable();
      latestChartWidgetSettingsForm.get('labelFont').disable();
      latestChartWidgetSettingsForm.get('labelColor').disable();
    }

    if (axisShowLabel) {
      latestChartWidgetSettingsForm.get('axisLabelFont').enable();
    } else {
      latestChartWidgetSettingsForm.get('axisLabelFont').disable();
    }

    if (axisShowTickLabels) {
      latestChartWidgetSettingsForm.get('axisTickLabelFont').enable();
      latestChartWidgetSettingsForm.get('axisTickLabelColor').enable();
    } else {
      latestChartWidgetSettingsForm.get('axisTickLabelFont').disable();
      latestChartWidgetSettingsForm.get('axisTickLabelColor').disable();
    }
  }
}
