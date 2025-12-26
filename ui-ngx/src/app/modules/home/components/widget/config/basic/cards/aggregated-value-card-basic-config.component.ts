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

import { ChangeDetectorRef, Component, Injector } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { BasicWidgetConfigComponent } from '@home/components/widget/config/widget-config.component.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { ComparisonResultType, DataKey, Datasource, WidgetConfig, widgetTitleAutocompleteValues, } from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import {
  getTimewindowConfig,
  setTimewindowConfig
} from '@home/components/widget/config/timewindow-config-panel.component';
import { isUndefined } from '@core/utils';
import {
  ColorType,
  constantColor,
  cssSizeToStrSize,
  DateFormatProcessor,
  DateFormatSettings,
  getDataKey,
  resolveCssSize
} from '@shared/models/widget-settings.models';
import {
  aggregatedValueCardDefaultSettings,
  AggregatedValueCardKeyPosition,
  AggregatedValueCardKeySettings,
  AggregatedValueCardWidgetSettings
} from '@home/components/widget/lib/cards/aggregated-value-card.models';
import {
  AggregationType,
  HistoryWindowType,
  HOUR,
  QuickTimeInterval,
  TimewindowType
} from '@shared/models/time/time.models';

@Component({
  selector: 'tb-aggregated-value-card-basic-config',
  templateUrl: './aggregated-value-card-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class AggregatedValueCardBasicConfigComponent extends BasicWidgetConfigComponent {

  public get datasource(): Datasource {
    const datasources: Datasource[] = this.aggregatedValueCardWidgetConfigForm.get('datasources').value;
    if (datasources && datasources.length) {
      return datasources[0];
    } else {
      return null;
    }
  }

  public get keyName(): string {
    const dataKey = getDataKey(this.aggregatedValueCardWidgetConfigForm.get('datasources').value);
    if (dataKey) {
      return dataKey.name;
    } else {
      return null;
    }
  }

  aggregatedValueCardWidgetConfigForm: UntypedFormGroup;

  datePreviewFn = this._datePreviewFn.bind(this);

  predefinedValues = widgetTitleAutocompleteValues;
  
  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private cd: ChangeDetectorRef,
              private $injector: Injector,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.aggregatedValueCardWidgetConfigForm;
  }

  protected setupDefaults(configData: WidgetConfigComponentData) {
    super.setupDefaults(configData);
    configData.config.useDashboardTimewindow = false;
    configData.config.displayTimewindow = true;
    configData.config.timewindow = {
      selectedTab: TimewindowType.HISTORY,
      history: {
        historyType: HistoryWindowType.INTERVAL,
        quickInterval: QuickTimeInterval.CURRENT_MONTH_SO_FAR,
        interval: 12 * HOUR,
      },
      aggregation: {
        type: AggregationType.AVG,
        limit: 5000
      }
    };
  }

  protected defaultDataKeys(configData: WidgetConfigComponentData): DataKey[] {
    return [
      { name: 'watermeter', label: 'Watermeter', type: DataKeyType.timeseries, color: 'rgba(0, 0, 0, 0.87)', units: 'm³', decimals: 0 }
    ];
  }

  protected defaultLatestDataKeys(configData: WidgetConfigComponentData): DataKey[] {
    return this.createDefaultAggregatedValueLatestDataKeys(configData, 'watermeter', 'm³', 0);
  }

  createDefaultAggregatedValueLatestDataKeys(configData: WidgetConfigComponentData, keyName: string,
                                             units: string, decimals: number): DataKey[] {
    let centerKeySettings: AggregatedValueCardKeySettings = {
      position: AggregatedValueCardKeyPosition.center,
      font: {
        family: 'Roboto',
        size: 52,
        sizeUnit: 'px',
        style: 'normal',
        weight: '500',
        lineHeight: '1'
      },
      color: constantColor('rgba(0, 0, 0, 0.87)'),
      showArrow: false
    };
    let rightTopKeySettings: AggregatedValueCardKeySettings = {
      position: AggregatedValueCardKeyPosition.rightTop,
      font: {
        family: 'Roboto',
        size: 14,
        sizeUnit: 'px',
        style: 'normal',
        weight: '500',
        lineHeight: '1'
      },
      color: {
        color: 'rgba(0, 0, 0, 0.87)',
        type: ColorType.range,
        rangeList: {
          range: [
            {to: 0, color: '#198038'},
            {from: 0, to: 0, color: 'rgba(0, 0, 0, 0.87)'},
            {from: 0, color: '#D12730'}
          ]
        },
        colorFunction: ''
      },
      showArrow: true
    };
    let rightBottomKeySettings: AggregatedValueCardKeySettings = {
      position: AggregatedValueCardKeyPosition.rightBottom,
      font: {
        family: 'Roboto',
        size: 11,
        sizeUnit: 'px',
        style: 'normal',
        weight: '400',
        lineHeight: '1'
      },
      color: constantColor('rgba(0, 0, 0, 0.38)'),
      showArrow: false
    };
    const datasources: Datasource[] = configData.config.datasources;
    if (datasources?.length) {
      const latestDataKeys = datasources[0].latestDataKeys;
      if (latestDataKeys) {
        const center = latestDataKeys.find(dataKey => dataKey.settings?.position === AggregatedValueCardKeyPosition.center);
        if (center) {
          centerKeySettings = {...centerKeySettings, ...center.settings};
        }
        const rightTop = latestDataKeys.find(dataKey => dataKey.settings?.position === AggregatedValueCardKeyPosition.rightTop);
        if (rightTop) {
          rightTopKeySettings = {...rightTopKeySettings, ...rightTop.settings};
        }
        const rightBottom = latestDataKeys.find(dataKey => dataKey.settings?.position === AggregatedValueCardKeyPosition.rightBottom);
        if (rightBottom) {
          rightBottomKeySettings = {...rightBottomKeySettings, ...rightBottom.settings};
        }
      }
    }
    return [
      {
        name: keyName, label: 'Latest', type: DataKeyType.timeseries, units, decimals,
        aggregationType: AggregationType.NONE,
        settings: centerKeySettings
      },
      {
        name: keyName, label: 'Delta percent', type: DataKeyType.timeseries, units: '%', decimals: 0,
        aggregationType: AggregationType.AVG,
        comparisonEnabled: true,
        timeForComparison: 'previousInterval',
        comparisonResultType: ComparisonResultType.DELTA_PERCENT,
        settings: rightTopKeySettings
      },
      {
        name: keyName, label: 'Delta absolute', type: DataKeyType.timeseries, units, decimals: 1,
        aggregationType: AggregationType.AVG,
        comparisonEnabled: true,
        timeForComparison: 'previousInterval',
        comparisonResultType: ComparisonResultType.DELTA_ABSOLUTE,
        settings: rightBottomKeySettings
      }
    ];
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: AggregatedValueCardWidgetSettings = {...aggregatedValueCardDefaultSettings, ...(configData.config.settings || {})};
    const dataKey = getDataKey(configData.config.datasources);
    const keyName = dataKey?.name;
    const iconSize = resolveCssSize(configData.config.iconSize);
    this.aggregatedValueCardWidgetConfigForm = this.fb.group({
      timewindowConfig: [getTimewindowConfig(configData.config), []],
      datasources: [configData.config.datasources, []],

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

      showSubtitle: [settings.showSubtitle, []],
      subtitle: [settings.subtitle, []],
      subtitleFont: [settings.subtitleFont, []],
      subtitleColor: [settings.subtitleColor, []],

      showDate: [settings.showDate, []],
      dateFormat: [settings.dateFormat, []],
      dateFont: [settings.dateFont, []],
      dateColor: [settings.dateColor, []],

      showChart: [settings.showChart, []],
      chartUnits: [dataKey?.units, []],
      chartDecimals: [dataKey?.decimals, []],
      chartColor: [dataKey?.color, []],

      values: [this.getValues(configData.config.datasources, keyName), []],

      background: [settings.background, []],

      cardButtons: [this.getCardButtons(configData.config), []],
      borderRadius: [configData.config.borderRadius, []],
      padding: [settings.padding, []],

      actions: [configData.config.actions || {}, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    setTimewindowConfig(this.widgetConfig.config, config.timewindowConfig);
    this.widgetConfig.config.datasources = config.datasources;

    this.widgetConfig.config.showTitle = config.showTitle;
    this.widgetConfig.config.title = config.title;
    this.widgetConfig.config.titleFont = config.titleFont;
    this.widgetConfig.config.titleColor = config.titleColor;

    this.widgetConfig.config.showTitleIcon = config.showIcon;
    this.widgetConfig.config.iconSize = cssSizeToStrSize(config.iconSize, config.iconSizeUnit);
    this.widgetConfig.config.titleIcon = config.icon;
    this.widgetConfig.config.iconColor = config.iconColor;

    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};

    this.widgetConfig.config.settings.autoScale = config.autoScale;
    this.widgetConfig.config.settings.showSubtitle = config.showSubtitle;
    this.widgetConfig.config.settings.subtitle = config.subtitle;
    this.widgetConfig.config.settings.subtitleFont = config.subtitleFont;
    this.widgetConfig.config.settings.subtitleColor = config.subtitleColor;

    this.widgetConfig.config.settings.showDate = config.showDate;
    this.widgetConfig.config.settings.dateFormat = config.dateFormat;
    this.widgetConfig.config.settings.dateFont = config.dateFont;
    this.widgetConfig.config.settings.dateColor = config.dateColor;

    this.widgetConfig.config.settings.showChart = config.showChart;

    const dataKey = getDataKey(this.widgetConfig.config.datasources);
    if (dataKey) {
      dataKey.units = config.chartUnits;
      dataKey.decimals = config.chartDecimals;
      dataKey.color = config.chartColor;
    }

    this.setValues(config.values, this.widgetConfig.config.datasources);

    this.widgetConfig.config.settings.background = config.background;

    this.setCardButtons(config.cardButtons, this.widgetConfig.config);
    this.widgetConfig.config.borderRadius = config.borderRadius;
    this.widgetConfig.config.settings.padding = config.padding;

    this.widgetConfig.config.actions = config.actions;
    return this.widgetConfig;
  }

  protected validatorTriggers(): string[] {
    return ['showTitle', 'showIcon', 'showSubtitle', 'showDate', 'showChart'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const showTitle: boolean = this.aggregatedValueCardWidgetConfigForm.get('showTitle').value;
    const showIcon: boolean = this.aggregatedValueCardWidgetConfigForm.get('showIcon').value;
    const showSubtitle: boolean = this.aggregatedValueCardWidgetConfigForm.get('showSubtitle').value;
    const showDate: boolean = this.aggregatedValueCardWidgetConfigForm.get('showDate').value;
    const showChart: boolean = this.aggregatedValueCardWidgetConfigForm.get('showChart').value;

    if (showTitle) {
      this.aggregatedValueCardWidgetConfigForm.get('title').enable();
      this.aggregatedValueCardWidgetConfigForm.get('titleFont').enable();
      this.aggregatedValueCardWidgetConfigForm.get('titleColor').enable();
      this.aggregatedValueCardWidgetConfigForm.get('showIcon').enable({emitEvent: false});
      if (showIcon) {
        this.aggregatedValueCardWidgetConfigForm.get('iconSize').enable();
        this.aggregatedValueCardWidgetConfigForm.get('iconSizeUnit').enable();
        this.aggregatedValueCardWidgetConfigForm.get('icon').enable();
        this.aggregatedValueCardWidgetConfigForm.get('iconColor').enable();
      } else {
        this.aggregatedValueCardWidgetConfigForm.get('iconSize').disable();
        this.aggregatedValueCardWidgetConfigForm.get('iconSizeUnit').disable();
        this.aggregatedValueCardWidgetConfigForm.get('icon').disable();
        this.aggregatedValueCardWidgetConfigForm.get('iconColor').disable();
      }
    } else {
      this.aggregatedValueCardWidgetConfigForm.get('title').disable();
      this.aggregatedValueCardWidgetConfigForm.get('titleFont').disable();
      this.aggregatedValueCardWidgetConfigForm.get('titleColor').disable();
      this.aggregatedValueCardWidgetConfigForm.get('showIcon').disable({emitEvent: false});
      this.aggregatedValueCardWidgetConfigForm.get('iconSize').disable();
      this.aggregatedValueCardWidgetConfigForm.get('iconSizeUnit').disable();
      this.aggregatedValueCardWidgetConfigForm.get('icon').disable();
      this.aggregatedValueCardWidgetConfigForm.get('iconColor').disable();
    }

    if (showSubtitle) {
      this.aggregatedValueCardWidgetConfigForm.get('subtitle').enable();
      this.aggregatedValueCardWidgetConfigForm.get('subtitleFont').enable();
      this.aggregatedValueCardWidgetConfigForm.get('subtitleColor').enable();
    } else {
      this.aggregatedValueCardWidgetConfigForm.get('subtitle').disable();
      this.aggregatedValueCardWidgetConfigForm.get('subtitleFont').disable();
      this.aggregatedValueCardWidgetConfigForm.get('subtitleColor').disable();
    }

    if (showDate) {
      this.aggregatedValueCardWidgetConfigForm.get('dateFormat').enable();
      this.aggregatedValueCardWidgetConfigForm.get('dateFont').enable();
      this.aggregatedValueCardWidgetConfigForm.get('dateColor').enable();
    } else {
      this.aggregatedValueCardWidgetConfigForm.get('dateFormat').disable();
      this.aggregatedValueCardWidgetConfigForm.get('dateFont').disable();
      this.aggregatedValueCardWidgetConfigForm.get('dateColor').disable();
    }

    if (showChart) {
      this.aggregatedValueCardWidgetConfigForm.get('chartUnits').enable();
      this.aggregatedValueCardWidgetConfigForm.get('chartDecimals').enable();
      this.aggregatedValueCardWidgetConfigForm.get('chartColor').enable();
    } else {
      this.aggregatedValueCardWidgetConfigForm.get('chartUnits').disable();
      this.aggregatedValueCardWidgetConfigForm.get('chartDecimals').disable();
      this.aggregatedValueCardWidgetConfigForm.get('chartColor').disable();
    }
  }

  private getValues(datasources: Datasource[], keyName: string): DataKey[] {
    if (datasources && datasources.length) {
      return (datasources[0].latestDataKeys || []).filter(k => k.name === keyName);
    }
    return [];
  }

  private setValues(values: DataKey[], datasources?: Datasource[]) {
    if (datasources && datasources.length) {
      datasources[0].latestDataKeys = values;
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

  private _datePreviewFn(): string {
    const dateFormat: DateFormatSettings = this.aggregatedValueCardWidgetConfigForm.get('dateFormat').value;
    const processor = DateFormatProcessor.fromSettings(this.$injector, dateFormat);
    processor.update(Date.now());
    return processor.formatted;
  }
}
