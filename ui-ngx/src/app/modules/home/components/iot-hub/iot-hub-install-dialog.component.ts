///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component, DestroyRef, Inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import { ItemType, itemTypeTranslations } from '@shared/models/iot-hub/iot-hub-item.models';
import {
  getInstalledItemUrl,
  InstallPlan,
  InstallPlanEntry,
  InstallPlanEntryStatus,
  InstallPlanResult,
  IotHubInstalledItemDescriptor,
  SolutionTemplateInstalledItemDescriptor
} from '@shared/models/iot-hub/iot-hub-installed-item.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { TranslateService } from '@ngx-translate/core';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { SolutionInstallDialogComponent } from '@home/components/iot-hub/solution-install-dialog.component';
import { Observable, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { AssetProfileService } from '@core/http/asset-profile.service';
import { RuleChainService } from '@core/http/rule-chain.service';
import { RuleChainId } from '@shared/models/id/rule-chain-id';

interface SelectEntityConfig {
  allowed: EntityType[];
  defaultType: EntityType;
  required: boolean;
  promptKey?: string;
}

interface PendingOverwrite {
  entityId: EntityId;
  profileName: string;
  existingRuleChainName: string;
}

export interface IotHubInstallDialogData {
  item: MpItemVersionView;
}

export type InstallState =
  | 'select-entity'
  | 'confirm-overwrite'
  | 'confirm'
  | 'plan'
  | 'installing'
  | 'success'
  | 'partial'
  | 'error';

@Component({
  selector: 'tb-iot-hub-install-dialog',
  standalone: false,
  templateUrl: './iot-hub-install-dialog.component.html',
  styleUrls: ['./iot-hub-install-dialog.component.scss']
})
export class TbIotHubInstallDialogComponent extends DialogComponent<TbIotHubInstallDialogComponent> {

  ItemType = ItemType;
  PlanStatus = InstallPlanEntryStatus;
  EntityType = EntityType;

  item: MpItemVersionView;
  typeTranslations = itemTypeTranslations;
  state!: InstallState;
  errorMessage = '';
  entityDetailsUrl: string | null = null;

  selectedEntityId: EntityId | null = null;
  pendingOverwrite: PendingOverwrite | null = null;
  ruleChainInstallForm!: FormGroup<{
    setAsDefault: FormControl<boolean>;
    entityType: FormControl<EntityType>;
    entityId: FormControl<string | null>;
  }>;

  installPlan: InstallPlan | null = null;
  planSummary: { willInstall: number; alreadyInstalled: number; missing: number } = {
    willInstall: 0,
    alreadyInstalled: 0,
    missing: 0
  };
  missingEntries: InstallPlanEntry[] = [];
  resolvingPlan = false;

  private readonly selectEntityConfig: Partial<Record<ItemType, SelectEntityConfig>> = {
    [ItemType.CALCULATED_FIELD]: {
      allowed: [EntityType.DEVICE, EntityType.ASSET, EntityType.DEVICE_PROFILE, EntityType.ASSET_PROFILE],
      defaultType: EntityType.DEVICE_PROFILE,
      required: true,
      promptKey: 'iot-hub.select-entity-for-cf',
    },
    [ItemType.ALARM_RULE]: {
      allowed: [EntityType.DEVICE, EntityType.ASSET, EntityType.DEVICE_PROFILE, EntityType.ASSET_PROFILE],
      defaultType: EntityType.DEVICE_PROFILE,
      required: true,
      promptKey: 'iot-hub.select-entity-for-alarm-rule',
    }
  };

  get activeSelectEntityConfig(): SelectEntityConfig | null {
    return this.selectEntityConfig[this.item.type] ?? null;
  }

  constructor(
    protected store: Store<AppState>,
    protected router: Router,
    protected dialogRef: MatDialogRef<TbIotHubInstallDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: IotHubInstallDialogData,
    private dialog: MatDialog,
    private translate: TranslateService,
    private iotHubApiService: IotHubApiService,
    private deviceProfileService: DeviceProfileService,
    private assetProfileService: AssetProfileService,
    private ruleChainService: RuleChainService,
    private fb: FormBuilder,
    private destroyRef: DestroyRef
  ) {
    super(store, router, dialogRef);
    this.item = data.item;
    this.state = this.computeInitialState();
    if (this.item.type === ItemType.RULE_CHAIN) {
      this.initRuleChainForm();
    }
  }

  private computeInitialState(): InstallState {
    return this.activeSelectEntityConfig || this.item.type === ItemType.RULE_CHAIN
      ? 'select-entity'
      : 'confirm';
  }

  private initRuleChainForm(): void {
    this.ruleChainInstallForm = this.fb.group({
      setAsDefault: this.fb.nonNullable.control<boolean>(false),
      entityType: this.fb.nonNullable.control<EntityType>(EntityType.DEVICE_PROFILE),
      entityId: this.fb.control<string | null>(null)
    });
    this.ruleChainInstallForm.controls.setAsDefault.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(setAsDefault => {
        const entityIdCtrl = this.ruleChainInstallForm.controls.entityId;
        if (setAsDefault) {
          entityIdCtrl.setValidators(Validators.required);
        } else {
          entityIdCtrl.clearValidators();
          entityIdCtrl.setValue(null);
        }
        entityIdCtrl.updateValueAndValidity();
      });
    this.ruleChainInstallForm.controls.entityType.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.ruleChainInstallForm.controls.entityId.setValue(null);
      });
  }

  getTypeLabel(): string {
    const key = this.typeTranslations.get(this.item.type);
    return key ? this.translate.instant(key) : '';
  }

  onEntitySelectInstall(): void {
    if (!this.selectedEntityId) {
      return;
    }
    this.install();
  }

  onRuleChainInstall(): void {
    const { setAsDefault, entityType, entityId } = this.ruleChainInstallForm.getRawValue();
    if (!setAsDefault) {
      this.selectedEntityId = null;
      this.install();
      return;
    }
    if (!entityId) {
      return;
    }
    const profileEntityId: EntityId = { entityType, id: entityId };
    this.selectedEntityId = profileEntityId;
    this.resolveOverwrite(profileEntityId).subscribe({
      next: (pending) => {
        if (pending) {
          this.pendingOverwrite = pending;
          this.state = 'confirm-overwrite';
        } else {
          this.pendingOverwrite = null;
          this.install();
        }
      },
      error: (err) => this.handleApiError(err)
    });
  }

  confirmOverwriteReplace(): void {
    this.install();
  }

  confirmOverwriteCancel(): void {
    this.pendingOverwrite = null;
    this.state = 'select-entity';
  }

  private resolveOverwrite(profileEntityId: EntityId): Observable<PendingOverwrite | null> {
    const lookupProfile$: Observable<{ name: string; defaultRuleChainId: RuleChainId | null }> =
      profileEntityId.entityType === EntityType.DEVICE_PROFILE
        ? this.deviceProfileService.getDeviceProfile(profileEntityId.id, { ignoreLoading: true }).pipe(
            map(p => ({ name: p.name, defaultRuleChainId: p.defaultRuleChainId ?? null }))
          )
        : this.assetProfileService.getAssetProfile(profileEntityId.id, { ignoreLoading: true }).pipe(
            map(p => ({ name: p.name, defaultRuleChainId: p.defaultRuleChainId ?? null }))
          );

    return lookupProfile$.pipe(
      switchMap(profile => {
        if (!profile.defaultRuleChainId) {
          return of<PendingOverwrite | null>(null);
        }
        return this.ruleChainService.getRuleChain(profile.defaultRuleChainId.id, { ignoreLoading: true }).pipe(
          map(existing => ({
            entityId: profileEntityId,
            profileName: profile.name,
            existingRuleChainName: existing.name,
          }))
        );
      })
    );
  }

  install(): void {
    // Keep the current state visible (confirm / select-entity / confirm-overwrite) while resolving —
    // a transient 'resolving-plan' state caused a visible "blink" for items without dependencies.
    this.resolvingPlan = true;
    const versionId = this.item.id as string;
    this.iotHubApiService.resolveInstallPlan(versionId, { ignoreLoading: true }).subscribe({
      next: (plan) => {
        this.resolvingPlan = false;
        const summary = this.summarizePlan(plan);
        // Show the plan only when there's something worth telling the user about: extra items to
        // install, items already installed, or missing dependencies. A lone root with nothing to
        // skip installs directly so the dialog doesn't flash a one-line "plan".
        const shouldShowPlan = plan.entries.length > 1
          || summary.alreadyInstalled > 0
          || summary.missing > 0;

        if (!shouldShowPlan) {
          this.installItem();
          return;
        }

        this.installPlan = plan;
        this.planSummary = summary;
        this.missingEntries = plan.entries.filter(e => e.status === InstallPlanEntryStatus.MISSING);
        this.state = 'plan';
      },
      error: (err) => {
        this.resolvingPlan = false;
        this.handleApiError(err);
      }
    });
  }

  installItemWithDependencies(): void {
    if (!this.installPlan) {
      return;
    }
    this.state = 'installing';
    this.iotHubApiService.installPlan(this.installPlan, this.installData(), { ignoreLoading: true }).subscribe({
      next: (result) => this.handlePlanResult(result),
      error: (err) => this.handleApiError(err)
    });
  }

  private installItem(): void {
    this.state = 'installing';
    const versionId = this.item.id as string;
    this.iotHubApiService.installItemVersion(versionId, { ignoreLoading: true }, this.installData()).subscribe({
      next: (result) => {
        if (!result.success) {
          this.state = 'error';
          this.errorMessage = result.errorMessage || this.translate.instant('iot-hub.install-error', { name: this.item.name });
          return;
        }
        this.handleInstalledDescriptor(result.descriptor);
      },
      error: (err) => this.handleApiError(err)
    });
  }

  private summarizePlan(plan: InstallPlan): { willInstall: number; alreadyInstalled: number; missing: number } {
    const summary = { willInstall: 0, alreadyInstalled: 0, missing: 0 };
    for (const entry of plan.entries) {
      switch (entry.status) {
        case InstallPlanEntryStatus.WILL_INSTALL: summary.willInstall++; break;
        case InstallPlanEntryStatus.ALREADY_INSTALLED: summary.alreadyInstalled++; break;
        case InstallPlanEntryStatus.MISSING: summary.missing++; break;
      }
    }
    return summary;
  }

  private handleApiError(err: any): void {
    this.state = 'error';
    this.errorMessage = err?.error?.message || err?.message ||
      this.translate.instant('iot-hub.install-error', { name: this.item.name });
  }

  private handlePlanResult(result: InstallPlanResult): void {
    if (!result.success) {
      this.state = 'error';
      let message = result.errorMessage || this.translate.instant('iot-hub.install-error', { name: this.item.name });
      // A failed cascade rolls back the items installed so far. When something was actually being
      // installed and the rollback came back partial (rolledBack === false), some entities are left
      // behind — tell the admin so they know manual cleanup may be needed. The willInstall guard
      // avoids the misleading warning on the "empty plan" failure, where nothing was installed.
      if (!result.rolledBack && this.planSummary.willInstall > 0) {
        message += ' ' + this.translate.instant('iot-hub.install-rollback-partial');
      }
      this.errorMessage = message;
      return;
    }
    if (this.installPlan) {
      this.installPlan = { ...this.installPlan, entries: result.entries ?? this.installPlan.entries };
      this.missingEntries = (result.entries ?? []).filter(e => e.status === InstallPlanEntryStatus.MISSING);
    }
    const hasMissing = (result.missingItemIds?.length ?? 0) > 0;
    if (result.rootDescriptor) {
      this.handleInstalledDescriptor(result.rootDescriptor, hasMissing);
    } else {
      this.state = hasMissing ? 'partial' : 'success';
    }
  }

  private handleInstalledDescriptor(descriptor: IotHubInstalledItemDescriptor, partial = false): void {
    if (descriptor?.type === 'SOLUTION_TEMPLATE') {
      const timeout = this.item.dataDescriptor?.installTimeoutMs;
      const openSolutionDialog = () => {
        this.dialogRef.close('installed');
        this.dialog.open(SolutionInstallDialogComponent, {
          disableClose: true,
          autoFocus: false,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
          data: { descriptor: descriptor as SolutionTemplateInstalledItemDescriptor }
        });
      };
      if (timeout > 0) {
        setTimeout(openSolutionDialog, timeout);
      } else {
        openSolutionDialog();
      }
      return;
    }
    this.entityDetailsUrl = getInstalledItemUrl(descriptor);
    this.state = partial ? 'partial' : 'success';
  }

  private installData(): any | undefined {
    return this.selectedEntityId ? { entityId: this.selectedEntityId } : undefined;
  }

  cancelPlan(): void {
    this.installPlan = null;
    this.state = this.computeInitialState();
  }

  openEntityDetails(): void {
    if (this.entityDetailsUrl) {
      this.dialogRef.close('installed');
      void this.router.navigateByUrl(this.entityDetailsUrl);
    }
  }

  close(): void {
    const installedStates: InstallState[] = ['success', 'partial'];
    this.dialogRef.close(installedStates.includes(this.state) ? 'installed' : false);
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

}
