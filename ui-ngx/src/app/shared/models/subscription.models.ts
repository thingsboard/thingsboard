// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { EntityType } from '@shared/models/entity-type.models';

export enum SubscriptionErrorCode {
  LIMIT_REACHED = 'LIMIT_REACHED',
  FEATURE_DISABLED = 'FEATURE_DISABLED'
}

export enum SubscriptionEntry {
  DEVICE_COUNT = 'DEVICE_COUNT',
  ASSET_COUNT = 'ASSET_COUNT',
  EDGE_COUNT = 'EDGE_COUNT',
  WHITE_LABELING = 'WHITE_LABELING',
  AGENT_COUNT = 'AGENT_COUNT'
}

export interface SubscriptionErrorData {
  subscriptionErrorCode: SubscriptionErrorCode;
  subscriptionEntry: SubscriptionEntry;
  subscriptionValue: any;
  message?: string;
}

export const subscriptionEntryToEntityType = new Map<SubscriptionEntry, EntityType>(
  [
    [SubscriptionEntry.DEVICE_COUNT, EntityType.DEVICE],
    [SubscriptionEntry.ASSET_COUNT, EntityType.ASSET],
    [SubscriptionEntry.EDGE_COUNT, EntityType.EDGE],
    [SubscriptionEntry.AGENT_COUNT, EntityType.AGENT]
  ]
);

export const subscriptionErrorsMap = new Map<SubscriptionErrorCode, Map<SubscriptionEntry, string>>(
  [
    [SubscriptionErrorCode.LIMIT_REACHED, new Map<SubscriptionEntry, string>(
      [
        [SubscriptionEntry.DEVICE_COUNT, 'subscription-error.limit-reached.device-count'],
        [SubscriptionEntry.ASSET_COUNT, 'subscription-error.limit-reached.asset-count'],
        [SubscriptionEntry.EDGE_COUNT, 'subscription-error.limit-reached.edge-count'],
        [SubscriptionEntry.AGENT_COUNT, 'subscription-error.limit-reached.agent-count']
      ]
    )],
    [SubscriptionErrorCode.FEATURE_DISABLED, new Map<SubscriptionEntry, string>(
      [
        [SubscriptionEntry.WHITE_LABELING, 'subscription-error.feature-disabled.white-labeling']
      ]
    )]
  ]
);

export enum PlanUiType {
  TbMaker = 'TbMaker',
  TbPrototype = 'TbPrototype',
  TbPilot = 'TbPilot',
  TbStartup = 'TbStartup',
  TbBusiness = 'TbBusiness',
  TbPerpetual = 'TbPerpetual',
  TbNonCommercial = 'TbNonCommercial',
  TbCommercialSmall = 'TbCommercialSmall',
  TbNonCommercialOffline = 'TbNonCommercialOffline',
  TbCommercialSmallOffline = 'TbCommercialSmallOffline',
  TbDevelopment = 'TbDevelopment',
  TbCommunityGrant = 'TbCommunityGrant'
}

export interface SubscriptionInfo {
  subscriptionId: string;
  subscriptionPlanName: string;
  planUiType: PlanUiType;
  perpetual: boolean;
  offline: boolean;
  // A perpetual subscription has no billing period and no upcoming invoice, so these four arrive as null.
  // endTs stays required: for a perpetual license it carries the software-update horizon.
  currentPeriodStartTs?: number;
  currentPeriodEndTs?: number;
  endTs: number;
  upcomingInvoiceDate?: number;
  upcomingInvoiceAmountDue?: number;
  planExtraDeviceEnabled: boolean;
  planEdgeEnabled: boolean;
  planExtraEdgeEnabled: boolean;
  planTrendzEnabled: boolean;
  planExtraAiCreditsEnabled: boolean;
  planExtraInstanceEnabled: boolean;
  planExtraAgentEnabled: boolean;

  dataTs: number;
  licenseServerEndpoint: string;

  maxDevices: number;
  maxAssets: number;
  maxEdges: number;
  maxAgents: number;
  maxInstances: number;
  maxAiCredits: number;
  whiteLabelingEnabled: boolean;
  edgeEnabled: boolean;
  trendzEnabled: boolean;
  development: boolean;
  // Keyless, no subscription at all - narrower than `development`, which is also true for a licensed
  // Development plan. Nothing to manage, nothing to refresh, no license data timestamp worth showing.
  nonProduction: boolean;

  devicesCount: number;
  assetsCount: number;
  edgesCount: number;
  agentsCount: number;
  instancesCount: number;
  usedAiCredits: number;

  communityGrantLicense: boolean;
}

