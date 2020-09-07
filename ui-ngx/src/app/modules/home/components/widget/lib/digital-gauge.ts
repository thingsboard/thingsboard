///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import * as CanvasGauges from 'canvas-gauges';
import { WidgetContext } from '@home/models/widget-component.models';
import {
  attributesGaugeType,
  AttributeSourceProperty,
  ColorLevelSetting,
  DigitalGaugeSettings,
  digitalGaugeSettingsSchema,
  FixedLevelColors
} from '@home/components/widget/lib/digital-gauge.models';
import * as tinycolor_ from 'tinycolor2';
import { isDefined, isDefinedAndNotNull } from '@core/utils';
import { prepareFontSettings } from '@home/components/widget/lib/settings.models';
import { CanvasDigitalGauge, CanvasDigitalGaugeOptions } from '@home/components/widget/lib/canvas-digital-gauge';
import { DatePipe } from '@angular/common';
import {
  DataKey,
  Datasource,
  DatasourceData,
  DatasourceType,
  JsonSettingsSchema,
  widgetType
} from '@shared/models/widget.models';
import { IWidgetSubscription, WidgetSubscriptionOptions } from '@core/api/widget-api.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { EMPTY, Observable } from 'rxjs';
import GenericOptions = CanvasGauges.GenericOptions;

const tinycolor = tinycolor_;

const digitalGaugeSettingsSchemaValue = digitalGaugeSettingsSchema;

export class TbCanvasDigitalGauge {

  static get settingsSchema(): JsonSettingsSchema {
    return digitalGaugeSettingsSchemaValue;
  }

