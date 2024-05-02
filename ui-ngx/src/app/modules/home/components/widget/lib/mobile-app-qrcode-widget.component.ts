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

import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { AppState } from '@core/core.state';
import { Store } from '@ngrx/store';
import { BadgePosition, BadgeStyle, badgeStyleURLMap, MobileAppSettings } from '@shared/models/mobile-app.models';
import { MobileAppService } from '@core/http/mobile-app.service';
import { WidgetContext } from '@home/models/widget-component.models';
import { UtilsService } from '@core/services/utils.service';
import { interval, mergeMap, Observable, Subject, takeUntil } from 'rxjs';
import { MINUTE } from '@shared/models/time/time.models';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
  selector: 'tb-mobile-app-qrcode-widget',
  templateUrl: './mobile-app-qrcode-widget.component.html',
  styleUrls: ['./mobile-app-qrcode-widget.component.scss']
})
export class MobileAppQrcodeWidgetComponent extends PageComponent implements OnInit, AfterViewInit, OnDestroy {

  @Input()
  ctx: WidgetContext;

  @Input()
  @coerceBoolean()
  previewMode: boolean;

  @Input()
  set mobileAppSettings(settings: MobileAppSettings) {
    if (settings) {
      this.mobileAppSettingsValue = settings;
    }
  };

  get mobileAppSettings() {
    return this.mobileAppSettingsValue;
  }

  @ViewChild('canvas', {static: false}) canvasRef: ElementRef<HTMLCanvasElement>;

  private readonly destroy$ = new Subject<void>();

  badgeStyle = BadgeStyle;
  badgePosition = BadgePosition;
  badgeStyleURLMap = badgeStyleURLMap;

  private mobileAppSettingsValue: MobileAppSettings;
  private deepLinkTTL: number;

  constructor(protected store: Store<AppState>,
              protected cd: ChangeDetectorRef,
              private mobileAppService: MobileAppService,
              private utilsService: UtilsService) {
    super(store);
  }

  ngOnInit(): void {
    if (this.ctx) {
      this.ctx.$scope.mobileAppQrcodeWidget = this;
    }
    if (!this.mobileAppSettings) {
      this.mobileAppService.getMobileAppSettings().subscribe((settings => {
        this.mobileAppSettings = settings;
        this.cd.detectChanges();
      }));
    }
  }

  ngAfterViewInit(): void {
    this.getMobileAppDeepLink().subscribe(link => {
      this.deepLinkTTL = Number(this.utilsService.getQueryParam('ttl', link)) * MINUTE;
      this.updateQRCode(link);
      interval(this.deepLinkTTL).pipe(
        takeUntil(this.destroy$),
        mergeMap(() => this.getMobileAppDeepLink())
      ).subscribe(link => this.updateQRCode(link));
    });
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  getMobileAppDeepLink(): Observable<string> {
    return this.mobileAppService.getMobileAppDeepLink();
  }

  updateQRCode(link: string) {
    import('qrcode').then((QRCode) => {
      QRCode.toCanvas(this.canvasRef.nativeElement, link, { width: 125});
    });
  }

}
