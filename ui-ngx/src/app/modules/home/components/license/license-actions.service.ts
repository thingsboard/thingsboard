// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { inject, Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { select, Store } from '@ngrx/store';
import { take } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { selectUserSettingsProperty } from '@core/auth/auth.selectors';
import { AddonType, createManageSubscriptionUrl, PlanUiType, SubscriptionInfo, UsageItemType } from '@shared/models/subscription.models';
import {
  LicenseHandOffCase,
  LicenseHandOffDialogComponent,
  LicenseHandOffDialogData
} from '@home/components/license/license-hand-off-dialog.component';

const LIMIT_WARN = 0.85;

const ADD_ITEMS: Partial<Record<UsageItemType, Record<string, number>>> = {
  [UsageItemType.DEVICES]: {extraDeviceCount: 100},
  [UsageItemType.EDGES]: {extraEdgeCount: 1},
  [UsageItemType.PROD_INSTANCES]: {extraInstanceCount: 1},
  [UsageItemType.AI_CREDITS]: {extraAiCreditsCount: 5},
  [UsageItemType.AGENTS]: {extraAgentCount: 1}
};

const ADD_ADDONS: Partial<Record<AddonType, Record<string, boolean>>> = {
  [AddonType.EDGE]: {edgeEnabled: true},
  [AddonType.TRENDZ]: {trendzEnabled: true},
  [AddonType.PROFESSIONAL_UPGRADE]: {professionalUpgradeEnabled: true}
};

export const licenseLimitWarn = (count: number, max: number): boolean => !!count && !!max && count / max >= LIMIT_WARN;

export const licenseLimitCritical = (count: number, max: number): boolean => !!count && !!max && count >= max;

export const licenseMeterWidth = (count: number, max: number): number => count > 0 && max > 0 ? Math.min(count / max, 1) * 100 : 0;

@Injectable({
  providedIn: 'root'
})
export class LicenseActionsService {

  private store = inject(Store<AppState>);
  private dialog = inject(MatDialog);

  manageLicense(info: SubscriptionInfo): void {
    this.openPortal(info, LicenseHandOffCase.MANAGE_LICENSE, null, null, createManageSubscriptionUrl(info));
  }

  addItems(info: SubscriptionInfo, itemType: UsageItemType): void {
    const items = ADD_ITEMS[itemType];
    if (items) {
      this.openPortal(info, LicenseHandOffCase.ADD_ITEMS, itemType, null, createManageSubscriptionUrl(info, items));
    }
  }

  addAddOn(info: SubscriptionInfo, addonType: AddonType): void {
    const items = ADD_ADDONS[addonType];
    if (items) {
      this.openPortal(info, LicenseHandOffCase.ADD_ADDON, null, addonType, createManageSubscriptionUrl(info, items));
    }
  }

  upgradePlanForItems(info: SubscriptionInfo, itemType: UsageItemType): void {
    let targetPlan = PlanUiType.TbPilot;
    if (itemType === UsageItemType.DEVICES) {
      switch (info.planUiType) {
        case PlanUiType.TbCommercialSmall:
          targetPlan = PlanUiType.TbPilot;
          break;
        case PlanUiType.TbPilot:
          targetPlan = PlanUiType.TbStartup;
          break;
        default:
          targetPlan = PlanUiType.TbBusiness;
      }
    }
    this.upgradePlan(info, itemType, null, targetPlan);
  }

  upgradePlanForAddon(info: SubscriptionInfo, addonType: AddonType): void {
    this.upgradePlan(info, null, addonType, PlanUiType.TbPilot);
  }

  private upgradePlan(info: SubscriptionInfo, itemType: UsageItemType, addonType: AddonType, targetPlan: PlanUiType): void {
    const url = `${info.licenseServerEndpoint}/?changeSubscriptionPlan=true&subscriptionId=${info.subscriptionId}&planUiType=${targetPlan}`;
    this.openPortal(info, itemType ? LicenseHandOffCase.UPGRADE_PLAN_FOR_ITEMS : LicenseHandOffCase.UPGRADE_PLAN_FOR_ADDON,
      itemType, addonType, url);
  }

  private openPortal(info: SubscriptionInfo, handOffCase: LicenseHandOffCase, itemType: UsageItemType, addonType: AddonType,
                     url: string): void {
    this.store.pipe(select(selectUserSettingsProperty('notDisplayLicenseHandOff')), take(1)).subscribe((skipDialog: boolean) => {
      if (skipDialog) {
        window.open(url, '_blank');
        return;
      }
      this.dialog.open<LicenseHandOffDialogComponent, LicenseHandOffDialogData, boolean>(LicenseHandOffDialogComponent, {
        disableClose: true,
        autoFocus: false,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          case: handOffCase,
          isPerpetual: info.perpetual,
          isCeGrant: info.communityGrantLicense,
          itemType,
          addonType,
          planName: info.subscriptionPlanName
        }
      }).afterClosed().subscribe(result => {
        if (result) {
          window.open(url, '_blank');
        }
      });
    });
  }
}