  constructor(protected ctx: WidgetContext, canvasId: string) {
    const gaugeElement = $('#' + canvasId, ctx.$container)[0];
    const settings: DigitalGaugeSettings = ctx.settings;

    this.localSettings = {};
    this.localSettings.minValue = settings.minValue || 0;
    this.localSettings.maxValue = settings.maxValue || 100;
    this.localSettings.gaugeType = settings.gaugeType || 'arc';
    this.localSettings.neonGlowBrightness = settings.neonGlowBrightness || 0;
    this.localSettings.dashThickness = settings.dashThickness || 0;
    this.localSettings.roundedLineCap = settings.roundedLineCap === true;

    const dataKey = ctx.data[0].dataKey;
    const keyColor = settings.defaultColor || dataKey.color;

    this.localSettings.unitTitle = ((settings.showUnitTitle === true) ?
      (settings.unitTitle && settings.unitTitle.length > 0 ?
        settings.unitTitle : dataKey.label) : '');

    this.localSettings.showTimestamp = settings.showTimestamp === true;
    this.localSettings.timestampFormat = settings.timestampFormat && settings.timestampFormat.length ?
      settings.timestampFormat : 'yyyy-MM-dd HH:mm:ss';

    this.localSettings.gaugeWidthScale = settings.gaugeWidthScale || 0.75;
    this.localSettings.gaugeColor = settings.gaugeColor || tinycolor(keyColor).setAlpha(0.2).toRgbString();

    this.localSettings.useFixedLevelColor = settings.useFixedLevelColor || false;
    if (!settings.useFixedLevelColor) {
      if (!settings.levelColors || settings.levelColors.length === 0) {
        this.localSettings.levelColors = [keyColor];
      } else {
        this.localSettings.levelColors = settings.levelColors.slice();
      }
    } else {
      this.localSettings.levelColors = [keyColor];
      this.localSettings.fixedLevelColors = settings.fixedLevelColors || [];
    }

    this.localSettings.showTicks = settings.showTicks || false;
    this.localSettings.ticks = [];
    this.localSettings.ticksValue = settings.ticksValue || [];
    this.localSettings.tickWidth = settings.tickWidth || 4;
    this.localSettings.colorTicks = settings.colorTicks || '#666';

    this.localSettings.decimals = isDefined(dataKey.decimals) ? dataKey.decimals :
      (isDefinedAndNotNull(settings.decimals) ? settings.decimals : ctx.decimals);

    this.localSettings.units = dataKey.units && dataKey.units.length ? dataKey.units :
      (isDefined(settings.units) && settings.units.length > 0 ? settings.units : ctx.units);

    this.localSettings.hideValue = settings.showValue !== true;
    this.localSettings.hideMinMax = settings.showMinMax !== true;
    this.localSettings.donutStartAngle = isDefinedAndNotNull(settings.donutStartAngle) ?
      -TbCanvasDigitalGauge.toRadians(settings.donutStartAngle) : null;

    this.localSettings.title = ((settings.showTitle === true) ?
      (settings.title && settings.title.length > 0 ?
        settings.title : dataKey.label) : '');

    if (!this.localSettings.unitTitle && this.localSettings.showTimestamp) {
      this.localSettings.unitTitle = ' ';
    }

    this.localSettings.titleFont = prepareFontSettings(settings.titleFont, {
      size: 12,
      style: 'normal',
      weight: '500',
      color: keyColor
    });

    this.localSettings.valueFont = prepareFontSettings(settings.valueFont, {
      size: 18,
      style: 'normal',
      weight: '500',
      color: keyColor
    });

    this.localSettings.minMaxFont = prepareFontSettings(settings.minMaxFont, {
      size: 10,
      style: 'normal',
      weight: '500',
      color: keyColor
    });

    this.localSettings.labelFont = prepareFontSettings(settings.labelFont, {
      size: 8,
      style: 'normal',
      weight: '500',
      color: keyColor
    });

    const gaugeData: CanvasDigitalGaugeOptions = {
      renderTo: gaugeElement,

      gaugeWidthScale: this.localSettings.gaugeWidthScale,
      gaugeColor: this.localSettings.gaugeColor,
      levelColors: this.localSettings.levelColors,

      colorTicks: this.localSettings.colorTicks,
      tickWidth: this.localSettings.tickWidth,
      ticks: this.localSettings.ticks,

      title: this.localSettings.title,

      fontTitleSize: this.localSettings.titleFont.size,
      fontTitleStyle: this.localSettings.titleFont.style,
      fontTitleWeight: this.localSettings.titleFont.weight,
      colorTitle: this.localSettings.titleFont.color,
      fontTitle: this.localSettings.titleFont.family,

      fontValueSize:  this.localSettings.valueFont.size,
      fontValueStyle: this.localSettings.valueFont.style,
      fontValueWeight: this.localSettings.valueFont.weight,
      colorValue: this.localSettings.valueFont.color,
      fontValue: this.localSettings.valueFont.family,

      fontMinMaxSize: this.localSettings.minMaxFont.size,
      fontMinMaxStyle: this.localSettings.minMaxFont.style,
      fontMinMaxWeight: this.localSettings.minMaxFont.weight,
      colorMinMax: this.localSettings.minMaxFont.color,
      fontMinMax: this.localSettings.minMaxFont.family,

      fontLabelSize: this.localSettings.labelFont.size,
      fontLabelStyle: this.localSettings.labelFont.style,
      fontLabelWeight: this.localSettings.labelFont.weight,
      colorLabel: this.localSettings.labelFont.color,
      fontLabel: this.localSettings.labelFont.family,

      minValue: this.localSettings.minValue,
      maxValue: this.localSettings.maxValue,
      gaugeType: this.localSettings.gaugeType,
      dashThickness: this.localSettings.dashThickness,
      roundedLineCap: this.localSettings.roundedLineCap,

      symbol: this.localSettings.units,
      label: this.localSettings.unitTitle,
      showTimestamp: this.localSettings.showTimestamp,
      hideValue: this.localSettings.hideValue,
      hideMinMax: this.localSettings.hideMinMax,

      donutStartAngle: this.localSettings.donutStartAngle,

      valueDec: this.localSettings.decimals,

      neonGlowBrightness: this.localSettings.neonGlowBrightness,

      // animations
      animation: settings.animation !== false && !ctx.isMobile,
      animationDuration: isDefinedAndNotNull(settings.animationDuration) ? settings.animationDuration : 500,
      animationRule: settings.animationRule || 'linear',

      isMobile: ctx.isMobile
    };

    this.gauge = new CanvasDigitalGauge(gaugeData).draw();
    this.init();
  }

