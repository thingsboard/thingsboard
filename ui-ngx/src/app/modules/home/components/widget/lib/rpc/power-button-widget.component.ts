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
  NgZone,
  OnDestroy,
  OnInit,
  Renderer2,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { BasicActionWidgetComponent, ValueSetter } from '@home/components/widget/lib/action/action-widget.models';
import { backgroundStyle, ComponentStyle, overlayStyle } from '@shared/models/widget-settings.models';
import { Observable } from 'rxjs';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import { ValueType } from '@shared/models/constants';
import {
  powerButtonDefaultSettings,
  PowerButtonShape,
  powerButtonShapeSize,
  PowerButtonWidgetSettings
} from '@home/components/widget/lib/rpc/power-button-widget.models';
import { SVG, Svg } from '@svgdotjs/svg.js';
import { MatIconRegistry } from '@angular/material/icon';

@Component({
    selector: 'tb-power-button-widget',
    templateUrl: './power-button-widget.component.html',
    styleUrls: ['../action/action-widget.scss', './power-button-widget.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class PowerButtonWidgetComponent extends
  BasicActionWidgetComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('powerButtonShape', {static: false})
  powerButtonShape: ElementRef<HTMLElement>;

  settings: PowerButtonWidgetSettings;

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};
  padding: string;

  value = false;
  disabled = false;

  private shapeResize$: ResizeObserver;
  private drawSvgShapePending = false;
  private svgShape: Svg;
  private powerButtonSvgShape: PowerButtonShape;
  private disabledState = false;

  private onValueSetter: ValueSetter<boolean>;
  private offValueSetter: ValueSetter<boolean>;

  constructor(protected imagePipe: ImagePipe,
              protected sanitizer: DomSanitizer,
              private renderer: Renderer2,
              private iconRegistry: MatIconRegistry,
              protected cd: ChangeDetectorRef,
              protected zone: NgZone) {
    super(cd);
  }

  ngOnInit(): void {
    super.ngOnInit();
    this.settings = {...powerButtonDefaultSettings, ...this.ctx.settings};

    this.backgroundStyle$ = backgroundStyle(this.settings.background, this.imagePipe, this.sanitizer);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);
    this.padding = this.settings.background.overlay.enabled ? undefined : this.settings.padding;

    const getInitialStateSettings =
      {...this.settings.initialState, actionLabel: this.ctx.translate.instant('widgets.rpc-state.initial-state')};
    this.createValueGetter(getInitialStateSettings, ValueType.BOOLEAN, {
      next: (value) => this.onValue(value)
    });

    const disabledStateSettings =
      {...this.settings.disabledState, actionLabel: this.ctx.translate.instant('widgets.rpc-state.disabled-state')};
    this.createValueGetter(disabledStateSettings, ValueType.BOOLEAN, {
      next: (value) => this.onDisabled(value)
    });

    const onUpdateStateSettings = {...this.settings.onUpdateState,
      actionLabel: this.ctx.translate.instant('widgets.power-button.power-on')};
    this.onValueSetter = this.createValueSetter(onUpdateStateSettings);

    const offUpdateStateSettings = {...this.settings.offUpdateState,
      actionLabel: this.ctx.translate.instant('widgets.power-button.power-off')};
    this.offValueSetter = this.createValueSetter(offUpdateStateSettings);

    this.loading$.subscribe((loading) => {
      this.updateDisabledState(loading || this.disabled);
      this.cd.markForCheck();
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
    if (this.powerButtonShape) {
      this.drawSvg();
    } else {
      this.drawSvgShapePending = true;
    }
    this.cd.detectChanges();
  }

  private onValue(value: boolean): void {
    const newValue = !!value;
    if (this.value !== newValue) {
      this.value = newValue;
      this.powerButtonSvgShape?.setValue(this.value);
      this.cd.markForCheck();
    }
  }

  private onDisabled(value: boolean): void {
    const newDisabled = !!value;
    if (this.disabled !== newDisabled) {
      this.disabled = newDisabled;
      this.updateDisabledState(this.disabled);
      this.cd.markForCheck();
    }
  }

  private onClick() {
    if (!this.ctx.isEdit && !this.ctx.isPreview && !this.disabledState) {
      this.onValue(!this.value);
      const targetValue = this.value;
      const targetSetter = targetValue ? this.onValueSetter : this.offValueSetter;
      this.powerButtonSvgShape?.setPressed(true);
      this.updateValue(targetSetter, targetValue, {
        next: () => {
          this.powerButtonSvgShape?.setPressed(false);
          this.onValue(targetValue);
        },
        error: () => {
          this.powerButtonSvgShape?.setPressed(false);
          this.onValue(!targetValue);
        }
      });
    }
  }

  private drawSvg() {
    this.svgShape = SVG().addTo(this.powerButtonShape.nativeElement).size(powerButtonShapeSize, powerButtonShapeSize);
    this.renderer.setStyle(this.svgShape.node, 'overflow', 'visible');
    this.renderer.setStyle(this.svgShape.node, 'user-select', 'none');

    this.zone.run(() => {
      this.powerButtonSvgShape = PowerButtonShape.fromSettings(this.ctx, this.svgShape, this.iconRegistry,
        this.settings, this.value, this.disabledState, () => this.onClick());
    });

    this.shapeResize$ = new ResizeObserver(() => {
      this.onResize();
    });
    this.shapeResize$.observe(this.powerButtonShape.nativeElement);
    this.onResize();
  }

  private updateDisabledState(disabled: boolean) {
    this.disabledState = disabled;
    this.powerButtonSvgShape?.setDisabled(this.disabledState);
  }

  private onResize() {
    const shapeWidth = this.powerButtonShape.nativeElement.getBoundingClientRect().width;
    const shapeHeight = this.powerButtonShape.nativeElement.getBoundingClientRect().height;
    const size = Math.min(shapeWidth, shapeHeight);
    const scale = size / powerButtonShapeSize;
    this.renderer.setStyle(this.svgShape.node, 'transform', `scale(${scale})`);
  }

}
