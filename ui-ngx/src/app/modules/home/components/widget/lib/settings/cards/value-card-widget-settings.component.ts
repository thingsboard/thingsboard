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

import { Component, Injector } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  valueCardDefaultSettings,
  ValueCardLayout,
  valueCardLayoutImages,
  valueCardLayouts,
  valueCardLayoutTranslations
} from '@home/components/widget/lib/cards/value-card-widget.models';
import { formatValue, isDefinedAndNotNull } from '@core/utils';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { DateFormatProcessor, DateFormatSettings, getLabel } from '@shared/models/widget-settings.models';
import { getSourceTbUnitSymbol } from '@shared/models/unit.models';

@Component({
  selector: 'tb-value-card-widget-settings',
  templateUrl: './value-card-widget-settings.component.html',
  styleUrls: []
})
export class ValueCardWidgetSettingsComponent extends WidgetSettingsComponent {

  valueCardLayouts: ValueCardLayout[] = [];

  valueCardLayoutTranslationMap = valueCardLayoutTranslations;
  valueCardLayoutImageMap = valueCardLayoutImages;

  horizontal = false;

  valueCardWidgetSettingsForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  datePreviewFn = this._datePreviewFn.bind(this);


  get label(): string {
    return getLabel(this.widgetConfig.config.datasources);
  }

  get dateEnabled(): boolean {
    const layout: ValueCardLayout = this.valueCardWidgetSettingsForm.get('layout').value;
    return ![ValueCardLayout.vertical, ValueCardLayout.simplified].includes(layout);
  }

  get iconEnabled(): boolean {
    const layout: ValueCardLayout = this.valueCardWidgetSettingsForm.get('layout').value;
    return layout !== ValueCardLayout.simplified;
  }

  constructor(protected store: Store<AppState>,
              private $injector: Injector,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.valueCardWidgetSettingsForm;
  }

  protected onWidgetConfigSet(widgetConfig: WidgetConfigComponentData) {
    const params = widgetConfig.typeParameters as any;
    this.horizontal  = isDefinedAndNotNull(params.horizontal) ? params.horizontal : false;
    this.valueCardLayouts = valueCardLayouts(this.horizontal);
  }

