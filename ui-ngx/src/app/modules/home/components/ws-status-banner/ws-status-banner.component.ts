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

@Component({
  selector: 'tb-ws-status-banner',
  templateUrl: './ws-status-banner.component.html',
  styleUrls: ['./ws-status-banner.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  animations: [
    trigger('slideInOut', [
      transition(':enter', [
        style({ height: 0, opacity: 0 }),
        animate('200ms ease-out', style({ height: '*', opacity: 1 }))
      ]),
      transition(':leave', [
        animate('200ms ease-in', style({ height: 0, opacity: 0 }))
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
        if (delayMs === null) {
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
