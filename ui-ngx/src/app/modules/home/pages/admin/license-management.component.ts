// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, OnDestroy, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import {
  AddonType,
  addonTypeTranslationMap,
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
import { TranslateService } from '@ngx-translate/core';
import { ActionUpdateLicenseParams } from '@core/auth/auth.actions';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { DateAgoPipe } from '@shared/pipe/date-ago.pipe';
import { interval, Subscription } from 'rxjs';
import { ErrorStateMatcher } from '@angular/material/core';
import { FormControl, FormGroupDirective, NgForm } from '@angular/forms';
import { SystemSetupService } from '@core/http/system-setup.service';
import {
  LicenseActionsService,
  licenseLimitCritical,
  licenseLimitWarn,
  licenseMeterWidth
} from '@home/components/license/license-actions.service';
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
              private licenseActions: LicenseActionsService) {
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
    this.licenseActions.manageLicense(this.subscriptionInfo);
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
    return licenseLimitWarn(count, max);
  }

  itemLimitCritical(count: number, max: number): boolean {
    return licenseLimitCritical(count, max);
  }

  itemMeterWidth(count: number, max: number): number {
    return licenseMeterWidth(count, max);
  }

  itemLimitClass(itemType: UsageItemType, count: number, max: number): string {
    if (max > 0 && count > max) {
      return 'item-critical';
    }
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
    this.licenseActions.addItems(this.subscriptionInfo, itemType);
  }

  upgradePlanForItems($event: Event, itemType: UsageItemType): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.licenseActions.upgradePlanForItems(this.subscriptionInfo, itemType);
  }

  addAddOn($event: Event, addonType: AddonType) {
    if ($event) {
      $event.stopPropagation();
    }
    this.licenseActions.addAddOn(this.subscriptionInfo, addonType);
  }

  upgradePlanForAddon($event: Event, addonType: AddonType): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.licenseActions.upgradePlanForAddon(this.subscriptionInfo, addonType);
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
