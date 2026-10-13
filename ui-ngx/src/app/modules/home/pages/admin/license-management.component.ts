// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, OnDestroy, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import {
  AddonType,
  addonTypeTranslationMap,
  createManageSubscriptionUrl,
  pePackOfferInfoMap,
  pePackOffers,
  PlanUiType,
  SubscriptionInfo,
  usageAddItemTypeTranslationMap,
  UsageItemType,
  usageItemTypeTranslationMap
} from '@shared/models/subscription.models';
import { ActivatedRoute } from '@angular/router';
import { AdminService } from '@core/http/admin.service';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { ActionUpdateLicenseParams } from '@core/auth/auth.actions';
import { getCurrentAuthState, selectUserSettingsProperty } from '@core/auth/auth.selectors';
import { DateAgoPipe } from '@shared/pipe/date-ago.pipe';
import { interval, Subscription } from 'rxjs';
import { ErrorStateMatcher } from '@angular/material/core';
import { FormControl, FormGroupDirective, NgForm } from '@angular/forms';
import { SystemSetupService } from '@core/http/system-setup.service';
import {
  LicenseHandOffCase,
  LicenseHandOffDialogComponent,
  LicenseHandOffDialogData
} from '@home/pages/admin/license-hand-off-dialog.component';
import { select } from '@ngrx/store';
import { take } from 'rxjs/operators';
import { resolveSendErrorMessage } from '@core/utils';

enum ManageLicenseState {
  MANAGE = 'MANAGE',
  ENTER_KEY = 'ENTER_KEY'
}

const AGO_TEXT_REFRESH_INTERVAL_MS = 30_000;

const KEY_MIN_LEN = 8;

@Component({
    selector: 'tb-license-management',
    templateUrl: './license-management.component.html',
    styleUrls: ['./license-management.component.scss'],
    standalone: false
})
export class LicenseManagementComponent extends PageComponent implements OnInit, OnDestroy {

  ManageLicenseState = ManageLicenseState;

  UsageItemType = UsageItemType;

  usageItemTypeTranslationMap = usageItemTypeTranslationMap;
  usageAddItemTypeTranslationMap = usageAddItemTypeTranslationMap;

  AddonType = AddonType;

  addonTypeTranslationMap = addonTypeTranslationMap;

  pePackOffers = pePackOffers;
  pePackOfferInfoMap = pePackOfferInfoMap;

  get planWithHue(): boolean {
    return [PlanUiType.TbNonCommercial, PlanUiType.TbNonCommercialOffline,
            PlanUiType.TbDevelopment, PlanUiType.TbCommunityGrant].includes(this.subscriptionInfo?.planUiType);
  }

  get planHueClass(): string {
    if (this.subscriptionInfo?.planUiType) {
      switch (this.subscriptionInfo?.planUiType) {
        case PlanUiType.TbNonCommercial:
        case PlanUiType.TbNonCommercialOffline:
          return 'nc';
        case PlanUiType.TbDevelopment:
          return 'dev';
        case PlanUiType.TbCommunityGrant:
          return 'grant';
      }
    }
    return '';
  }

  get isCeGrant(): boolean {
    return this.subscriptionInfo?.communityGrantLicense;
  }

  manageLicenseState = ManageLicenseState.MANAGE;

  subscriptionInfo: SubscriptionInfo;
  aiEnabled = getCurrentAuthState(this.store).aiEnabled;

  updatedAgo: string;

  keySendError = false;
  keySendNetworkError = false;
  keySendErrorMessage: string | null = null;

  keyInput = '';
  keyError = false;

