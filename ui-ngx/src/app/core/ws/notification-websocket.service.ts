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
