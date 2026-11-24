// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  inject,
  NgZone,
  signal
} from '@angular/core';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { ActionPreferencesUpdateHomeCards } from '@core/auth/auth.actions';
import { UserSettingsService } from '@core/http/user-settings.service';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { EntityCountCmd, TelemetrySubscriber } from '@shared/models/telemetry/telemetry.models';
import { AliasFilterType } from '@shared/models/alias.models';
import { EntityType } from '@shared/models/entity-type.models';
import { FeaturesInfo } from '@shared/models/settings.models';
import { HomeCardId } from '@shared/models/user-settings.models';
import { HOME_CARDS } from '@home/pages/home-page/home-cards.models';
import { SysAdminHomeService } from '@home/pages/home-page/sys-admin-home.service';

type SysAdminStepKey = 'tenant' | 'email' | 'sms' | 'wl' | '2fa' | 'oauth' | 'slack';

export interface SysAdminStep {
  key: SysAdminStepKey;
  needed: boolean;
  link: string;
  queryParams?: Record<string, string>;
  guide: string;
  feature?: keyof FeaturesInfo;
}

const STEPS: SysAdminStep[] = [
  {key: 'tenant', needed: true, link: '/tenants', queryParams: {action: 'add'}, guide: 'user-guide/multi-tenancy/'},
  {key: 'email', needed: true, link: '/settings/outgoing-mail', guide: 'user-guide/ui/mail-settings/', feature: 'emailEnabled'},
  {key: 'sms', needed: false, link: '/settings/notifications', guide: 'user-guide/ui/sms-provider-settings/', feature: 'smsEnabled'},
  {key: 'wl', needed: false, link: '/white-labeling/whiteLabel', guide: 'user-guide/white-labeling/', feature: 'whiteLabelingEnabled'},
  {key: '2fa', needed: false, link: '/security-settings/2fa', guide: 'user-guide/security/two-factor-authentication/',
    feature: 'twoFaEnabled'},
  {key: 'oauth', needed: false, link: '/security-settings/oauth2', guide: 'user-guide/security/oauth-2-support/', feature: 'oauthEnabled'},
  {key: 'slack', needed: false, link: '/settings/notifications', guide: 'user-guide/ui/slack-settings/', feature: 'notificationEnabled'}
];

const LOADING_HEIGHT = 398;
const HEIGHT_KEY = 'tb-home-get-started-sys-admin-height';

const storedHeight = (): number => {
  try {
    return Number(localStorage.getItem(HEIGHT_KEY)) || LOADING_HEIGHT;
  } catch {
    return LOADING_HEIGHT;
  }
};

export type StepState = 'done' | 'skipped' | 'todo';

@Component({
  selector: 'tb-get-started-sys-admin-home',
  templateUrl: './get-started-sys-admin-home.component.html',
  styleUrls: ['./get-started-sys-admin-home.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GetStartedSysAdminHomeComponent {

  private readonly card = HOME_CARDS[HomeCardId.GET_STARTED_SYS_ADMIN];

  readonly total = STEPS.length;

  readonly groups = [
    {label: 'home.get-started-sys-admin.before-tenants', steps: STEPS.filter(step => step.needed)},
    {label: 'home.get-started-sys-admin.when-needed', steps: STEPS.filter(step => !step.needed)}
  ];

  private store = inject(Store<AppState>);
  private translate = inject(TranslateService);
  private userSettingsService = inject(UserSettingsService);
  private readonly docs = inject(WhiteLabelingService).getDocsUrl();

  private readonly tenants = signal<number | null>(null);
  private readonly features = signal<FeaturesInfo | null>(null);
  private readonly skipped = signal<string[] | null>(null);
  private readonly selected = signal<SysAdminStepKey | '-' | null>(null);

  readonly loaded = computed(() => this.tenants() !== null && this.features() !== null && this.skipped() !== null);

  readonly cardDefinition = computed(() => this.loaded() ? this.card : {...this.card, height: storedHeight()});

  readonly states = computed<Record<SysAdminStepKey, StepState>>(() => {
    const features = this.features();
    const skipped = this.skipped() ?? [];
    return Object.fromEntries(STEPS.map(step => {
      const done = step.key === 'tenant' ? !!this.tenants() : !!features?.[step.feature];
      let state: StepState = 'todo';
      if (done) {
        state = 'done';
      } else if (!step.needed && skipped.includes(step.key)) {
        state = 'skipped';
      }
      return [step.key, state];
    })) as Record<SysAdminStepKey, StepState>;
  });

  readonly doneCount = computed(() => STEPS.filter(step => this.states()[step.key] === 'done').length);

  readonly progress = computed(() => this.doneCount()
    ? this.translate.instant('home.get-started-sys-admin.progress', {done: this.doneCount(), total: STEPS.length}) : null);

  readonly finished = computed(() => this.loaded() && STEPS.every(step => ['done', 'skipped'].includes(this.states()[step.key])));

  readonly open = computed<SysAdminStep | null>(() => {
    const selected = this.selected();
    if (selected === '-') {
      return null;
    }
    return STEPS.find(step => step.key === selected) ?? STEPS.find(step => this.states()[step.key] === 'todo') ?? null;
  });

  readonly tenantCount = computed(() => this.tenants() ?? 0);

  constructor() {
    const config = {ignoreErrors: true, ignoreLoading: true};
    inject(SysAdminHomeService).featuresInfo$.subscribe(features => this.features.set(features ?? {} as FeaturesInfo));
    this.userSettingsService.getGettingStarted(config).pipe(catchError(() => of(null)))
      .subscribe(settings => this.skipped.set(settings?.skipped ?? []));
    const cmd = new EntityCountCmd();
    cmd.query = {entityFilter: {type: AliasFilterType.entityType, entityType: EntityType.TENANT}};
    const subscriber = new TelemetrySubscriber(inject(TelemetryWebsocketService), inject(NgZone));
    subscriber.subscriptionCommands.push(cmd);
    subscriber.entityCount$.subscribe(update => this.tenants.set(update.count ?? 0));
    subscriber.subscribe();
    const host = inject<ElementRef<HTMLElement>>(ElementRef);
    const observer = new ResizeObserver(() => this.rememberHeight(host.nativeElement));
    observer.observe(host.nativeElement);
    inject(DestroyRef).onDestroy(() => {
      subscriber.unsubscribe();
      observer.disconnect();
    });
  }

  state(step: SysAdminStep): StepState {
    return this.states()[step.key];
  }

  number(step: SysAdminStep): number {
    return STEPS.indexOf(step) + 1;
  }

  docUrl(step: SysAdminStep): string {
    return this.docs + step.guide;
  }

  toggle(step: SysAdminStep): void {
    this.selected.set(this.open() === step ? '-' : step.key);
  }

  skip(step: SysAdminStep): void {
    const skipped = this.skipped() ?? [];
    const next = skipped.includes(step.key) ? skipped.filter(key => key !== step.key) : [...skipped, step.key];
    this.skipped.set(next);
    this.selected.set(null);
    this.userSettingsService.updateGettingStarted({skipped: next}).subscribe();
  }

  private rememberHeight(element: HTMLElement): void {
    const height = Math.round(element.getBoundingClientRect().height);
    if (!this.loaded() || !height) {
      return;
    }
    try {
      localStorage.setItem(HEIGHT_KEY, String(height));
    } catch {
      return;
    }
  }

  close(): void {
    this.store.dispatch(new ActionPreferencesUpdateHomeCards({[HomeCardId.GET_STARTED_SYS_ADMIN]: false}));
  }
}
