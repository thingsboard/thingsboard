// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, DestroyRef, ElementRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Store } from '@ngrx/store';
import { interval } from 'rxjs';
import { AppState } from '@core/core.state';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { AdminService } from '@core/http/admin.service';
import { DateAgoPipe } from '@shared/pipe/date-ago.pipe';
import { HomeCardId } from '@shared/models/user-settings.models';
import {
  AddonType,
  addonTypeTranslationMap,
  PlanUiType,
  SubscriptionInfo,
  usageAddItemTypeTranslationMap,
  UsageItemType,
  usageItemTypeTranslationMap
} from '@shared/models/subscription.models';
import { HOME_CARDS } from '@home/pages/home-page/home-cards.models';
import {
  LicenseActionsService,
  licenseLimitCritical,
  licenseLimitWarn,
  licenseMeterWidth
} from '@home/components/license/license-actions.service';

const LOADING_HEIGHT = 398;
const HEIGHT_KEY = 'tb-home-license-height';

const storedHeight = (): number => {
  try {
    return Number(localStorage.getItem(HEIGHT_KEY)) || LOADING_HEIGHT;
  } catch {
    return LOADING_HEIGHT;
  }
};

const AGO_REFRESH_MS = 30_000;

const PLANS_URL = 'https://thingsboard.io/pricing/?product=thingsboard-pe';

const TONED_ITEMS = [UsageItemType.DEVICES, UsageItemType.ASSETS, UsageItemType.AI_CREDITS];

type LimitTone = 'critical' | 'near' | null;

type LimitAction = 'add' | 'upgrade' | null;

interface LicenseLimit {
  itemType: UsageItemType;
  label: string;
  used: number;
  max: number;
  percent: number;
  width: number;
  tone: LimitTone;
  action: LimitAction;
  addLabel: string;
}

interface LicenseLimitGroup {
  id: string;
  label: string;
  icon: string;
  rows: LicenseLimit[];
}

type AddonState = 'included' | 'not-included' | 'add' | 'upgrade' | 'not-in-license';

interface LicenseAddon {
  type: AddonType;
  label: string;
  state: AddonState;
}

interface LicenseOffer {
  icon: string;
  title: string;
  text: string;
}

const PE_PACK_OFFERS: LicenseOffer[] = [
  {icon: 'brush', title: 'subscription.white-labeling', text: 'subscription.white-labeling-promo'},
  {icon: 'integration_instructions', title: 'subscription.integrations', text: 'subscription.integrations-promo'},
  {icon: 'schedule', title: 'subscription.scheduler', text: 'subscription.scheduler-promo'},
  {icon: 'assessment', title: 'subscription.reports', text: 'subscription.reports-promo'}
];

const PRODUCTION_OFFERS: LicenseOffer[] = [
  {icon: 'rocket_launch', title: 'home.license.production-use', text: 'home.license.production-use-text'},
  {icon: 'brush', title: 'subscription.white-labeling', text: 'subscription.white-labeling-promo'},
  {icon: 'auto_awesome', title: 'subscription.ai-credits', text: 'home.license.ai-credits-text'},
  {icon: 'extension', title: 'subscription.add-ons', text: 'home.license.add-ons-text'}
];

