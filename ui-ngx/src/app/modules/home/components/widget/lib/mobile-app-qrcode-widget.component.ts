///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { ChangeDetectorRef, Component, ElementRef, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { AppState } from '@core/core.state';
import { Store } from '@ngrx/store';
import { BadgePosition, BadgeStyle, badgeStyleURLMap, MobileAppSettings } from '@shared/models/mobile-app.models';
import { MobileAppService } from '@core/http/mobile-app.service';
import { WidgetContext } from '@home/models/widget-component.models';
import { UtilsService } from '@core/services/utils.service';
import { Subject } from 'rxjs';
import { MINUTE } from '@shared/models/time/time.models';
import { MobileAppQrCodeWidgetSettings } from '@home/components/widget/lib/cards/mobile-app-qr-code-widget.models';
import { isDefinedAndNotNull } from '@core/utils';
import { ResizeObserver } from '@juggle/resize-observer';

@Component({
  selector: 'tb-mobile-app-qrcode-widget',
  templateUrl: './mobile-app-qrcode-widget.component.html',
  styleUrls: ['./mobile-app-qrcode-widget.component.scss']
})
export class MobileAppQrcodeWidgetComponent extends PageComponent implements OnInit, OnDestroy {

  @Input()
  ctx: WidgetContext;

  @Input()
  set mobileAppSettings(settings: MobileAppSettings | MobileAppQrCodeWidgetSettings) {
    if (settings) {
      this.mobileAppSettingsValue = settings;
    }
  };

  get mobileAppSettings(): MobileAppSettings | MobileAppQrCodeWidgetSettings {
    return this.mobileAppSettingsValue;
  }

  @ViewChild('canvas', {static: true}) canvasRef: ElementRef<HTMLCanvasElement>;

  private readonly destroy$ = new Subject<void>();
  private widgetResize$: ResizeObserver;

  badgeStyle = BadgeStyle;
  badgePosition = BadgePosition;
  badgeStyleURLMap = badgeStyleURLMap;
  showBadgeContainer = true;

  private mobileAppSettingsValue: MobileAppSettings | MobileAppQrCodeWidgetSettings;
  private deepLinkTTL: number;
  private deepLinkTTLTimeoutID: NodeJS.Timeout;

  constructor(protected store: Store<AppState>,
              protected cd: ChangeDetectorRef,
              private mobileAppService: MobileAppService,
              private utilsService: UtilsService,
              private elementRef: ElementRef) {
    super(store);
  }

  ngOnInit(): void {
    if (!this.mobileAppSettings) {
      if (isDefinedAndNotNull(this.ctx.settings.useSystemSettings) && !this.ctx.settings.useSystemSettings) {
        this.mobileAppSettings = this.ctx.settings;
      } else {
        this.mobileAppService.getMobileAppSettings().subscribe((settings => {
          this.mobileAppSettings = settings;
          this.cd.markForCheck();
        }));
      }
    }
    this.initMobileAppQRCode();
    this.widgetResize$ = new ResizeObserver(() => {
      const showHideBadgeContainer = this.elementRef.nativeElement.offsetWidth > 250;
      if (showHideBadgeContainer !== this.showBadgeContainer) {
        this.showBadgeContainer = showHideBadgeContainer;
        this.cd.markForCheck();
      }
    });
    this.widgetResize$.observe(this.elementRef.nativeElement);
  }

  ngOnDestroy() {
    if (this.widgetResize$) {
      this.widgetResize$.disconnect();
    }
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
    clearTimeout(this.deepLinkTTLTimeoutID);
  }

  private initMobileAppQRCode() {
    if (this.deepLinkTTLTimeoutID) {
      clearTimeout(this.deepLinkTTLTimeoutID);
      this.deepLinkTTLTimeoutID = null;
    }
    this.mobileAppService.getMobileAppDeepLink().subscribe(link => {
      this.deepLinkTTL = Number(this.utilsService.getQueryParam('ttl', link)) * MINUTE;
      this.updateQRCode(link);
      this.deepLinkTTLTimeoutID = setTimeout(() => this.initMobileAppQRCode(), this.deepLinkTTL);
    });
  }

  private updateQRCode(link: string) {
    import('qrcode').then((QRCode) => {
      QRCode.toCanvas(this.canvasRef.nativeElement, link, { width: 100 });
    });
  }

}