export enum AddonType {
  EDGE = 'EDGE',
  TRENDZ = 'TRENDZ',
  WHITE_LABELING = 'WHITE_LABELING',
  PROFESSIONAL_UPGRADE = 'PROFESSIONAL_UPGRADE'
}

export const addonTypeTranslationMap = new Map<AddonType, string>(
  [
    [AddonType.EDGE, 'subscription.edge'],
    [AddonType.TRENDZ, 'subscription.trendz'],
    [AddonType.WHITE_LABELING, 'subscription.white-labeling'],
    [AddonType.PROFESSIONAL_UPGRADE, 'subscription.pe-pack'],
  ]
);

export enum UsageItemType {
  DEVICES = 'DEVICES',
  ASSETS = 'ASSETS',
  PROD_INSTANCES = 'PROD_INSTANCES',
  AI_CREDITS = 'AI_CREDITS',
  EDGES = 'EDGES',
  AGENTS = 'AGENTS'
}

export const usageItemTypeTranslationMap = new Map<UsageItemType, string>(
  [
    [UsageItemType.DEVICES, 'subscription.devices'],
    [UsageItemType.ASSETS, 'subscription.assets'],
    [UsageItemType.PROD_INSTANCES, 'subscription.prod-instances'],
    [UsageItemType.AI_CREDITS, 'subscription.ai-credits'],
    [UsageItemType.EDGES, 'subscription.edges'],
    [UsageItemType.AGENTS, 'subscription.agents']
  ]
);

export const usageAddItemTypeTranslationMap = new Map<UsageItemType, string>(
  [
    [UsageItemType.DEVICES, 'subscription.add-devices'],
    [UsageItemType.ASSETS, 'subscription.add-assets'],
    [UsageItemType.PROD_INSTANCES, 'subscription.add-instances'],
    [UsageItemType.AI_CREDITS, 'subscription.add-credits'],
    [UsageItemType.EDGES, 'subscription.add-edges'],
    [UsageItemType.AGENTS, 'subscription.add-agents']
  ]
);

export const usageItemUpgradePlanTranslationMap = new Map<UsageItemType, string>(
  [
    [UsageItemType.DEVICES, 'subscription.upgrade-plan-devices'],
    [UsageItemType.PROD_INSTANCES, 'subscription.upgrade-plan-instances'],
    [UsageItemType.AI_CREDITS, 'subscription.upgrade-plan-credits'],
    [UsageItemType.EDGES, 'subscription.upgrade-plan-edges'],
    [UsageItemType.AGENTS, 'subscription.upgrade-plan-agents']
  ]
);

export const createManageSubscriptionUrl = (subscriptionInfo: SubscriptionInfo, items?: any) => {
  let url = `${subscriptionInfo.licenseServerEndpoint}/?manageSubscription=true&subscriptionId=${subscriptionInfo.subscriptionId}`;
  if (subscriptionInfo.perpetual) {
    url += '&perpetual=true';
  }
  if (items) {
    url += `&manageAddons=true&items=${encodeURIComponent(JSON.stringify(items))}`;
  }
  return url;
}

export enum PlatformFeature {
  INTEGRATIONS = 'INTEGRATIONS',
  SCHEDULER = 'SCHEDULER',
  REPORTING = 'REPORTING'
}

/**
 * The four features surfaced by the Professional Pack. Kept as its own enum (rather than being conflated
 * with PlatformFeature) so callers that need White-labeling on the same footing as Integrations / Scheduler
 * / Reports have a single ordered catalogue to iterate. The order below is the render order in both the
 * license-management pack grid and the pe-pack-offer-cards component.
 */
export enum PePackOffer {
  WHITE_LABELING = 'WHITE_LABELING',
  INTEGRATIONS = 'INTEGRATIONS',
  SCHEDULER = 'SCHEDULER',
  REPORTS = 'REPORTS'
}

export interface PePackOfferInfo {
  /** Translation key for the card title. */
  title: string;
  /** Default promo copy — read on read-only surfaces (e.g. the license-management pack grid). */
  promo: string;
  /**
   * Interactive-surface promo copy — used by pe-pack-offer-cards when the card is clickable. Only defined
   * for offers where the two variants read differently (currently just White-labeling, which invites the
   * user to open its page).
   */
  promoTry?: string;
  /** In-app page opened when the card is clicked (relative router URL, no leading slash). */
  page: string;
  /** Inline SVG markup for the card's leading icon. Rendered via [innerHTML] + | safe:'html'. */
  svg: string;
}

/** Ordered catalogue — iterate this to render offers in the same order every consumer expects. */
export const pePackOffers: PePackOffer[] = [
  PePackOffer.WHITE_LABELING,
  PePackOffer.INTEGRATIONS,
  PePackOffer.SCHEDULER,
  PePackOffer.REPORTS,
];

