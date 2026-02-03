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
  OnDestroy,
  OnInit,
  Renderer2,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { BasicActionWidgetComponent, ValueSetter } from '@home/components/widget/lib/action/action-widget.models';
import {
  backgroundStyle,
  ComponentStyle,
  iconStyle,
  overlayStyle,
  textStyle,
  ValueFormatProcessor
} from '@shared/models/widget-settings.models';
import { Observable } from 'rxjs';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import { ValueType } from '@shared/models/constants';
import { UtilsService } from '@core/services/utils.service';
import {
  SliderLayout,
  sliderWidgetDefaultSettings,
  SliderWidgetSettings
} from '@home/components/widget/lib/rpc/slider-widget.models';
import { isDefinedAndNotNull, isNumeric } from '@core/utils';
import { WidgetComponent } from '@home/components/widget/widget.component';
import tinycolor from 'tinycolor2';
import { UnitService } from '@core/services/unit.service';

@Component({
    selector: 'tb-slider-widget',
    templateUrl: './slider-widget.component.html',
    styleUrls: ['../action/action-widget.scss', './slider-widget.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class SliderWidgetComponent extends
  BasicActionWidgetComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('sliderContent', {static: false})
  sliderContent: ElementRef<HTMLElement>;

  @ViewChild('sliderValueContainer', {static: false})
  sliderValueContainer: ElementRef<HTMLElement>;

  @ViewChild('sliderValue', {static: false})
  sliderValue: ElementRef<HTMLElement>;

  @ViewChild('sliderTickMinContainer', {static: false})
  sliderTickMinContainer: ElementRef<HTMLElement>;

  @ViewChild('sliderTickMin', {static: false})
  sliderTickMin: ElementRef<HTMLElement>;

  @ViewChild('sliderTickMaxContainer', {static: false})
  sliderTickMaxContainer: ElementRef<HTMLElement>;

  @ViewChild('sliderTickMax', {static: false})
  sliderTickMax: ElementRef<HTMLElement>;

  @ViewChild('leftSliderIconContainer', {static: false, read: ElementRef})
  leftSliderIconContainer: ElementRef<HTMLElement>;

  @ViewChild('leftSliderIcon', {static: false, read: ElementRef})
  leftSliderIcon: ElementRef<HTMLElement>;

  @ViewChild('rightSliderIconContainer', {static: false, read: ElementRef})
  rightSliderIconContainer: ElementRef<HTMLElement>;

  @ViewChild('rightSliderIcon', {static: false, read: ElementRef})
  rightSliderIcon: ElementRef<HTMLElement>;

  settings: SliderWidgetSettings;

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};
  padding: string;

  value: number = null;
  private prevValue: number = null;

  disabled = false;

  layout: SliderLayout;

  showValue = true;
  valueText = 'N/A';
  valueStyle: ComponentStyle = {};

  showLeftRightIcon = false;
  leftIcon = '';
  leftIconStyle: ComponentStyle = {};
  rightIcon = '';
  rightIconStyle: ComponentStyle = {};

  showTicks = true;
  ticksStyle: ComponentStyle = {};
  tickMinText: number;
  tickMaxText: number;

  sliderStep: number = undefined;

  autoScale = false;

  showWidgetTitlePanel = this.widgetComponent.dashboardWidget.showWidgetTitlePanel;

  sliderValueText = this._sliderValueText.bind(this);

  private panelResize$: ResizeObserver;

  private valueSetter: ValueSetter<number>;
  private valueFormat: ValueFormatProcessor;

  private sliderCssClass: string;

  constructor(protected imagePipe: ImagePipe,
              protected sanitizer: DomSanitizer,
              private renderer: Renderer2,
              private utils: UtilsService,
              private widgetComponent: WidgetComponent,
              protected cd: ChangeDetectorRef,
              private elementRef: ElementRef,
              private unitService: UnitService) {
    super(cd);
  }

  ngOnInit(): void {
    super.ngOnInit();
    this.settings = {...sliderWidgetDefaultSettings, ...this.ctx.settings};

    this.backgroundStyle$ = backgroundStyle(this.settings.background, this.imagePipe, this.sanitizer);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);
    this.padding = this.settings.background.overlay.enabled ? undefined : this.settings.padding;

    this.layout = this.settings.layout;

    this.autoScale = this.settings.autoScale;

    this.showValue = this.layout !== SliderLayout.simplified && this.settings.showValue;
    this.valueStyle = textStyle(this.settings.valueFont);
    this.valueStyle.color = this.settings.valueColor;

    this.valueFormat = ValueFormatProcessor.fromSettings(this.ctx.$injector, {
      units: this.settings.valueUnits,
      decimals: this.settings.valueDecimals
    });

    this.showLeftRightIcon = this.layout === SliderLayout.extended;
    if (this.showLeftRightIcon) {
      this.leftIcon = this.settings.leftIcon;
      this.leftIconStyle = iconStyle(this.settings.leftIconSize, this.settings.leftIconSizeUnit );
      this.rightIcon = this.settings.rightIcon;
      this.rightIconStyle = iconStyle(this.settings.rightIconSize, this.settings.rightIconSizeUnit );
      if (!this.autoScale) {
        const leftIconMargin = this.settings.leftIconSize / 2 + (this.settings.leftIconSizeUnit || 'px');
        this.leftIconStyle.marginTop = `calc(-${leftIconMargin} + 3px)`;
        const rightIconMargin = this.settings.rightIconSize / 2 + (this.settings.rightIconSizeUnit || 'px');
        this.rightIconStyle.marginTop = `calc(-${rightIconMargin} + 3px)`;
      }
    }

    this.showTicks = this.settings.showTicks;
    if (this.showTicks) {
      this.ticksStyle = textStyle(this.settings.ticksFont);
      this.ticksStyle.color = this.settings.ticksColor;
      this.tickMinText = this.unitService.convertUnitValue(this.settings.tickMin, this.settings.valueUnits);
      this.tickMaxText = this.unitService.convertUnitValue(this.settings.tickMax, this.settings.valueUnits);
    }

    if (this.settings.showTickMarks) {
      const range = this.settings.tickMax - this.settings.tickMin;
      this.sliderStep = range / (this.settings.tickMarksCount - 1);
    }

    const mainColorInstance = tinycolor(this.settings.mainColor);
    const hoverRippleColor = mainColorInstance.clone().setAlpha(mainColorInstance.getAlpha() * 0.05).toRgbString();
    const focusRippleColor = mainColorInstance.clone().setAlpha(mainColorInstance.getAlpha() * 0.2).toRgbString();

    const sliderVariablesCss = `.tb-slider-panel {\n`+
      `--tb-slider-main-color: ${this.settings.mainColor};\n`+
      `--tb-slider-background-color: ${this.settings.backgroundColor};\n`+
      `--tb-slider-hover-ripple-color: ${hoverRippleColor};\n`+
      `--tb-slider-focus-ripple-color: ${focusRippleColor};\n`+
      `--tb-slider-tick-marks-color: ${this.settings.tickMarksColor};\n`+
      `--tb-slider-main-color-disabled: ${this.settings.mainColorDisabled};\n`+
      `--tb-slider-background-disabled: ${this.settings.backgroundColorDisabled};\n`+
      `}`;
    this.sliderCssClass =
      this.utils.applyCssToElement(this.renderer, this.elementRef.nativeElement, 'tb-slider', sliderVariablesCss);

    const getInitialStateSettings =
      {...this.settings.initialState, actionLabel: this.ctx.translate.instant('widgets.slider.initial-value')};
    this.createValueGetter(getInitialStateSettings, ValueType.INTEGER, {
      next: (value) => this.onValue(value)
    });

    const disabledStateSettings =
      {...this.settings.disabledState, actionLabel: this.ctx.translate.instant('widgets.rpc-state.disabled-state')};
    this.createValueGetter(disabledStateSettings, ValueType.BOOLEAN, {
      next: (value) => this.onDisabled(value)
    });

    const valueChangeSettings = {...this.settings.valueChange,
      actionLabel: this.ctx.translate.instant('widgets.slider.on-value-change')};
    this.valueSetter = this.createValueSetter(valueChangeSettings);
  }

  ngAfterViewInit(): void {
    if (this.autoScale) {
      this.panelResize$ = new ResizeObserver(() => {
        this.onResize();
      });
      this.panelResize$.observe(this.sliderContent.nativeElement);
      if (this.showValue) {
        this.panelResize$.observe(this.sliderValueContainer.nativeElement);
      }
      this.onResize();
    }
    super.ngAfterViewInit();
  }

  ngOnDestroy() {
    if (this.panelResize$) {
      this.panelResize$.disconnect();
    }
    if (this.sliderCssClass) {
      this.utils.clearCssElement(this.renderer, this.sliderCssClass);
    }
    super.ngOnDestroy();
  }

  public onInit() {
    super.onInit();
    const borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.overlayStyle = {...this.overlayStyle, ...{borderRadius}};
    this.cd.detectChanges();
  }

  public onSliderChange() {
    this.updateValueText();
    if (!this.ctx.isEdit && !this.ctx.isPreview) {
      const prevValue = this.prevValue;
      const targetValue = this.value;
      this.updateValue(this.valueSetter, targetValue, {
        next: () => this.onValue(targetValue),
        error: () => this.onValue(prevValue)
      });
    }
  }

  private _sliderValueText(value: number): string {
    return this.valueFormat.format(value);
  }

  private onValue(value: number): void {
    this.value = value;
    this.prevValue = value;
    this.updateValueText();
    this.cd.markForCheck();
  }

  private updateValueText() {
    if (isDefinedAndNotNull(this.value) && isNumeric(this.value)) {
      this.valueText = this.valueFormat.format(this.value);
    } else {
      this.valueText = 'N/A';
    }
  }

  private onDisabled(value: boolean): void {
    this.disabled = !!value;
    this.cd.markForCheck();
  }

  private onResize() {
    const panelWidth = this.sliderContent.nativeElement.getBoundingClientRect().width;
    const panelHeight = this.sliderContent.nativeElement.getBoundingClientRect().height;

    if (this.showValue) {
      this.resetScale(this.sliderValueContainer.nativeElement, this.sliderValue.nativeElement);
    }

    if (this.showLeftRightIcon) {
      this.resetScale(this.leftSliderIconContainer.nativeElement, this.leftSliderIcon.nativeElement);
      this.resetScale(this.rightSliderIconContainer.nativeElement, this.rightSliderIcon.nativeElement);
    }

    if (this.showTicks) {
      this.resetScale(this.sliderTickMinContainer.nativeElement, this.sliderTickMin.nativeElement);
      this.resetScale(this.sliderTickMaxContainer.nativeElement, this.sliderTickMax.nativeElement);
    }

    let minAspect = 0.2;
    let avgContentHeight = 35;
    if (this.showTicks) {
      minAspect += 0.1;
      avgContentHeight += 20;
    }
    if (this.showValue) {
      minAspect += 0.1;
      avgContentHeight += 50;
    }
    const aspect = Math.min(panelHeight / panelWidth, minAspect);
    const targetHeight = panelWidth * aspect;
    const scale = targetHeight / avgContentHeight;

    if (this.showValue) {
      this.updateScale(this.sliderValueContainer.nativeElement, this.sliderValue.nativeElement, scale);
    }
    if (this.showLeftRightIcon) {
      const leftIconContainerRect = this.leftSliderIconContainer.nativeElement.getBoundingClientRect();
      const leftIconContainerMarginTop = -(leftIconContainerRect.width * scale) / 2 + 3;
      this.renderer.setStyle(this.leftSliderIconContainer.nativeElement, 'marginTop', `${leftIconContainerMarginTop}px`);
      this.updateScale(this.leftSliderIconContainer.nativeElement, this.leftSliderIcon.nativeElement, scale, true);
      const rightIconContainerRect = this.rightSliderIconContainer.nativeElement.getBoundingClientRect();
      const rightIconContainerMarginTop = -(rightIconContainerRect.width * scale) / 2 + 3;
      this.renderer.setStyle(this.rightSliderIconContainer.nativeElement, 'marginTop', `${rightIconContainerMarginTop}px`);
      this.updateScale(this.rightSliderIconContainer.nativeElement, this.rightSliderIcon.nativeElement, scale, true);
    }
    if (this.showTicks) {
      this.updateScale(this.sliderTickMinContainer.nativeElement, this.sliderTickMin.nativeElement, scale);
      this.updateScale(this.sliderTickMaxContainer.nativeElement, this.sliderTickMax.nativeElement, scale);
    }
  }

  private resetScale(container: HTMLElement, element: HTMLElement): void {
    this.renderer.setStyle(container, 'width', '');
    this.renderer.setStyle(container, 'height', '');
    this.renderer.setStyle(element, 'transform', '');
  }

  private updateScale(container: HTMLElement, element: HTMLElement, scale: number, sameHeight = false): void {
    const rect = container.getBoundingClientRect();
    this.renderer.setStyle(container, 'width', `${rect.width * scale}px`);
    this.renderer.setStyle(container, 'height', `${(sameHeight ? rect.width : rect.height) * scale}px`);
    this.renderer.setStyle(element, 'transform', `scale(${scale})`);
    this.renderer.setStyle(element, 'transform-origin', 'left top');
  }

}
