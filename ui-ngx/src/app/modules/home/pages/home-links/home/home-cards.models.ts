// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { HomeCardId, HomeCardsSettings } from '@shared/models/user-settings.models';

export interface HomeCardDefinition {
  id: HomeCardId;
  icon: string;
  name: string;
  link?: string;
  height: number;
}

export const HOME_CARDS: Record<HomeCardId, HomeCardDefinition> = {
  [HomeCardId.IOT_HUB]: {id: HomeCardId.IOT_HUB, icon: 'hub', name: 'home.cards.iot-hub', link: '/iot-hub', height: 500},
  [HomeCardId.ALARMS]: {id: HomeCardId.ALARMS, icon: 'warning', name: 'home.cards.alarms', link: '/alarms', height: 430},
  [HomeCardId.DASHBOARDS]: {id: HomeCardId.DASHBOARDS, icon: 'dashboard', name: 'home.cards.dashboards', link: '/dashboards', height: 357},
  [HomeCardId.DEVICES]: {id: HomeCardId.DEVICES, icon: 'memory', name: 'home.cards.devices', link: '/entities/devices', height: 430},
  [HomeCardId.ACTIVITY]: {id: HomeCardId.ACTIVITY, icon: 'show_chart', name: 'home.cards.activity', height: 250},
  [HomeCardId.GET_STARTED]: {id: HomeCardId.GET_STARTED, icon: 'rocket_launch', name: 'home.cards.get-started', height: 405},
  [HomeCardId.NOTIFICATIONS]: {id: HomeCardId.NOTIFICATIONS, icon: 'notifications', name: 'home.cards.notifications',
    link: '/notification/inbox', height: 525},
  [HomeCardId.QUICK_LINKS]: {id: HomeCardId.QUICK_LINKS, icon: 'link', name: 'home.cards.quick-links', height: 205},
  [HomeCardId.USAGE]: {id: HomeCardId.USAGE, icon: 'data_usage', name: 'home.cards.usage', link: '/usage', height: 232},
  [HomeCardId.MOBILE_APP]: {id: HomeCardId.MOBILE_APP, icon: 'phone_iphone', name: 'home.cards.mobile-app', height: 225}
};

export const isHomeCardShown = (settings: HomeCardsSettings, id: HomeCardId): boolean => settings?.[id] !== false;
