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
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { WidgetContext } from '@home/models/widget-component.models';
import { isDefinedAndNotNull, isNumeric } from '@core/utils';
import {
  backgroundStyle,
  ColorProcessor,
  ComponentStyle,
  createValueFormatterFromSettings,
  getSingleTsValue,
  overlayStyle,
  textStyle,
  ValueFormatProcessor
} from '@shared/models/widget-settings.models';
import {
  batteryLevelDefaultSettings,
  BatteryLevelLayout,
  BatteryLevelWidgetSettings
} from '@home/components/widget/lib/indicator/battery-level-widget.models';
import { Observable } from 'rxjs';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';

const verticalBatteryDimensions = {
  shapeAspectRatio: 64 / 113,
  widthRatio: {
    valueTopBottomPaddingRatio: 8 / 64,
    valueLeftRightPaddingRatio: 12 / 64,
    valueFontSizeRatio: 20 / 64,
    valueLineHeightRaio: 24 / 64
  },
  heightRatio: {
    valueTopBottomPaddingRatio: 8 / 113,
    valueLeftRightPaddingRatio: 12 / 113,
    valueFontSizeRatio: 20 / 113,
    valueLineHeightRaio: 24 / 113
  }
};

const horizontalBatteryDimensions = {
  shapeAspectRatio: 113 / 64,
  heightRatio: {
    valueTopBottomPaddingRatio: 4 / 64,
    valueFontSizeRatio: 20 / 64,
    valueLineHeightRatio: 24 / 64
  }
};

