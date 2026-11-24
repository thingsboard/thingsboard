// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  ElementRef,
  inject,
  Injector,
  signal,
  untracked
} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { ClipboardService } from 'ngx-clipboard';
import { UserSettingsService } from '@core/http/user-settings.service';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { ToastNotificationService } from '@core/services/toast-notification.service';
import { baseUrl } from '@core/utils';
import { GettingStarted, HomeCardId } from '@shared/models/user-settings.models';
import { HOME_CARDS } from '@home/pages/home-links/home/home-cards.models';
import { decorateCodeBlocks } from '@home/components/ai/ai-code-block';

export interface GetStartedStep {
  key: string;
  doc: string;
  link?: string;
  page?: string;
  code?: string;
}

const UNDO_MS = 6000;

const sendCommand = (temperature: number): string =>
  `curl -X POST ${baseUrl()}/api/v1/$ACCESS_TOKEN/telemetry \\\n  -H "Content-Type: application/json" \\\n  -d '{"temperature": ${temperature}}'`;

@Component({
  selector: 'tb-get-started-home',
  templateUrl: './get-started-home.component.html',
  styleUrls: ['./get-started-home.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GetStartedHomeComponent {

  readonly card = HOME_CARDS[HomeCardId.GET_STARTED];

  private userSettingsService = inject(UserSettingsService);
  private translate = inject(TranslateService);
  private clipboard = inject(ClipboardService);
  private toast = inject(ToastNotificationService);
  private host = inject<ElementRef<HTMLElement>>(ElementRef);
  private injector = inject(Injector);

  private readonly docs = inject(WhiteLabelingService).getDocsUrl();

  readonly steps: GetStartedStep[] = [
    {key: 'add-device', doc: 'getting-started/1/#step-11-create-a-device', link: '/entities/devices/all', page: 'device.devices'},
    {key: 'send-value', doc: 'getting-started/1/#step-12-verify-device-connectivity', code: sendCommand(25)},
    {key: 'add-dashboard', doc: 'getting-started/2/', link: '/dashboards/all', page: 'dashboard.dashboards'},
    {key: 'add-alarm-rule', doc: 'getting-started/3/#step-31-configure-alarm-rule', link: '/alarms/alarm-rules',
      page: 'home.get-started.alarm-rules'},
    {key: 'raise-alarm', doc: 'getting-started/3/#step-32-trigger-an-alarm', code: sendCommand(26)},
    {key: 'share', doc: 'getting-started/4/#step-41-create-a-customer', link: '/customers/all', page: 'customer.customers'}
  ];

  private readonly settings = signal<GettingStarted | null>(null);

  readonly loaded = computed(() => this.settings() !== null);

  readonly done = computed(() => Math.min(this.steps.length, Math.max(0, this.settings()?.maxSelectedIndex ?? 0)));

  readonly allDone = computed(() => this.done() >= this.steps.length);

  readonly open = computed(() => {
    if (this.allDone()) {
      return -1;
    }
    const last = this.settings()?.lastSelectedIndex;
    return last != null && last >= 0 && last < this.steps.length ? last : this.done();
  });

  readonly progress = computed(() => {
    if (!this.loaded() || !this.done()) {
      return null;
    }
    return this.allDone()
      ? this.translate.instant('home.get-started.all-done')
      : this.translate.instant('home.get-started.progress', {done: this.done(), total: this.steps.length});
  });

  constructor() {
    this.userSettingsService.getGettingStarted({ignoreLoading: true}).subscribe({
      next: settings => this.settings.set({lastSelectedIndex: settings?.lastSelectedIndex ?? 0, maxSelectedIndex: settings?.maxSelectedIndex ?? 0}),
      error: () => this.settings.set({lastSelectedIndex: 0, maxSelectedIndex: 0})
    });
    effect(() => {
      this.open();
      untracked(() => afterNextRender(() => decorateCodeBlocks(this.host.nativeElement, this.translate,
        text => this.clipboard.copyFromContent(text)), {injector: this.injector}));
    });
  }

  docUrl(step: GetStartedStep): string {
    return this.docs + step.doc;
  }

  isDone(index: number): boolean {
    return index < this.done();
  }

  select(index: number): void {
    if (index !== this.open()) {
      this.save({...this.settings(), lastSelectedIndex: index});
    }
  }

  toggle(index: number): void {
    if (this.isDone(index)) {
      this.save({lastSelectedIndex: index, maxSelectedIndex: index});
    } else {
      const done = index + 1;
      this.save({lastSelectedIndex: done, maxSelectedIndex: Math.max(this.done(), done)});
    }
  }

  startOver(): void {
    const before = this.settings();
    this.save({lastSelectedIndex: 0, maxSelectedIndex: 0});
    this.toast.dispatchNotification({
      message: this.translate.instant('home.get-started.started-over'),
      type: 'info',
      duration: UNDO_MS,
      horizontalPosition: 'right',
      verticalPosition: 'bottom',
      panelClass: 'tb-home-toast',
      actionLabel: this.translate.instant('home.customize.undo'),
      onAction: () => this.save(before)
    });
  }

  private save(settings: GettingStarted): void {
    this.settings.set(settings);
    this.userSettingsService.updateGettingStarted(settings, {ignoreLoading: true}).subscribe();
  }
}
