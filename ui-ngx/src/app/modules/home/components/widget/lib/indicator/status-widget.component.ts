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
import { BasicActionWidgetComponent } from '@home/components/widget/lib/action/action-widget.models';
import {
  statusWidgetDefaultSettings,
  StatusWidgetLayout,
  StatusWidgetSettings,
  StatusWidgetStateSettings
} from '@home/components/widget/lib/indicator/status-widget.models';
import { Observable } from 'rxjs';
import {
  backgroundStyle,
  ComponentStyle,
  iconStyle,
  overlayStyle,
  resolveCssSize,
  textStyle
} from '@shared/models/widget-settings.models';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import { ValueType } from '@shared/models/constants';

const initialStatusWidgetSize = 147;

@Component({
  selector: 'tb-status-widget',
  templateUrl: './status-widget.component.html',
  styleUrls: ['../action/action-widget.scss', './status-widget.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class StatusWidgetComponent extends
  BasicActionWidgetComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('statusWidgetPanel', {static: false})
  statusWidgetPanel: ElementRef<HTMLElement>;

  @ViewChild('statusWidgetContent', {static: false})
  statusWidgetContent: ElementRef<HTMLElement>;

  settings: StatusWidgetSettings;

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};
  padding: string;

  overlayInset = '12px';
  borderRadius = '';

  layout: StatusWidgetLayout;

  showLabel = true;
  label$: Observable<string>;
  labelStyle: ComponentStyle = {};

  showStatus = true;
  status$: Observable<string>;
  statusStyle: ComponentStyle = {};

  icon = '';
  iconStyle: ComponentStyle = {};

  private panelResize$: ResizeObserver;

  private onLabel$: Observable<string>;
  private onStatus$: Observable<string>;
  private onBackground$: Observable<ComponentStyle>;
  private onBackgroundDisabled$: Observable<ComponentStyle>;

  private offLabel$: Observable<string>;
  private offStatus$: Observable<string>;
  private offBackground$: Observable<ComponentStyle>;
  private offBackgroundDisabled$: Observable<ComponentStyle>;

  private state = false;
  private disabled = false;
  private disabledState = false;

  constructor(protected imagePipe: ImagePipe,
              protected sanitizer: DomSanitizer,
              private renderer: Renderer2,
              protected cd: ChangeDetectorRef) {
    super(cd);
  }

  ngOnInit(): void {
    super.ngOnInit();
    this.settings = {...statusWidgetDefaultSettings, ...this.ctx.settings};
    this.layout = this.settings.layout;

    this.onLabel$ = this.ctx.registerLabelPattern(this.settings.onState.label, this.onLabel$);
    this.onStatus$ = this.ctx.registerLabelPattern(this.settings.onState.status, this.onStatus$);
    this.onBackground$ = backgroundStyle(this.settings.onState.background, this.imagePipe, this.sanitizer);
    this.onBackgroundDisabled$ = backgroundStyle(this.settings.onState.backgroundDisabled, this.imagePipe, this.sanitizer);

    this.offLabel$ = this.ctx.registerLabelPattern(this.settings.offState.label, this.offLabel$);
    this.offStatus$ = this.ctx.registerLabelPattern(this.settings.offState.status, this.offStatus$);
    this.offBackground$ = backgroundStyle(this.settings.offState.background, this.imagePipe, this.sanitizer);
    this.offBackgroundDisabled$ = backgroundStyle(this.settings.offState.backgroundDisabled, this.imagePipe, this.sanitizer);

    const getInitialStateSettings =
      {...this.settings.initialState, actionLabel: this.ctx.translate.instant('widgets.rpc-state.initial-state')};
    this.createValueGetter(getInitialStateSettings, ValueType.BOOLEAN, {
      next: (value) => this.onState(value)
    });

    const disabledStateSettings =
      {...this.settings.disabledState, actionLabel: this.ctx.translate.instant('widgets.rpc-state.disabled-state')};
    this.createValueGetter(disabledStateSettings, ValueType.BOOLEAN, {
      next: (value) => this.onDisabled(value)
    });

    this.loading$.subscribe((loading) => {
      this.updateDisabledState(loading || this.disabled);
    });

    this.updateStyle(this.state, this.disabled);
  }

  ngAfterViewInit(): void {
    this.renderer.setStyle(this.statusWidgetContent.nativeElement, 'overflow', 'visible');
    this.renderer.setStyle(this.statusWidgetContent.nativeElement, 'position', 'absolute');
    this.panelResize$ = new ResizeObserver(() => {
      this.onResize();
    });
    this.panelResize$.observe(this.statusWidgetPanel.nativeElement);
    if (this.showLabel) {
      this.panelResize$.observe(this.statusWidgetPanel.nativeElement);
    }
    this.onResize();
    super.ngAfterViewInit();
  }

  ngOnDestroy() {
    if (this.panelResize$) {
      this.panelResize$.disconnect();
    }
    super.ngOnDestroy();
  }

  public onInit() {
    super.onInit();
    this.borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.overlayStyle = {...this.overlayStyle, ...{borderRadius: this.borderRadius}};
    this.cd.detectChanges();
  }

  private onState(value: boolean): void {
    const newState = !!value;
    if (this.state !== newState) {
      this.state = newState;
      this.updateStyle(this.state, this.disabled || this.disabledState);
    }
  }

  private onDisabled(value: boolean): void {
    const newDisabled = !!value;
    if (this.disabled !== newDisabled) {
      this.disabled = newDisabled;
      this.updateDisabledState(this.disabled);
    }
  }

  private updateDisabledState(disabled: boolean) {
    this.disabledState = disabled;
    this.updateStyle(this.state, this.disabledState);
  }

  private onResize() {
    const computedStyle = getComputedStyle(this.statusWidgetPanel.nativeElement);
    const [pLeft, pRight, pTop, pBottom] = ['paddingLeft', 'paddingRight', 'paddingTop', 'paddingBottom']
      .map(side => resolveCssSize(computedStyle[side])[0]);

    const widgetBoundingClientRect = this.statusWidgetPanel.nativeElement.getBoundingClientRect();
    const panelWidth = widgetBoundingClientRect.width - (pLeft + pRight);
    const panelHeight = widgetBoundingClientRect.height - (pTop + pBottom);
    const targetSize = Math.min(panelWidth, panelHeight);
    const scale = targetSize / initialStatusWidgetSize;
    const width = initialStatusWidgetSize;
    const height = initialStatusWidgetSize;
    this.renderer.setStyle(this.statusWidgetContent.nativeElement, 'width', width + 'px');
    this.renderer.setStyle(this.statusWidgetContent.nativeElement, 'height', height + 'px');
    this.renderer.setStyle(this.statusWidgetContent.nativeElement, 'transform', `scale(${scale})`);
    this.overlayInset = (Math.floor(12 * scale * 100) / 100) + 'px';
    this.cd.markForCheck();
  }

  private updateStyle(state: boolean, disabled: boolean) {
    let stateSettings: StatusWidgetStateSettings;
    if (state) {
      this.label$ = this.onLabel$;
      this.status$ = this.onStatus$;
      this.backgroundStyle$ = disabled ? this.onBackgroundDisabled$ : this.onBackground$;
      stateSettings = this.settings.onState;
    } else {
      this.label$ = this.offLabel$;
      this.status$ = this.offStatus$;
      this.backgroundStyle$ = disabled ? this.offBackgroundDisabled$ : this.offBackground$;
      stateSettings = this.settings.offState;
    }
    this.showLabel = stateSettings.showLabel && this.layout !== StatusWidgetLayout.icon;
    this.showStatus = stateSettings.showStatus && this.layout !== StatusWidgetLayout.icon;
    this.icon = stateSettings.icon;
    this.padding = stateSettings.backgroundDisabled.overlay.enabled || stateSettings.background.overlay.enabled
      ? undefined
      : this.settings.padding;

    const primaryColor = disabled ? stateSettings.primaryColorDisabled : stateSettings.primaryColor;
    const secondaryColor = disabled ? stateSettings.secondaryColorDisabled : stateSettings.secondaryColor;

    this.labelStyle = textStyle(stateSettings.labelFont);
    this.labelStyle.color = primaryColor;

    this.statusStyle = textStyle(stateSettings.statusFont);
    this.statusStyle.color = secondaryColor;

    this.iconStyle = iconStyle(stateSettings.iconSize, stateSettings.iconSizeUnit);
    this.iconStyle.color = primaryColor;

    this.overlayStyle = overlayStyle(disabled ? stateSettings.backgroundDisabled.overlay : stateSettings.background.overlay);
    if (this.borderRadius) {
      this.overlayStyle = {...this.overlayStyle, ...{borderRadius: this.borderRadius}};
    }
    this.cd.detectChanges();
  }
}
