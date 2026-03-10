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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy } from '@angular/core';
import { Subject, Subscription, interval } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { animate, style, transition, trigger } from '@angular/animations';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';

// Only surface the banner for reconnect delays at or above this threshold.
// Short 2 s first-retry attempts are silent — no reason to alarm the user.
const MIN_VISIBLE_DELAY_MS = 4000;

@Component({
  selector: 'tb-ws-status-banner',
  templateUrl: './ws-status-banner.component.html',
  styleUrls: ['./ws-status-banner.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  animations: [
    trigger('fadeSlide', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateY(-8px)' }),
        animate('220ms ease-out', style({ opacity: 1, transform: 'translateY(0)' }))
      ]),
      transition(':leave', [
        animate('180ms ease-in', style({ opacity: 0, transform: 'translateY(-8px)' }))
      ])
    ])
  ]
})
export class WsStatusBannerComponent implements OnDestroy {

  isVisible = false;
  countdown = 0;
  totalSeconds = 0;

  get progressValue(): number {
    return this.totalSeconds > 0 ? (this.countdown / this.totalSeconds) * 100 : 0;
  }

  private destroy$ = new Subject<void>();
  private countdownSub: Subscription;

  constructor(private telemetryWsService: TelemetryWebsocketService,
              private cd: ChangeDetectorRef) {
    this.telemetryWsService.reconnectStatus$
      .pipe(takeUntil(this.destroy$))
      .subscribe(delayMs => {
        if (delayMs === null || delayMs < MIN_VISIBLE_DELAY_MS) {
          this.isVisible = false;
          this.countdownSub?.unsubscribe();
        } else {
          this.isVisible = true;
          this.startCountdown(delayMs);
        }
        this.cd.markForCheck();
      });
  }

  retryNow() {
    this.telemetryWsService.reconnectNow();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private startCountdown(delayMs: number) {
    this.countdownSub?.unsubscribe();
    this.countdown = Math.round(delayMs / 1000);
    this.totalSeconds = this.countdown;
    this.countdownSub = interval(1000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        if (this.countdown > 0) {
          this.countdown--;
          this.cd.markForCheck();
        }
      });
  }
}
