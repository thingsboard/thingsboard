// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { MobileApplicationService } from '@core/http/mobile-application.service';
import { UtilsService } from '@core/services/utils.service';
import { unwrapModule } from '@core/utils';
import { BadgePosition, QrCodeSettings } from '@shared/models/mobile-app.models';
import { MINUTE } from '@shared/models/time/time.models';
import { HomeCardId } from '@shared/models/user-settings.models';
import { HOME_CARDS } from '@home/pages/home-page/home-cards.models';

@Component({
  selector: 'tb-mobile-app-home',
  templateUrl: './mobile-app-home.component.html',
  styleUrls: ['./mobile-app-home.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MobileAppHomeComponent {

  readonly card = HOME_CARDS[HomeCardId.MOBILE_APP];

  private mobileAppService = inject(MobileApplicationService);
  private utils = inject(UtilsService);

  readonly settings = signal<QrCodeSettings | null>(null);

  readonly qrCode = signal('');

  readonly badges = computed(() => {
    const settings = this.settings();
    return !!settings?.qrCodeConfig?.badgeEnabled && (settings.iosEnabled || settings.androidEnabled);
  });

  readonly badgesLeft = computed(() => this.settings()?.qrCodeConfig?.badgePosition === BadgePosition.LEFT);

  readonly label = computed(() => {
    const config = this.settings()?.qrCodeConfig;
    return config?.qrCodeLabelEnabled ? config.qrCodeLabel : '';
  });

  private deepLink = '';
  private deepLinkTimer: ReturnType<typeof setTimeout> | null = null;
  private destroyed = false;

  constructor() {
    this.mobileAppService.getMergedMobileAppSettings().subscribe(settings => this.settings.set(settings));
    this.loadDeepLink();
    inject(DestroyRef).onDestroy(() => {
      this.destroyed = true;
      clearTimeout(this.deepLinkTimer);
    });
  }

  openDeepLink(): void {
    if (this.deepLink && window.matchMedia('(pointer: coarse)').matches) {
      window.open(this.deepLink, '_blank');
    }
  }

  private loadDeepLink(): void {
    clearTimeout(this.deepLinkTimer);
    this.mobileAppService.getMobileAppDeepLink().subscribe(link => {
      if (this.destroyed) {
        return;
      }
      this.deepLink = link;
      import('qrcode').then(QRCode => unwrapModule(QRCode).toString(link, {type: 'svg', margin: 0}, (_error: Error, svg: string) => {
        if (!this.destroyed) {
          this.qrCode.set(svg);
        }
      }));
      const ttl = Number(this.utils.getQueryParam('ttl', link)) * MINUTE;
      if (ttl > 0) {
        this.deepLinkTimer = setTimeout(() => this.loadDeepLink(), ttl);
      }
    });
  }
}
