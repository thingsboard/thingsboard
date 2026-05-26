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
import { SolutionTemplateInstalledItemDescriptor } from '@shared/models/iot-hub/iot-hub-installed-item.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { TranslateService } from '@ngx-translate/core';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { resolveEntityDetailsUrl } from './iot-hub-components.models';
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
  | 'installing'
  | 'success'
  | 'error';

@Component({
  selector: 'tb-iot-hub-install-dialog',
  standalone: false,
  templateUrl: './iot-hub-install-dialog.component.html',
  styleUrls: ['./iot-hub-install-dialog.component.scss']
})
export class TbIotHubInstallDialogComponent extends DialogComponent<TbIotHubInstallDialogComponent> {

  ItemType = ItemType;
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
      error: (err) => {
        this.state = 'error';
        this.errorMessage = err?.error?.message || err?.message ||
          this.translate.instant('iot-hub.install-error', { name: this.item.name });
      }
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
    this.state = 'installing';
    const versionId = this.item.id as string;
    const data = this.selectedEntityId ? { entityId: this.selectedEntityId } : undefined;
    this.iotHubApiService.installItemVersion(versionId, { ignoreLoading: true }, data).subscribe({
      next: (result) => {
        if (result.success) {
          if (result.descriptor?.type === 'SOLUTION_TEMPLATE') {
            const timeout = this.item.dataDescriptor?.installTimeoutMs;
            const openSolutionDialog = () => {
              this.dialogRef.close('installed');
              this.dialog.open(SolutionInstallDialogComponent, {
                disableClose: true,
                autoFocus: false,
                panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
                data: { descriptor: result.descriptor as SolutionTemplateInstalledItemDescriptor }
              });
            };
            if (timeout > 0) {
              setTimeout(openSolutionDialog, timeout);
            } else {
              openSolutionDialog();
            }
          } else {
            this.state = 'success';
            this.entityDetailsUrl = resolveEntityDetailsUrl(result.descriptor, this.item.type);
          }
        } else {
          this.state = 'error';
          this.errorMessage = result.errorMessage || this.translate.instant('iot-hub.install-error', { name: this.item.name });
        }
      },
      error: (err) => {
        this.state = 'error';
        this.errorMessage = err?.error?.message || err?.message || this.translate.instant('iot-hub.install-error', { name: this.item.name });
      }
    });
  }

  openEntityDetails(): void {
    if (this.entityDetailsUrl) {
      this.dialogRef.close('installed');
      void this.router.navigateByUrl(this.entityDetailsUrl);
    }
  }

  close(): void {
    this.dialogRef.close(this.state === 'success' ? 'installed' : false);
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

}
