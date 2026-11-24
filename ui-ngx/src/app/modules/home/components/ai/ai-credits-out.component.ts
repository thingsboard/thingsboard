// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, input, NgZone, OnInit, output, signal } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { NotificationService } from '@core/http/notification.service';
import { ShortNumberPipe } from '@shared/pipe/short-number.pipe';
import { AiCreditsLimitType, aiCreditsResetDate, watchAiCredits } from '@home/components/ai/ai-credits.models';

type RequestState = 'idle' | 'sending' | 'sent' | 'failed';

@Component({
  selector: 'tb-ai-credits-out',
  templateUrl: './ai-credits-out.component.html',
  styleUrls: ['./ai-credits-out.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class AiCreditsOutComponent implements OnInit {

  limitType = input<AiCreditsLimitType>('TENANT');

  dismissed = output<void>();

  private notificationService = inject(NotificationService);
  private shortNumber = inject(ShortNumberPipe);
  private readonly tenantId = getCurrentAuthUser(inject(Store<AppState>))?.tenantId;

  readonly used = signal<number | null>(null);
  readonly limit = signal<number | null>(null);
  readonly resetDate = aiCreditsResetDate();
  readonly state = signal<RequestState>('idle');

  readonly tenantLimit = computed(() => this.limitType() === 'TENANT');
  readonly hasUsage = computed(() => this.tenantLimit() && !!this.limit());
  readonly usedLabel = computed(() => this.shortNumber.transform(this.used() ?? 0, {roundDown: true}));
  readonly limitLabel = computed(() => this.shortNumber.transform(this.limit() ?? 0));
  readonly percent = computed(() => {
    const limit = this.limit();
    return limit ? Math.min(100, Math.round((this.used() ?? 0) / limit * 100)) : 100;
  });

  constructor() {
    const subscriber = watchAiCredits(inject(TelemetryWebsocketService), inject(NgZone), update => {
      if (update.count !== undefined) {
        this.used.set(update.count);
      }
      if (update.limit !== undefined) {
        this.limit.set(update.limit);
      }
    });
    inject(DestroyRef).onDestroy(() => subscriber.unsubscribe());
  }

  ngOnInit(): void {
    if (this.wasRequested()) {
      this.state.set('sent');
    }
  }

  request(): void {
    if (this.state() === 'sending' || this.state() === 'sent') {
      return;
    }
    this.state.set('sending');
    this.notificationService.sendAiCreditsLimitIncreaseRequest(this.limitType(), {ignoreLoading: true, ignoreErrors: true}).subscribe({
      next: () => {
        this.rememberRequested();
        this.state.set('sent');
      },
      error: () => this.state.set('failed')
    });
  }

  private requestKey(): string {
    return `tb-ai-credits-request:${this.tenantId}:${this.limitType()}`;
  }

  private wasRequested(): boolean {
    try {
      return localStorage.getItem(this.requestKey()) === String(this.periodStart());
    } catch {
      return false;
    }
  }

  private rememberRequested(): void {
    try {
      localStorage.setItem(this.requestKey(), String(this.periodStart()));
    } catch {
      return;
    }
  }

  private periodStart(): number {
    const now = new Date();
    return Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), 1);
  }
}
