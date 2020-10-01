import {Component, Inject, OnInit} from '@angular/core';
import {EntityType} from '../../../../shared/models/entity-type.models';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {EntityComponent} from '../../components/entity/entity.component';
import {QueueInfo, QueueProcessingStrategyTypes, QueueSubmitStrategyTypes} from '../../../../shared/models/queue.models';
import {Store} from '@ngrx/store';
import {AppState} from '../../../../core/core.state';
import {TranslateService} from '@ngx-translate/core';
import {EntityTableConfig} from '../../models/entity/entities-table-config.models';

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

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: QueueInfo,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<QueueInfo>,
              public fb: FormBuilder) {
    super(store, fb, entityValue, entitiesTableConfigValue);
  }

  ngOnInit(): void {
    this.submitStrategies = Object.values(QueueSubmitStrategyTypes);
    this.processingStrategies = Object.values(QueueProcessingStrategyTypes);
  }

  buildForm(entity: QueueInfo): FormGroup {
    return this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required]],
        topic: [entity ? entity.topic : '', [Validators.required]],
        pollInterval: [entity && entity.pollInterval ? entity.pollInterval : 25,
          [Validators.min(1)]],
        partitions: [entity && entity.partitions ? entity.partitions : 10,
          [Validators.min(1)]],
        packProcessingTimeout: [entity && entity.packProcessingTimeout ? entity.packProcessingTimeout : 2000,
          [Validators.min(1)]],
        submitStrategy: this.fb.group({
          type: [entity ? entity.submitStrategy?.type : null, [Validators.required]],
          batchSize: [entity && entity.submitStrategy?.batchSize ? entity.submitStrategy?.batchSize : 1000],
        }),
        processingStrategy: this.fb.group({
          type: [entity ? entity.processingStrategy?.type : null, [Validators.required]],
          retries: [entity && entity.processingStrategy?.retries ? entity.processingStrategy?.retries : 3],
          failurePercentage: [entity && entity.processingStrategy?.failurePercentage ? entity.processingStrategy?.failurePercentage : 0],
          pauseBetweenRetries:
            [entity && entity.processingStrategy?.pauseBetweenRetries ? entity.processingStrategy?.pauseBetweenRetries : 3],
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
    this.entityForm.patchValue({name: entity.name});
    this.entityForm.patchValue({topic: entity.topic});
    this.entityForm.patchValue({pollInterval: entity.pollInterval});
    this.entityForm.patchValue({partitions: entity.partitions});
    this.entityForm.patchValue({packProcessingTimeout: entity.packProcessingTimeout});
    this.entityForm.patchValue({submitStrategy: {
      type: entity.submitStrategy?.type,
      batchSize: entity.submitStrategy?.batchSize,
    }});
    this.entityForm.patchValue({processingStrategy: {
      type: entity.processingStrategy?.type,
      retries: entity.processingStrategy?.retries,
      failurePercentage: entity.processingStrategy?.failurePercentage,
      pauseBetweenRetries: entity.processingStrategy?.pauseBetweenRetries,
    }});
  }

}