@Component({
  selector: 'tb-license-home',
  templateUrl: './license-home.component.html',
  styleUrls: ['./license-home.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LicenseHomeComponent {

  private adminService = inject(AdminService);
  private dateAgoPipe = inject(DateAgoPipe);
  private licenseActions = inject(LicenseActionsService);
  private aiEnabled = getCurrentAuthState(inject(Store<AppState>)).aiEnabled;

  readonly pePackOffers = PE_PACK_OFFERS;
  readonly productionOffers = PRODUCTION_OFFERS;

  readonly info = signal<SubscriptionInfo | null>(null);
  readonly refreshing = signal(false);
  private readonly now = signal(Date.now());

  readonly cardDefinition = computed(() => {
    const card = HOME_CARDS[HomeCardId.LICENSE];
    return this.info() ? card : {...card, height: storedHeight()};
  });

  readonly kind = computed(() => {
    const info = this.info();
    if (!info) {
      return null;
    }
    if (info.development) {
      return 'development';
    }
    if (info.communityGrantLicense) {
      return 'grant';
    }
    return info.perpetual ? 'perpetual' : 'subscription';
  });

  readonly markHue = computed(() => {
    switch (this.info()?.planUiType) {
      case PlanUiType.TbNonCommercial:
      case PlanUiType.TbNonCommercialOffline:
      case PlanUiType.TbCommunityGrant:
        return true;
      default:
        return false;
    }
  });

  readonly managed = computed(() => {
    const info = this.info();
    return !!info && !info.offline && !info.nonProduction;
  });

  readonly updatedAgo = computed(() => {
    this.now();
    const dataTs = this.info()?.dataTs;
    return dataTs ? this.dateAgoPipe.transform(dataTs, {applyAgo: true, long: true, textPart: true}) : null;
  });

  readonly limitGroups = computed<LicenseLimitGroup[]>(() => {
    const info = this.info();
    if (!info || info.development) {
      return [];
    }
    const groups: LicenseLimitGroup[] = [];
    const entities = [
      this.limit(info, UsageItemType.DEVICES, info.devicesCount, info.maxDevices, info.planExtraDeviceEnabled),
      this.limit(info, UsageItemType.ASSETS, info.assetsCount, info.maxAssets, false)
    ].filter(row => !!row);
    if (entities.length) {
      groups.push({id: 'entities', label: 'home.license.devices-and-assets', icon: 'memory', rows: entities});
    }
    const installations = [
      info.maxInstances > 0 ?
        this.limit(info, UsageItemType.PROD_INSTANCES, info.instancesCount, info.maxInstances, info.planExtraInstanceEnabled) : null,
      info.edgeEnabled ? this.limit(info, UsageItemType.EDGES, info.edgesCount, info.maxEdges, info.planExtraEdgeEnabled) : null,
      info.maxAgents > 0 || info.planExtraAgentEnabled ?
        this.limit(info, UsageItemType.AGENTS, info.agentsCount, info.maxAgents, info.planExtraAgentEnabled) : null
    ].filter(row => !!row);
    if (installations.length) {
      groups.push({id: 'installations', label: 'home.license.installations', icon: 'lan', rows: installations});
    }
    if (this.aiEnabled && !info.offline && info.maxAiCredits > 0) {
      const credits = this.limit(info, UsageItemType.AI_CREDITS, info.usedAiCredits, info.maxAiCredits, info.planExtraAiCreditsEnabled,
        'home.license.this-month');
      groups.push({id: 'credits', label: 'subscription.ai-credits', icon: 'auto_awesome', rows: [credits]});
    }
    return groups;
  });

  readonly offer = computed<'pe-pack' | 'production' | null>(() => {
    const info = this.info();
    if (!info) {
      return null;
    }
    if (info.development) {
      return 'production';
    }
    return info.communityGrantLicense && !info.whiteLabelingEnabled ? 'pe-pack' : null;
  });

  readonly addons = computed<LicenseAddon[]>(() => {
    const info = this.info();
    if (!info || this.offer()) {
      return [];
    }
    const addons: LicenseAddon[] = [];
    if (!info.communityGrantLicense || info.whiteLabelingEnabled) {
      addons.push(this.addon(info, AddonType.WHITE_LABELING, info.whiteLabelingEnabled, false));
    }
    if (!info.perpetual || info.planEdgeEnabled) {
      addons.push(this.addon(info, AddonType.EDGE, info.edgeEnabled, info.planEdgeEnabled));
    }
    if (!info.perpetual || info.planTrendzEnabled) {
      addons.push(this.addon(info, AddonType.TRENDZ, info.trendzEnabled, info.planTrendzEnabled));
    }
    if (info.communityGrantLicense && info.whiteLabelingEnabled) {
      addons.push(this.addon(info, AddonType.PROFESSIONAL_UPGRADE, true, false));
    }
    return addons;
  });

  constructor() {
    const destroyRef = inject(DestroyRef);
    this.adminService.getSubscriptionInfo({ignoreLoading: true}).pipe(takeUntilDestroyed(destroyRef))
      .subscribe(info => this.info.set(info));
    interval(AGO_REFRESH_MS).pipe(takeUntilDestroyed(destroyRef)).subscribe(() => this.now.set(Date.now()));
    const host = inject<ElementRef<HTMLElement>>(ElementRef);
    const observer = new ResizeObserver(() => this.rememberHeight(host.nativeElement));
    observer.observe(host.nativeElement);
    destroyRef.onDestroy(() => observer.disconnect());
  }

  refresh(): void {
    this.refreshing.set(true);
    this.adminService.refreshLicense({ignoreLoading: true}).subscribe({
      next: info => {
        this.info.set(info);
        this.refreshing.set(false);
      },
      error: () => this.refreshing.set(false)
    });
  }

  manageLicense(): void {
    this.licenseActions.manageLicense(this.info());
  }

  limitAction(row: LicenseLimit): void {
    if (row.action === 'add') {
      this.licenseActions.addItems(this.info(), row.itemType);
    } else if (row.action === 'upgrade') {
      this.licenseActions.upgradePlanForItems(this.info(), row.itemType);
    }
  }

  addonAction(addon: LicenseAddon): void {
    if (addon.state === 'add') {
      this.licenseActions.addAddOn(this.info(), addon.type);
    } else if (addon.state === 'upgrade') {
      this.licenseActions.upgradePlanForAddon(this.info(), addon.type);
    }
  }

  addProfessionalPack(): void {
    this.licenseActions.addAddOn(this.info(), AddonType.PROFESSIONAL_UPGRADE);
  }

  seePlans(): void {
    window.open(PLANS_URL, '_blank');
  }

  private rememberHeight(element: HTMLElement): void {
    const height = Math.round(element.getBoundingClientRect().height);
    if (!this.info() || !height) {
      return;
    }
    try {
      localStorage.setItem(HEIGHT_KEY, String(height));
    } catch {
      return;
    }
  }

  private limit(info: SubscriptionInfo, itemType: UsageItemType, used: number, max: number, addEnabled: boolean,
                label = usageItemTypeTranslationMap.get(itemType)): LicenseLimit | null {
    if (max == null || max < 0) {
      return null;
    }
    const count = used ?? 0;
    let tone: LimitTone = null;
    if (max > 0 && count > max) {
      tone = 'critical';
    } else if (TONED_ITEMS.includes(itemType)) {
      tone = licenseLimitCritical(count, max) ? 'critical' : licenseLimitWarn(count, max) ? 'near' : null;
    }
    let action: LimitAction = null;
    if ((tone || max === 0) && !info.offline && itemType !== UsageItemType.ASSETS) {
      action = addEnabled ? 'add' : !info.perpetual ? 'upgrade' : null;
    }
    return {
      itemType,
      label,
      used: count,
      max,
      percent: max > 0 ? Math.round(count / max * 100) : 0,
      width: licenseMeterWidth(count, max),
      tone,
      action,
      addLabel: usageAddItemTypeTranslationMap.get(itemType)
    };
  }

  private addon(info: SubscriptionInfo, type: AddonType, enabled: boolean, addEnabled: boolean): LicenseAddon {
    let state: AddonState;
    if (enabled) {
      state = 'included';
    } else if (info.offline) {
      state = 'not-included';
    } else if (addEnabled) {
      state = 'add';
    } else if (!info.perpetual) {
      state = 'upgrade';
    } else {
      state = 'not-in-license';
    }
    return {type, label: addonTypeTranslationMap.get(type), state};
  }
}