  keyErrorStateMatcher: ErrorStateMatcher = {
    isErrorState: (control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean => {
      return !!(control && !this.subscriptionPreview && (this.keyError || this.keySendError) && (control.dirty || control.touched));
    }
  };

  licenseError = false;
  licenseNetworkError = false;
  licenseErrorMessage: string | null = null;

  submittingKey = false;

  subscriptionPreview: SubscriptionInfo;

  newLicenseApplied = false;

  private updatedAgoTimer?: Subscription;

  constructor(private route: ActivatedRoute,
              private adminService: AdminService,
              private systemSetupService: SystemSetupService,
              private translate: TranslateService,
              private dateAgoPipe: DateAgoPipe,
              private dialog: MatDialog) {
    super();
  }

  ngOnInit() {
    this.subscriptionInfo = this.route.snapshot.data.subscriptionInfo;
    this.refreshAgoTextIfNeeded();
  }

  ngOnDestroy(): void {
    this.stopAgoTextTimer();
    super.ngOnDestroy();
  }

  manageSubscription($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = createManageSubscriptionUrl(this.subscriptionInfo);
    this.openPortalLinkMaybeShowDialog(LicenseHandOffCase.MANAGE_LICENSE, null, null, url);
  }

  refreshLicenseInfo($event?: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.clearLicenseErrors();
    this.adminService.refreshLicense({ignoreErrors: true, ignoreLoading: true}).subscribe(
      {
        next: subscriptionInfo => {
          this.updateSubscriptionInfo(subscriptionInfo);
        },
        error: err => {
          void this.onLicenseError(err);
        }
      }
    );
  }

  gotoEnterKey() {
    this.keyInput = '';
    this.subscriptionPreview = null;
    this.newLicenseApplied = false;
    this.clearKeyErrors();
    this.manageLicenseState = ManageLicenseState.ENTER_KEY;
  }

  gotoManageLicense(refresh = true) {
    this.manageLicenseState = ManageLicenseState.MANAGE;
    if (refresh) {
      this.refreshLicenseInfo();
    }
  }

  itemLimitWarn(count: number, max: number): boolean {
    if (count && max) {
      return (count / max) >= 0.85;
    }
    return false;
  }

  itemLimitCritical(count: number, max: number): boolean {
    if (count && max) {
      return count >= max;
    }
    return false;
  }

  itemLimitClass(itemType: UsageItemType, count: number, max: number): string {
    if ([UsageItemType.DEVICES, UsageItemType.ASSETS, UsageItemType.AI_CREDITS].includes(itemType)) {
      if (this.itemLimitCritical(count, max)) {
        return 'item-critical';
      } else if (this.itemLimitWarn(count, max)) {
        return 'item-warn';
      }
    }
    return null;
  }

  addItems($event: Event, itemType: UsageItemType): void {
    if ($event) {
      $event.stopPropagation();
    }
    switch (itemType) {
      case UsageItemType.DEVICES:
        this.addDevices();
        break;
      case UsageItemType.AI_CREDITS:
        this.addAiCredits();
        break;
      case UsageItemType.EDGES:
        this.addEdges();
        break;
      case UsageItemType.PROD_INSTANCES:
        this.addInstances();
        break;
      case UsageItemType.AGENTS:
        this.addAgents();
        break;
    }
  }

  upgradePlanForItems($event: Event, itemType: UsageItemType): void {
    if ($event) {
      $event.stopPropagation();
    }
    let targetPlan: PlanUiType;
    if (itemType === UsageItemType.DEVICES) {
      if (this.subscriptionInfo.planUiType === PlanUiType.TbCommercialSmall) {
        targetPlan = PlanUiType.TbPilot;
      } else if (this.subscriptionInfo.planUiType === PlanUiType.TbNonCommercial) {
        targetPlan = PlanUiType.TbBusiness;
      } else if (this.subscriptionInfo.planUiType === PlanUiType.TbPilot) {
        targetPlan = PlanUiType.TbStartup;
      } else {
        targetPlan = PlanUiType.TbBusiness;
      }
    } else{
      targetPlan = PlanUiType.TbPilot;
    }
    this.upgradePlan(itemType, null, targetPlan);
  }

  addAddOn($event: Event, addonType: AddonType) {
    if ($event) {
      $event.stopPropagation();
    }
    if (addonType === AddonType.EDGE) {
      this.addEdge();
    } else if (addonType === AddonType.TRENDZ) {
      this.addTrendz();
    } else if (addonType === AddonType.PROFESSIONAL_UPGRADE) {
      this.addProfessionalUpgrade();
    }
  }

  upgradePlanForAddon($event: Event, addonType: AddonType): void {
    if ($event) {
      $event.stopPropagation();
    }
    const targetPlan = PlanUiType.TbPilot;
    this.upgradePlan(null, addonType, targetPlan);
  }

  async pasteKey() {
    try {
      this.keyInput = await navigator.clipboard.readText();
    } catch (_err) { /* empty */ }
    this.onKeyChange();
  }

  onKeyChange(): void {
    this.subscriptionPreview = null;
    this.clearKeyErrors();
  }

  checkKey(control: FormControl): void {
    this.subscriptionPreview = null;
    this.clearKeyErrors();
    const secret = (this.keyInput || '').trim();
    if (secret.length < KEY_MIN_LEN) {
      this.keyError = true;
      return;
    }
    this.submittingKey = true;
    this.systemSetupService.previewLicenseKey(secret).subscribe({
      next: (info) => {
        this.submittingKey = false;
        this.subscriptionPreview = info;
      },
      error: (err) => {
        control.markAsTouched();
        this.submittingKey = false;
        void this.onKeySendError(err);
      }
    });
  }

  changeKey(): void {
    this.clearKeyErrors();
    const secret = (this.keyInput || '').trim();
    this.submittingKey = true;
    this.systemSetupService.changeLicenseKey(secret).subscribe({
      next: (info) => {
        this.submittingKey = false;
        this.updateSubscriptionInfo(info.subscription);
        this.newLicenseApplied = true;
        this.gotoManageLicense(false);
      },
      error: (err) => {
        this.submittingKey = false;
        void this.onKeySendError(err);
      }
    });
  }

  enabledAddOnsText(subscription: SubscriptionInfo): string {
    const addons: string[] = [];
    if (subscription.whiteLabelingEnabled) {
      addons.push(this.translate.instant('subscription.white-labeling'));
    }
    if (subscription.edgeEnabled) {
      addons.push(this.translate.instant('subscription.edge'));
    }
    if (subscription.trendzEnabled) {
      addons.push(this.translate.instant('subscription.trendz'));
    }
    return addons.join(', ');
  }

  private updateSubscriptionInfo(subscriptionInfo: SubscriptionInfo): void {
    this.subscriptionInfo = subscriptionInfo;
    this.refreshAgoTextIfNeeded();
    this.store.dispatch(new ActionUpdateLicenseParams(
      {
        edgeEnabled: subscriptionInfo.edgeEnabled,
        trendzEnabled: subscriptionInfo.trendzEnabled,
        communityGrantLicense: subscriptionInfo.communityGrantLicense
      }
    ));
  }

  private addDevices() {
    this.addLicenseItem(UsageItemType.DEVICES, null, { extraDeviceCount: 100 });
  }

  private addEdges() {
    this.addLicenseItem(UsageItemType.EDGES, null, { extraEdgeCount: 1 });
  }

  private addInstances() {
    this.addLicenseItem(UsageItemType.PROD_INSTANCES, null, { extraInstanceCount: 1 });
  }

  private addAiCredits() {
    this.addLicenseItem(UsageItemType.AI_CREDITS, null, { extraAiCreditsCount: 5 });
  }

  private addAgents() {
    this.addLicenseItem(UsageItemType.AGENTS, null, { extraAgentCount: 1 });
  }

  private addEdge() {
    this.addLicenseItem(null, AddonType.EDGE, { edgeEnabled: true });
  }

  private addTrendz() {
    this.addLicenseItem(null, AddonType.TRENDZ, { trendzEnabled: true });
  }

  private addProfessionalUpgrade() {
    this.addLicenseItem(null, AddonType.PROFESSIONAL_UPGRADE, { professionalUpgradeEnabled: true });
  }

  private addLicenseItem(itemType: UsageItemType, addonType: AddonType, items: any) {
    const url = createManageSubscriptionUrl(this.subscriptionInfo, items);
    const handOffCase = !!itemType ? LicenseHandOffCase.ADD_ITEMS : LicenseHandOffCase.ADD_ADDON;
    this.openPortalLinkMaybeShowDialog(handOffCase, itemType, addonType, url);
  }

  private upgradePlan(itemType: UsageItemType, addonType: AddonType, targetPlan: PlanUiType): void {
    const url = `${this.subscriptionInfo.licenseServerEndpoint}/?changeSubscriptionPlan=true&subscriptionId=${this.subscriptionInfo.subscriptionId}&planUiType=${targetPlan}`;
    const handOffCase = !!itemType ? LicenseHandOffCase.UPGRADE_PLAN_FOR_ITEMS : LicenseHandOffCase.UPGRADE_PLAN_FOR_ADDON;
    this.openPortalLinkMaybeShowDialog(handOffCase, itemType, addonType, url);
  }

  private openPortalLinkMaybeShowDialog(handOffCase: LicenseHandOffCase,
                                        itemType: UsageItemType, addonType: AddonType, url: string): void {
    this.store.pipe(select(selectUserSettingsProperty('notDisplayLicenseHandOff'))).pipe(
      take(1)
    ).subscribe((settings: boolean) => {
      if (!settings) {
        this.dialog.open<LicenseHandOffDialogComponent, LicenseHandOffDialogData, boolean>(LicenseHandOffDialogComponent,
          {
            disableClose: true,
            autoFocus: false,
            panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
            data: {
              case: handOffCase,
              isPerpetual: this.subscriptionInfo.perpetual,
              isCeGrant: this.isCeGrant,
              itemType,
              addonType,
              planName: this.subscriptionInfo.subscriptionPlanName
            }
          }).afterClosed().subscribe(
          (result) => {
            if (result) {
              window.open(url, '_blank');
            }
          }
        );
      } else {
        window.open(url, '_blank');
      }
    });
  }

  private refreshAgoTextIfNeeded(): void {
    this.stopAgoTextTimer();
    if (!this.subscriptionInfo.offline && !this.subscriptionInfo.nonProduction) {
      this.refreshAgoText();
      this.updatedAgoTimer = interval(AGO_TEXT_REFRESH_INTERVAL_MS).subscribe(() => this.refreshAgoText());
    }
  }

  private refreshAgoText(): void {
    this.updatedAgo = this.dateAgoPipe.transform(this.subscriptionInfo.dataTs, {applyAgo: true, long: true, textPart: true});
  }

  private stopAgoTextTimer(): void {
    if (this.updatedAgoTimer) {
      this.updatedAgoTimer.unsubscribe();
      this.updatedAgoTimer = undefined;
    }
  }

  private clearKeyErrors(): void {
    this.keyError = false;
    this.keySendError = false;
    this.keySendNetworkError = false;
    this.keySendErrorMessage = null;
  }

  private async onKeySendError(err: any): Promise<void> {
    this.keySendError = true;
    this.keySendNetworkError = err?.status === 0;
    this.keySendErrorMessage = this.keySendNetworkError ? null : await resolveSendErrorMessage(err, this.translate);
  }

  private clearLicenseErrors(): void {
    this.licenseError = false;
    this.licenseNetworkError = false;
    this.licenseErrorMessage = null;
  }

  private async onLicenseError(err: any): Promise<void> {
    this.licenseError = true;
    this.licenseNetworkError = err?.status === 0;
    this.licenseErrorMessage = this.licenseNetworkError ? null : await resolveSendErrorMessage(err, this.translate);
  }

}