  protected defaultSettings(): WidgetSettings {
    return valueCardDefaultSettings(this.horizontal);
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.valueCardWidgetSettingsForm = this.fb.group({
      layout: [settings.layout, []],
      autoScale: [settings.autoScale, []],

      showLabel: [settings.showLabel, []],
      labelFont: [settings.labelFont, []],
      labelColor: [settings.labelColor, []],

      showIcon: [settings.showIcon, []],
      iconSize: [settings.iconSize, [Validators.min(0)]],
      iconSizeUnit: [settings.iconSizeUnit, []],
      icon: [settings.icon, []],
      iconColor: [settings.iconColor, []],

      valueFont: [settings.valueFont, []],
      valueColor: [settings.valueColor, []],

      showDate: [settings.showDate, []],
      dateFormat: [settings.dateFormat, []],
      dateFont: [settings.dateFont, []],
      dateColor: [settings.dateColor, []],

      background: [settings.background, []],
      padding: [settings.padding, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['layout', 'showLabel', 'showIcon', 'showDate'];
  }

  protected updateValidators(emitEvent: boolean) {
    const layout: ValueCardLayout = this.valueCardWidgetSettingsForm.get('layout').value;
    const showLabel: boolean = this.valueCardWidgetSettingsForm.get('showLabel').value;
    const showIcon: boolean = this.valueCardWidgetSettingsForm.get('showIcon').value;
    const showDate: boolean = this.valueCardWidgetSettingsForm.get('showDate').value;

    const dateEnabled = ![ValueCardLayout.vertical, ValueCardLayout.simplified].includes(layout);
    const iconEnabled = layout !== ValueCardLayout.simplified;

    if (showLabel) {
      this.valueCardWidgetSettingsForm.get('labelFont').enable();
      this.valueCardWidgetSettingsForm.get('labelColor').enable();
    } else {
      this.valueCardWidgetSettingsForm.get('labelFont').disable();
      this.valueCardWidgetSettingsForm.get('labelColor').disable();
    }

    if (iconEnabled) {
      this.valueCardWidgetSettingsForm.get('showIcon').enable({emitEvent: false});
      if (showIcon) {
        this.valueCardWidgetSettingsForm.get('iconSize').enable();
        this.valueCardWidgetSettingsForm.get('iconSizeUnit').enable();
        this.valueCardWidgetSettingsForm.get('icon').enable();
        this.valueCardWidgetSettingsForm.get('iconColor').enable();
      } else {
        this.valueCardWidgetSettingsForm.get('iconSize').disable();
        this.valueCardWidgetSettingsForm.get('iconSizeUnit').disable();
        this.valueCardWidgetSettingsForm.get('icon').disable();
        this.valueCardWidgetSettingsForm.get('iconColor').disable();
      }
    } else {
      this.valueCardWidgetSettingsForm.get('showIcon').disable({emitEvent: false});
      this.valueCardWidgetSettingsForm.get('iconSize').disable();
      this.valueCardWidgetSettingsForm.get('iconSizeUnit').disable();
      this.valueCardWidgetSettingsForm.get('icon').disable();
      this.valueCardWidgetSettingsForm.get('iconColor').disable();
    }

    if (dateEnabled) {
      this.valueCardWidgetSettingsForm.get('showDate').enable({emitEvent: false});
      if (showDate) {
        this.valueCardWidgetSettingsForm.get('dateFormat').enable();
        this.valueCardWidgetSettingsForm.get('dateFont').enable();
        this.valueCardWidgetSettingsForm.get('dateColor').enable();
      } else {
        this.valueCardWidgetSettingsForm.get('dateFormat').disable();
        this.valueCardWidgetSettingsForm.get('dateFont').disable();
        this.valueCardWidgetSettingsForm.get('dateColor').disable();
      }
    } else {
      this.valueCardWidgetSettingsForm.get('showDate').disable({emitEvent: false});
      this.valueCardWidgetSettingsForm.get('dateFormat').disable();
      this.valueCardWidgetSettingsForm.get('dateFont').disable();
      this.valueCardWidgetSettingsForm.get('dateColor').disable();
    }
    this.valueCardWidgetSettingsForm.get('showIcon').updateValueAndValidity({emitEvent: false});
    this.valueCardWidgetSettingsForm.get('showDate').updateValueAndValidity({emitEvent: false});
    this.valueCardWidgetSettingsForm.get('labelFont').updateValueAndValidity({emitEvent});
    this.valueCardWidgetSettingsForm.get('labelColor').updateValueAndValidity({emitEvent});
    this.valueCardWidgetSettingsForm.get('iconSize').updateValueAndValidity({emitEvent});
    this.valueCardWidgetSettingsForm.get('iconSizeUnit').updateValueAndValidity({emitEvent});
    this.valueCardWidgetSettingsForm.get('icon').updateValueAndValidity({emitEvent});
    this.valueCardWidgetSettingsForm.get('iconColor').updateValueAndValidity({emitEvent});
    this.valueCardWidgetSettingsForm.get('dateFormat').updateValueAndValidity({emitEvent});
    this.valueCardWidgetSettingsForm.get('dateFont').updateValueAndValidity({emitEvent});
    this.valueCardWidgetSettingsForm.get('dateColor').updateValueAndValidity({emitEvent});
  }

  private _valuePreviewFn(): string {
    const units = getSourceTbUnitSymbol(this.widgetConfig.config.units);
    const decimals: number = this.widgetConfig.config.decimals;
    return formatValue(22, decimals, units, true);
  }

  private _datePreviewFn(): string {
    const dateFormat: DateFormatSettings = this.valueCardWidgetSettingsForm.get('dateFormat').value;
    const processor = DateFormatProcessor.fromSettings(this.$injector, dateFormat);
    processor.update(Date.now());
    return processor.formatted;
  }
}
