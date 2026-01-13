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
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  NgZone,
  OnDestroy,
  Renderer2,
  ViewContainerRef
} from '@angular/core';
import { NotificationWebsocketService } from '@core/ws/notification-websocket.service';
import { BehaviorSubject, ReplaySubject, Subscription } from 'rxjs';
import { distinctUntilChanged, map, share, skip, tap } from 'rxjs/operators';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { ShowNotificationPopoverComponent } from '@home/components/notification/show-notification-popover.component';
import { NotificationSubscriber } from '@shared/models/telemetry/telemetry.models';
import { select, Store } from '@ngrx/store';
import { selectIsAuthenticated } from '@core/auth/auth.selectors';
import { AppState } from '@core/core.state';

@Component({
  selector: 'tb-notification-bell',
  templateUrl: './notification-bell.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NotificationBellComponent implements OnDestroy {

  private notificationSubscriber: NotificationSubscriber;
  private notificationCountSubscriber: Subscription;
  private countSubject = new BehaviorSubject(0);

  count$ = this.countSubject.asObservable().pipe(
    distinctUntilChanged(),
    map((value) => value >= 100 ? '99+' : value),
    tap(() => setTimeout(() => this.cd.markForCheck())),
    share({
      connector: () => new ReplaySubject(1)
    })
  );

  constructor(
    private notificationWsService: NotificationWebsocketService,
    private zone: NgZone,
    private cd: ChangeDetectorRef,
    private popoverService: TbPopoverService,
    private renderer: Renderer2,
    private viewContainerRef: ViewContainerRef,
    private store: Store<AppState>,) {
    this.store.pipe(select(selectIsAuthenticated)).subscribe((value) => {
      if (value) {
        this.initSubscription();
      }
    });
  }

  ngOnDestroy() {
    this.unsubscribeSubscription();
  }

  showNotification($event: Event, createVersionButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = createVersionButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      this.unsubscribeSubscription();
      const showNotificationPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, ShowNotificationPopoverComponent, 'bottom', true, null,
        {
          onClose: () => {
            showNotificationPopover.hide();
            this.initSubscription();
          },
          counter: this.countSubject
        },
        {maxHeight: '90vh', height: '100%', padding: '10px'},
        {width: '400px', minWidth: '100%', maxWidth: '100%'},
        {height: '100%', flexDirection: 'column', boxSizing: 'border-box', display: 'flex', margin: '0 -16px'}, false);
      showNotificationPopover.tbComponentRef.instance.popoverComponent = showNotificationPopover;
    }
  }

  private initSubscription() {
    this.notificationSubscriber = NotificationSubscriber.createNotificationCountSubscription(this.notificationWsService, this.zone);
    this.notificationCountSubscriber = this.notificationSubscriber.notificationCount$.pipe(
      skip(1),
    ).subscribe(value => this.countSubject.next(value));

    this.notificationSubscriber.subscribe();
  }

  private unsubscribeSubscription() {
    this.notificationCountSubscriber.unsubscribe();
    this.notificationSubscriber.unsubscribe();
    this.notificationSubscriber = null;
  }
}
