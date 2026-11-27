// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, input, NgZone, signal, WritableSignal } from '@angular/core';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { EntityCountCmd, TelemetrySubscriber } from '@shared/models/telemetry/telemetry.models';
import { AliasFilterType } from '@shared/models/alias.models';
import { EntityType } from '@shared/models/entity-type.models';
import { FeaturesInfo } from '@shared/models/settings.models';
import { HomeCardId } from '@shared/models/user-settings.models';
import { HOME_CARDS } from '@home/pages/home-page/home-cards.models';
import { SysAdminHomeService } from '@home/pages/home-page/sys-admin-home.service';

interface InstallCount {
  label: string;
  entityType: EntityType;
  link?: string;
  count: WritableSignal<number | null>;
}

interface InstallFeature {
  label: string;
  key: keyof FeaturesInfo;
  link: string;
}

const FEATURES: InstallFeature[] = [
  {label: 'home.install.white-labeling', key: 'whiteLabelingEnabled', link: '/white-labeling/whiteLabel'},
  {label: 'home.install.mail-server', key: 'emailEnabled', link: '/settings/outgoing-mail'},
  {label: 'home.install.sms-provider', key: 'smsEnabled', link: '/settings/notifications'},
  {label: 'home.install.slack', key: 'notificationEnabled', link: '/settings/notifications'},
  {label: 'home.install.oauth', key: 'oauthEnabled', link: '/security-settings/oauth2'},
  {label: 'home.install.two-fa', key: 'twoFaEnabled', link: '/security-settings/2fa'}
];

@Component({
  selector: 'tb-install-items-home',
  templateUrl: './install-items-home.component.html',
  styleUrls: ['./install-items-home.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class InstallItemsHomeComponent {

  withFeatures = input(true);

  readonly card = HOME_CARDS[HomeCardId.INSTALL_ITEMS];

  readonly counts: InstallCount[] = [
    {label: 'home.install.tenants', entityType: EntityType.TENANT, link: '/tenants', count: signal(null)},
    {label: 'home.install.tenant-profiles', entityType: EntityType.TENANT_PROFILE, link: '/tenantProfiles', count: signal(null)},
    {label: 'home.install.users', entityType: EntityType.USER, count: signal(null)},
    {label: 'home.install.customers', entityType: EntityType.CUSTOMER, count: signal(null)},
    {label: 'home.install.dashboards', entityType: EntityType.DASHBOARD, count: signal(null)},
    {label: 'home.install.integrations', entityType: EntityType.INTEGRATION, count: signal(null)}
  ];

  private readonly featuresInfo = signal<FeaturesInfo | null>(null);

  readonly features = computed(() => {
    const info = this.featuresInfo();
    return FEATURES.map(feature => ({...feature, on: info ? !!info[feature.key] : null}));
  });

  readonly loaded = computed(() => !!this.featuresInfo());

  readonly configured = computed(() => this.features().filter(feature => feature.on).length);

  constructor() {
    inject(SysAdminHomeService).featuresInfo$.subscribe(info => this.featuresInfo.set(info));
    const telemetryWsService = inject(TelemetryWebsocketService);
    const zone = inject(NgZone);
    const subscribers = this.counts.map(item => {
      const cmd = new EntityCountCmd();
      cmd.query = {entityFilter: {type: AliasFilterType.entityType, entityType: item.entityType}};
      const subscriber = new TelemetrySubscriber(telemetryWsService, zone);
      subscriber.subscriptionCommands.push(cmd);
      subscriber.entityCount$.subscribe(update => item.count.set(update.count ?? 0));
      subscriber.subscribe();
      return subscriber;
    });
    inject(DestroyRef).onDestroy(() => subscribers.forEach(subscriber => subscriber.unsubscribe()));
  }
}
