///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  Input,
  OnInit,
  TemplateRef,
  ViewChild
} from '@angular/core';
import {
  aggregatedValueCardDefaultSettings,
  AggregatedValueCardKeyPosition,
  AggregatedValueCardValue,
  AggregatedValueCardWidgetSettings,
  computeAggregatedCardValue,
  getTsValueByLatestDataKey
} from '@home/components/widget/lib/cards/aggregated-value-card.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { Observable } from 'rxjs';
import {
  backgroundStyle,
  ComponentStyle,
  DateFormatProcessor,
  getDataKey,
  getLatestSingleTsValue,
  overlayStyle,
  textStyle
} from '@shared/models/widget-settings.models';
import { DatePipe } from '@angular/common';
import { TbFlot } from '@home/components/widget/lib/flot-widget';
import { TbFlotKeySettings, TbFlotSettings } from '@home/components/widget/lib/flot-widget.models';
import { DataKey } from '@shared/models/widget.models';
import { formatNumberValue, formatValue, isDefined } from '@core/utils';
import { map } from 'rxjs/operators';

@Component({
  selector: 'tb-aggregated-value-card-widget',
  templateUrl: './aggregated-value-card-widget.component.html',
  styleUrls: ['./aggregated-value-card-widget.component.scss']
})
export class AggregatedValueCardWidgetComponent implements OnInit, AfterViewInit {

  @ViewChild('chartElement', {static: false}) chartElement: ElementRef;

  aggregatedValueCardKeyPosition = AggregatedValueCardKeyPosition;

  settings: AggregatedValueCardWidgetSettings;

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  showSubtitle = true;
  subtitle$: Observable<string>;
  subtitleStyle: ComponentStyle = {};
  subtitleColor: string;

  showValues = false;

  values: {[key: string]: AggregatedValueCardValue} = {};

  showChart = true;

  showDate = true;
  dateFormat: DateFormatProcessor;
  dateStyle: ComponentStyle = {};
  dateColor: string;

  backgroundStyle: ComponentStyle = {};
  overlayStyle: ComponentStyle = {};

  private flot: TbFlot;
  private flotDataKey: DataKey;

  private lastUpdateTs: number;

  tickMin$: Observable<string>;
  tickMax$: Observable<string>;

  constructor(private date: DatePipe,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.ctx.$scope.aggregatedValueCardWidget = this;
    this.settings = {...aggregatedValueCardDefaultSettings, ...this.ctx.settings};
    this.showSubtitle = this.settings.showSubtitle;
    const subtitle = this.settings.subtitle;
    this.subtitle$ = this.ctx.registerLabelPattern(subtitle, this.subtitle$);
    this.subtitleStyle = textStyle(this.settings.subtitleFont, '0.25px');
    this.subtitleColor =  this.settings.subtitleColor;

    const dataKey = getDataKey(this.ctx.defaultSubscription.datasources);
    if (dataKey?.name && this.ctx.defaultSubscription.firstDatasource?.latestDataKeys?.length) {
      const dataKeys = this.ctx.defaultSubscription.firstDatasource?.latestDataKeys;
      for (const position of Object.keys(AggregatedValueCardKeyPosition)) {
        const value = computeAggregatedCardValue(dataKeys, dataKey?.name, AggregatedValueCardKeyPosition[position]);
        if (value) {
          this.values[position] = value;
        }
      }
      this.showValues = !!Object.keys(this.values).length;
    }

    this.showChart = this.settings.showChart;
    if (this.showChart) {
      if (this.ctx.defaultSubscription.firstDatasource?.dataKeys?.length) {
        this.flotDataKey = this.ctx.defaultSubscription.firstDatasource?.dataKeys[0];
        this.flotDataKey.settings = {
          fillLines: false,
          showLines: true,
          lineWidth: 2
        } as TbFlotKeySettings;
      }
    }

    this.showDate = this.settings.showDate;
    this.dateFormat = DateFormatProcessor.fromSettings(this.ctx.$injector, this.settings.dateFormat);
    this.dateStyle = textStyle(this.settings.dateFont,  '0.25px');
    this.dateColor = this.settings.dateColor;

    this.backgroundStyle = backgroundStyle(this.settings.background);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);
  }

  ngAfterViewInit(): void {
    if (this.showChart) {
      const settings = {
        shadowSize: 0,
        enableSelection: false,
        smoothLines: false,
        grid: {
          tickColor: 'rgba(0,0,0,0.12)',
          horizontalLines: true,
          verticalLines: false,
          outlineWidth: 0,
          minBorderMargin: 0,
          margin: 0
        },
        yaxis: {
          showLabels: false,
          tickGenerator: 'return [(axis.max + axis.min) / 2];'
        },
        xaxis: {
          showLabels: false
        }
      } as TbFlotSettings;
      this.flot = new TbFlot(this.ctx, 'line', $(this.chartElement.nativeElement), settings);
      this.tickMin$ = this.flot.yMin$.pipe(
        map((value) => formatValue(value, (this.flotDataKey?.decimals || this.ctx.decimals),
            (this.flotDataKey?.units || this.ctx.units))
      ));
      this.tickMax$ = this.flot.yMax$.pipe(
        map((value) => formatValue(value, (this.flotDataKey?.decimals || this.ctx.decimals),
          (this.flotDataKey?.units || this.ctx.units))
        ));
    }
  }

  public onInit() {
    const borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.overlayStyle = {...this.overlayStyle, ...{borderRadius}};
    this.cd.detectChanges();
  }

  public onDataUpdated() {
    const tsValue = getLatestSingleTsValue(this.ctx.data);
    let ts;
    if (tsValue) {
      ts = tsValue[0];
    }

    if (this.showChart) {
      this.flot.update();
    }

    this.updateLastUpdateTs(ts);
    this.cd.detectChanges();
  }

  public onLatestDataUpdated() {
    if (this.showValues) {
      for (const aggValue of Object.values(this.values)) {
        const tsValue = getTsValueByLatestDataKey(this.ctx.latestData, aggValue.key);
        let ts;
        let value;
        if (tsValue) {
          ts = tsValue[0];
          value = tsValue[1];
          aggValue.value = formatValue(value, (aggValue.key.decimals || this.ctx.decimals), null, false);
        } else {
          aggValue.value = 'N/A';
        }
        const numeric = formatNumberValue(value, (aggValue.key.decimals || this.ctx.decimals));
        aggValue.color.update(numeric);
        if (aggValue.showArrow && isDefined(numeric)) {
          aggValue.upArrow = numeric > 0;
          aggValue.downArrow = numeric < 0;
        } else {
          aggValue.upArrow = aggValue.downArrow = false;
        }
        this.updateLastUpdateTs(ts);
      }
      this.cd.detectChanges();
    }
  }

  public onResize() {
    if (this.showChart) {
      this.flot.resize();
    }
  }

  public onEditModeChanged() {
    if (this.showChart) {
      this.flot.checkMouseEvents();
    }
  }

  public onDestroy() {
    if (this.showChart) {
      this.flot.destroy();
    }
  }

  private updateLastUpdateTs(ts: number) {
    if (ts && (!this.lastUpdateTs || ts > this.lastUpdateTs)) {
      this.lastUpdateTs = ts;
      this.dateFormat.update(ts);
    }
  }

}
