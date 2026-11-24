// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { ToastNotificationService } from '@core/services/toast-notification.service';
import { UpdateMessage } from '@shared/models/settings.models';
import { SysAdminHomeService } from '@home/pages/home-page/sys-admin-home.service';

const UNDO_MS = 6000;

@Component({
  selector: 'tb-product-release-home',
  templateUrl: './product-release-home.component.html',
  styleUrls: ['./product-release-home.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductReleaseHomeComponent {

  private translate = inject(TranslateService);
  private toast = inject(ToastNotificationService);
  private sysAdminHome = inject(SysAdminHomeService);

  readonly update = signal<UpdateMessage | null>(null);

  readonly shown = computed(() => {
    const update = this.update();
    return !!update?.updateAvailable && this.sysAdminHome.hiddenRelease() !== update.latestVersion;
  });

  constructor() {
    this.sysAdminHome.update$.subscribe(update => this.update.set(update));
  }

  hide(): void {
    const version = this.update().latestVersion;
    this.sysAdminHome.setHiddenRelease(version);
    this.toast.dispatchNotification({
      message: this.translate.instant('home.release.hidden', {version}),
      type: 'info',
      duration: UNDO_MS,
      horizontalPosition: 'right',
      verticalPosition: 'bottom',
      panelClass: 'tb-home-toast',
      actionLabel: this.translate.instant('home.customize.undo'),
      onAction: () => this.sysAdminHome.setHiddenRelease(null)
    });
  }
}