export const pePackOfferInfoMap = new Map<PePackOffer, PePackOfferInfo>([
  [PePackOffer.WHITE_LABELING, {
    title: 'subscription.white-labeling',
    promo: 'subscription.white-labeling-promo',
    promoTry: 'subscription.white-labeling-promo-try',
    page: 'white-labeling/whiteLabel',
    svg: '<svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor" aria-hidden="true"><path d="M18.35 3.05a2.35 2.35 0 0 1 3.3 3.3l-7.5 7.5-3.3-3.3z"></path><path d="M9.4 13.1c-2.25 0-4.05 1.85-4.05 4.05 0 1.35-1.15 2.05-2.15 2.5 1.25 1.45 3.05 2.15 4.9 2.15a4.8 4.8 0 0 0 4.8-4.8c0-2.25-1.55-3.9-3.5-3.9z"></path></svg>'
  }],
  [PePackOffer.INTEGRATIONS, {
    title: 'subscription.integrations',
    promo: 'subscription.integrations-promo',
    page: 'integrationsCenter/integrations',
    svg: '<svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor" aria-hidden="true"><path fill-rule="evenodd" d="M6.6 3h10.8A3.6 3.6 0 0 1 21 6.6v10.8a3.6 3.6 0 0 1-3.6 3.6H6.6A3.6 3.6 0 0 1 3 17.4V6.6A3.6 3.6 0 0 1 6.6 3zm3.4 5.6L6.6 12l3.4 3.4 1.3-1.3L9.2 12l2.1-2.1zm4 0-1.3 1.3L14.8 12l-2.1 2.1 1.3 1.3L17.4 12z"></path></svg>'
  }],
  [PePackOffer.SCHEDULER, {
    title: 'subscription.scheduler',
    promo: 'subscription.scheduler-promo',
    page: 'features/scheduler',
    svg: '<svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor" aria-hidden="true"><path fill-rule="evenodd" d="M12 2.3a9.7 9.7 0 1 0 0 19.4 9.7 9.7 0 0 0 0-19.4zm-1 4.4h2v5.7l4 2.4-1 1.7-5-3z"></path></svg>'
  }],
  [PePackOffer.REPORTS, {
    title: 'subscription.reports',
    promo: 'subscription.reports-promo',
    page: 'reporting/templates',
    svg: '<svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor" aria-hidden="true"><path fill-rule="evenodd" d="M6.6 3h10.8A3.6 3.6 0 0 1 21 6.6v10.8a3.6 3.6 0 0 1-3.6 3.6H6.6A3.6 3.6 0 0 1 3 17.4V6.6A3.6 3.6 0 0 1 6.6 3zm1 13.5h2v-4.3h-2zm3.4 0h2V9.6h-2zm3.4 0h2v-2.9h-2z"></path></svg>'
  }],
]);

export interface PlatformFeatureOfferInfo {
  notInCeGrant: string;
  packOfferTitle: string;
  packOfferLede: string;
  overviewItems: { svg: string; title: string; description: string; }[];
}

