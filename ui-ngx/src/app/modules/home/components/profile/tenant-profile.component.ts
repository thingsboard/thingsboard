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

import { ChangeDetectorRef, Component, DestroyRef, Inject, Input, Optional } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { createTenantProfileConfiguration, TenantProfile, TenantProfileType } from '@shared/models/tenant.model';
import { ActionNotificationShow } from '@app/core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { EntityComponent } from '../entity/entity.component';
import { guid } from '@core/utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-tenant-profile',
  templateUrl: './tenant-profile.component.html',
  styleUrls: ['./tenant-profile.component.scss']
})
export class TenantProfileComponent extends EntityComponent<TenantProfile> {

  @Input()
  standalone = false;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Optional() @Inject('entity') protected entityValue: TenantProfile,
              @Optional() @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<TenantProfile>,
              protected fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  buildForm(entity: TenantProfile): UntypedFormGroup {
    const mainQueue = [
      {
        id: guid(),
        consumerPerPartition: false,
        name: 'Main',
        packProcessingTimeout: 10000,
        partitions: 1,
        pollInterval: 2000,
        processingStrategy: {
          failurePercentage: 0,
          maxPauseBetweenRetries: 3,
          pauseBetweenRetries: 3,
          retries: 3,
          type: 'SKIP_ALL_FAILURES'
        },
        submitStrategy: {
          batchSize: 1000,
          type: 'BURST'
        },
        topic: 'tb_rule_engine.main',
        additionalInfo: {
          description: '',
          customProperties: '',
          duplicateMsgToAllPartitions: false
        }
      },
      {
        id: guid(),
        name: 'HighPriority',
        topic: 'tb_rule_engine.hp',
        pollInterval: 2000,
        partitions: 1,
        consumerPerPartition: false,
        packProcessingTimeout: 10000,
        submitStrategy: {
          type: 'BURST',
          batchSize: 100
        },
        processingStrategy: {
          type: 'RETRY_FAILED_AND_TIMED_OUT',
          retries: 0,
          failurePercentage: 0,
          pauseBetweenRetries: 5,
          maxPauseBetweenRetries: 5
        },
        additionalInfo: {
          description: '',
          customProperties: '',
          duplicateMsgToAllPartitions: false
        }
      },
      {
        id: guid(),
        name: 'SequentialByOriginator',
        topic: 'tb_rule_engine.sq',
        pollInterval: 2000,
        partitions: 1,
        consumerPerPartition: false,
        packProcessingTimeout: 10000,
        submitStrategy: {
          type: 'SEQUENTIAL_BY_ORIGINATOR',
          batchSize: 100
        },
        processingStrategy: {
          type: 'RETRY_FAILED_AND_TIMED_OUT',
          retries: 3,
          failurePercentage: 0,
          pauseBetweenRetries: 5,
          maxPauseBetweenRetries: 5
        },
        additionalInfo: {
          description: '',
          customProperties: '',
          duplicateMsgToAllPartitions: false
        }
      }
    ];
    const formGroup = this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
        isolatedTbRuleEngine: [entity ? entity.isolatedTbRuleEngine : false, []],
        profileData: this.fb.group({
          configuration: [entity && !this.isAdd ? entity?.profileData.configuration
            : createTenantProfileConfiguration(TenantProfileType.DEFAULT), []],
          queueConfiguration: [entity && !this.isAdd ? entity?.profileData.queueConfiguration : null, []]
        }),
        description: [entity ? entity.description : '', []],
      }
    );
    formGroup.get('isolatedTbRuleEngine').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((value) => {
      if (value) {
        formGroup.get('profileData').patchValue({
            queueConfiguration: mainQueue
          }, {emitEvent: false});
      } else {
        formGroup.get('profileData').patchValue({
            queueConfiguration: null
          }, {emitEvent: false});
      }
    });
    return formGroup;
  }

  updateForm(entity: TenantProfile) {
    this.entityForm.patchValue({name: entity.name}, {emitEvent: false});
    this.entityForm.patchValue({isolatedTbRuleEngine: entity.isolatedTbRuleEngine}, {emitEvent: false});
    this.entityForm.get('profileData').patchValue({
      configuration: !this.isAdd ? entity.profileData?.configuration : createTenantProfileConfiguration(TenantProfileType.DEFAULT)
    }, {emitEvent: false});
    this.entityForm.get('profileData').patchValue({queueConfiguration: entity.profileData?.queueConfiguration}, {emitEvent: false});
    this.entityForm.patchValue({description: entity.description}, {emitEvent: false});
  }

  updateFormState() {
    if (this.entityForm) {
      if (this.isEditValue) {
        this.entityForm.enable({emitEvent: false});
      } else {
        this.entityForm.disable({emitEvent: false});
      }
    }
  }

  onTenantProfileIdCopied(event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('tenant-profile.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

}
