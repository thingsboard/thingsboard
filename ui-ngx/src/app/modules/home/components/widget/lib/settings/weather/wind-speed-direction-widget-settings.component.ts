// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { formatValue, isDefinedAndNotNull } from '@core/utils';
import {
  windSpeedDirectionDefaultSettings,
  WindSpeedDirectionLayout,
  windSpeedDirectionLayoutImages,
  windSpeedDirectionLayouts,
  windSpeedDirectionLayoutTranslations
} from '@home/components/widget/lib/weather/wind-speed-direction-widget.models';
import { getDataKey } from '@shared/models/widget-settings.models';
import { getSourceTbUnitSymbol, TbUnit } from '@shared/models/unit.models';

@Component({
    selector: 'tb-wind-speed-direction-widget-settings',
    templateUrl: './wind-speed-direction-widget-settings.component.html',
    styleUrls: [],
    standalone: false
})
export class WindSpeedDirectionWidgetSettingsComponent extends WidgetSettingsComponent {

  get hasCenterValue(): boolean {
    return !!getDataKey(this.widgetConfig.config.datasources, 1);
  }

  get majorTicksFontEnabled(): boolean {
    const layout: WindSpeedDirectionLayout = this.windSpeedDirectionWidgetSettingsForm.get('layout').value;
    return [ WindSpeedDirectionLayout.default, WindSpeedDirectionLayout.advanced ].includes(layout);
  }

  get minorTicksFontEnabled(): boolean {
    const layout: WindSpeedDirectionLayout = this.windSpeedDirectionWidgetSettingsForm.get('layout').value;
    return layout === WindSpeedDirectionLayout.advanced;
  }

  windSpeedDirectionLayouts = windSpeedDirectionLayouts;

  windSpeedDirectionLayoutTranslationMap = windSpeedDirectionLayoutTranslations;
  windSpeedDirectionLayoutImageMap = windSpeedDirectionLayoutImages;

  windSpeedDirectionWidgetSettingsForm: UntypedFormGroup;

  centerValuePreviewFn = this._centerValuePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.windSpeedDirectionWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return windSpeedDirectionDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.windSpeedDirectionWidgetSettingsForm = this.fb.group({
      layout: [settings.layout, []],

      centerValueFont: [settings.centerValueFont, []],
      centerValueColor: [settings.centerValueColor, []],

      ticksColor: [settings.ticksColor, []],
      directionalNamesElseDegrees: [settings.directionalNamesElseDegrees, []],

      majorTicksFont: [settings.majorTicksFont, []],
      majorTicksColor: [settings.majorTicksColor, []],

      minorTicksFont: [settings.minorTicksFont, []],
      minorTicksColor: [settings.minorTicksColor, []],

      arrowColor: [settings.arrowColor, []],

      background: [settings.background, []],
      padding: [settings.padding, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['layout'];
  }

  protected updateValidators(emitEvent: boolean) {
    const layout: WindSpeedDirectionLayout = this.windSpeedDirectionWidgetSettingsForm.get('layout').value;

    const majorTicksFontEnabled = [ WindSpeedDirectionLayout.default, WindSpeedDirectionLayout.advanced ].includes(layout);
    const minorTicksFontEnabled = layout === WindSpeedDirectionLayout.advanced;

    if (majorTicksFontEnabled) {
      this.windSpeedDirectionWidgetSettingsForm.get('majorTicksFont').enable();
    } else {
      this.windSpeedDirectionWidgetSettingsForm.get('majorTicksFont').disable();
    }

    if (minorTicksFontEnabled) {
      this.windSpeedDirectionWidgetSettingsForm.get('minorTicksFont').enable();
    } else {
      this.windSpeedDirectionWidgetSettingsForm.get('minorTicksFont').disable();
    }
  }

  private _centerValuePreviewFn(): string {
    const centerValueDataKey = getDataKey(this.widgetConfig.config.datasources, 1);
    if (centerValueDataKey) {
      let units: TbUnit = this.widgetConfig.config.units;
      let decimals: number = this.widgetConfig.config.decimals;
      if (isDefinedAndNotNull(centerValueDataKey?.decimals)) {
        decimals = centerValueDataKey.decimals;
      }
      if (centerValueDataKey?.units) {
        units = centerValueDataKey.units;
      }
      return formatValue(25, decimals, getSourceTbUnitSymbol(units), true);
    } else {
      return '225°';
    }
  }

}