export const platformFeatureOffers = new Map<PlatformFeature, PlatformFeatureOfferInfo>([
  [PlatformFeature.INTEGRATIONS, {
    notInCeGrant: 'platform-feature.integrations.not-in-ce-grant',
    packOfferTitle: 'platform-feature.integrations.pack-offer-title',
    packOfferLede: 'platform-feature.integrations.pack-offer-lede',
    overviewItems: [
      {
        svg: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="3.2" y="3.2" width="17.6" height="17.6" rx="4.2"></rect><path d="M10.3 9.1 7.7 12l2.6 2.9M13.7 9.1 16.3 12l-2.6 2.9"></path></svg>',
        title: 'platform-feature.integrations.overview.brokers-and-platforms',
        description: 'platform-feature.integrations.overview.brokers-and-platforms-desc'
      },
      {
        svg: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M3.6 8.9h13.9M14.2 5.6l3.3 3.3-3.3 3.3M20.4 15.1H6.5M9.8 11.8l-3.3 3.3 3.3 3.3"></path></svg>',
        title: 'platform-feature.integrations.overview.data-converters',
        description: 'platform-feature.integrations.overview.data-converters-desc'
      },
      {
        svg: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 3.6v10.8M7.9 10.5 12 14.6l4.1-4.1M4.6 20.4h14.8"></path></svg>',
        title: 'platform-feature.integrations.overview.downlink',
        description: 'platform-feature.integrations.overview.downlink-desc'
      },
      {
        svg: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="2.8" y="12.4" width="8.4" height="7.4" rx="1.8"></rect><rect x="12.8" y="4.2" width="8.4" height="7.4" rx="1.8"></rect><path d="M11.4 14.4l2.6-2.6"></path></svg>',
        title: 'platform-feature.integrations.overview.remote-integrations',
        description: 'platform-feature.integrations.overview.remote-integrations-desc'
      }
    ]
  }],
  [PlatformFeature.SCHEDULER, {
    notInCeGrant: 'platform-feature.scheduler.not-in-ce-grant',
    packOfferTitle: 'platform-feature.scheduler.pack-offer-title',
    packOfferLede: 'platform-feature.scheduler.pack-offer-lede',
    overviewItems: [
      {
        svg: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="8.7"></circle><path d="M12 6.9V12l3.4 2"></path></svg>',
        title: 'platform-feature.scheduler.overview.repeats-and-timezones',
        description: 'platform-feature.scheduler.overview.repeats-and-timezones-desc'
      },
      {
        svg: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="5.3" y="10.2" width="13.4" height="10.2" rx="2.6"></rect><path d="M12 3.3v5.3M9.4 6 12 8.6 14.6 6"></path></svg>',
        title: 'platform-feature.scheduler.overview.device-commands',
        description: 'platform-feature.scheduler.overview.device-commands-desc'
      },
      {
        svg: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="6.6" y="6.6" width="10.8" height="10.8" rx="2.2"></rect><path d="M12 14.6V9.8M9.9 11.9 12 9.8l2.1 2.1"></path><path d="M9.7 3.4v3.2M14.3 3.4v3.2M9.7 17.4v3.2M14.3 17.4v3.2M3.4 9.7h3.2M3.4 14.3h3.2M17.4 9.7h3.2M17.4 14.3h3.2"></path></svg>',
        title: 'platform-feature.scheduler.overview.firmware-and-software',
        description: 'platform-feature.scheduler.overview.firmware-and-software-desc'
      },
      {
        svg: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="5.8" cy="12" r="2.7"></circle><circle cx="18.2" cy="6.2" r="2.7"></circle><circle cx="18.2" cy="17.8" r="2.7"></circle><path d="M8.2 10.8 15.8 7.3M8.2 13.2l7.6 3.5"></path></svg>',
        title: 'platform-feature.scheduler.overview.rule-chain-triggers',
        description: 'platform-feature.scheduler.overview.rule-chain-triggers-desc'
      }
    ]
  }],
  [PlatformFeature.REPORTING, {
    notInCeGrant: 'platform-feature.reporting.not-in-ce-grant',
    packOfferTitle: 'platform-feature.reporting.pack-offer-title',
    packOfferLede: 'platform-feature.reporting.pack-offer-lede',
    overviewItems: [
      {
        svg: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="7.2" y="3.3" width="13.5" height="13.5" rx="2.8"></rect><path d="M11 13.3V9.7M14 13.3V7.3M17 13.3v-2.5"></path><path d="M16.8 20.7H6.1a2.8 2.8 0 0 1-2.8-2.8V7.2"></path></svg>',
        title: 'platform-feature.reporting.overview.your-own-layout',
        description: 'platform-feature.reporting.overview.your-own-layout-desc'
      },
      {
        svg: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M13.6 3.3H7.5A2.6 2.6 0 0 0 4.9 5.9v12.2a2.6 2.6 0 0 0 2.6 2.6h9a2.6 2.6 0 0 0 2.6-2.6V8.5z"></path><path d="M13.6 3.3v5.2h5.5"></path><path d="M8.4 13.2h7.2M8.4 16.6h4.6"></path></svg>',
        title: 'platform-feature.reporting.overview.pdf-or-csv',
        description: 'platform-feature.reporting.overview.pdf-or-csv-desc'
      },
      {
        svg: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="2.8" y="5.3" width="18.4" height="13.4" rx="2.6"></rect><path d="M3.8 7.2 12 13.1l8.2-5.9"></path></svg>',
        title: 'platform-feature.reporting.overview.channel',
        description: 'platform-feature.reporting.overview.channel-desc'
      },
      {
        svg: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M17.1 15.5V9.9a5.1 5.1 0 0 0-10.2 0v5.6L5.2 17.7h13.6z"></path><path d="M10 20.4a2.2 2.2 0 0 0 4 0"></path><path d="M12 4.8V3.2"></path></svg>',
        title: 'platform-feature.reporting.overview.attached-to-alarm',
        description: 'platform-feature.reporting.overview.attached-to-alarm-desc'
      }
    ]
  }]
]);
