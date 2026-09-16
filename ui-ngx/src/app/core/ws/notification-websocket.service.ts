// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Inject, Injectable, NgZone } from '@angular/core';
import {
  TelemetryPluginCmdsWrapper,
  TelemetrySubscriber,
  WebsocketDataMsg
} from '@shared/models/telemetry/telemetry.models';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AuthService } from '@core/auth/auth.service';
import { WINDOW } from '@core/services/window.service';
import { WebsocketService } from '@core/ws/websocket.service';

// @dynamic
@Injectable({
  providedIn: 'root'
})
export class NotificationWebsocketService extends WebsocketService<TelemetrySubscriber> {

  constructor(private telemetryWebsocketService: TelemetryWebsocketService,
              protected store: Store<AppState>,
              protected authService: AuthService,
              protected ngZone: NgZone,
              @Inject(WINDOW) protected window: Window) {
    super(store, authService, ngZone, 'api/ws/plugins/telemetry', new TelemetryPluginCmdsWrapper(), window);
  }

  public subscribe(subscriber: TelemetrySubscriber) {
    this.telemetryWebsocketService.subscribe(subscriber);
  }

  public update(subscriber: TelemetrySubscriber) {
    this.telemetryWebsocketService.update(subscriber);
  }

  public unsubscribe(subscriber: TelemetrySubscriber) {
    this.telemetryWebsocketService.unsubscribe(subscriber);
  }

  processOnMessage(message: WebsocketDataMsg) {
    this.telemetryWebsocketService.processOnMessage(message);
  }
}
