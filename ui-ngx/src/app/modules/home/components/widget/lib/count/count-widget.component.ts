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

import { ChangeDetectorRef, Component, Input, OnInit, TemplateRef } from '@angular/core';
import { WidgetContext } from '@home/models/widget-component.models';
import { formatValue } from '@core/utils';
import {
  ColorProcessor,
  ComponentStyle,
  getSingleTsValue,
  iconStyle,
  textStyle
} from '@shared/models/widget-settings.models';
import { WidgetComponent } from '@home/components/widget/widget.component';
import {
  CountCardLayout,
  countDefaultSettings,
  CountWidgetSettings
} from '@home/components/widget/lib/count/count-widget.models';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
  selector: 'tb-count-widget',
  templateUrl: './count-widget.component.html',
  styleUrls: ['./count-widget.component.scss']
})
export class CountWidgetComponent implements OnInit {

  settings: CountWidgetSettings;

  countCardLayout = CountCardLayout;

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  @coerceBoolean()
  @Input()
  alarmElseEntity: boolean;

  layout: CountCardLayout;

  showLabel = true;
  label: string;
  labelStyle: ComponentStyle = {};
  labelColor: ColorProcessor;

  showIcon = true;
  icon = '';
  iconStyle: ComponentStyle = {};
  iconColor: ColorProcessor;

  showIconBackground = true;
  iconBackgroundSize: string;
  iconBackgroundStyle: ComponentStyle = {};
  iconBackgroundColor: ColorProcessor;

  valueText = 'N/A';
  valueStyle: ComponentStyle = {};
  valueColor: ColorProcessor;

  showChevron = false;
  chevronStyle: ComponentStyle = {};

  hasCardClickAction = false;

  constructor(private widgetComponent: WidgetComponent,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.ctx.$scope.countWidget = this;
    this.settings = {...countDefaultSettings(this.alarmElseEntity), ...this.ctx.settings};

    this.layout = this.settings.layout;

    this.showLabel = this.settings.showLabel;
    this.label = this.settings.label;
    this.labelStyle = textStyle(this.settings.labelFont, '0.4px');
    this.labelColor = ColorProcessor.fromSettings(this.settings.labelColor);

    this.showIcon = this.settings.showIcon;
    this.icon = this.settings.icon;
    this.iconStyle = iconStyle(this.settings.iconSize, this.settings.iconSizeUnit);
    this.iconColor = ColorProcessor.fromSettings(this.settings.iconColor);

    this.showIconBackground = this.settings.showIconBackground;
    if (this.showIconBackground) {
      this.iconBackgroundSize = this.settings.iconBackgroundSize + this.settings.iconBackgroundSizeUnit;
    } else {
      this.iconBackgroundSize = this.settings.iconSize + this.settings.iconSizeUnit;
    }
    this.iconBackgroundStyle = {
      width: this.iconBackgroundSize,
      height: this.iconBackgroundSize,
      borderRadius: '4px'
    };
    this.iconBackgroundColor = ColorProcessor.fromSettings(this.settings.iconBackgroundColor);

    this.valueStyle = textStyle(this.settings.valueFont, '0.1px');
    this.valueColor = ColorProcessor.fromSettings(this.settings.valueColor);

    this.showChevron = this.settings.showChevron;
    this.chevronStyle = iconStyle(this.settings.chevronSize, this.settings.chevronSizeUnit);
    this.chevronStyle.color = this.settings.chevronColor;

    this.hasCardClickAction = this.ctx.actionsApi.getActionDescriptors('cardClick').length > 0;
  }

  public onInit() {
  }

  public onDataUpdated() {
    const tsValue = getSingleTsValue(this.ctx.data);
    let value: any;
    if (tsValue) {
      value = tsValue[1];
      this.valueText = formatValue(value, 0);
    } else {
      this.valueText = 'N/A';
    }
    this.labelColor.update(value);
    this.iconColor.update(value);
    this.iconBackgroundColor.update(value);
    this.valueColor.update(value);
    this.cd.detectChanges();
  }

  public cardClick($event: Event) {
    this.ctx.actionsApi.cardClick($event);
  }
}
