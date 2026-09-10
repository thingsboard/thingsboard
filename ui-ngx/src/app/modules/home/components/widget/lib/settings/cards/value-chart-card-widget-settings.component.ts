// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Injector } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { formatValue } from '@core/utils';
import {
  valueCartCardLayouts,
  valueChartCardDefaultSettings,
  valueChartCardLayoutImages,
  valueChartCardLayoutTranslations
} from '@home/components/widget/lib/cards/value-chart-card-widget.models';
import { getSourceTbUnitSymbol } from '@shared/models/unit.models';

@Component({
    selector: 'tb-value-chart-card-widget-settings',
    templateUrl: './value-chart-card-widget-settings.component.html',
    styleUrls: [],
    standalone: false
})
export class ValueChartCardWidgetSettingsComponent extends WidgetSettingsComponent {

  valueChartCardLayouts = valueCartCardLayouts;

  valueChartCardLayoutTranslationMap = valueChartCardLayoutTranslations;
  valueChartCardLayoutImageMap = valueChartCardLayoutImages;

  valueChartCardWidgetSettingsForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              private $injector: Injector,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.valueChartCardWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return valueChartCardDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.valueChartCardWidgetSettingsForm = this.fb.group({
      layout: [settings.layout, []],
      autoScale: [settings.autoScale, []],

      showValue: [settings.showValue, []],
      valueFont: [settings.valueFont, []],
      valueColor: [settings.valueColor, []],

      background: [settings.background, []],
      padding: [settings.padding, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['showValue'];
  }

  protected updateValidators(emitEvent: boolean) {
    const showValue: boolean = this.valueChartCardWidgetSettingsForm.get('showValue').value;

    if (showValue) {
      this.valueChartCardWidgetSettingsForm.get('valueFont').enable();
      this.valueChartCardWidgetSettingsForm.get('valueColor').enable();
    } else {
      this.valueChartCardWidgetSettingsForm.get('valueFont').disable();
      this.valueChartCardWidgetSettingsForm.get('valueColor').disable();
    }

    this.valueChartCardWidgetSettingsForm.get('valueFont').updateValueAndValidity({emitEvent});
    this.valueChartCardWidgetSettingsForm.get('valueColor').updateValueAndValidity({emitEvent});
  }

  private _valuePreviewFn(): string {
    const units = getSourceTbUnitSymbol(this.widgetConfig.config.units);
    const decimals: number = this.widgetConfig.config.decimals;
    return formatValue(22, decimals, units, true);
  }

}