  private localSettings: DigitalGaugeSettings;
  private levelColorsSourcesSubscription: IWidgetSubscription;
  private ticksSourcesSubscription: IWidgetSubscription;

  private gauge: CanvasDigitalGauge;

  static generateDatasource(ctx: WidgetContext, datasources: Datasource[], entityAlias: string,
                            attribute: string, settings: any): Datasource[]{
    const entityAliasId = ctx.aliasController.getEntityAliasId(entityAlias);
    if (!entityAliasId) {
      throw new Error('Not valid entity aliase name ' + entityAlias);
    }

    const datasource = datasources.find((datasourceIteration) => {
      return datasourceIteration.entityAliasId === entityAliasId;
    });

    const dataKey: DataKey = {
      type: DataKeyType.attribute,
      name: attribute,
      label: attribute,
      settings: [settings],
      _hash: Math.random()
    };

    if (datasource) {
      const findDataKey = datasource.dataKeys.find((dataKeyIteration) => {
        return dataKeyIteration.name === attribute;
      });

      if (findDataKey) {
        findDataKey.settings.push(settings);
      } else {
        datasource.dataKeys.push(dataKey);
      }
    } else {
      const datasourceAttribute: Datasource = {
        type: DatasourceType.entity,
        name: entityAlias,
        aliasName: entityAlias,
        entityAliasId,
        dataKeys: [dataKey]
      };
      datasources.push(datasourceAttribute);
    }

    return datasources;
  }

  private static toRadians(angle: number): number {
    return angle * (Math.PI / 180);
  }

  init() {
    let updateSetting = false;

    if (this.localSettings.useFixedLevelColor && this.localSettings.fixedLevelColors?.length > 0) {
      this.localSettings.levelColors = this.settingLevelColorsSubscribe(this.localSettings.fixedLevelColors);
      updateSetting = true;
    }
    if (this.localSettings.showTicks && this.localSettings.ticksValue?.length) {
      this.localSettings.ticks = this.settingTicksSubscribe(this.localSettings.ticksValue);
      updateSetting = true;
    }

    if (updateSetting) {
      this.updateSetting();
    }
  }

  settingLevelColorsSubscribe(options: FixedLevelColors[]): ColorLevelSetting[] {
    let levelColorsDatasource: Datasource[] = [];
    const predefineLevelColors: ColorLevelSetting[] = [];

    function setLevelColor(levelSetting: AttributeSourceProperty, color: string) {
      if (levelSetting.valueSource === 'predefinedValue' && isFinite(levelSetting.value)) {
        predefineLevelColors.push({
          value: levelSetting.value,
          color
        });
      } else if (levelSetting.entityAlias && levelSetting.attribute) {
        try {
          levelColorsDatasource = TbCanvasDigitalGauge.generateDatasource(this.ctx, levelColorsDatasource,
            levelSetting.entityAlias, levelSetting.attribute, {color, index: predefineLevelColors.length});
        } catch (e) {
          return;
        }
        predefineLevelColors.push(null);
      }
    }

    for (const levelColor of options) {
      if (levelColor.from) {
        setLevelColor.call(this, levelColor.from, levelColor.color);
      }
      if (levelColor.to) {
        setLevelColor.call(this, levelColor.to, levelColor.color);
      }
    }

    this.subscribeAttributes(levelColorsDatasource, 'levelColors').subscribe((subscription) => {
      this.levelColorsSourcesSubscription = subscription;
    });

    return predefineLevelColors;
  }

