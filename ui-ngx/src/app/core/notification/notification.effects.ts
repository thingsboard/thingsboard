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

import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { map } from 'rxjs/operators';

import { NotificationActions, NotificationActionTypes } from '@app/core/notification/notification.actions';
import { ToastNotificationService } from '@core/services/toast-notification.service';

@Injectable()
export class NotificationEffects {
  constructor(
    private actions$: Actions<NotificationActions>,
    private notificationService: ToastNotificationService
  ) {
  }

  
  dispatchNotification = createEffect(() => this.actions$.pipe(
    ofType(
      NotificationActionTypes.SHOW_NOTIFICATION,
    ),
    map(({ notification }) => {
      this.notificationService.dispatchNotification(notification);
    })
  ), {dispatch: false});

  
  hideNotification = createEffect(() => this.actions$.pipe(
    ofType(
      NotificationActionTypes.HIDE_NOTIFICATION,
    ),
    map(({ hideNotification }) => {
      this.notificationService.hideNotification(hideNotification);
    })
  ), {dispatch: false});
}
