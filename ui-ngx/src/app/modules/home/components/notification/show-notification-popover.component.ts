// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { ChangeDetectorRef, Component, Input, NgZone, OnDestroy } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Notification, NotificationRequest } from '@shared/models/notification.models';
import { NotificationWebsocketService } from '@core/ws/notification-websocket.service';
import { BehaviorSubject, Observable, shareReplay } from 'rxjs';
import { filter, skip, tap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { NotificationSubscriber } from '@shared/models/telemetry/telemetry.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-show-notification-popover',
    templateUrl: './show-notification-popover.component.html',
    styleUrls: ['show-notification-popover.component.scss'],
    standalone: false
})
export class ShowNotificationPopoverComponent extends PageComponent implements OnDestroy {

  @Input()
  onClose: () => void;

  @Input()
  counter: BehaviorSubject<number>;

  @Input()
  popoverComponent: TbPopoverComponent;

  private notificationSubscriber = NotificationSubscriber.createNotificationsSubscription(this.notificationWsService, this.zone, 6);

  notifications$: Observable<Notification[]> = this.notificationSubscriber.notifications$.pipe(
    filter(value => Array.isArray(value)),
    shareReplay(1),
    tap(() => setTimeout(() => this.cd.markForCheck()))
  );

  constructor(protected store: Store<AppState>,
              private notificationWsService: NotificationWebsocketService,
              private zone: NgZone,
              private cd: ChangeDetectorRef,
              private router: Router) {
    super(store);
    this.notificationSubscriber.notificationCount$.pipe(
      skip(1),
      takeUntilDestroyed()
    ).subscribe(value => this.counter.next(value));
    this.notificationSubscriber.subscribe();
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.notificationSubscriber.unsubscribe();
    this.onClose();
  }

  markAsRead(id: string) {
    const cmd = NotificationSubscriber.createMarkAsReadCommand(this.notificationWsService, [id]);
    cmd.subscribe();
  }

  markAsAllRead($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const cmd = NotificationSubscriber.createMarkAllAsReadCommand(this.notificationWsService);
    cmd.subscribe();
  }

  viewAll($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.onClose();
    this.router.navigateByUrl(this.router.parseUrl('/notification/inbox')).then(() => {});
  }

  trackById(index: number, item: NotificationRequest): string {
    return item.id.id;
  }
}