@Component({
  selector: 'tb-battery-level-widget',
  templateUrl: './battery-level-widget.component.html',
  styleUrls: ['./battery-level-widget.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class BatteryLevelWidgetComponent implements OnInit, OnDestroy, AfterViewInit {

  @ViewChild('batteryLevelContent', {static: true})
  batteryLevelContent: ElementRef<HTMLElement>;

  @ViewChild('batteryLevelBox', {static: true})
  batteryLevelBox: ElementRef<HTMLElement>;

  @ViewChild('batteryLevelRectangle', {static: true})
  batteryLevelRectangle: ElementRef<HTMLElement>;

  @ViewChild('batteryLevelValueBox', {static: false})
  batteryLevelValueBox: ElementRef<HTMLElement>;

  @ViewChild('batteryLevelValue', {static: false})
  batteryLevelValue: ElementRef<HTMLElement>;

  settings: BatteryLevelWidgetSettings;

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  layout: BatteryLevelLayout;
  layoutClass = 'vertical';

  vertical = true;
  solid = true;

  showValue = true;
  autoScaleValueSize = true;
  valueText = 'N/A';
  valueStyle: ComponentStyle = {};
  valueColor: ColorProcessor;

  value: number;

  batteryFillValue: number;

  batterySections: boolean[];
  dividedBorderRadius: string;
  dividedGap: string;

  batteryLevelColor: ColorProcessor;

  batteryShapeColor: ColorProcessor;

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};
  padding: string;

  batteryBoxResize$: ResizeObserver;

  hasCardClickAction = false;

  private valueFormat: ValueFormatProcessor;

  constructor(private imagePipe: ImagePipe,
              private sanitizer: DomSanitizer,
              private renderer: Renderer2,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.ctx.$scope.batteryLevelWidget = this;
    this.settings = {...batteryLevelDefaultSettings, ...this.ctx.settings};

    this.valueFormat = createValueFormatterFromSettings(this.ctx);

    this.layout = this.settings.layout;

    this.vertical = [BatteryLevelLayout.vertical_solid, BatteryLevelLayout.vertical_divided].includes(this.layout);
    this.layoutClass = this.vertical ? 'vertical' : 'horizontal';
    this.solid = [BatteryLevelLayout.vertical_solid, BatteryLevelLayout.horizontal_solid].includes(this.layout);
    if (!this.solid) {
      let sectionsCount = this.settings.sectionsCount;
      if (!sectionsCount) {
        sectionsCount = 4;
      }
      sectionsCount = Math.min(Math.max(sectionsCount, 2), 20);
      this.batterySections = Array.from(Array(sectionsCount), () => false);
      const gap = 1 + (24 - sectionsCount) / 10;
      this.dividedGap = `${gap}%`;
      const containerAspect = 0.5567;
      const sectionHeight = (100 - (gap * (sectionsCount - 1))) / sectionsCount;
      const sectionAspect = 100 * containerAspect / sectionHeight;
      const rad1 = 8.425 - sectionsCount * 0.32125;
      const rad2 = rad1 * sectionAspect;
      if (this.vertical) {
        this.dividedBorderRadius = `${rad1}% / ${rad2}%`;
      } else {
        this.dividedBorderRadius = `${rad2}% / ${rad1}%`;
      }
    }

    this.showValue = this.settings.showValue;
    this.autoScaleValueSize = this.showValue && this.settings.autoScaleValueSize;
    this.valueStyle = textStyle(this.settings.valueFont);
    this.valueColor = ColorProcessor.fromColorProcessorSettings({
      settings: this.settings.valueColor,
      ctx: this.ctx,
      minGradientValue: 0,
      maxGradientValue: 100
    });

    this.batteryLevelColor = ColorProcessor.fromColorProcessorSettings({
      settings: this.settings.batteryLevelColor,
      ctx: this.ctx,
      minGradientValue: 0,
      maxGradientValue: 100
    });

    this.batteryShapeColor = ColorProcessor.fromColorProcessorSettings({
      settings: this.settings.batteryShapeColor,
      ctx: this.ctx,
      minGradientValue: 0,
      maxGradientValue: 100
    });

    this.backgroundStyle$ = backgroundStyle(this.settings.background, this.imagePipe, this.sanitizer);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);
    this.padding = this.settings.background.overlay.enabled ? undefined : this.settings.padding;

    this.hasCardClickAction = this.ctx.actionsApi.getActionDescriptors('cardClick').length > 0;

    this.valueColor.colorUpdated?.subscribe(() => this.cd.markForCheck());
    this.batteryLevelColor.colorUpdated?.subscribe(() => this.cd.markForCheck());
    this.batteryShapeColor.colorUpdated?.subscribe(() => this.cd.markForCheck());
  }

  ngAfterViewInit() {
    this.batteryBoxResize$ = new ResizeObserver(() => {
      this.onResize();
    });
    this.batteryBoxResize$.observe(this.batteryLevelContent.nativeElement);
    if (this.showValue) {
      this.batteryBoxResize$.observe(this.batteryLevelValueBox.nativeElement);
    }
    this.onResize();
  }

  ngOnDestroy() {
    if (this.batteryBoxResize$) {
      this.batteryBoxResize$.disconnect();
    }

    this.batteryLevelColor.destroy();
    this.valueColor.destroy();
    this.batteryShapeColor.destroy();
  }

  public onInit() {
    const borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.overlayStyle = {...this.overlayStyle, ...{borderRadius}};
    this.cd.detectChanges();
  }

  public onDataUpdated() {
    const tsValue = getSingleTsValue(this.ctx.data);
    this.batteryFillValue = 0;
    if (tsValue && isDefinedAndNotNull(tsValue[1]) && isNumeric(tsValue[1])) {
      this.value = tsValue[1];
      this.batteryFillValue = this.parseBatteryFillValue(this.value);
      this.valueText = this.valueFormat.format(this.value);
    } else {
      this.valueText = 'N/A';
    }
    if (!this.solid) {
      const sectionSize = 100 / this.batterySections.length;
      for (let i=0; i<this.batterySections.length; i++) {
        this.batterySections[i] = this.value > sectionSize * i;
      }
    }
    this.valueColor.update(this.value);
    this.batteryLevelColor.update(this.value);
    this.batteryShapeColor.update(this.value);
    this.cd.detectChanges();
  }

  parseBatteryFillValue(value: number) {
    if (value < 0) {
      return 0;
    } else if (value > 100) {
      return 100;
    } else {
      return value;
    }
  }

  public trackBySection(index: number): number {
    return index;
  }

  public cardClick($event: Event) {
    this.ctx.actionsApi.cardClick($event);
  }

  private onResize() {
    if (this.vertical) {
      if (this.batteryLevelValue) {
        const contentWidth = this.batteryLevelContent.nativeElement.getBoundingClientRect().width;
        const boxWidth = (contentWidth - 16) / 2;
        const boxHeight = this.batteryLevelContent.nativeElement.getBoundingClientRect().height;
        const ratios = contentWidth > boxHeight ? verticalBatteryDimensions.heightRatio : verticalBatteryDimensions.widthRatio;
        const boxSize = contentWidth > boxHeight ? boxHeight : boxWidth;
        const topBottomValuePadding = ratios.valueTopBottomPaddingRatio * boxSize;
        const leftRightValuePadding = ratios.valueLeftRightPaddingRatio * boxSize;
        const valuePadding = `${topBottomValuePadding}px ${leftRightValuePadding}px`;
        this.renderer.setStyle(this.batteryLevelValue.nativeElement, 'padding', valuePadding);
        if (this.autoScaleValueSize) {
          const valueFontSize = ratios.valueFontSizeRatio * boxSize;
          const valueLineHeight = ratios.valueLineHeightRaio * boxSize;
          this.renderer.setStyle(this.batteryLevelValue.nativeElement, 'minWidth', '0');
          this.setValueFontSize(valueFontSize, valueLineHeight, boxWidth);
        }
        const fontSize = parseInt(window.getComputedStyle(this.batteryLevelValue.nativeElement).fontSize, 10) || 10;
        this.renderer.setStyle(this.batteryLevelValue.nativeElement, 'minWidth', `${Math.min(fontSize*4, boxWidth)}px`);
      }
      let height = this.batteryLevelContent.nativeElement.getBoundingClientRect().height;
      const width = height * verticalBatteryDimensions.shapeAspectRatio;
      this.renderer.setStyle(this.batteryLevelBox.nativeElement, 'width', width + 'px');
      const realWidth = this.batteryLevelBox.nativeElement.getBoundingClientRect().width;
      if (realWidth < width) {
        height = realWidth / verticalBatteryDimensions.shapeAspectRatio;
        this.renderer.setStyle(this.batteryLevelRectangle.nativeElement, 'height', height + 'px');
      } else {
        this.renderer.setStyle(this.batteryLevelRectangle.nativeElement, 'height', null);
      }
    } else {
      const width = this.batteryLevelContent.nativeElement.getBoundingClientRect().width;
      let height = width / horizontalBatteryDimensions.shapeAspectRatio;
      this.renderer.setStyle(this.batteryLevelBox.nativeElement, 'height', height + 'px');
      const realHeight = this.batteryLevelBox.nativeElement.getBoundingClientRect().height;
      if (realHeight < height) {
        height = realHeight;
        const newWidth = height * horizontalBatteryDimensions.shapeAspectRatio;
        this.renderer.setStyle(this.batteryLevelRectangle.nativeElement, 'width', newWidth + 'px');
      } else {
        this.renderer.setStyle(this.batteryLevelRectangle.nativeElement, 'width', width + 'px');
      }
      if (this.batteryLevelValue) {
        const ratios = horizontalBatteryDimensions.heightRatio;
        const valuePadding = `${(ratios.valueTopBottomPaddingRatio * height)}px 6px`;
        this.renderer.setStyle(this.batteryLevelValue.nativeElement, 'padding', valuePadding);
        if (this.autoScaleValueSize) {
          const valueFontSize = ratios.valueFontSizeRatio * height;
          const valueLineHeight = ratios.valueLineHeightRatio * height;
          const boxWidth = this.batteryLevelContent.nativeElement.getBoundingClientRect().width;
          this.setValueFontSize(valueFontSize, valueLineHeight, boxWidth);
        }
      }
    }
  }

  private setValueFontSize(valueFontSize: number, valueLineHeight: number, maxWidth: number) {
    this.renderer.setStyle(this.batteryLevelValue.nativeElement, 'fontSize', valueFontSize + 'px');
    this.renderer.setStyle(this.batteryLevelValue.nativeElement, 'lineHeight', valueLineHeight + 'px');
    let valueWidth = this.batteryLevelValue.nativeElement.getBoundingClientRect().width;
    while (valueWidth > maxWidth && valueFontSize > 6) {
      valueFontSize--;
      valueLineHeight--;
      this.renderer.setStyle(this.batteryLevelValue.nativeElement, 'fontSize', valueFontSize + 'px');
      this.renderer.setStyle(this.batteryLevelValue.nativeElement, 'lineHeight', valueLineHeight + 'px');
      valueWidth = this.batteryLevelValue.nativeElement.getBoundingClientRect().width;
    }
  }
}
