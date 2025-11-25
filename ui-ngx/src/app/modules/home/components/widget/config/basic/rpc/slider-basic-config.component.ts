///
/// Copyright © 2016-2025 The Thingsboard Authors
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
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { BasicWidgetConfigComponent } from '@home/components/widget/config/widget-config.component.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { TargetDevice, WidgetConfig, widgetTitleAutocompleteValues, } from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { formatValue, isUndefined } from '@core/utils';
import { ValueType } from '@shared/models/constants';
import {
  SliderLayout,
  sliderLayoutImages,
  sliderLayouts,
  sliderLayoutTranslations,
  sliderWidgetDefaultSettings,
  SliderWidgetSettings
} from '@home/components/widget/lib/rpc/slider-widget.models';
import { cssSizeToStrSize, resolveCssSize } from '@shared/models/widget-settings.models';
import { getSourceTbUnitSymbol } from '@shared/models/unit.models';

@Component({
  selector: 'tb-slider-basic-config',
  templateUrl: './slider-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class SliderBasicConfigComponent extends BasicWidgetConfigComponent {

  get targetDevice(): TargetDevice {
    return this.sliderWidgetConfigForm.get('targetDevice').value;
  }

  sliderLayout = SliderLayout;

  sliderLayouts = sliderLayouts;

  sliderLayoutTranslationMap = sliderLayoutTranslations;
  sliderLayoutImageMap = sliderLayoutImages;

  valueType = ValueType;

  sliderWidgetConfigForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  predefinedValues = widgetTitleAutocompleteValues;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.sliderWidgetConfigForm;
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: SliderWidgetSettings = {...sliderWidgetDefaultSettings, ...(configData.config.settings || {})};
    const iconSize = resolveCssSize(configData.config.iconSize);
    this.sliderWidgetConfigForm = this.fb.group({
      targetDevice: [configData.config.targetDevice, []],

      initialState: [settings.initialState, []],
      valueChange: [settings.valueChange, []],
      disabledState: [settings.disabledState, []],

      layout: [settings.layout, []],
      autoScale: [settings.autoScale, []],

      showTitle: [configData.config.showTitle, []],
      title: [configData.config.title, []],
      titleFont: [configData.config.titleFont, []],
      titleColor: [configData.config.titleColor, []],

      showIcon: [configData.config.showTitleIcon, []],
      iconSize: [iconSize[0], [Validators.min(0)]],
      iconSizeUnit: [iconSize[1], []],
      icon: [configData.config.titleIcon, []],
      iconColor: [configData.config.iconColor, []],

      showValue: [settings.showValue, []],
      valueUnits: [settings.valueUnits, []],
      valueDecimals: [settings.valueDecimals, []],
      valueFont: [settings.valueFont, []],
      valueColor: [settings.valueColor, []],

      tickMin: [settings.tickMin, []],
      tickMax: [settings.tickMax, []],

      showTicks: [settings.showTicks, []],
      ticksFont: [settings.ticksFont, []],
      ticksColor: [settings.ticksColor, []],

      showTickMarks: [settings.showTickMarks, []],
      tickMarksCount: [settings.tickMarksCount, [Validators.min(2)]],
      tickMarksColor: [settings.tickMarksColor, []],

      mainColor: [settings.mainColor, []],
      backgroundColor: [settings.backgroundColor, []],

      mainColorDisabled: [settings.mainColorDisabled, []],
      backgroundColorDisabled: [settings.backgroundColorDisabled, []],

      leftIconSize: [settings.leftIconSize, [Validators.min(0)]],
      leftIconSizeUnit: [settings.leftIconSizeUnit, []],
      leftIcon: [settings.leftIcon, []],
      leftIconColor: [settings.leftIconColor, []],

      rightIconSize: [settings.rightIconSize, [Validators.min(0)]],
      rightIconSizeUnit: [settings.rightIconSizeUnit, []],
      rightIcon: [settings.rightIcon, []],
      rightIconColor: [settings.rightIconColor, []],

      background: [settings.background, []],

      cardButtons: [this.getCardButtons(configData.config), []],
      borderRadius: [configData.config.borderRadius, []],
      padding: [settings.padding, []],

      actions: [configData.config.actions || {}, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    this.widgetConfig.config.targetDevice = config.targetDevice;

    this.widgetConfig.config.showTitle = config.showTitle;
    this.widgetConfig.config.title = config.title;
    this.widgetConfig.config.titleFont = config.titleFont;
    this.widgetConfig.config.titleColor = config.titleColor;

    this.widgetConfig.config.showTitleIcon = config.showIcon;
    this.widgetConfig.config.iconSize = cssSizeToStrSize(config.iconSize, config.iconSizeUnit);
    this.widgetConfig.config.titleIcon = config.icon;
    this.widgetConfig.config.iconColor = config.iconColor;

    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};

    this.widgetConfig.config.settings.initialState = config.initialState;
    this.widgetConfig.config.settings.valueChange = config.valueChange;
    this.widgetConfig.config.settings.disabledState = config.disabledState;

    this.widgetConfig.config.settings.layout = config.layout;
    this.widgetConfig.config.settings.autoScale = config.autoScale;

    this.widgetConfig.config.settings.showValue = config.showValue;
    this.widgetConfig.config.settings.valueUnits = config.valueUnits;
    this.widgetConfig.config.settings.valueDecimals = config.valueDecimals;
    this.widgetConfig.config.settings.valueFont = config.valueFont;
    this.widgetConfig.config.settings.valueColor = config.valueColor;

    this.widgetConfig.config.settings.tickMin = config.tickMin;
    this.widgetConfig.config.settings.tickMax = config.tickMax;

    this.widgetConfig.config.settings.showTicks = config.showTicks;
    this.widgetConfig.config.settings.ticksFont = config.ticksFont;
    this.widgetConfig.config.settings.ticksColor = config.ticksColor;

    this.widgetConfig.config.settings.showTickMarks = config.showTickMarks;
    this.widgetConfig.config.settings.tickMarksCount = config.tickMarksCount;
    this.widgetConfig.config.settings.tickMarksColor = config.tickMarksColor;

    this.widgetConfig.config.settings.mainColor = config.mainColor;
    this.widgetConfig.config.settings.backgroundColor = config.backgroundColor;

    this.widgetConfig.config.settings.mainColorDisabled = config.mainColorDisabled;
    this.widgetConfig.config.settings.backgroundColorDisabled = config.backgroundColorDisabled;

    this.widgetConfig.config.settings.leftIconSize = config.leftIconSize;
    this.widgetConfig.config.settings.leftIconSizeUnit = config.leftIconSizeUnit;
    this.widgetConfig.config.settings.leftIcon = config.leftIcon;
    this.widgetConfig.config.settings.leftIconColor = config.leftIconColor;

    this.widgetConfig.config.settings.rightIconSize = config.rightIconSize;
    this.widgetConfig.config.settings.rightIconSizeUnit = config.rightIconSizeUnit;
    this.widgetConfig.config.settings.rightIcon = config.rightIcon;
    this.widgetConfig.config.settings.rightIconColor = config.rightIconColor;

    this.widgetConfig.config.settings.background = config.background;

    this.setCardButtons(config.cardButtons, this.widgetConfig.config);
    this.widgetConfig.config.borderRadius = config.borderRadius;
    this.widgetConfig.config.settings.padding = config.padding;

    this.widgetConfig.config.actions = config.actions;
    return this.widgetConfig;
  }

  protected validatorTriggers(): string[] {
    return ['showTitle', 'showIcon', 'showValue', 'showTicks', 'showTickMarks', 'layout'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const showTitle: boolean = this.sliderWidgetConfigForm.get('showTitle').value;
    const showIcon: boolean = this.sliderWidgetConfigForm.get('showIcon').value;
    const showValue: boolean = this.sliderWidgetConfigForm.get('showValue').value;
    const showTicks: boolean = this.sliderWidgetConfigForm.get('showTicks').value;
    const showTickMarks: boolean = this.sliderWidgetConfigForm.get('showTickMarks').value;
    const layout: SliderLayout = this.sliderWidgetConfigForm.get('layout').value;

    const valueEnabled = layout !== SliderLayout.simplified;
    const leftRightIconsEnabled = layout === SliderLayout.extended;

    if (showTitle) {
      this.sliderWidgetConfigForm.get('title').enable();
      this.sliderWidgetConfigForm.get('titleFont').enable();
      this.sliderWidgetConfigForm.get('titleColor').enable();
      this.sliderWidgetConfigForm.get('showIcon').enable({emitEvent: false});
      if (showIcon) {
        this.sliderWidgetConfigForm.get('iconSize').enable();
        this.sliderWidgetConfigForm.get('iconSizeUnit').enable();
        this.sliderWidgetConfigForm.get('icon').enable();
        this.sliderWidgetConfigForm.get('iconColor').enable();
      } else {
        this.sliderWidgetConfigForm.get('iconSize').disable();
        this.sliderWidgetConfigForm.get('iconSizeUnit').disable();
        this.sliderWidgetConfigForm.get('icon').disable();
        this.sliderWidgetConfigForm.get('iconColor').disable();
      }
    } else {
      this.sliderWidgetConfigForm.get('title').disable();
      this.sliderWidgetConfigForm.get('titleFont').disable();
      this.sliderWidgetConfigForm.get('titleColor').disable();
      this.sliderWidgetConfigForm.get('showIcon').disable({emitEvent: false});
      this.sliderWidgetConfigForm.get('iconSize').disable();
      this.sliderWidgetConfigForm.get('iconSizeUnit').disable();
      this.sliderWidgetConfigForm.get('icon').disable();
      this.sliderWidgetConfigForm.get('iconColor').disable();
    }

    if (valueEnabled && showValue) {
      this.sliderWidgetConfigForm.get('valueUnits').enable();
      this.sliderWidgetConfigForm.get('valueDecimals').enable();
      this.sliderWidgetConfigForm.get('valueFont').enable();
      this.sliderWidgetConfigForm.get('valueColor').enable();
    } else {
      this.sliderWidgetConfigForm.get('valueUnits').disable();
      this.sliderWidgetConfigForm.get('valueDecimals').disable();
      this.sliderWidgetConfigForm.get('valueFont').disable();
      this.sliderWidgetConfigForm.get('valueColor').disable();
    }

    if (showTicks) {
      this.sliderWidgetConfigForm.get('ticksFont').enable();
      this.sliderWidgetConfigForm.get('ticksColor').enable();
    } else {
      this.sliderWidgetConfigForm.get('ticksFont').disable();
      this.sliderWidgetConfigForm.get('ticksColor').disable();
    }

    if (showTickMarks) {
      this.sliderWidgetConfigForm.get('tickMarksCount').enable();
      this.sliderWidgetConfigForm.get('tickMarksColor').enable();
    } else {
      this.sliderWidgetConfigForm.get('tickMarksCount').disable();
      this.sliderWidgetConfigForm.get('tickMarksColor').disable();
    }

    if (leftRightIconsEnabled) {
      this.sliderWidgetConfigForm.get('leftIconSize').enable();
      this.sliderWidgetConfigForm.get('leftIconSizeUnit').enable();
      this.sliderWidgetConfigForm.get('leftIcon').enable();
      this.sliderWidgetConfigForm.get('leftIconColor').enable();
      this.sliderWidgetConfigForm.get('rightIconSize').enable();
      this.sliderWidgetConfigForm.get('rightIconSizeUnit').enable();
      this.sliderWidgetConfigForm.get('rightIcon').enable();
      this.sliderWidgetConfigForm.get('rightIconColor').enable();
    } else {
      this.sliderWidgetConfigForm.get('leftIconSize').disable();
      this.sliderWidgetConfigForm.get('leftIconSizeUnit').disable();
      this.sliderWidgetConfigForm.get('leftIcon').disable();
      this.sliderWidgetConfigForm.get('leftIconColor').disable();
      this.sliderWidgetConfigForm.get('rightIconSize').disable();
      this.sliderWidgetConfigForm.get('rightIconSizeUnit').disable();
      this.sliderWidgetConfigForm.get('rightIcon').disable();
      this.sliderWidgetConfigForm.get('rightIconColor').disable();
    }
  }

  private getCardButtons(config: WidgetConfig): string[] {
    const buttons: string[] = [];
    if (isUndefined(config.enableFullscreen) || config.enableFullscreen) {
      buttons.push('fullscreen');
    }
    return buttons;
  }

  private setCardButtons(buttons: string[], config: WidgetConfig) {
    config.enableFullscreen = buttons.includes('fullscreen');
  }

  private _valuePreviewFn(): string {
    const units: string = getSourceTbUnitSymbol(this.sliderWidgetConfigForm.get('valueUnits').value);
    const decimals: number = this.sliderWidgetConfigForm.get('valueDecimals').value;
    return formatValue(48, decimals, units, false);
  }
}
