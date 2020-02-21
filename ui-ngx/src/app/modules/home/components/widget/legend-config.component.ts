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

import { Component, forwardRef, Inject, Input, OnDestroy, OnInit, ViewChild, ViewContainerRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { DOCUMENT } from '@angular/common';
import { CdkOverlayOrigin, ConnectedPosition, Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal, PortalInjector } from '@angular/cdk/portal';
import { MediaBreakpoints } from '@shared/models/constants';
import { BreakpointObserver } from '@angular/cdk/layout';
import { WINDOW } from '@core/services/window.service';
import { deepClone } from '@core/utils';
import { LegendConfig } from '@shared/models/widget.models';
import {
  LEGEND_CONFIG_PANEL_DATA,
  LegendConfigPanelComponent,
  LegendConfigPanelData
} from '@home/components/widget/legend-config-panel.component';

// @dynamic
@Component({
  selector: 'tb-legend-config',
  templateUrl: './legend-config.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => LegendConfigComponent),
      multi: true
    }
  ]
})
export class LegendConfigComponent implements OnInit, OnDestroy, ControlValueAccessor {

  @Input() disabled: boolean;

  @ViewChild('legendConfigPanelOrigin') legendConfigPanelOrigin: CdkOverlayOrigin;

  innerValue: LegendConfig;

  private propagateChange = (_: any) => {};

  constructor(private overlay: Overlay,
              public viewContainerRef: ViewContainerRef,
              public breakpointObserver: BreakpointObserver,
              @Inject(DOCUMENT) private document: Document,
              @Inject(WINDOW) private window: Window) {
  }

  ngOnInit(): void {
  }

  ngOnDestroy(): void {
  }

  openEditMode() {
    if (this.disabled) {
      return;
    }
    const isGtSm = this.breakpointObserver.isMatched(MediaBreakpoints['gt-sm']);
    const position = this.overlay.position();
    const config = new OverlayConfig({
      panelClass: 'tb-legend-config-panel',
      backdropClass: 'cdk-overlay-transparent-backdrop',
      hasBackdrop: isGtSm,
    });
    if (isGtSm) {
      config.minWidth = '220px';
      config.maxHeight = '300px';
      const panelHeight = 220;
      const panelWidth = 220;
      const el = this.legendConfigPanelOrigin.elementRef.nativeElement;
      const offset = el.getBoundingClientRect();
      const scrollTop = this.window.pageYOffset || this.document.documentElement.scrollTop || this.document.body.scrollTop || 0;
      const scrollLeft = this.window.pageXOffset || this.document.documentElement.scrollLeft || this.document.body.scrollLeft || 0;
      const bottomY = offset.bottom - scrollTop;
      const leftX = offset.left - scrollLeft;
      let originX;
      let originY;
      let overlayX;
      let overlayY;
      const wHeight = this.document.documentElement.clientHeight;
      const wWidth = this.document.documentElement.clientWidth;
      if (bottomY + panelHeight > wHeight) {
        originY = 'top';
        overlayY = 'bottom';
      } else {
        originY = 'bottom';
        overlayY = 'top';
      }
      if (leftX + panelWidth > wWidth) {
        originX = 'end';
        overlayX = 'end';
      } else {
        originX = 'start';
        overlayX = 'start';
      }
      const connectedPosition: ConnectedPosition = {
        originX,
        originY,
        overlayX,
        overlayY
      };
      config.positionStrategy = position.flexibleConnectedTo(this.legendConfigPanelOrigin.elementRef)
        .withPositions([connectedPosition]);
    } else {
      config.minWidth = '100%';
      config.minHeight = '100%';
      config.positionStrategy = position.global().top('0%').left('0%')
        .right('0%').bottom('0%');
    }

    const overlayRef = this.overlay.create(config);

    overlayRef.backdropClick().subscribe(() => {
      overlayRef.dispose();
    });

    const injector = this._createLegendConfigPanelInjector(
      overlayRef,
      {
        legendConfig: deepClone(this.innerValue),
        legendConfigUpdated: this.legendConfigUpdated.bind(this)
      }
    );

    overlayRef.attach(new ComponentPortal(LegendConfigPanelComponent, this.viewContainerRef, injector));
  }

  private _createLegendConfigPanelInjector(overlayRef: OverlayRef, data: LegendConfigPanelData): PortalInjector {
    const injectionTokens = new WeakMap<any, any>([
      [LEGEND_CONFIG_PANEL_DATA, data],
      [OverlayRef, overlayRef]
    ]);
    return new PortalInjector(this.viewContainerRef.injector, injectionTokens);
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(obj: LegendConfig): void {
    this.innerValue = obj;
  }

  private legendConfigUpdated(legendConfig: LegendConfig) {
    this.innerValue = legendConfig;
    this.propagateChange(this.innerValue);
  }
}
