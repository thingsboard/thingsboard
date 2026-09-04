// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
