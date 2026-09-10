// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { ChangeDetectorRef, Component, Inject } from '@angular/core';
import { EntityType } from '@shared/models/entity-type.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { EntityComponent } from '@home/components/entity/entity.component';
import { QueueInfo } from '@shared/models/queue.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';

@Component({
    selector: 'tb-queue',
    templateUrl: './queue.component.html',
    styleUrls: [],
    standalone: false
})
export class QueueComponent extends EntityComponent<QueueInfo> {
  entityForm: UntypedFormGroup;

  entityType = EntityType;
  submitStrategies: string[] = [];
  processingStrategies: string[] = [];

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: QueueInfo,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<QueueInfo>,
              protected cd: ChangeDetectorRef,
              public fb: UntypedFormBuilder) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  ngOnInit() {
    super.ngOnInit();
  }

  buildForm(entity: QueueInfo): UntypedFormGroup {
    return this.fb.group({
      queue: [entity]
    });
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
      queue: entity
    }, {emitEvent: false});
  }

  prepareFormValue(formValue: any) {
    return super.prepareFormValue(formValue.queue);
  }

  onQueueIdCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('queue.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }
}
