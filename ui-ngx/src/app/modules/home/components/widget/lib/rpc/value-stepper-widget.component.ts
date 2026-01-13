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
  DestroyRef,
  ElementRef,
  inject,
  NgZone,
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
  overlayStyle,
  textStyle,
  ValueFormatProcessor
} from '@shared/models/widget-settings.models';
import { BehaviorSubject, combineLatest, Observable } from 'rxjs';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import { ValueType } from '@shared/models/constants';
import {
  PowerButtonLayout,
  PowerButtonShape,
  powerButtonShapeSize,
  PowerButtonWidgetSettings
} from '@home/components/widget/lib/rpc/power-button-widget.models';
import { SVG, Svg } from '@svgdotjs/svg.js';
import { MatIconRegistry } from '@angular/material/icon';
import { isDefinedAndNotNull, isNumeric } from '@core/utils';
import {
  valueStepperDefaultSettings,
  ValueStepperWidgetSettings
} from '@home/components/widget/lib/rpc/value-stepper-widget.models';
import { UtilsService } from '@core/services/utils.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-value-stepper-widget',
  templateUrl: './value-stepper-widget.component.html',
  styleUrls: ['../action/action-widget.scss', './value-stepper-widget.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ValueStepperWidgetComponent extends
  BasicActionWidgetComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('leftButton', {static: true})
  leftButton: ElementRef<HTMLElement>;

  @ViewChild('rightButton', {static: true})
  rightButton: ElementRef<HTMLElement>;

  @ViewChild('stepperContent', {static: true})
  stepperContent: ElementRef<HTMLElement>;

  @ViewChild('valueBoxContainer', {static: true})
  valueBox: ElementRef<HTMLElement>;

  @ViewChild('value', {static: true})
  valueElement: ElementRef<HTMLElement>;

  settings: ValueStepperWidgetSettings;

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};
  padding: string;

  valueStyle: ComponentStyle = {};
  valueStyleColor = '';
  disabledColor = 'rgba(0, 0, 0, 0.38)';
  value: number = null;

  autoScale = false;

  showValueBox = true;
  showLeftButton = true;
  showRightButton = true;

  valueText = 'N/A';

  disabledState$ = new BehaviorSubject(false);

  private prevValue: number = null;
  private shapeResize$: ResizeObserver;
  private drawSvgShapePending = false;
  private svgShapeLeft: Svg;
  private svgShapeRight: Svg;
  private powerButtonSvgShapeLeft: PowerButtonShape;
  private powerButtonSvgShapeRight: PowerButtonShape;

  private disabledState = false;
  public leftDisabledState = false;
  public rightDisabledState = false;

  private valueSetterLeft: ValueSetter<number>;
  private valueSetterRight: ValueSetter<number>;

  private leftDisabledState$ = new BehaviorSubject(false);
  private rightDisabledState$ = new BehaviorSubject(false);

  private valueFormat: ValueFormatProcessor;
  protected destroyRef = inject(DestroyRef);

  constructor(protected imagePipe: ImagePipe,
              protected sanitizer: DomSanitizer,
              private renderer: Renderer2,
              private iconRegistry: MatIconRegistry,
              private utils: UtilsService,
              private elementRef: ElementRef,
              protected cd: ChangeDetectorRef,
              protected zone: NgZone) {
    super(cd);
  }

  ngOnInit(): void {
    super.ngOnInit();
    this.settings = {...valueStepperDefaultSettings, ...this.ctx.settings};

    this.autoScale = this.settings.appearance.autoScale;

    this.backgroundStyle$ = backgroundStyle(this.settings.background, this.imagePipe, this.sanitizer);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);
    this.padding = this.settings.background.overlay.enabled ? undefined : this.settings.padding;

    this.showValueBox = this.settings.appearance.showValueBox;
    this.showLeftButton = this.settings.buttonAppearance.leftButton.showButton;
    this.showRightButton = this.settings.buttonAppearance.rightButton.showButton;
    this.valueStyle = textStyle(this.settings.appearance.valueFont);
    this.valueStyleColor = this.settings.appearance.valueColor;

    this.valueFormat = ValueFormatProcessor.fromSettings(this.ctx.$injector, {
      units: this.settings.appearance.valueUnits,
      decimals: this.settings.appearance.valueDecimals
    });

    if (this.showValueBox) {
      const valueBoxCss = `.tb-value-stepper-value-box {\n`+
        `border: ${this.settings.appearance.showBorder ?
          `${this.settings.appearance.borderWidth}px solid ${this.settings.appearance.borderColor}` :
          'none'};\n`+
        `background-color: ${this.settings.appearance.valueBoxBackground}` +
        `}`;
      this.utils.applyCssToElement(this.renderer, this.elementRef.nativeElement, 'tb-value-stepper-value-box', valueBoxCss);
    }

    const getInitialStateSettings =
      {...this.settings.initialState, actionLabel: this.ctx.translate.instant('widgets.slider.initial-value')};
    this.createValueGetter(getInitialStateSettings, ValueType.INTEGER, {
      next: (value) => this.onValue(value)
    });
    const disabledStateSettings =
      {...this.settings.disabledState, actionLabel: this.ctx.translate.instant('widgets.rpc-state.disabled-state')};
    this.createValueGetter(disabledStateSettings, ValueType.BOOLEAN, {
      next: (value) => this.disabledState$.next(value)
    });

    const leftButtonClick = {...this.settings.leftButtonClick,
      actionLabel: this.ctx.translate.instant('widgets.slider.on-value-change')};
    this.valueSetterLeft = this.createValueSetter(leftButtonClick);

    const rightButtonClick = {...this.settings.rightButtonClick,
      actionLabel: this.ctx.translate.instant('widgets.slider.on-value-change')};
    this.valueSetterRight = this.createValueSetter(rightButtonClick);

    combineLatest([
      this.loading$,
      this.disabledState$.asObservable(),
      this.leftDisabledState$.asObservable()
    ]).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => {
      const state = value.includes(true);
      this.updateLeftDisabledState(state)
    });

    combineLatest([
      this.loading$,
      this.disabledState$.asObservable(),
      this.rightDisabledState$.asObservable()
    ]).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => {
      const state = value.includes(true);
      this.updateRightDisabledState(state)
    });
  }

  ngAfterViewInit(): void {
    if (this.drawSvgShapePending) {
      this.drawSvg();
    }
    super.ngAfterViewInit();
  }

  ngOnDestroy() {
    if (this.shapeResize$) {
      this.shapeResize$.disconnect();
    }
    super.ngOnDestroy();
  }

  public onInit() {
    super.onInit();
    const borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.overlayStyle = {...this.overlayStyle, ...{borderRadius}};
    if (this.leftButton || this.rightButton) {
      this.drawSvg();
    } else {
      this.drawSvgShapePending = true;
    }
    this.cd.detectChanges();
  }

  private onValue(value: number): void {
    this.value = value;
    this.prevValue = value;
    if ((this.value + this.settings.appearance.valueStep) > this.settings.appearance.maxValueRange) {
      this.rightDisabledState$.next(true);
    } else {
      this.rightDisabledState$.next(false);
    }
    if ((this.value - this.settings.appearance.valueStep) < this.settings.appearance.minValueRange) {
      this.leftDisabledState$.next(true);
    } else {
      this.leftDisabledState$.next(false);
    }
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

  private onClick(rightButtonClick: boolean = false) {
    this.updateValueText();
    if (!this.ctx.isEdit && !this.ctx.isPreview && !this.disabledState) {
      const prevValue = this.prevValue;
      const targetValue = rightButtonClick ?
        (this.value + this.settings.appearance.valueStep) :
        (this.value - this.settings.appearance.valueStep);
      this.updateValue(rightButtonClick ? this.valueSetterRight : this.valueSetterLeft, targetValue, {
        next: () => this.onValue(targetValue),
        error: () => this.onValue(prevValue)
      });
    }
  }

  private drawSvg() {
    let leftButtonSetting: PowerButtonWidgetSettings;
    let rightButtonSetting: PowerButtonWidgetSettings;
    if (this.showLeftButton) {
      this.svgShapeLeft = SVG().addTo(this.leftButton.nativeElement).size(powerButtonShapeSize, powerButtonShapeSize);
      this.renderer.setStyle(this.svgShapeLeft.node, 'overflow', 'visible');
      this.renderer.setStyle(this.svgShapeLeft.node, 'user-select', 'none');
      leftButtonSetting = {
        layout: PowerButtonLayout[this.settings.appearance.type],
        onButtonIcon: {
          showIcon: true,
          icon: this.settings.buttonAppearance.leftButton.icon,
          iconSize: this.settings.buttonAppearance.leftButton.iconSize * 1.7,
          iconSizeUnit: this.settings.buttonAppearance.leftButton.iconSizeUnit
        },
        offButtonIcon: {
          showIcon: true,
          icon: this.settings.buttonAppearance.leftButton.icon,
          iconSize: this.settings.buttonAppearance.leftButton.iconSize * 1.7,
          iconSizeUnit: this.settings.buttonAppearance.leftButton.iconSizeUnit
        },
        mainColorOn: this.settings.buttonAppearance.leftButton.mainColorOn,
        backgroundColorOn: this.settings.buttonAppearance.leftButton.backgroundColorOn,
        mainColorOff: this.settings.buttonAppearance.leftButton.mainColorOff,
        backgroundColorOff: this.settings.buttonAppearance.leftButton.backgroundColorOff,
        mainColorDisabled: this.settings.buttonAppearance.leftButton.mainColorDisabled,
        backgroundColorDisabled: this.settings.buttonAppearance.leftButton.backgroundColorDisabled
      };
    }
    if (this.showRightButton) {
      this.svgShapeRight = SVG().addTo(this.rightButton.nativeElement).size(powerButtonShapeSize, powerButtonShapeSize);
      this.renderer.setStyle(this.svgShapeRight.node, 'overflow', 'visible');
      this.renderer.setStyle(this.svgShapeRight.node, 'user-select', 'none');

      rightButtonSetting = {
        layout: PowerButtonLayout[this.settings.appearance.type],
        onButtonIcon: {
          showIcon: true,
          icon: this.settings.buttonAppearance.rightButton.icon,
          iconSize: this.settings.buttonAppearance.rightButton.iconSize * 1.7,
          iconSizeUnit: this.settings.buttonAppearance.rightButton.iconSizeUnit
        },
        offButtonIcon: {
          showIcon: true,
          icon: this.settings.buttonAppearance.rightButton.icon,
          iconSize: this.settings.buttonAppearance.rightButton.iconSize * 1.7,
          iconSizeUnit: this.settings.buttonAppearance.rightButton.iconSizeUnit
        },
        mainColorOn: this.settings.buttonAppearance.rightButton.mainColorOn,
        backgroundColorOn: this.settings.buttonAppearance.rightButton.backgroundColorOn,
        mainColorOff: this.settings.buttonAppearance.rightButton.mainColorOff,
        backgroundColorOff: this.settings.buttonAppearance.rightButton.backgroundColorOff,
        mainColorDisabled: this.settings.buttonAppearance.rightButton.mainColorDisabled,
        backgroundColorDisabled: this.settings.buttonAppearance.rightButton.backgroundColorDisabled
      };
    }

    this.zone.run(() => {
      if (this.showLeftButton) {
        this.powerButtonSvgShapeLeft = PowerButtonShape.fromSettings(this.ctx, this.svgShapeLeft, this.iconRegistry,
          leftButtonSetting , true, this.leftDisabledState, () => this.onClick());
      }
      if (this.showRightButton) {
        this.powerButtonSvgShapeRight = PowerButtonShape.fromSettings(this.ctx, this.svgShapeRight, this.iconRegistry,
          rightButtonSetting,  true, this.rightDisabledState, () => this.onClick(true));
      }
    });

    this.shapeResize$ = new ResizeObserver(() => {
      this.onResize();
    });
    if (this.autoScale) {
      this.shapeResize$.observe(this.stepperContent.nativeElement);
    }
    if (this.showLeftButton) {
      this.shapeResize$.observe(this.leftButton.nativeElement);
    }
    if (this.showRightButton) {
      this.shapeResize$.observe(this.rightButton.nativeElement);
    }
    this.onResize();
  }

  private updateLeftDisabledState(disabled: boolean) {
    this.leftDisabledState = disabled;
    this.powerButtonSvgShapeLeft?.setDisabled(this.leftDisabledState);
    this.cd.markForCheck();
  }


  private updateRightDisabledState(disabled: boolean) {
    this.rightDisabledState = disabled;
    this.powerButtonSvgShapeRight?.setDisabled(this.rightDisabledState);
    this.cd.markForCheck();
  }

  private onResize() {
    const panelWidth = this.stepperContent.nativeElement.getBoundingClientRect().width;
    const panelHeight = this.stepperContent.nativeElement.getBoundingClientRect().height;

    const minAspect = 0.2;
    const avgContentHeight = 32;
    const targetHeight = panelWidth * Math.min(panelHeight / panelWidth, minAspect);
    const multiplier = targetHeight / avgContentHeight;
    const size = avgContentHeight * multiplier;

    if (this.showValueBox) {
      this.renderer.setStyle(this.valueBox?.nativeElement, 'height', `${size}px`);
      this.renderer.setStyle(this.valueElement?.nativeElement, 'font-size', `${this.settings.appearance.valueFont.size * multiplier}px`);
    }
    if (this.showLeftButton) {
      this.renderer.setStyle(this.leftButton?.nativeElement, 'width', `${size}px`);
      this.renderer.setStyle(this.leftButton?.nativeElement, 'height', `${size}px`);
    }
    if (this.showRightButton) {
      this.renderer.setStyle(this.rightButton?.nativeElement, 'width', `${size}px`);
      this.renderer.setStyle(this.rightButton?.nativeElement, 'height', `${size}px`);
    }
    if (size) {
      const scale = size / powerButtonShapeSize;
      if (this.showLeftButton) {
        this.renderer.setStyle(this.svgShapeLeft?.node, 'transform', `scale(${scale})`);
      }
      if (this.showRightButton) {
        this.renderer.setStyle(this.svgShapeRight?.node, 'transform', `scale(${scale})`);
      }
    }
  }

}
