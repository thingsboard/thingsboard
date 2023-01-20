///
/// Copyright © 2016-2022 The Thingsboard Authors
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

import { ChangeDetectorRef, Component, Input, NgZone, OnDestroy, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Notification } from '@shared/models/notification.models';
import { NotificationWebsocketService } from '@core/ws/notification-websocket.service';
import { Observable } from 'rxjs';
import { publishReplay, refCount, tap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { NotificationSubscriber } from '@shared/models/notification-ws.models';

@Component({
  selector: 'tb-show-notification-popover',
  templateUrl: './show-notification-popover.component.html',
  styleUrls: []
})
export class ShowNotificationPopoverComponent extends PageComponent implements OnDestroy, OnInit {

  @Input()
  onClose: () => void;

  @Input()
  popoverComponent: TbPopoverComponent;

  private notificationSubscriber: NotificationSubscriber;
  notifications$: Observable<Notification[]>;

  constructor(protected store: Store<AppState>,
              private notificationWsService: NotificationWebsocketService,
              private zone: NgZone,
              private cd: ChangeDetectorRef,
              private router: Router) {
    super(store);
  }

  ngOnInit() {
    this.notificationSubscriber = NotificationSubscriber.createNotificationsSubscription(
      this.notificationWsService, this.zone);
    this.notifications$ = this.notificationSubscriber.notifications$.pipe(
      publishReplay(1),
      refCount(),
      tap(() => {
        setTimeout(() => {
          this.cd.markForCheck();
        });
      })
    );
    this.notificationSubscriber.subscribe();
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.notificationSubscriber.unsubscribe();
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
    this.router.navigateByUrl(this.router.createUrlTree(['notification-center'])).then(() => {});
  }
}
