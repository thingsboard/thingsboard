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

import { Component, ElementRef, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { WidgetContext } from '@home/models/widget-component.models';
import { UtilsService } from '@core/services/utils.service';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { isDefined, isNumber } from '@core/utils';
import { CanvasDigitalGauge, CanvasDigitalGaugeOptions } from '@home/components/widget/lib/canvas-digital-gauge';
import * as tinycolor_ from 'tinycolor2';
import { ResizeObserver } from '@juggle/resize-observer';
import GenericOptions = CanvasGauges.GenericOptions;

const tinycolor = tinycolor_;

interface KnobSettings {
  minValue: number;
  maxValue: number;
  initialValue: number;
  title: string;
  getValueMethod: string;
  setValueMethod: string;
  requestTimeout: number;
}

@Component({
  selector: 'tb-knob',
  templateUrl: './knob.component.html',
  styleUrls: ['./knob.component.scss']
})
export class KnobComponent extends PageComponent implements OnInit, OnDestroy {

  @ViewChild('knob', {static: true}) knobRef: ElementRef<HTMLElement>;
  @ViewChild('knobContainer', {static: true}) knobContainerRef: ElementRef<HTMLElement>;
  @ViewChild('knobTopPointerContainer', {static: true}) knobTopPointerContainerRef: ElementRef<HTMLElement>;
  @ViewChild('knobTopPointer', {static: true}) knobTopPointerRef: ElementRef<HTMLElement>;
  @ViewChild('knobValueContainer', {static: true}) knobValueContainerRef: ElementRef<HTMLElement>;
  @ViewChild('knobValue', {static: true}) knobValueRef: ElementRef<HTMLElement>;
  @ViewChild('knobErrorContainer', {static: true}) knobErrorContainerRef: ElementRef<HTMLElement>;
  @ViewChild('knobError', {static: true}) knobErrorRef: ElementRef<HTMLElement>;
  @ViewChild('knobTitleContainer', {static: true}) knobTitleContainerRef: ElementRef<HTMLElement>;
  @ViewChild('knobTitle', {static: true}) knobTitleRef: ElementRef<HTMLElement>;
  @ViewChild('knobMinmaxContainer', {static: true}) knobMinmaxContainerRef: ElementRef<HTMLElement>;
  @ViewChild('textMeasure', {static: true}) textMeasureRef: ElementRef<HTMLElement>;
  @ViewChild('canvasBar', {static: true}) canvasBarElementRef: ElementRef<HTMLElement>;

  @Input()
  ctx: WidgetContext;

  value = '0';
  error = '';
  title = '';
  minValue: number;
  maxValue: number;
  newValue = 0;

  private startDeg = -1;
  private currentDeg = 0;
  private rotation = 0;
  private lastDeg = 0;
  private moving = false;

  private minDeg = -45;
  private maxDeg = 225;

  private isSimulated: boolean;
  private requestTimeout: number;
  private getValueMethod: string;
  private setValueMethod: string;

  private executingUpdateValue: boolean;
  private scheduledValue: number;
  private rpcValue: number;

  private knob: JQuery<HTMLElement>;
  private knobContainer: JQuery<HTMLElement>;
  private knobTopPointerContainer: JQuery<HTMLElement>;
  private knobTopPointer: JQuery<HTMLElement>;
  private knobValueContainer: JQuery<HTMLElement>;
  private knobValue: JQuery<HTMLElement>;
  private knobTitleContainer: JQuery<HTMLElement>;
  private knobTitle: JQuery<HTMLElement>;
  private knobErrorContainer: JQuery<HTMLElement>;
  private knobError: JQuery<HTMLElement>;
  private knobMinmaxContainer: JQuery<HTMLElement>;
  private minmaxLabel: JQuery<HTMLElement>;
  private textMeasure: JQuery<HTMLElement>;
  private canvasBarElement: HTMLElement;

  private canvasBar: CanvasDigitalGauge;

  private knobResize$: ResizeObserver;

  constructor(private utils: UtilsService,
              protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit(): void {
    this.knob = $(this.knobRef.nativeElement);
    this.knobContainer = $(this.knobContainerRef.nativeElement);
    this.knobTopPointerContainer = $(this.knobTopPointerContainerRef.nativeElement);
    this.knobTopPointer = $(this.knobTopPointerRef.nativeElement);
    this.knobValueContainer = $(this.knobValueContainerRef.nativeElement);
    this.knobValue = $(this.knobValueRef.nativeElement);
    this.knobTitleContainer = $(this.knobTitleContainerRef.nativeElement);
    this.knobTitle = $(this.knobTitleRef.nativeElement);
    this.knobErrorContainer = $(this.knobErrorContainerRef.nativeElement);
    this.knobError = $(this.knobErrorRef.nativeElement);
    this.knobMinmaxContainer = $(this.knobMinmaxContainerRef.nativeElement);
    this.minmaxLabel = this.knobMinmaxContainer.find<HTMLElement>('.minmax-label');
    this.textMeasure = $(this.textMeasureRef.nativeElement);
    this.canvasBarElement = this.canvasBarElementRef.nativeElement;

    this.knobResize$ = new ResizeObserver(() => {
      this.resize();
    });
    this.knobResize$.observe(this.knobContainerRef.nativeElement);
    this.init();
  }

  ngOnDestroy(): void {
    if (this.knobResize$) {
      this.knobResize$.disconnect();
    }
  }

  private init() {
    const settings: KnobSettings = this.ctx.settings;

    this.minValue = isDefined(settings.minValue) ? settings.minValue : 0;
    this.maxValue = isDefined(settings.maxValue) ? settings.maxValue : 100;
    this.title = isDefined(settings.title) ? settings.title : '';

    const levelColors = ['#19ff4b', '#ffff19', '#ff3232'];

    const canvasBarData: CanvasDigitalGaugeOptions = {
      renderTo: this.canvasBarElement,
      hideValue: true,
      neonGlowBrightness: 0,
      gaugeWidthScale: 0.4,
      gaugeColor: 'rgba(0, 0, 0, 0)',
      levelColors,
      minValue: this.minValue,
      maxValue: this.maxValue,
      gaugeType: 'donut',
      dashThickness: 2,
      donutStartAngle: 3/4*Math.PI,
      donutEndAngle: 9/4*Math.PI,
      animation: false
    };

    this.canvasBar = new CanvasDigitalGauge(canvasBarData).draw();

    this.knob.on('click', (e) => {
        if (this.moving) {
          this.moving = false;
          return false;
        }
        e.preventDefault();

        const offset = this.knob.offset();
        const center = {
          y: offset.top + this.knob.height() / 2,
          x: offset.left + this.knob.width() / 2
        };
        const rad2deg = 180 / Math.PI;
        const t: Touch = ((e.originalEvent as any).touches) ? (e.originalEvent as any).touches[0] : e;
        const a = center.y - t.pageY;
        const b = center.x - t.pageX;
        let deg = Math.atan2(a, b) * rad2deg;
        if (deg < 0) {
          deg = 360 + deg;
        }
        if (deg > this.maxDeg) {
          if (deg - 360 > this.minDeg) {
            deg = deg - 360;
          } else {
            return false;
          }
        }
        this.currentDeg = deg;
        this.lastDeg = deg;
        this.knobTopPointerContainer.css('transform', 'rotate(' + (this.currentDeg) + 'deg)');
        this.turn(this.degreeToRatio(this.currentDeg));
        this.rotation = this.currentDeg;
        this.startDeg = -1;
        this.rpcUpdateValue(this.newValue);
    });



    this.knob.on('mousedown touchstart', (e) => {
      this.moving  = false;
      e.preventDefault();
      const offset = this.knob.offset();
      const center = {
        y : offset.top + this.knob.height()/2,
        x: offset.left + this.knob.width()/2
      };
      const rad2deg = 180/Math.PI;

      $(document).on('mouseup touchend', (e) => {
        if(this.newValue !== this.rpcValue && this.moving) {
          this.rpcUpdateValue(this.newValue);
        }
      });

      $(document).on('mousemove.rem touchmove.rem', (ev) => {
        this.moving = true;
        const t: Touch = ((ev.originalEvent as any).touches) ? (ev.originalEvent as any).touches[0] : ev;

        const a = center.y - t.pageY;
        const b = center.x - t.pageX;
        let deg = Math.atan2(a,b)*rad2deg;
        if(deg < 0){
          deg = 360 + deg;
        }

        if(this.startDeg === -1){
          this.startDeg = deg;
        }

        let tmp = Math.floor((deg-this.startDeg) + this.rotation);

        if(tmp < 0){
          tmp = 360 + tmp;
        }
        else if(tmp > 359){
          tmp = tmp % 360;
        }

        if (tmp > this.maxDeg) {
          if (tmp - 360 > this.minDeg) {
            tmp = tmp - 360;
          } else {
            const deltaMax = Math.abs(this.maxDeg - this.lastDeg);
            const deltaMin = Math.abs(this.minDeg - this.lastDeg);
            if (deltaMax < deltaMin) {
              tmp = this.maxDeg;
            } else {
              tmp = this.minDeg;
            }
          }
        }
        if(Math.abs(tmp - this.lastDeg) > 180){
          this.startDeg = deg;
          this.rotation = this.currentDeg;
          return false;
        }

        this.currentDeg = tmp;
        this.lastDeg = tmp;

        this.knobTopPointerContainer.css('transform','rotate('+(this.currentDeg)+'deg)');
        this.turn(this.degreeToRatio(this.currentDeg));
      });

      $(document).on('mouseup.rem  touchend.rem',() => {
        this.knob.off('.rem');
        $(document).off('.rem');
        this.rotation = this.currentDeg;
        this.startDeg = -1;
      });

    });

    const initialValue = isDefined(settings.initialValue) ? settings.initialValue : this.minValue;
    this.setValue(initialValue);

    const subscription = this.ctx.defaultSubscription;
    const rpcEnabled = subscription.rpcEnabled;

    this.isSimulated = this.utils.widgetEditMode;

    this.requestTimeout = 500;
    if (settings.requestTimeout) {
      this.requestTimeout = settings.requestTimeout;
    }
    this.getValueMethod = 'getValue';
    if (settings.getValueMethod && settings.getValueMethod.length) {
      this.getValueMethod = settings.getValueMethod;
    }
    this.setValueMethod = 'setValue';
    if (settings.setValueMethod && settings.setValueMethod.length) {
      this.setValueMethod = settings.setValueMethod;
    }
    if (!rpcEnabled) {
      this.onError('Target device is not set!');
    } else {
      if (!this.isSimulated) {
        this.rpcRequestValue();
      }
    }
  }

  private degreeToRatio(degree: number): number {
    return (degree-this.minDeg)/(this.maxDeg-this.minDeg);
  }

  private ratioToDegree(ratio: number): number {
    return this.minDeg + ratio*(this.maxDeg-this.minDeg);
  }

  private turn(ratio: number) {
    this.newValue = Number((this.minValue + (this.maxValue - this.minValue)*ratio).toFixed(this.ctx.decimals));
    if (this.canvasBar.value !== this.newValue) {
      this.canvasBar.value = this.newValue;
    }
    this.updateColor(this.canvasBar.getValueColor());
    this.onValue(this.newValue);
  }

  private resize() {
    const width = this.knobContainer.width();
    const height = this.knobContainer.height();
    const size = Math.min(width, height);
    this.knob.css({width: size, height: size});
    this.canvasBar.update({width: size, height: size} as GenericOptions);
    this.setFontSize(this.knobTitle, this.title, this.knobTitleContainer.height(), this.knobTitleContainer.width());
    this.setFontSize(this.knobError, this.error, this.knobErrorContainer.height(), this.knobErrorContainer.width());
    const minmaxHeight = this.knobMinmaxContainer.height();
    this.minmaxLabel.css({fontSize: minmaxHeight+'px', lineHeight: minmaxHeight+'px'});
    this.checkValueSize();
  }

  private checkValueSize() {
    const fontSize = this.knobValueContainer.height()/3.3;
    const containerWidth = this.knobValueContainer.width();
    this.setFontSize(this.knobValue, this.value+'', fontSize, containerWidth);
  }

  private setFontSize(element: JQuery<HTMLElement>, text: string, fontSize: number, maxWidth: number) {
    let textWidth = this.measureTextWidth(text, fontSize);
    while (textWidth > maxWidth) {
      fontSize--;
      if (fontSize < 0) {
        break;
      }
      textWidth = this.measureTextWidth(text, fontSize);
    }
    element.css({fontSize: fontSize+'px', lineHeight: fontSize+'px'});
  }

  private measureTextWidth(text: string, fontSize: number): number {
    this.textMeasure.css({fontSize: fontSize+'px', lineHeight: fontSize+'px'});
    this.textMeasure.html(text);
    return this.textMeasure.width();
  }

  private setValue(value: number) {
    const ratio = (value-this.minValue) / (this.maxValue - this.minValue);
    this.rotation = this.lastDeg = this.currentDeg = this.ratioToDegree(ratio);
    this.knobTopPointerContainer.css('transform','rotate('+(this.currentDeg)+'deg)');
    if (this.canvasBar.value !== value) {
      this.canvasBar.value = value;
    }
    this.updateColor(this.canvasBar.getValueColor());
    this.value = this.formatValue(value);
    this.checkValueSize();
    this.ctx.detectChanges();
  }

  private updateColor(color: string) {
    const glowColor = tinycolor(color).brighten(30).toHexString();
    this.knobValue.css({color: glowColor});
    const textShadow = `${color} 1px 1px 10px, ${glowColor} 1px 1px 10px`;
    this.knobValue.css({textShadow});
    this.knobTopPointer.css({backgroundColor: glowColor});
    const boxShadow = `inset 1px 0 2px #040404, 1px 1px 8px 2px ${glowColor}`;
    this.knobTopPointer.css({boxShadow});
  }

  private onValue(value: number) {
    this.value = this.formatValue(value);
    this.checkValueSize();
    this.ctx.detectChanges();
  }

  private formatValue(value: any): string {
    return this.ctx.utils.formatValue(value, this.ctx.decimals, this.ctx.units, true);
  }

  private rpcRequestValue() {
    this.error = '';
    this.ctx.controlApi.sendTwoWayCommand(this.getValueMethod, null, this.requestTimeout).subscribe(
      (responseBody) => {
        if (isNumber(responseBody)) {
          const numValue = Number(Number(responseBody).toFixed(this.ctx.decimals));
          this.setValue(numValue);
        } else {
          const errorText = `Unable to parse response: ${responseBody}`;
          this.onError(errorText);
        }
      },
      () => {
        const errorText = this.ctx.defaultSubscription.rpcErrorText;
        this.onError(errorText);
      }
    );
  }

  private rpcUpdateValue(value: number) {
    if (this.executingUpdateValue) {
      this.scheduledValue = value;
      return;
    } else {
      this.scheduledValue = null;
      this.rpcValue = value;
      this.executingUpdateValue = true;
    }
    this.error = '';
    this.ctx.controlApi.sendOneWayCommand(this.setValueMethod, value, this.requestTimeout).subscribe(
      () => {
        this.executingUpdateValue = false;
        if (this.scheduledValue != null && this.scheduledValue !== this.rpcValue) {
          this.rpcUpdateValue(this.scheduledValue);
        }
      },
      () => {
        this.executingUpdateValue = false;
        const errorText = this.ctx.defaultSubscription.rpcErrorText;
        this.onError(errorText);
      }
    );
  }

  private onError(error: string) {
    this.error = error;
    this.setFontSize(this.knobError, this.error, this.knobErrorContainer.height(), this.knobErrorContainer.width());
    this.ctx.detectChanges();
  }

}
