// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import { AppState } from '@core/core.state';
import { getCurrentAuthState, getCurrentAuthUser } from '@core/auth/auth.selectors';
import { NotificationService } from '@core/http/notification.service';
import { Authority } from '@shared/models/authority.enum';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { AddonType, SubscriptionEntry, subscriptionEntryToEntityType } from '@shared/models/subscription.models';
import { AiPlatformLimit } from '@shared/models/ai-chat.models';

type RequestState = 'idle' | 'sending' | 'sent' | 'failed';

type LimitAction = 'request' | 'license' | 'pricing' | 'none';

interface LimitView {
  icon: string;
  title: string;
  text: string;
  action: LimitAction;
  usage: boolean;
  requestLabel?: string;
  storageKey: string;
  send?: () => Observable<void>;
}

const PRICING_URL = 'https://thingsboard.io/pricing/';
const WL_REQUESTED_STORAGE_KEY = 'wlRequested';
const PE_PACK_REQUESTED_STORAGE_KEY = 'pePackRequested';
const FEATURE_KEYS: Record<string, string> = {
  [SubscriptionEntry.WHITE_LABELING]: 'white-labeling',
  INTEGRATIONS: 'integrations',
  SCHEDULER: 'scheduler',
  REPORTING: 'reporting'
};

@Component({
  selector: 'tb-ai-limit-out',
  templateUrl: './ai-limit-out.component.html',
  styleUrls: ['./ai-credits-out.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class AiLimitOutComponent {

  platformLimit = input.required<AiPlatformLimit>();

  dismissed = output<void>();

  private notificationService = inject(NotificationService);
  private translate = inject(TranslateService);
  private readonly store = inject(Store<AppState>);
  private readonly user = getCurrentAuthUser(this.store);
  private readonly authState = getCurrentAuthState(this.store);

  readonly pricingUrl = PRICING_URL;
  readonly state = signal<RequestState>('idle');
  readonly view = computed(() => this.viewOf(this.platformLimit()));

  constructor() {
    effect(() => this.state.set(this.wasRequested(this.view().storageKey) ? 'sent' : 'idle'));
  }

  request(): void {
    const send = this.view().send;
    if (!send || this.state() === 'sending' || this.state() === 'sent') {
      return;
    }
    this.state.set('sending');
    send().subscribe({
      next: () => {
        this.rememberRequested(this.view().storageKey);
        this.state.set('sent');
      },
      error: () => this.state.set('failed')
    });
  }

  private viewOf(limit: AiPlatformLimit): LimitView {
    const tenantAdmin = this.user?.authority === Authority.TENANT_ADMIN;
    const sysAdmin = this.user?.authority === Authority.SYS_ADMIN;
    const customer = this.user?.authority === Authority.CUSTOMER_USER;
    const oldLicense = !(this.authState?.licenseVersion > 1);
    const config = {ignoreLoading: true, ignoreErrors: true};
    if (limit.errorCode === 41 && limit.entityType) {
      const entityType = limit.entityType;
      return {
        icon: 'data_usage',
        title: this.say('entity-title', {entities: this.entities(entityType)}),
        text: customer
          ? this.say('tenant-text-plain', {entities: this.entities(entityType).toLowerCase()})
          : this.say('tenant-text', {limit: limit.limit, entities: this.entities(entityType).toLowerCase()}),
        action: 'request',
        usage: tenantAdmin,
        requestLabel: 'ai-assistant.limit-out.request',
        storageKey: `tb-entity-limit-request:${this.user?.tenantId}:${entityType}:${limit.limit}`,
        send: () => this.notificationService.sendEntitiesLimitIncreaseRequest(entityType, false, config)
      };
    }
    const entry = limit.subscriptionEntry as SubscriptionEntry;
    const licensedType = subscriptionEntryToEntityType.get(entry);
    if (limit.subscriptionErrorCode === 'LIMIT_REACHED' && licensedType) {
      return {
        icon: 'data_usage',
        title: this.say('entity-title', {entities: this.entities(licensedType)}),
        text: oldLicense
          ? this.say('license-text-old', {entities: this.entities(licensedType).toLowerCase()})
          : this.say('license-text', {entities: this.entities(licensedType).toLowerCase()}),
        action: oldLicense ? 'pricing' : (sysAdmin ? 'license' : 'request'),
        usage: false,
        requestLabel: 'ai-assistant.limit-out.request',
        storageKey: `tb-entity-limit-request:${this.user?.tenantId}:${licensedType}:license`,
        send: () => this.notificationService.sendEntitiesLimitIncreaseRequest(licensedType, true, config)
      };
    }
    const featureKey = FEATURE_KEYS[limit.subscriptionEntry ?? ''];
    if (limit.subscriptionErrorCode === 'FEATURE_DISABLED' && featureKey) {
      const whiteLabeling = entry === SubscriptionEntry.WHITE_LABELING;
      const grant = !!this.authState?.communityGrantLicense;
      const addon = whiteLabeling && !grant ? AddonType.WHITE_LABELING : AddonType.PROFESSIONAL_UPGRADE;
      return {
        icon: whiteLabeling ? 'palette' : 'lock',
        title: grant
          ? this.translate.instant(`platform-feature.${featureKey}.not-in-ce-grant`)
          : (whiteLabeling ? this.translate.instant('platform-feature.white-labeling.not-in-plan')
            : this.say('feature-title', {feature: this.say(`feature.${featureKey}`)})),
        text: this.say(addon === AddonType.WHITE_LABELING ? 'white-labeling-text' : 'pack-text'),
        action: sysAdmin ? 'license' : (oldLicense && !grant ? 'pricing' : 'request'),
        usage: false,
        requestLabel: 'subscription.request-access',
        storageKey: addon === AddonType.WHITE_LABELING ? WL_REQUESTED_STORAGE_KEY : PE_PACK_REQUESTED_STORAGE_KEY,
        send: () => this.notificationService.sendAddonAccessRequest(addon, config)
      };
    }
    return {
      icon: 'lock',
      title: this.say('license-required'),
      text: limit.message ?? '',
      action: sysAdmin ? 'license' : 'none',
      usage: false,
      storageKey: ''
    };
  }

  private entities(entityType: EntityType): string {
    const translations = entityTypeTranslations.get(entityType);
    return translations ? this.translate.instant(translations.typePlural) as string : entityType;
  }

  private say(key: string, params?: object): string {
    return this.translate.instant('ai-assistant.limit-out.' + key, params);
  }

  private wasRequested(key: string): boolean {
    if (!key) {
      return false;
    }
    try {
      const stored = localStorage.getItem(key);
      return stored === 'sent' || stored === 'true';
    } catch {
      return false;
    }
  }

  private rememberRequested(key: string): void {
    if (!key) {
      return;
    }
    try {
      localStorage.setItem(key, key === WL_REQUESTED_STORAGE_KEY || key === PE_PACK_REQUESTED_STORAGE_KEY ? JSON.stringify(true) : 'sent');
    } catch {
      return;
    }
  }
}
