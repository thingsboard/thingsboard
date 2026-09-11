// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NotificationState } from './notification.models';
import { NotificationActions, NotificationActionTypes } from './notification.actions';

export const initialState: NotificationState = {
  notification: null,
  hideNotification: null
};

export function notificationReducer(
  state: NotificationState = initialState,
  action: NotificationActions
): NotificationState {
  switch (action.type) {
    case NotificationActionTypes.SHOW_NOTIFICATION:
      return { ...state, notification: action.notification };
    case NotificationActionTypes.HIDE_NOTIFICATION:
      return { ...state, hideNotification: action.hideNotification };
    default:
      return state;
  }
}
