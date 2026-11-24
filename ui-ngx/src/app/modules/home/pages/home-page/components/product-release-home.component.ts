// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { AdminService } from '@core/http/admin.service';
import { ToastNotificationService } from '@core/services/toast-notification.service';
import { UpdateMessage } from '@shared/models/settings.models';

const UNDO_MS = 6000;

let hiddenVersion: string | null = null;

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

  readonly update = signal<UpdateMessage | null>(null);

  private readonly hidden = signal<string | null>(hiddenVersion);

  readonly shown = computed(() => {
    const update = this.update();
    return !!update?.updateAvailable && this.hidden() !== update.latestVersion;
  });

  constructor() {
    inject(AdminService).checkUpdates({ignoreErrors: true, ignoreLoading: true}).subscribe(update => this.update.set(update));
  }

  hide(): void {
    const version = this.update().latestVersion;
    this.setHidden(version);
    this.toast.dispatchNotification({
      message: this.translate.instant('home.release.hidden', {version}),
      type: 'info',
      duration: UNDO_MS,
      horizontalPosition: 'right',
      verticalPosition: 'bottom',
      panelClass: 'tb-home-toast',
      actionLabel: this.translate.instant('home.customize.undo'),
      onAction: () => this.setHidden(null)
    });
  }

  private setHidden(version: string | null): void {
    hiddenVersion = version;
    this.hidden.set(version);
  }
}
