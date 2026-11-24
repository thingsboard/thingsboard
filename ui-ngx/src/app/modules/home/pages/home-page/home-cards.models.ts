// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { HomeCardId, HomeCardsSettings } from '@shared/models/user-settings.models';

export interface HomeCardDefinition {
  id: HomeCardId;
  icon: string;
  name: string;
  link?: string;
  height?: number;
}

export const HOME_CARDS: Record<HomeCardId, HomeCardDefinition> = {
  [HomeCardId.IOT_HUB]: {id: HomeCardId.IOT_HUB, icon: 'hub', name: 'home.cards.iot-hub', link: '/iot-hub'},
  [HomeCardId.ALARMS]: {id: HomeCardId.ALARMS, icon: 'warning', name: 'home.cards.alarms', link: '/alarms'},
  [HomeCardId.DASHBOARDS]: {id: HomeCardId.DASHBOARDS, icon: 'dashboard', name: 'home.cards.dashboards', link: '/dashboards'},
  [HomeCardId.DEVICES]: {id: HomeCardId.DEVICES, icon: 'memory', name: 'home.cards.devices', link: '/entities/devices'},
  [HomeCardId.ACTIVITY]: {id: HomeCardId.ACTIVITY, icon: 'show_chart', name: 'home.cards.activity'},
  [HomeCardId.GET_STARTED]: {id: HomeCardId.GET_STARTED, icon: 'rocket_launch', name: 'home.cards.get-started'},
  [HomeCardId.NOTIFICATIONS]: {id: HomeCardId.NOTIFICATIONS, icon: 'notifications', name: 'home.cards.notifications',
    link: '/notification/inbox'},
  [HomeCardId.QUICK_LINKS]: {id: HomeCardId.QUICK_LINKS, icon: 'link', name: 'home.cards.quick-links'},
  [HomeCardId.USAGE]: {id: HomeCardId.USAGE, icon: 'data_usage', name: 'home.cards.usage', link: '/usage'},
  [HomeCardId.MOBILE_APP]: {id: HomeCardId.MOBILE_APP, icon: 'phone_iphone', name: 'home.cards.mobile-app'},
  [HomeCardId.PRODUCT_RELEASE]: {id: HomeCardId.PRODUCT_RELEASE, icon: 'new_releases', name: 'home.cards.product-release', height: 100},
  [HomeCardId.GET_STARTED_SYS_ADMIN]: {id: HomeCardId.GET_STARTED_SYS_ADMIN, icon: 'rocket_launch', name: 'home.cards.get-started'},
  [HomeCardId.SYSTEM_STATUS]: {id: HomeCardId.SYSTEM_STATUS, icon: 'dns', name: 'home.cards.system-status'},
  [HomeCardId.LICENSE]: {id: HomeCardId.LICENSE, icon: 'verified', name: 'home.cards.license', link: '/license', height: 398},
  [HomeCardId.INSTALL_ITEMS]: {id: HomeCardId.INSTALL_ITEMS, icon: 'domain', name: 'home.cards.install-items'}
};

export const isHomeCardShown = (settings: HomeCardsSettings, id: HomeCardId): boolean => settings?.[id] !== false;
