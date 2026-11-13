// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import {
  AddonType,
  addonTypeTranslationMap,
  usageAddItemTypeTranslationMap,
  UsageItemType, usageItemUpgradePlanTranslationMap
} from '@shared/models/subscription.models';
import { TranslateService } from '@ngx-translate/core';
import { ActionPreferencesPutUserSettings } from '@core/auth/auth.actions';

export enum LicenseHandOffCase {
  MANAGE_LICENSE = 'MANAGE_LICENSE',
  ADD_ITEMS = 'ADD_ITEMS',
  ADD_ADDON = 'ADD_ADDON',
  UPGRADE_PLAN_FOR_ITEMS = 'UPGRADE_PLAN_FOR_ITEMS',
  UPGRADE_PLAN_FOR_ADDON = 'UPGRADE_PLAN_FOR_ADDON'
}

export interface LicenseHandOffDialogData {
  case: LicenseHandOffCase;
  isPerpetual: boolean;
  isCeGrant: boolean;
  itemType?: UsageItemType;
  addonType?: AddonType;
  planName?: string;
}

@Component({
  selector: 'tb-license-handoff-dialog',
  templateUrl: './license-hand-off-dialog.component.html',
  styleUrls: ['./license-hand-off-dialog.component.scss'],
  standalone: false
})

export class LicenseHandOffDialogComponent extends DialogComponent<LicenseHandOffDialogComponent, boolean> {

  notShowAgain = false;

  dialogTitle: string;
  caseText: string;
  caseText2: string;
  openPortalText: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private translate: TranslateService,
              @Inject(MAT_DIALOG_DATA) public data: LicenseHandOffDialogData,
              public dialogRef: MatDialogRef<LicenseHandOffDialogComponent, boolean>) {
    super(store, router, dialogRef);
    switch (data.case) {
      case LicenseHandOffCase.MANAGE_LICENSE:
        this.dialogTitle = this.translate.instant('subscription.manage-license');
        this.openPortalText = this.translate.instant('subscription.open-portal-manage-license');
        break;
      case LicenseHandOffCase.ADD_ITEMS:
        this.dialogTitle = this.translate.instant(usageAddItemTypeTranslationMap.get(data.itemType));
        this.caseText = null;
        this.openPortalText = this.translate.instant('subscription.open-portal-manage-license');
        break;
      case LicenseHandOffCase.ADD_ADDON:
        const addonName = this.translate.instant(addonTypeTranslationMap.get(data.addonType));
        this.dialogTitle = this.translate.instant('subscription.add-addon', {type: data.addonType, name: addonName});
        this.caseText = this.translate.instant(data.isPerpetual ? 'subscription.add-addon-perpetual' : 'subscription.add-addon-subscription', {name: addonName});
        this.openPortalText = this.translate.instant('subscription.open-portal-manage-license');
        break;
      case LicenseHandOffCase.UPGRADE_PLAN_FOR_ITEMS:
        this.dialogTitle = this.translate.instant('subscription.upgrade-plan');
        this.caseText = this.translate.instant(usageItemUpgradePlanTranslationMap.get(data.itemType), {plan: data.planName});
        this.openPortalText = this.translate.instant('subscription.open-portal-upgrade-plan');
        break;
      case LicenseHandOffCase.UPGRADE_PLAN_FOR_ADDON:
        this.dialogTitle = this.translate.instant('subscription.upgrade-plan');
        if (data.addonType === AddonType.WHITE_LABELING) {
          this.caseText = this.translate.instant('subscription.upgrade-plan-white-labeling');
        } else {
          const addonName = this.translate.instant(addonTypeTranslationMap.get(data.addonType));
          this.caseText = this.translate.instant('subscription.upgrade-plan-addon', {name: addonName});
        }
        this.openPortalText = this.translate.instant('subscription.open-portal-upgrade-plan');
        break;
    }
    if (data.isCeGrant && data.case !== LicenseHandOffCase.MANAGE_LICENSE) {
      if (data.case === LicenseHandOffCase.ADD_ADDON && data.addonType === AddonType.PROFESSIONAL_UPGRADE) {
        this.caseText = this.translate.instant('subscription.pe-pack-case');
        this.caseText2 =  this.translate.instant('subscription.ce-grant-white-label');
      } else {
        this.caseText = this.translate.instant('subscription.ce-grant-case');
      }
    }
  }

  cancel(): void {
    if (this.notShowAgain) {
      this.store.dispatch(new ActionPreferencesPutUserSettings({ notDisplayLicenseHandOff: true }));
    }
    this.dialogRef.close(false);
  }

  openPortal(): void {
    if (this.notShowAgain) {
      this.store.dispatch(new ActionPreferencesPutUserSettings({ notDisplayLicenseHandOff: true }));
    }
    this.dialogRef.close(true);
  }

}
