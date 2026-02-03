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

import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  Input,
  OnDestroy,
  OnInit,
  Renderer2,
  TemplateRef,
  ViewChild
} from '@angular/core';
import { WidgetContext } from '@home/models/widget-component.models';
import { formatValue } from '@core/utils';
import {
  ColorProcessor,
  ComponentStyle,
  getSingleTsValue,
  iconStyle,
  textStyle
} from '@shared/models/widget-settings.models';
import {
  CountCardLayout,
  countDefaultSettings,
  CountWidgetSettings
} from '@home/components/widget/lib/count/count-widget.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { UtilsService } from '@core/services/utils.service';

const layoutHeight = 36;
const layoutHeightWithTitle = 60;
const layoutPadding = 24;

@Component({
    selector: 'tb-count-widget',
    templateUrl: './count-widget.component.html',
    styleUrls: ['./count-widget.component.scss'],
    standalone: false
})
export class CountWidgetComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('countPanel', {static: false})
  countPanel: ElementRef<HTMLElement>;

  @ViewChild('countPanelContent', {static: false})
  countPanelContent: ElementRef<HTMLElement>;

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

  private panelResize$: ResizeObserver;
  private hasTitle = false;

  constructor(private renderer: Renderer2,
              private utils: UtilsService,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.ctx.$scope.countWidget = this;
    this.settings = {...countDefaultSettings(this.alarmElseEntity), ...this.ctx.settings};

    this.layout = this.settings.layout;

    this.showLabel = this.settings.showLabel;
    this.label = this.utils.customTranslation(this.settings.label, this.settings.label);
    this.labelStyle = textStyle(this.settings.labelFont);
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

    this.valueStyle = textStyle(this.settings.valueFont);
    this.valueColor = ColorProcessor.fromSettings(this.settings.valueColor);

    this.showChevron = this.settings.showChevron;
    this.chevronStyle = iconStyle(this.settings.chevronSize, this.settings.chevronSizeUnit);
    this.chevronStyle.color = this.settings.chevronColor;

    this.hasCardClickAction = this.ctx.actionsApi.getActionDescriptors('cardClick').length > 0;
    this.hasTitle = this.ctx.widgetConfig.showTitle;
  }

  public ngAfterViewInit() {
    if (this.settings.autoScale) {
      const height = this.hasTitle ? layoutHeightWithTitle : layoutHeight;
      this.renderer.setStyle(this.countPanelContent.nativeElement, 'height', height + 'px');
      this.renderer.setStyle(this.countPanelContent.nativeElement, 'overflow', 'visible');
      this.renderer.setStyle(this.countPanelContent.nativeElement, 'position', 'absolute');
      this.panelResize$ = new ResizeObserver(() => {
        this.onResize();
      });
      this.panelResize$.observe(this.countPanel.nativeElement);
      this.onResize();
    }
  }

  ngOnDestroy() {
    if (this.panelResize$) {
      this.panelResize$.disconnect();
    }
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

  private onResize() {
    const panelWidth = this.countPanel.nativeElement.getBoundingClientRect().width - layoutPadding;
    const panelHeight = this.countPanel.nativeElement.getBoundingClientRect().height - layoutPadding;
    const targetWidth = panelWidth;
    let minAspect = 0.25;
    if (this.settings.showChevron) {
      minAspect -= 0.05;
    }
    if (this.hasTitle) {
      minAspect += 0.15;
    }
    const aspect = Math.min(panelHeight / targetWidth, minAspect);
    const targetHeight = targetWidth * aspect;
    const height = this.hasTitle ? layoutHeightWithTitle : layoutHeight;
    const scale = targetHeight / height;
    const width = targetWidth / scale;
    this.renderer.setStyle(this.countPanelContent.nativeElement, 'width', width + 'px');
    this.renderer.setStyle(this.countPanelContent.nativeElement, 'transform', `scale(${scale})`);
  }
}