  settingTicksSubscribe(options: AttributeSourceProperty[]): number[] {
    let ticksDatasource: Datasource[] = [];
    const predefineTicks: number[] = [];

    for (const tick of options) {
      if (tick.valueSource === 'predefinedValue' && isFinite(tick.value)) {
        predefineTicks.push(tick.value);
      } else if (tick.entityAlias && tick.attribute) {
        try {
          ticksDatasource = TbCanvasDigitalGauge
            .generateDatasource(this.ctx, ticksDatasource, tick.entityAlias, tick.attribute, predefineTicks.length);
        } catch (e) {
          continue;
        }
        predefineTicks.push(null);
      }
    }

    this.subscribeAttributes(ticksDatasource, 'ticks').subscribe((subscription) => {
      this.ticksSourcesSubscription = subscription;
    });

    return predefineTicks;
  }

  subscribeAttributes(datasource: Datasource[], typeAttributes: attributesGaugeType): Observable<IWidgetSubscription> {
    if (!datasource.length) {
      return EMPTY;
    }

    const levelColorsSourcesSubscriptionOptions: WidgetSubscriptionOptions = {
      datasources: datasource,
      useDashboardTimewindow: false,
      type: widgetType.latest,
      callbacks: {
        onDataUpdated: (subscription) => {
          this.updateAttribute(subscription.data, typeAttributes);
        }
      }
    };

    return this.ctx.subscriptionApi.createSubscription(levelColorsSourcesSubscriptionOptions, true);
  }

  updateAttribute(data: Array<DatasourceData>, typeAttributes: attributesGaugeType) {
    for (const keyData of data) {
      if (keyData && keyData.data && keyData.data[0]) {
        const attrValue = keyData.data[0][1];
        if (isFinite(attrValue)) {
          for (const setting of keyData.dataKey.settings) {
            switch (typeAttributes) {
              case 'levelColors':
                this.localSettings.levelColors[setting.index] = {
                  value: attrValue,
                  color: setting.color
                };
                break;
              case 'ticks':
                this.localSettings.ticks[setting] = attrValue;
                break;
            }
          }
        }
      }
    }
    this.updateSetting();
  }

  updateSetting() {
    (this.gauge.options as CanvasDigitalGaugeOptions).ticks = this.localSettings.ticks;
    (this.gauge.options as CanvasDigitalGaugeOptions).levelColors = this.localSettings.levelColors;
    this.gauge.options = CanvasDigitalGauge.configure(this.gauge.options);
    this.gauge.update({} as CanvasDigitalGaugeOptions);
  }

  update() {
    if (this.ctx.data.length > 0) {
      const cellData = this.ctx.data[0];
      if (cellData.data.length > 0) {
        const tvPair = cellData.data[cellData.data.length -
        1];
        let timestamp;
        if (this.localSettings.showTimestamp) {
          timestamp = tvPair[0];
          const filter = this.ctx.$injector.get(DatePipe);
          (this.gauge.options as CanvasDigitalGaugeOptions).label =
            filter.transform(timestamp, this.localSettings.timestampFormat);
        }
        const value = tvPair[1];
        if (value !== this.gauge.value) {
          if (!this.gauge.options.animation) {
            this.gauge._value = value;
          }
          this.gauge.value = value;
        } else if (this.localSettings.showTimestamp && this.gauge.timestamp !== timestamp) {
          this.gauge.timestamp = timestamp;
        }
      }
    }
  }

  mobileModeChanged() {
    const animation = this.ctx.settings.animation !== false && !this.ctx.isMobile;
    this.gauge.update({animation, isMobile: this.ctx.isMobile} as CanvasDigitalGaugeOptions);
  }

  resize() {
    this.gauge.update({width: this.ctx.width, height: this.ctx.height} as GenericOptions);
  }

}
