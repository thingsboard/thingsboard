// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { TranslateService } from '@ngx-translate/core';

export const HOME_LIST_PREVIEW = 5;
export const HOME_LIST_SLACK = 2;
export const HOME_LIST_LIMIT = 10;

const MINUTE = 60000;

export const homeListPageSize = (expanded: boolean): number => expanded ? HOME_LIST_LIMIT : HOME_LIST_PREVIEW + HOME_LIST_SLACK;

export const homeListFew = (total: number): boolean => total <= HOME_LIST_PREVIEW + HOME_LIST_SLACK;

export const homeListShown = (total: number, expanded: boolean): number => {
  if (homeListFew(total)) {
    return total;
  }
  return expanded ? Math.min(total, HOME_LIST_LIMIT) : HOME_LIST_PREVIEW;
};

export const homeAgo = (translate: TranslateService, ms: number): string => {
  const min = ms / MINUTE;
  if (min < 1) {
    return translate.instant('home.ago.now');
  }
  if (min < 60) {
    return translate.instant('home.ago.min', {count: Math.round(min)});
  }
  if (min < 24 * 60) {
    return translate.instant('home.ago.hour', {count: Math.floor(min / 60)});
  }
  if (min < 7 * 24 * 60) {
    return translate.instant('home.ago.day', {count: Math.floor(min / (24 * 60))});
  }
  return translate.instant('home.ago.week', {count: Math.floor(min / (7 * 24 * 60))});
};
