///
/// Copyright © 2016-2022 The Thingsboard Authors
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

import { ChangeDetectorRef, Component, Inject } from '@angular/core';
import { EntityType } from '@shared/models/entity-type.models';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { EntityComponent } from '@home/components/entity/entity.component';
import { QueueInfo, QueueProcessingStrategyTypes, QueueSubmitStrategyTypes } from '@shared/models/queue.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';

@Component({
  selector: 'tb-queue',
  templateUrl: './queue.component.html',
  styleUrls: ['./queue.component.scss']
})
export class QueueComponent extends EntityComponent<QueueInfo> {
  entityForm: FormGroup;

  entityType = EntityType;
  submitStrategies: string[] = [];
  processingStrategies: string[] = [];

  QueueSubmitStrategyTypes = QueueSubmitStrategyTypes;
  hideBatchSize = false;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: QueueInfo,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<QueueInfo>,
              protected cd: ChangeDetectorRef,
              public fb: FormBuilder) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
    this.submitStrategies = Object.values(QueueSubmitStrategyTypes);
    this.processingStrategies = Object.values(QueueProcessingStrategyTypes);
  }

  ngOnInit() {
    super.ngOnInit();
    this.entityForm.get('submitStrategy').get('type').valueChanges.subscribe(() => {
      this.submitStrategyTypeChanged();
    });
  }

  buildForm(entity: QueueInfo): FormGroup {
    return this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required]],
        pollInterval: [
          entity && entity.pollInterval ? entity.pollInterval : 25,
          [Validators.min(1), Validators.required]
        ],
        partitions: [
          entity && entity.partitions ? entity.partitions : 10,
          [Validators.min(1), Validators.required]
        ],
        consumerPerPartition: [entity ? entity.consumerPerPartition : false, []],
        packProcessingTimeout: [
          entity && entity.packProcessingTimeout ? entity.packProcessingTimeout : 2000,
          [Validators.min(1), Validators.required]
        ],
        submitStrategy: this.fb.group({
          type: [entity ? entity.submitStrategy?.type : null, [Validators.required]],
          batchSize: [
            entity && entity.submitStrategy?.batchSize ? entity.submitStrategy?.batchSize : 1000,
            [Validators.min(1), Validators.required]
          ],
        }),
        processingStrategy: this.fb.group({
          type: [entity ? entity.processingStrategy?.type : null, [Validators.required]],
          retries: [
            entity && entity.processingStrategy?.retries ? entity.processingStrategy?.retries : 3,
            [Validators.min(0), Validators.required]
          ],
          failurePercentage: [
            entity && entity.processingStrategy?.failurePercentage ? entity.processingStrategy?.failurePercentage : 0,
            [Validators.min(0), Validators.required, Validators.max(100)]
          ],
          pauseBetweenRetries: [
            entity && entity.processingStrategy?.pauseBetweenRetries ? entity.processingStrategy?.pauseBetweenRetries : 3,
            [Validators.min(1), Validators.required]
          ],
          maxPauseBetweenRetries: [
            entity && entity.processingStrategy?.maxPauseBetweenRetries ? entity.processingStrategy?.maxPauseBetweenRetries : 3,
            [Validators.min(1), Validators.required]
          ],
        })
      }
    );
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  updateForm(entity: QueueInfo) {
    this.entityForm.patchValue({
      name: entity.name,
      pollInterval: entity.pollInterval,
      partitions: entity.partitions,
      consumerPerPartition: entity.consumerPerPartition,
      packProcessingTimeout: entity.packProcessingTimeout,
      submitStrategy: {
        type: entity.submitStrategy?.type,
        batchSize: entity.submitStrategy?.batchSize,
      },
      processingStrategy: {
        type: entity.processingStrategy?.type,
        retries: entity.processingStrategy?.retries,
        failurePercentage: entity.processingStrategy?.failurePercentage,
        pauseBetweenRetries: entity.processingStrategy?.pauseBetweenRetries,
        maxPauseBetweenRetries: entity.processingStrategy?.maxPauseBetweenRetries,
      }
    }, {emitEvent: true});

    if (!this.isAdd) {
      this.entityForm.get('name').disable({emitEvent: false});
    }
  }

  submitStrategyTypeChanged() {
    const form = this.entityForm.get('submitStrategy') as FormGroup;
    const type: QueueSubmitStrategyTypes = form.get('type').value;
    const batchSizeField = form.get('batchSize');
    if (type === QueueSubmitStrategyTypes.BATCH) {
      batchSizeField.enable();
      batchSizeField.patchValue(1000);
      this.hideBatchSize = true;
    } else {
      batchSizeField.patchValue(null);
      batchSizeField.disable();
      this.hideBatchSize = false;
    }
  }
}
